package com.cratt.kidsstreaming;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final List<String> BLOCKED_TERMS = Arrays.asList(
            "porn", "sex", "nsfw", "xxx", "gore", "violence", "gun", "kill", "terror", "drugs"
    );

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new CrattWebViewClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private final class CrattWebViewClient extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            return intercept(request.getUrl());
        }

        @Override
        @SuppressWarnings("deprecation")
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            return intercept(Uri.parse(url));
        }

        private WebResourceResponse intercept(Uri uri) {
            String path = uri.getPath();
            if (path == null) return null;

            try {
                if (path.endsWith("/api/youtube/search")) {
                    return handleSearch(uri.getQueryParameter("q"));
                }
                if (path.endsWith("/api/youtube/resolve")) {
                    return handleResolve(uri.getQueryParameter("v"));
                }
            } catch (Exception error) {
                return jsonResponse(500, errorJson("Failed to process request"));
            }
            return null;
        }
    }

    private WebResourceResponse handleSearch(String input) throws Exception {
        String rawQuery = input == null ? "" : input.trim();
        if (rawQuery.isEmpty()) {
            JSONObject body = new JSONObject();
            body.put("success", true);
            body.put("videos", new JSONArray());
            return jsonResponse(200, body);
        }

        String lowerQuery = rawQuery.toLowerCase(Locale.ROOT);
        if (containsBlockedTerm(lowerQuery)) {
            return jsonResponse(400, errorJson("Search term not allowed in CRATT"));
        }

        String targetQuery = rawQuery + " kids";
        String url = "https://www.youtube.com/results?search_query="
                + URLEncoder.encode(targetQuery, StandardCharsets.UTF_8.name());

        String html = httpGet(url, true);

        Pattern initialDataPattern = Pattern.compile(
                "(?:var\\s+)?ytInitialData\\s*=\\s*(\\{.*?\\});</script>",
                Pattern.DOTALL
        );
        Matcher matcher = initialDataPattern.matcher(html);
        if (!matcher.find()) {
            JSONObject body = new JSONObject();
            body.put("success", true);
            body.put("count", 0);
            body.put("videos", new JSONArray());
            return jsonResponse(200, body);
        }

        JSONObject data = new JSONObject(matcher.group(1));
        JSONArray videos = extractVideos(data);

        JSONObject body = new JSONObject();
        body.put("success", true);
        body.put("count", videos.length());
        body.put("videos", videos);
        return jsonResponse(200, body);
    }

    private JSONArray extractVideos(JSONObject data) {
        JSONArray output = new JSONArray();
        try {
            JSONObject contents = data.optJSONObject("contents");
            JSONObject twoColumn = contents == null ? null : contents.optJSONObject("twoColumnSearchResultsRenderer");
            JSONObject primary = twoColumn == null ? null : twoColumn.optJSONObject("primaryContents");
            JSONObject sectionList = primary == null ? null : primary.optJSONObject("sectionListRenderer");
            JSONArray sections = sectionList == null ? null : sectionList.optJSONArray("contents");
            if (sections == null) return output;

            for (int s = 0; s < sections.length() && output.length() < 30; s++) {
                JSONObject section = sections.optJSONObject(s);
                if (section == null) continue;
                JSONObject itemSection = section.optJSONObject("itemSectionRenderer");
                JSONArray items = itemSection == null ? null : itemSection.optJSONArray("contents");
                if (items == null) continue;

                for (int i = 0; i < items.length() && output.length() < 30; i++) {
                    JSONObject item = items.optJSONObject(i);
                    JSONObject vr = item == null ? null : item.optJSONObject("videoRenderer");
                    if (vr == null) continue;

                    String videoId = vr.optString("videoId", "");
                    String title = runsText(vr.optJSONObject("title"));
                    if (videoId.isEmpty() || title.isEmpty()) continue;
                    if (containsBlockedTerm(title.toLowerCase(Locale.ROOT))) continue;

                    String channelName = "YouTube Channel";
                    JSONObject ownerText = vr.optJSONObject("ownerText");
                    if (ownerText != null) {
                        String candidate = runsText(ownerText);
                        if (!candidate.isEmpty()) channelName = candidate;
                    }

                    String duration = "4:00";
                    JSONObject lengthText = vr.optJSONObject("lengthText");
                    if (lengthText != null) {
                        String simple = lengthText.optString("simpleText", "");
                        if (!simple.isEmpty()) duration = simple;
                    }

                    String thumbnail = "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";
                    JSONObject thumbnailObject = vr.optJSONObject("thumbnail");
                    JSONArray thumbnails = thumbnailObject == null ? null : thumbnailObject.optJSONArray("thumbnails");
                    if (thumbnails != null && thumbnails.length() > 0) {
                        JSONObject last = thumbnails.optJSONObject(thumbnails.length() - 1);
                        if (last != null && !last.optString("url", "").isEmpty()) {
                            thumbnail = last.optString("url");
                        }
                    }

                    String voiceLang = detectLanguage(title + " " + channelName);

                    JSONObject video = new JSONObject();
                    video.put("id", "yt-live-" + videoId);
                    video.put("title", title);
                    video.put("description", "Filtered YouTube video from channel " + channelName + ".");
                    video.put("source", "youtube");
                    video.put("embedId", videoId);
                    video.put("thumbnailUrl", thumbnail);
                    video.put("duration", duration);
                    video.put("ageBracket", "4-6");
                    video.put("language", voiceLang);
                    video.put("voiceLanguage", voiceLang);
                    video.put("audioTrackLabel", voiceLang.equals("pa")
                            ? "Spoken Voice: Punjabi (ਪੰਜਾਬੀ)"
                            : "Spoken Voice: Multi-Language");
                    video.put("isAnimated", true);
                    video.put("videoFormat", "Animation");
                    video.put("animationStyle", "2D Cartoon");
                    video.put("topic", "songs");
                    video.put("channelName", channelName);
                    video.put("learningGoal", "Fun kid-friendly music, animation, and cultural learning");
                    video.put("tags", new JSONArray(Arrays.asList(
                            "youtube", "kids", "safe", voiceLang, "rhymes", "cartoon"
                    )));
                    video.put("characters", new JSONArray(Arrays.asList(channelName)));
                    output.put(video);
                }
            }
        } catch (Exception ignored) {
            // Return any videos parsed before an individual malformed item.
        }
        return output;
    }

    private WebResourceResponse handleResolve(String input) throws Exception {
        String videoIdOrUrl = input == null ? "" : input.trim();
        if (videoIdOrUrl.isEmpty()) {
            return jsonResponse(400, errorJson("Missing video parameter"));
        }

        String videoId = videoIdOrUrl;
        Pattern urlPattern = Pattern.compile(
                "(?:youtu\\.be/|youtube\\.com/(?:watch\\?v=|embed/|shorts/))([\\w-]{11})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher urlMatcher = urlPattern.matcher(videoIdOrUrl);
        if (urlMatcher.find()) videoId = urlMatcher.group(1);

        if (!videoId.matches("^[a-zA-Z0-9_-]{11}$")) {
            return jsonResponse(400, errorJson("Invalid YouTube Video ID format"));
        }

        String title = "YouTube Kids Video (" + videoId + ")";
        String channelName = "YouTube Kids";
        String thumbnail = "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";

        try {
            String watchUrl = "https://www.youtube.com/watch?v=" + videoId;
            String oembedUrl = "https://www.youtube.com/oembed?url="
                    + URLEncoder.encode(watchUrl, StandardCharsets.UTF_8.name())
                    + "&format=json";
            JSONObject oembed = new JSONObject(httpGet(oembedUrl, false));
            title = oembed.optString("title", title);
            channelName = oembed.optString("author_name", channelName);
            thumbnail = oembed.optString("thumbnail_url", thumbnail);
        } catch (Exception ignored) {
            // Keep fallback metadata if oEmbed is unavailable.
        }

        JSONObject video = new JSONObject();
        video.put("id", "yt-custom-" + videoId);
        video.put("title", title);
        video.put("description", "Custom added YouTube video by " + channelName + ".");
        video.put("source", "youtube");
        video.put("embedId", videoId);
        video.put("thumbnailUrl", thumbnail);
        video.put("duration", "4:00");
        video.put("ageBracket", "4-6");
        video.put("language", "en");
        video.put("voiceLanguage", "en");
        video.put("audioTrackLabel", "Kid Safe Audio");
        video.put("isAnimated", true);
        video.put("videoFormat", "Animation");
        video.put("topic", "songs");
        video.put("channelName", channelName);
        video.put("learningGoal", "Curated entertainment & learning");
        video.put("tags", new JSONArray(Arrays.asList("youtube", "custom", "safe")));
        video.put("characters", new JSONArray(Arrays.asList(channelName)));

        JSONObject body = new JSONObject();
        body.put("success", true);
        body.put("video", video);
        return jsonResponse(200, body);
    }

    private String runsText(JSONObject object) {
        if (object == null) return "";
        String simple = object.optString("simpleText", "");
        if (!simple.isEmpty()) return simple;

        JSONArray runs = object.optJSONArray("runs");
        if (runs == null) return "";

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < runs.length(); i++) {
            JSONObject run = runs.optJSONObject(i);
            if (run != null) text.append(run.optString("text", ""));
        }
        return text.toString();
    }

    private boolean containsBlockedTerm(String lowerText) {
        for (String term : BLOCKED_TERMS) {
            if (lowerText.contains(term)) return true;
        }
        return false;
    }

    private String detectLanguage(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        if (Pattern.compile("[\\u0A00-\\u0A7F]").matcher(value).find()
                || lower.contains("punjabi") || lower.contains("panjabi")) return "pa";
        if (Pattern.compile("[\\u0A80-\\u0AFF]").matcher(value).find()
                || lower.contains("gujarati")) return "gu";
        if (Pattern.compile("[\\u0900-\\u097F]").matcher(value).find()
                || lower.contains("hindi") || lower.contains("balgeet") || lower.contains("kahani")) return "hi";
        if (lower.contains("polski") || lower.contains("piosenki") || lower.contains("dla dzieci")) return "pl";
        if (lower.contains("nederlands") || lower.contains("kinderliedjes")) return "nl";
        return "en";
    }

    private String httpGet(String address, boolean youtubeHeaders) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setRequestMethod("GET");
        if (youtubeHeaders) {
            connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
            );
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9,pa;q=0.8,hi;q=0.7");
        }

        int status = connection.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                status >= 200 && status < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream(),
                StandardCharsets.UTF_8
        ));

        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) result.append(line).append('\n');
        reader.close();
        connection.disconnect();

        if (status < 200 || status >= 300) {
            throw new IllegalStateException("HTTP " + status);
        }
        return result.toString();
    }

    private JSONObject errorJson(String message) {
        JSONObject body = new JSONObject();
        try {
            body.put("error", message);
        } catch (Exception ignored) {
        }
        return body;
    }

    private WebResourceResponse jsonResponse(int status, JSONObject body) {
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        return new WebResourceResponse(
                "application/json",
                "UTF-8",
                status,
                status >= 200 && status < 300 ? "OK" : "Error",
                java.util.Collections.singletonMap("Access-Control-Allow-Origin", "*"),
                new ByteArrayInputStream(bytes)
        );
    }
}
