/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from 'react';
import { 
  Wifi, 
  Battery, 
  BatteryCharging, 
  Volume2, 
  VolumeX, 
  Flashlight, 
  Lock, 
  Smartphone, 
  Monitor, 
  Sparkles, 
  Heart, 
  ShieldCheck,
  Radio,
  FolderArchive,
  Settings
} from 'lucide-react';
import { PersonalityMode } from '../types/myra';

interface AndroidFrameProps {
  children: React.ReactNode;
  personality: PersonalityMode;
  onPersonalityChange: (mode: PersonalityMode) => void;
  torchOn: boolean;
  onToggleTorch: () => void;
  onLockScreen: () => void;
  isMuted: boolean;
  onToggleMute: () => void;
  batteryLevel?: number;
  isCharging?: boolean;
  onOpenDownloadModal?: () => void;
  onOpenSettings?: () => void;
}

export const AndroidFrame: React.FC<AndroidFrameProps> = ({
  children,
  personality,
  onPersonalityChange,
  torchOn,
  onToggleTorch,
  onLockScreen,
  isMuted,
  onToggleMute,
  batteryLevel = 84,
  isCharging = true,
  onOpenDownloadModal,
  onOpenSettings,
}) => {
  const [timeStr, setTimeStr] = useState('09:41');
  const [deviceFrameMode, setDeviceFrameMode] = useState<boolean>(true);

  useEffect(() => {
    const updateClock = () => {
      const d = new Date();
      const hours = d.getHours().toString().padStart(2, '0');
      const mins = d.getMinutes().toString().padStart(2, '0');
      setTimeStr(`${hours}:${mins}`);
    };
    updateClock();
    const interval = setInterval(updateClock, 10000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="min-h-screen bg-[#060911] text-slate-100 flex flex-col items-center justify-start p-0 sm:py-6 sm:px-4 font-['Plus_Jakarta_Sans',sans-serif]">
      {/* Top Desktop Controls Bar */}
      <header className="w-full max-w-5xl mx-auto flex items-center justify-between px-4 py-2 mb-2 text-xs text-slate-400">
        <div className="flex items-center gap-2">
          <span className="font-bold text-base tracking-tight text-white flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-cyan-400 shadow-[0_0_8px_rgba(6,182,212,0.8)] animate-pulse"></span>
            MYRA
          </span>
          <span className="bg-[#090d16] text-cyan-400 border border-cyan-500/30 px-2 py-0.5 rounded font-mono font-semibold text-[10px]">
            2026 EDITION
          </span>
          <span className="hidden sm:inline text-slate-500">• Hindi/Hinglish Android AI Assistant</span>
        </div>

        <div className="flex items-center gap-2">
          {/* Settings Quick Pill */}
          {onOpenSettings && (
            <button
              id="top-bar-settings-btn"
              onClick={onOpenSettings}
              className="px-2.5 py-1 rounded-full text-xs font-semibold bg-cyan-950/60 hover:bg-cyan-900/60 text-cyan-300 border border-cyan-500/40 flex items-center gap-1.5 transition-all shadow-sm cursor-pointer"
              title="Open Settings Dashboard"
            >
              <Settings className="w-3.5 h-3.5 text-cyan-400" />
              <span>Settings</span>
            </button>
          )}

          {/* Download Project ZIP Quick Pill */}
          <button
            onClick={onOpenDownloadModal}
            className="px-2.5 py-1 rounded-full text-xs font-semibold bg-[#0D1424] hover:bg-slate-800 text-slate-300 border border-slate-700 flex items-center gap-1.5 transition-all shadow-sm cursor-pointer"
            title="Download MYRA_ANDROID_FINAL_PROJECT.zip"
          >
            <FolderArchive className="w-3.5 h-3.5 text-cyan-400" />
            <span className="font-mono text-[11px] hidden sm:inline">MYRA_ANDROID_FINAL_PROJECT.zip</span>
            <span className="font-mono text-[11px] sm:hidden">FINAL ZIP</span>
          </button>

          {/* Personality Mode Quick Pill */}
          <button
            id="toggle-personality-mode"
            onClick={() => {
              if (personality === 'friendly_assistant') onPersonalityChange('romantic_gf');
              else if (personality === 'romantic_gf') onPersonalityChange('professional');
              else onPersonalityChange('friendly_assistant');
            }}
            className={`px-2.5 py-1 rounded-full text-xs font-medium border flex items-center gap-1.5 transition-all cursor-pointer ${
              personality === 'romantic_gf'
                ? 'bg-rose-950/60 border-rose-500/50 text-rose-300 hover:bg-rose-900/60'
                : personality === 'professional'
                ? 'bg-emerald-950/60 border-emerald-500/50 text-emerald-300 hover:bg-emerald-900/60'
                : 'bg-cyan-950/60 border-cyan-500/40 text-cyan-300 hover:bg-cyan-900/60'
            }`}
            title="Toggle Voice Persona"
          >
            {personality === 'romantic_gf' ? (
              <>
                <Heart className="w-3 h-3 text-rose-400 fill-rose-400" />
                <span>GF Mode (Romantic)</span>
              </>
            ) : personality === 'professional' ? (
              <>
                <ShieldCheck className="w-3 h-3 text-emerald-400" />
                <span>Pro Executive</span>
              </>
            ) : (
              <>
                <Sparkles className="w-3 h-3 text-cyan-400" />
                <span>Friendly Voice</span>
              </>
            )}
          </button>

          {/* Device Frame View Toggle */}
          <button
            id="toggle-device-frame"
            onClick={() => setDeviceFrameMode(!deviceFrameMode)}
            className="p-1.5 rounded-lg bg-[#090d16] hover:bg-slate-800 text-slate-300 border border-cyan-500/30 transition-colors cursor-pointer"
            title={deviceFrameMode ? 'Switch to Fullscreen View' : 'Switch to Android Phone View'}
          >
            {deviceFrameMode ? <Monitor className="w-4 h-4 text-cyan-400" /> : <Smartphone className="w-4 h-4 text-cyan-400" />}
          </button>
        </div>
      </header>

      {/* Screen Torch Simulation Overlay */}
      {torchOn && (
        <div className="fixed inset-0 z-50 pointer-events-none bg-white/90 backdrop-blur-sm animate-pulse flex items-center justify-center">
          <div className="bg-[#090d16] text-slate-100 px-4 py-2 rounded-full font-bold text-sm shadow-2xl border border-white/20 flex items-center gap-2">
            <Flashlight className="w-4 h-4 text-amber-400 fill-amber-400" />
            Torch is ON (Tap flashlight to turn off)
          </div>
        </div>
      )}

      {/* Main Container: Android Handset Shell OR Responsive View */}
      <div
        className={`w-full transition-all duration-300 ${
          deviceFrameMode
            ? 'max-w-[420px] rounded-[48px] border-[10px] border-[#131b2e] shadow-[0_25px_60px_-15px_rgba(0,0,0,0.9),0_0_40px_rgba(6,182,212,0.15)] overflow-hidden bg-[#090d16]'
            : 'max-w-4xl rounded-2xl border border-cyan-500/30 bg-[#090d16] shadow-2xl'
        }`}
      >
        {/* Android Status Bar */}
        <div className="w-full bg-[#060911] px-5 py-2.5 flex items-center justify-between text-xs text-slate-300 select-none border-b border-cyan-500/20">
          <div className="flex items-center gap-2 font-semibold tracking-wide">
            <span>{timeStr}</span>
            <span className="w-1.5 h-1.5 rounded-full bg-cyan-400 shadow-[0_0_6px_rgba(6,182,212,0.8)]"></span>
          </div>

          {/* Camera Punch-Hole Simulation */}
          {deviceFrameMode && (
            <div className="w-3.5 h-3.5 rounded-full bg-black border border-slate-700/60 shadow-inner flex items-center justify-center">
              <span className="w-1 h-1 rounded-full bg-cyan-900/60"></span>
            </div>
          )}

          <div className="flex items-center gap-2">
            <button
              onClick={onToggleTorch}
              className={`p-0.5 rounded transition-colors ${torchOn ? 'text-amber-400' : 'text-slate-400 hover:text-white'}`}
              title="Flashlight"
            >
              <Flashlight className="w-3.5 h-3.5" />
            </button>
            <button
              onClick={onToggleMute}
              className="p-0.5 rounded text-slate-400 hover:text-white transition-colors"
              title={isMuted ? 'Unmute' : 'Mute'}
            >
              {isMuted ? <VolumeX className="w-3.5 h-3.5 text-rose-400" /> : <Volume2 className="w-3.5 h-3.5" />}
            </button>
            <button
              onClick={onLockScreen}
              className="p-0.5 rounded text-slate-400 hover:text-white transition-colors"
              title="Lock Screen"
            >
              <Lock className="w-3.5 h-3.5" />
            </button>
            <Radio className="w-3.5 h-3.5 text-cyan-400" />
            <Wifi className="w-3.5 h-3.5 text-slate-300" />
            <div className="flex items-center gap-1">
              <span className="text-[11px] font-mono">{batteryLevel}%</span>
              {isCharging ? (
                <BatteryCharging className="w-3.5 h-3.5 text-emerald-400" />
              ) : (
                <Battery className="w-3.5 h-3.5 text-slate-300" />
              )}
            </div>
          </div>
        </div>

        {/* MYRA Preview Mode Notification Pill */}
        <div className="w-full bg-[#060911]/90 border-b border-cyan-500/20 px-3 py-1 flex items-center justify-between text-[11px] text-cyan-300">
          <div className="flex items-center gap-1.5 font-mono">
            <span className="w-2 h-2 rounded-full bg-cyan-400 shadow-[0_0_6px_rgba(6,182,212,0.8)] animate-pulse" />
            <span className="font-semibold text-white">MYRA Preview Mode</span>
            <span className="text-cyan-400/80 hidden sm:inline">• Web Browser Simulation</span>
          </div>
          <span className="text-[10px] text-slate-400">
            Native hardware actions (APK) फोन पर उपलब्ध
          </span>
        </div>

        {/* Dynamic Children Content */}
        <div className="w-full flex-1 flex flex-col h-[640px] sm:h-[700px] max-h-[82vh] overflow-hidden relative">
          {children}
        </div>

        {/* Android Gesture Pill Navigation Bar */}
        <div className="w-full bg-[#070b13] py-2 flex items-center justify-center select-none border-t border-slate-800/40">
          <div className="w-32 h-1 bg-slate-600 rounded-full hover:bg-slate-400 cursor-pointer transition-colors" />
        </div>
      </div>
    </div>
  );
};
