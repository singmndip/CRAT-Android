export type VideoSource = 'youtube' | 'vimeo' | 'dailymotion';

export type AgeBracket = '0-3' | '4-6' | '7-9' | '10-12';

export interface AgeCategoryInfo {
  id: AgeBracket;
  label: string;
  tagline: string;
  minAge: number;
  maxAge: number;
  color: string;
  bgColor: string;
}

export type LanguageCode =
  | 'en'
  | 'es'
  | 'fr'
  | 'de'
  | 'zh'
  | 'hi'
  | 'ja'
  | 'pt'
  | 'ar'
  | 'pa'
  | 'gu'
  | 'pl'
  | 'nl';

export interface LanguageInfo {
  code: LanguageCode;
  name: string;
  nativeName: string;
  flag: string;
}

export type ContentTopic =
  | 'all'
  | 'songs'
  | 'science'
  | 'stories'
  | 'crafts'
  | 'animals'
  | 'bedtime';

export interface VideoItem {
  id: string;
  title: string;
  description: string;
  source: VideoSource;
  embedId: string;
  thumbnailUrl: string;
  duration: string;
  ageBracket: AgeBracket;
  language: LanguageCode;
  voiceLanguage: LanguageCode | 'universal';
  audioTrackLabel: string;
  isAnimated: boolean;
  videoFormat?: 'Animation' | 'Live Action' | 'Nature & Animals' | 'Hands-on Science' | 'Puppets & Fun' | 'Music & Dance';
  animationStyle?: '3D CGI' | '2D Cartoon' | 'Stop Motion' | 'Claymation' | 'Storybook' | 'Papercraft';
  topic: ContentTopic;
  channelName: string;
  learningGoal?: string;
  tags: string[];
  characters?: string[];
}

export interface KidProfile {
  id: string;
  name: string;
  avatar: string;
  ageBracket: AgeBracket;
  preferredLanguage: LanguageCode;
  dailyTimeLimitMinutes: number;
  restrictToAssignedAge: boolean;
  allowSearch: boolean;
  favorites: string[];
  watchHistory: {
    videoId: string;
    watchedAt: string;
  }[];
}

export interface ParentalSettings {
  pin: string;
  mathChallengeEnabled: boolean;
  bedtimeBlackoutEnabled: boolean;
  bedtimeStart: string;
  bedtimeEnd: string;
  blockedVideoIds: string[];
  blockedChannelNames: string[];
  requirePinForProfileSwitch: boolean;
}

export interface ScreenTimeState {
  date: string;
  minutesUsed: Record<string, number>;
}

export type SubscriptionTier = 'free' | 'plus' | 'family';

export interface UserSubscription {
  tier: SubscriptionTier;
  isActive: boolean;
  renewalDate: string;
  offlineDownloadsCount: number;
}

export interface CreatorTip {
  id: string;
  creatorChannel: string;
  amount: number;
  currency: string;
  createdAt: string;
}
