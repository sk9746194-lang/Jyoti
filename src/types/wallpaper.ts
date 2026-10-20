/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

export type WallpaperQuality = 'low' | 'medium' | 'high';
export type WallpaperSpeed = 'slow' | 'normal' | 'fast';
export type WallpaperTheme = 'blue' | 'red' | 'blue_red';

export interface WallpaperSettings {
  enabled: boolean;
  quality: WallpaperQuality;
  speed: WallpaperSpeed;
  theme: WallpaperTheme;
  showAppIcons: boolean;
  showOrb: boolean;
  showEarth: boolean;
  particleEffects: boolean;
  batterySaver: boolean;
}

export const DEFAULT_WALLPAPER_SETTINGS: WallpaperSettings = {
  enabled: true,
  quality: 'high',
  speed: 'normal',
  theme: 'blue_red',
  showAppIcons: true,
  showOrb: true,
  showEarth: true,
  particleEffects: true,
  batterySaver: false,
};
