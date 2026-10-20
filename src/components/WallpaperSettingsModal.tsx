/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from 'react';
import {
  X,
  Sparkles,
  Zap,
  CheckCircle2,
  Sliders,
  Battery,
  Layers,
  Globe,
  Radio,
  Eye,
  Smartphone,
  Check,
  ExternalLink
} from 'lucide-react';
import {
  DEFAULT_WALLPAPER_SETTINGS,
  WallpaperQuality,
  WallpaperSettings,
  WallpaperSpeed,
  WallpaperTheme,
} from '../types/wallpaper';
import { LiveWallpaperPreview } from './LiveWallpaperPreview';

interface WallpaperSettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  onApplyAssistantTrigger?: () => void;
}

export const WallpaperSettingsModal: React.FC<WallpaperSettingsModalProps> = ({
  isOpen,
  onClose,
  onApplyAssistantTrigger,
}) => {
  const [settings, setSettings] = useState<WallpaperSettings>(() => {
    try {
      const saved = localStorage.getItem('myra_live_wallpaper_settings');
      return saved ? JSON.parse(saved) : DEFAULT_WALLPAPER_SETTINGS;
    } catch {
      return DEFAULT_WALLPAPER_SETTINGS;
    }
  });

  const [activeTab, setActiveTab] = useState<'preview' | 'controls'>('preview');
  const [appliedNotice, setAppliedNotice] = useState<boolean>(false);

  if (!isOpen) return null;

  const updateSetting = <K extends keyof WallpaperSettings>(key: K, value: WallpaperSettings[K]) => {
    setSettings((prev) => {
      const next = { ...prev, [key]: value };
      try {
        localStorage.setItem('myra_live_wallpaper_settings', JSON.stringify(next));
      } catch {}
      return next;
    });
  };

  const handleSetWallpaper = () => {
    setAppliedNotice(true);
    setTimeout(() => setAppliedNotice(false), 3500);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center myra-modal-overlay p-2 sm:p-4 animate-in fade-in duration-200">
      <div className="relative w-full max-w-4xl max-h-[94vh] bg-[#090d16] border border-cyan-500/35 rounded-2xl shadow-[0_0_50px_rgba(6,182,212,0.25)] flex flex-col overflow-hidden text-slate-100">
        {/* Header */}
        <div className="flex items-center justify-between px-4 py-3.5 border-b border-slate-800 bg-[#0d1424]">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-blue-600 to-fuchsia-600 flex items-center justify-center shadow-[0_0_12px_rgba(59,130,246,0.5)]">
              <Sparkles className="w-4 h-4 text-white" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-100 tracking-wide flex items-center gap-2">
                MYRA Live Wallpaper
                <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-blue-500/20 text-blue-400 border border-blue-500/40">
                  9:16 Futuristic AI
                </span>
              </h2>
              <p className="text-[11px] text-slate-400">
                Holographic Earth • HUD Rings • Neon AI Core • Radial App Galaxy
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Body: Split Preview on desktop, tabs on mobile */}
        <div className="flex-1 flex flex-col md:flex-row overflow-hidden">
          {/* Left: 9:16 Canvas Live Wallpaper */}
          <div className="flex-1 bg-[#050811] flex flex-col items-center justify-center p-4 border-b md:border-b-0 md:border-r border-slate-800 relative">
            <div className="w-full max-w-[260px] sm:max-w-[280px]">
              <div className="text-center mb-2">
                <span className="text-[10px] uppercase tracking-wider text-cyan-400 font-semibold">
                  Tap Wallpaper to interact • 60 FPS Engine
                </span>
              </div>
              <LiveWallpaperPreview
                settings={settings}
                className="w-full mx-auto ring-1 ring-blue-500/30"
                onTap={() => {
                  if (onApplyAssistantTrigger) {
                    onApplyAssistantTrigger();
                  }
                }}
              />
            </div>

            {/* Set Wallpaper Quick Action */}
            <div className="mt-3 w-full max-w-[280px] flex flex-col items-center gap-1.5">
              <button
                onClick={handleSetWallpaper}
                className="w-full py-2.5 px-4 bg-gradient-to-r from-blue-600 via-indigo-600 to-fuchsia-600 hover:brightness-110 active:scale-[0.98] rounded-xl text-white font-bold text-xs shadow-[0_0_20px_rgba(59,130,246,0.4)] flex items-center justify-center gap-2 transition"
              >
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                Set as Live Wallpaper
              </button>

              {appliedNotice && (
                <div className="text-[11px] text-emerald-400 font-medium animate-in fade-in flex items-center gap-1.5">
                  <Check className="w-3.5 h-3.5" />
                  Live Wallpaper configuration active!
                </div>
              )}
            </div>
          </div>

          {/* Right: Controls & Settings */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4 text-xs bg-[#090d16]">
            {/* Master Toggle */}
            <div className="flex items-center justify-between p-3 rounded-xl bg-slate-900/80 border border-slate-800">
              <div>
                <span className="font-bold text-slate-100 block">Live Wallpaper Engine</span>
                <span className="text-[11px] text-slate-400">
                  Enable high-performance background rendering
                </span>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={settings.enabled}
                  onChange={(e) => updateSetting('enabled', e.target.checked)}
                  className="sr-only peer"
                />
                <div className="w-10 h-5 bg-slate-700 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-blue-600"></div>
              </label>
            </div>

            {/* Neon Theme */}
            <div className="space-y-2">
              <label className="text-[11px] font-bold text-blue-400 uppercase tracking-wider block">
                Neon Theme
              </label>
              <div className="grid grid-cols-3 gap-2">
                {[
                  { id: 'blue', label: 'Electric Blue', colors: 'from-blue-500 to-cyan-400' },
                  { id: 'red', label: 'Cyber Red', colors: 'from-rose-500 to-red-600' },
                  { id: 'blue_red', label: 'Dual Blue + Red', colors: 'from-blue-500 via-purple-500 to-rose-500' },
                ].map((th) => (
                  <button
                    key={th.id}
                    onClick={() => updateSetting('theme', th.id as WallpaperTheme)}
                    className={`py-2 px-2 rounded-xl text-center border transition flex flex-col items-center gap-1.5 ${
                      settings.theme === th.id
                        ? 'border-blue-400 bg-blue-600/20 text-white font-bold shadow-[0_0_12px_rgba(59,130,246,0.3)]'
                        : 'border-slate-800 bg-slate-900/60 text-slate-400 hover:text-slate-200'
                    }`}
                  >
                    <div className={`w-4 h-4 rounded-full bg-gradient-to-r ${th.colors}`} />
                    <span className="text-[10px]">{th.label}</span>
                  </button>
                ))}
              </div>
            </div>

            {/* Quality & Speed */}
            <div className="grid grid-cols-2 gap-3">
              {/* Quality */}
              <div className="space-y-2">
                <label className="text-[11px] font-bold text-blue-400 uppercase tracking-wider block">
                  Quality / FPS
                </label>
                <div className="flex flex-col gap-1.5">
                  {(['low', 'medium', 'high'] as WallpaperQuality[]).map((q) => (
                    <button
                      key={q}
                      onClick={() => updateSetting('quality', q)}
                      className={`py-1.5 px-2.5 rounded-lg border text-left transition capitalize ${
                        settings.quality === q
                          ? 'border-blue-400 bg-blue-600/20 text-blue-300 font-bold'
                          : 'border-slate-800 bg-slate-900/50 text-slate-400 hover:text-slate-200'
                      }`}
                    >
                      {q === 'low' ? 'Low (30 FPS)' : q === 'medium' ? 'Medium (45 FPS)' : 'High (60 FPS)'}
                    </button>
                  ))}
                </div>
              </div>

              {/* Speed */}
              <div className="space-y-2">
                <label className="text-[11px] font-bold text-blue-400 uppercase tracking-wider block">
                  Animation Speed
                </label>
                <div className="flex flex-col gap-1.5">
                  {(['slow', 'normal', 'fast'] as WallpaperSpeed[]).map((s) => (
                    <button
                      key={s}
                      onClick={() => updateSetting('speed', s)}
                      className={`py-1.5 px-2.5 rounded-lg border text-left transition capitalize ${
                        settings.speed === s
                          ? 'border-cyan-400 bg-cyan-600/20 text-cyan-300 font-bold'
                          : 'border-slate-800 bg-slate-900/50 text-slate-400 hover:text-slate-200'
                      }`}
                    >
                      {s === 'slow' ? 'Slow (0.6x)' : s === 'normal' ? 'Normal (1.0x)' : 'Fast (1.5x)'}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            {/* Visual Components Toggles */}
            <div className="space-y-2">
              <label className="text-[11px] font-bold text-blue-400 uppercase tracking-wider block">
                Visual Scenery Layers
              </label>

              <div className="space-y-1.5 rounded-xl bg-slate-900/60 border border-slate-800 p-2.5">
                {[
                  {
                    key: 'showEarth' as const,
                    label: 'Show Holographic Earth',
                    sub: 'Rotating 3D globe with atmosphere halo',
                    icon: Globe,
                  },
                  {
                    key: 'showOrb' as const,
                    label: 'Show MYRA AI Orb',
                    sub: 'Breathing core with revolving cybernetic rings',
                    icon: Radio,
                  },
                  {
                    key: 'showAppIcons' as const,
                    label: 'Show Radial App Icons',
                    sub: 'Chrome, WhatsApp, YouTube, Camera galaxy',
                    icon: Layers,
                  },
                  {
                    key: 'particleEffects' as const,
                    label: 'Cosmic Particle Effects',
                    sub: 'Upward floating sparks and moving stars',
                    icon: Sparkles,
                  },
                  {
                    key: 'batterySaver' as const,
                    label: 'Battery Saver Mode',
                    sub: 'Locks at 30 FPS, stops completely on screen off',
                    icon: Battery,
                  },
                ].map((item) => {
                  const Icon = item.icon;
                  return (
                    <div
                      key={item.key}
                      className="flex items-center justify-between py-1.5 px-1 border-b border-slate-800/60 last:border-b-0"
                    >
                      <div className="flex items-center gap-2">
                        <Icon className="w-3.5 h-3.5 text-blue-400" />
                        <div>
                          <span className="font-semibold text-slate-200 block text-[11px]">
                            {item.label}
                          </span>
                          <span className="text-[9px] text-slate-400">{item.sub}</span>
                        </div>
                      </div>

                      <input
                        type="checkbox"
                        checked={settings[item.key]}
                        onChange={(e) => updateSetting(item.key, e.target.checked)}
                        className="rounded border-slate-700 text-blue-600 focus:ring-blue-500 w-3.5 h-3.5 cursor-pointer"
                      />
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Android System Live Wallpaper Info */}
            <div className="p-3 rounded-xl bg-blue-950/30 border border-blue-900/50 text-[11px] text-slate-300">
              <span className="font-bold text-blue-400 block mb-1">
                Android WallpaperService Integration
              </span>
              MYRA Live Wallpaper is packaged with native Android{' '}
              <code className="text-cyan-300">MyraLiveWallpaperService.kt</code> and{' '}
              <code className="text-cyan-300">MyraWallpaperEngine.kt</code> in the APK. You can apply it directly to your Home Screen and Lock Screen through Android's system Live Wallpaper picker!
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
