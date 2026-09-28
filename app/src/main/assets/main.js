const app = document.querySelector('#app');

app.innerHTML = `
  <main class="shell">
    <header class="hero">
      <div class="badge">★ CRAT</div>
      <h1>Happy videos for curious kids</h1>
      <p>Search YouTube through a simple filtered mode, then watch inside CRAT.</p>
    </header>

    <section class="panel search-panel">
      <form id="searchForm" class="search-row">
        <input id="searchInput" autocomplete="off" placeholder="Try: Punjabi rhymes, space for kids, animal songs…" />
        <button type="submit">Search</button>
      </form>
      <div class="chips" aria-label="Quick searches">
        <button class="chip" data-query="nursery rhymes">Nursery rhymes</button>
        <button class="chip" data-query="punjabi kids songs">Punjabi songs</button>
        <button class="chip" data-query="space for kids">Space</button>
        <button class="chip" data-query="animals for kids">Animals</button>
      </div>
      <p id="status" class="status">Search for something fun.</p>
    </section>

    <section id="playerPanel" class="panel player-panel hidden">
      <div class="player-wrap">
        <iframe
          id="player"
          title="CRAT video player"
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
          allowfullscreen
          referrerpolicy="strict-origin-when-cross-origin"
        ></iframe>
      </div>
      <div class="player-copy">
        <span id="nowPlayingChannel" class="eyebrow"></span>
        <h2 id="nowPlayingTitle"></h2>
        <p id="nowPlayingMeta"></p>
      </div>
    </section>

    <section class="section-head">
      <div>
        <span class="eyebrow">Discover</span>
        <h2>Videos</h2>
      </div>
    </section>
    <section id="results" class="grid"></section>

    <section class="panel add-panel">
      <div>
        <span class="eyebrow">Parent shortcut</span>
        <h2>Add a YouTube video</h2>
        <p>Paste an 11-character video ID or a normal YouTube link.</p>
      </div>
      <form id="resolveForm" class="resolve-row">
        <input id="resolveInput" autocomplete="off" placeholder="https://youtu.be/…" />
        <button type="submit">Add video</button>
      </form>
      <p id="resolveStatus" class="status"></p>
    </section>

    <footer>
      <p>CRAT uses a basic keyword/title filter. A parent or guardian should still supervise viewing.</p>
    </footer>
  </main>
`;

const searchForm = document.querySelector('#searchForm');
const searchInput = document.querySelector('#searchInput');
const resolveForm = document.querySelector('#resolveForm');
const resolveInput = document.querySelector('#resolveInput');
const results = document.querySelector('#results');
const status = document.querySelector('#status');
const resolveStatus = document.querySelector('#resolveStatus');
const playerPanel = document.querySelector('#playerPanel');
const player = document.querySelector('#player');
const nowPlayingTitle = document.querySelector('#nowPlayingTitle');
const nowPlayingChannel = document.querySelector('#nowPlayingChannel');
const nowPlayingMeta = document.querySelector('#nowPlayingMeta');

function escapeHtml(value) {
  return String(value).replace(/[&<>'"]/g, (char) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
  })[char]);
}

function playVideo(video) {
  player.src = `https://www.youtube-nocookie.com/embed/${encodeURIComponent(video.embedId)}?autoplay=1&rel=0`;
  nowPlayingTitle.textContent = video.title;
  nowPlayingChannel.textContent = video.channelName;
  nowPlayingMeta.textContent = `${video.duration} • ${video.audioTrackLabel}`;
  playerPanel.classList.remove('hidden');
  playerPanel.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function renderVideos(videos) {
  results.innerHTML = '';
  if (!videos.length) {
    results.innerHTML = '<div class="empty">No videos found. Try another search.</div>';
    return;
  }
  for (const video of videos) {
    const card = document.createElement('article');
    card.className = 'card';
    card.innerHTML = `
      <button class="thumb-button" aria-label="Play ${escapeHtml(video.title)}">
        <img src="${escapeHtml(video.thumbnailUrl)}" alt="" loading="lazy" />
        <span class="play">▶</span>
        <span class="duration">${escapeHtml(video.duration)}</span>
      </button>
      <div class="card-body">
        <span class="eyebrow">${escapeHtml(video.channelName)}</span>
        <h3>${escapeHtml(video.title)}</h3>
        <p>${escapeHtml(video.description)}</p>
        <button class="watch-button">Watch now</button>
      </div>`;
    card.querySelectorAll('button').forEach((button) => button.addEventListener('click', () => playVideo(video)));
    results.appendChild(card);
  }
}

async function search(query) {
  status.textContent = `Searching for “${query}”…`;
  results.innerHTML = '<div class="loading">Finding videos…</div>';
  try {
    const response = await fetch(`/api/youtube/search?q=${encodeURIComponent(query)}`);
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Search failed');
    renderVideos(data.videos || []);
    status.textContent = data.count
      ? `${data.count} filtered result${data.count === 1 ? '' : 's'} found.`
      : 'No filtered results found.';
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Search failed';
    status.textContent = message;
    results.innerHTML = `<div class="empty">${escapeHtml(message)}</div>`;
  }
}

searchForm.addEventListener('submit', (event) => {
  event.preventDefault();
  const query = searchInput.value.trim();
  if (query) search(query);
});

document.querySelectorAll('.chip').forEach((chip) => chip.addEventListener('click', () => {
  const query = chip.dataset.query || '';
  searchInput.value = query;
  search(query);
}));

resolveForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const value = resolveInput.value.trim();
  if (!value) return;
  resolveStatus.textContent = 'Checking video…';
  try {
    const response = await fetch(`/api/youtube/resolve?v=${encodeURIComponent(value)}`);
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Could not add video');
    resolveStatus.textContent = 'Video added.';
    playVideo(data.video);
  } catch (error) {
    resolveStatus.textContent = error instanceof Error ? error.message : 'Could not add video';
  }
});
