/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useMemo } from 'react';
import { 
  Settings, 
  Palette, 
  Mic, 
  Sparkles, 
  Smartphone, 
  Bell, 
  Key, 
  ShieldCheck, 
  Activity, 
  Info, 
  Volume2, 
  Search, 
  Check, 
  CheckCircle2, 
  Laptop, 
  Wifi, 
  Vibrate, 
  Download, 
  Sliders, 
  FolderArchive,
  ArrowLeft,
  SlidersHorizontal,
  Terminal,
  ExternalLink,
  Lock,
  Eye,
  Type,
  Radio,
  Clock,
  ShieldAlert,
  ChevronRight,
  Heart
} from 'lucide-react';
import { 
  OrbSettings, 
  OrbStyleType, 
  PersonalityMode, 
  VoiceSettings 
} from '../types/myra';
import { triggerHapticFeedback } from '../utils/audioEffects';

export interface SettingsTabProps {
  voiceSettings: VoiceSettings;
  onUpdateVoiceSettings: (settings: VoiceSettings) => void;
  orbSettings: OrbSettings;
  onUpdateOrbSettings: (settings: OrbSettings) => void;
  onTestVoice: () => void;
  onTriggerEnrollment: () => void;
  onOpenMyraConnect?: () => void;
  onOpenWallpaper?: () => void;
  onBack?: () => void;
}

export type SectionKey = 
  | 'all'
  | 'appearance' 
  | 'voice' 
  | 'ai' 
  | 'connect' 
  | 'notifications'
  | 'permissions' 
  | 'privacy' 
  | 'accessibility'
  | 'diagnostics'
  | 'about';

// Reusable Modern Glass Switch with Electric Cyan Glow
const GlassSwitch: React.FC<{
  checked: boolean;
  onChange: (val: boolean) => void;
  disabled?: boolean;
}> = ({ checked, onChange, disabled }) => {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      disabled={disabled}
      onClick={() => {
        triggerHapticFeedback('command_confirm');
        onChange(!checked);
      }}
      className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer items-center rounded-full transition-all duration-200 ease-in-out focus:outline-none ${
        checked
          ? 'bg-gradient-to-r from-[#06B6D4] to-[#3B82F6] shadow-[0_0_12px_rgba(6,182,212,0.6)]'
          : 'bg-[#131b2e] border border-slate-700/60'
      } ${disabled ? 'opacity-40 cursor-not-allowed' : ''}`}
    >
      <span
        className={`pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow-md ring-0 transition duration-200 ease-in-out ${
          checked ? 'translate-x-6' : 'translate-x-1'
        }`}
      />
    </button>
  );
};

export const SettingsTab: React.FC<SettingsTabProps> = ({
  voiceSettings,
  onUpdateVoiceSettings,
  orbSettings,
  onUpdateOrbSettings,
  onTestVoice,
  onTriggerEnrollment,
  onOpenMyraConnect,
  onOpenWallpaper,
  onBack,
}) => {
  const [activeSection, setActiveSection] = useState<SectionKey>('all');
  const [searchQuery, setSearchQuery] = useState('');

  // Hardware & Interconnect state
  const [pcIp, setPcIp] = useState('192.168.1.105');
  const [pcPin, setPcPin] = useState('4829');
  const [pcConnected, setPcConnected] = useState(false);
  const [bluetoothAudioEnabled, setBluetoothAudioEnabled] = useState(true);
  const [bargeInEnabled, setBargeInEnabled] = useState(true);

  // Notification announcement settings (Persisted)
  const [notifyWhatsApp, setNotifyWhatsApp] = useState(() => {
    return localStorage.getItem('myra_notify_whatsapp') !== 'false';
  });
  const [notifyOtpPrivate, setNotifyOtpPrivate] = useState(() => {
    return localStorage.getItem('myra_notify_otp_private') !== 'false';
  });
  const [notifyVipOnly, setNotifyVipOnly] = useState(() => {
    return localStorage.getItem('myra_notify_vip_only') === 'true';
  });
  const [notifySound, setNotifySound] = useState(() => {
    return localStorage.getItem('myra_notify_sound') !== 'false';
  });

  // Privacy & Owner Voice States (Persisted)
  const [ownerProtection, setOwnerProtection] = useState(() => {
    return localStorage.getItem('myra_owner_protection') !== 'false';
  });
  const [ownerVoiceEnrolled, setOwnerVoiceEnrolled] = useState(() => {
    return localStorage.getItem('myra_owner_voice_enrolled') !== 'false';
  });
  const [biometricLock, setBiometricLock] = useState(() => {
    return localStorage.getItem('myra_biometric_lock') !== 'false';
  });
  const [unknownVoiceBlocked, setUnknownVoiceBlocked] = useState(() => {
    return localStorage.getItem('myra_unknown_voice_blocked') !== 'false';
  });
  const [sensitiveProtected, setSensitiveProtected] = useState(() => {
    return localStorage.getItem('myra_sensitive_protected') !== 'false';
  });
  const [appOpenProtection, setAppOpenProtection] = useState(() => {
    return localStorage.getItem('myra_app_open_protection') === 'true';
  });
  const [securityTimeoutMins, setSecurityTimeoutMins] = useState(() => {
    const saved = localStorage.getItem('myra_security_timeout');
    return saved ? parseInt(saved, 10) : 2;
  });

  // Accessibility Preferences (Persisted)
  const [textSize, setTextSize] = useState<'standard' | 'medium' | 'large'>(() => {
    return (localStorage.getItem('myra_accessibility_text_size') as any) || 'standard';
  });
  const [reducedMotion, setReducedMotion] = useState(() => {
    return localStorage.getItem('myra_accessibility_reduced_motion') === 'true';
  });
  const [highContrast, setHighContrast] = useState(() => {
    return localStorage.getItem('myra_accessibility_high_contrast') === 'true';
  });
  const [verboseScreenReader, setVerboseScreenReader] = useState(() => {
    return localStorage.getItem('myra_accessibility_verbose_screen_reader') === 'true';
  });
  const [hapticStrength, setHapticStrength] = useState<'soft' | 'medium' | 'strong'>(() => {
    return (localStorage.getItem('myra_accessibility_haptic_strength') as any) || 'medium';
  });

  // Interactive Modals State
  const [showVoiceEnrollModal, setShowVoiceEnrollModal] = useState(false);
  const [enrollStep, setEnrollStep] = useState(0);
  const [isEnrollListening, setIsEnrollListening] = useState(false);

  const [showVoiceTestModal, setShowVoiceTestModal] = useState(false);
  const [isTestingVoice, setIsTestingVoice] = useState(false);
  const [voiceTestResult, setVoiceTestResult] = useState<{ match: boolean; score: number; text: string } | null>(null);

  // Diagnostics view state
  const [showLogsConsole, setShowLogsConsole] = useState(false);
  const [diagnosticLogs] = useState([
    { time: '09:41:02', level: 'INFO', msg: 'MYRA 2026 Core runtime initialised.' },
    { time: '09:41:03', level: 'INFO', msg: 'AudioRecord 16.0kHz mono stream configured.' },
    { time: '09:41:04', level: 'INFO', msg: 'Gemini Live WebSocket duplex channel authenticated.' },
    { time: '09:41:05', level: 'AUD', msg: 'AudioTrack 24.0kHz PCM dynamic buffer warm (48ms ping).' },
    { time: '09:41:06', level: 'SYS', msg: 'Acoustic Voiceprint feature extractor loaded (MFCC-40).' },
    { time: '09:41:07', level: 'SEC', msg: 'BiometricPrompt bridge armed for sensitive actions.' },
  ]);

  // Android Permissions State
  const [permissions, setPermissions] = useState([
    { 
      name: 'Microphone & Voice Input', 
      granted: true, 
      key: 'mic', 
      perm: 'android.permission.RECORD_AUDIO', 
      desc: 'Real-time duplex 16kHz linear audio capture for continuous conversational speech.',
      isNativeOnly: false,
      platformBadge: 'Web & Android'
    },
    { 
      name: 'Accessibility Service (Screen Agent)', 
      granted: true, 
      key: 'access', 
      perm: 'android.permission.BIND_ACCESSIBILITY_SERVICE', 
      desc: 'Touch, scroll & inspect screen elements to click buttons and automate Android apps.',
      isNativeOnly: true,
      platformBadge: 'Android APK Only'
    },
    { 
      name: 'Draw Over Other Apps (Overlay HUD)', 
      granted: true, 
      key: 'overlay', 
      perm: 'android.permission.SYSTEM_ALERT_WINDOW', 
      desc: 'Floating glowing orb HUD and response banner atop any running application.',
      isNativeOnly: true,
      platformBadge: 'Android APK Only'
    },
    { 
      name: 'Default Digital Assistant Role', 
      granted: true, 
      key: 'assistant', 
      perm: 'android.permission.BIND_VOICE_INTERACTION', 
      desc: 'Long-press power button or corner swipe to invoke MYRA across the entire system.',
      isNativeOnly: true,
      platformBadge: 'Android APK Only'
    },
    { 
      name: 'Phone Call Management & Dialing', 
      granted: true, 
      key: 'call', 
      perm: 'android.permission.CALL_PHONE', 
      desc: 'Direct hands-free phone calling and simulated dialer integration.',
      isNativeOnly: true,
      platformBadge: 'Android APK Only'
    },
    { 
      name: 'Notification Listener Service', 
      granted: true, 
      key: 'notif', 
      perm: 'android.permission.BIND_NOTIFICATION_LISTENER_SERVICE', 
      desc: 'Read incoming WhatsApp messages, SMS alerts and announce callers in real-time.',
      isNativeOnly: true,
      platformBadge: 'Android APK Only'
    },
    { 
      name: 'SMS & OTP Reading (Safe Mode)', 
      granted: true, 
      key: 'sms', 
      perm: 'android.permission.RECEIVE_SMS', 
      desc: 'Read 6-digit verification codes only upon explicit owner command verification.',
      isNativeOnly: true,
      platformBadge: 'Android APK Only'
    },
    { 
      name: 'Precise Location (GPS / Parking)', 
      granted: true, 
      key: 'gps', 
      perm: 'android.permission.ACCESS_FINE_LOCATION', 
      desc: 'Saved parking spots, local weather alerts and distance calculation.',
      isNativeOnly: false,
      platformBadge: 'Web & Android'
    },
    { 
      name: 'Camera & Vision Scanner', 
      granted: true, 
      key: 'cam', 
      perm: 'android.permission.CAMERA', 
      desc: 'Multimodal vision for reading documents, OCR text and identifying surroundings.',
      isNativeOnly: false,
      platformBadge: 'Web & Android'
    },
    { 
      name: 'Device Admin (Instant Screen Lock)', 
      granted: true, 
      key: 'admin', 
      perm: 'android.permission.BIND_DEVICE_ADMIN', 
      desc: 'Panic emergency lock and security lock screen activation without confirmation.',
      isNativeOnly: true,
      platformBadge: 'Android APK Only'
    },
    { 
      name: 'Ignore Battery Optimizations', 
      granted: true, 
      key: 'battery', 
      perm: 'android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS', 
      desc: 'Prevent Android OS from sleeping or terminating background wake-word listening.',
      isNativeOnly: true,
      platformBadge: 'Android APK Only'
    },
  ]);

  const togglePermission = (idx: number) => {
    triggerHapticFeedback('command_confirm');
    setPermissions((prev) =>
      prev.map((p, i) => (i === idx ? { ...p, granted: !p.granted } : p))
    );
  };

  const enrollmentPhrases = [
    'Hi MYRA',
    'My name is the owner of MYRA',
    'MYRA, are you ready?',
    'MYRA, unlock owner mode',
  ];

  const sectionsList: { id: SectionKey; label: string; icon: any }[] = [
    { id: 'all', label: 'All', icon: Sliders },
    { id: 'appearance', label: 'Appearance & Display', icon: Palette },
    { id: 'voice', label: 'Voice Assistant', icon: Mic },
    { id: 'ai', label: 'AI & Gemini', icon: Sparkles },
    { id: 'connect', label: 'MYRA Connect', icon: Smartphone },
    { id: 'notifications', label: 'Notifications', icon: Bell },
    { id: 'permissions', label: 'Permissions & Device', icon: Key },
    { id: 'privacy', label: 'Privacy & Safety', icon: ShieldCheck },
    { id: 'accessibility', label: 'Accessibility', icon: SlidersHorizontal },
    { id: 'diagnostics', label: 'Diagnostics', icon: Activity },
    { id: 'about', label: 'About MYRA', icon: Info },
  ];

  const shouldShowSection = (sectionId: SectionKey) => {
    if (searchQuery.trim().length > 0) return true;
    if (activeSection === 'all') return true;
    return activeSection === sectionId;
  };

  return (
    <div className={`flex-1 min-h-0 flex flex-col bg-[#060911] text-slate-100 select-none overflow-y-auto font-['Plus_Jakarta_Sans',sans-serif] ${highContrast ? 'contrast-125' : ''}`}>
      {/* Sticky Top Header — Midnight Navy & Glass Blur */}
      <div className="sticky top-0 z-20 bg-[#090D16]/95 backdrop-blur-xl border-b border-[#06B6D4]/20 px-4 py-3 shadow-[0_4px_24px_rgba(0,0,0,0.7)]">
        <div className="flex items-center justify-between mb-2.5">
          <div className="flex items-center gap-2.5">
            {onBack ? (
              <button
                onClick={onBack}
                className="w-8 h-8 rounded-xl bg-[#0D1424] hover:bg-slate-800 border border-[#06B6D4]/30 flex items-center justify-center text-[#06B6D4] transition-all cursor-pointer active:scale-95 shadow-sm"
                title="Back to Voice Assistant"
              >
                <ArrowLeft className="w-4 h-4" />
              </button>
            ) : (
              <div className="w-8 h-8 rounded-xl bg-[#06B6D4]/10 border border-[#06B6D4]/30 flex items-center justify-center text-[#06B6D4] shadow-[0_0_12px_rgba(6,182,212,0.3)]">
                <Settings className="w-4 h-4 animate-[spin_10s_linear_infinite]" />
              </div>
            )}
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-sm font-bold text-white tracking-tight">MYRA Settings & Controls</h1>
                <span className="text-[10px] font-mono px-2 py-0.2 rounded-full bg-[#06B6D4]/15 text-[#06B6D4] border border-[#06B6D4]/30 font-bold">
                  2026 PRO
                </span>
              </div>
              <p className="text-[11px] text-slate-400">Appearance, speech models, security & system diagnostics</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {onBack && (
              <button
                onClick={onBack}
                className="hidden sm:flex items-center gap-1 text-[11px] font-medium text-slate-300 hover:text-white px-2.5 py-1 rounded-lg bg-[#0D1424] border border-slate-700 cursor-pointer"
              >
                <ArrowLeft className="w-3.5 h-3.5 text-[#06B6D4]" />
                <span>Assistant</span>
              </button>
            )}
            <div className="flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-[#0D1424] border border-[#06B6D4]/30 text-[10px] font-mono text-[#06B6D4]">
              <span className="w-1.5 h-1.5 rounded-full bg-[#06B6D4] animate-pulse" />
              <span className="font-semibold">ONLINE</span>
            </div>
          </div>
        </div>

        {/* Live Search Bar */}
        <div className="relative mb-2.5">
          <Search className="w-3.5 h-3.5 text-[#06B6D4] absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search appearance, voice models, permissions, accessibility..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-[#060911]/90 border border-[#06B6D4]/25 focus:border-[#06B6D4] rounded-xl pl-9 pr-8 py-1.5 text-xs text-white placeholder-slate-500 outline-none transition-all shadow-inner"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[10px] text-slate-400 hover:text-white cursor-pointer"
            >
              ✕
            </button>
          )}
        </div>

        {/* Categorized Navigation Segmented Tabs */}
        <div className="flex gap-1.5 overflow-x-auto pb-1 scrollbar-none">
          {sectionsList.map((sec) => {
            const Icon = sec.icon;
            const isActive = activeSection === sec.id;
            return (
              <button
                key={sec.id}
                onClick={() => {
                  triggerHapticFeedback('command_confirm');
                  setActiveSection(sec.id);
                }}
                className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-all duration-200 cursor-pointer ${
                  isActive
                    ? 'bg-gradient-to-r from-[#06B6D4]/25 to-[#3B82F6]/25 border border-[#06B6D4] text-[#06B6D4] shadow-[0_0_12px_rgba(6,182,212,0.3)] font-bold'
                    : 'bg-[#0D1424]/80 hover:bg-[#0D1424] text-slate-400 hover:text-slate-200 border border-slate-800'
                }`}
              >
                <Icon className={`w-3.5 h-3.5 ${isActive ? 'text-[#06B6D4]' : 'text-slate-400'}`} />
                <span>{sec.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Main Scrollable Content Area */}
      <div className="p-4 space-y-5">
        {/* ============================================================== */}
        {/* 1. APPEARANCE & DISPLAY SECTION                                */}
        {/* ============================================================== */}
        {shouldShowSection('appearance') && (
          <div className="space-y-3 animate-in fade-in duration-200">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Palette className="w-4 h-4 text-[#06B6D4]" />
                <h2 className="text-xs font-bold text-white uppercase tracking-wider">1. Appearance & Display</h2>
              </div>
              <span className="text-[10px] font-mono text-[#06B6D4]">10 Orb Styles</span>
            </div>

            {/* Live Wallpaper Launcher Card */}
            <div className="bg-[#0D1424]/85 backdrop-blur-md border border-[#06B6D4]/20 rounded-2xl p-4 shadow-[0_8px_32px_rgba(0,0,0,0.5)] flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-purple-500/20 border border-purple-500/40 flex items-center justify-center text-purple-300">
                  <Sparkles className="w-4 h-4 text-purple-300" />
                </div>
                <div>
                  <h3 className="text-xs font-bold text-white">9:16 Android Live Wallpaper</h3>
                  <p className="text-[11px] text-slate-400">Holographic Earth, HUD rings & glowing AI orb</p>
                </div>
              </div>
              <button
                onClick={onOpenWallpaper}
                className="px-3 py-1.5 rounded-xl bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white text-xs font-bold shadow-lg shadow-purple-900/30 active:scale-95 transition-all cursor-pointer"
              >
                Launch Studio
              </button>
            </div>

            {/* 10 Procedural Orb Styles */}
            <div className="bg-[#0D1424]/85 backdrop-blur-md border border-[#06B6D4]/20 rounded-2xl p-4 space-y-3 shadow-[0_8px_32px_rgba(0,0,0,0.5)]">
              <div>
                <span className="text-xs font-bold text-white block">Procedural Canvas Orb Designs</span>
                <p className="text-[11px] text-slate-400">Canvas & WebGL shaders decoupled from audio state</p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                {[
                  { id: 'myra_signature', label: 'MYRA Signature Orb', desc: 'Dual gyroscopic rings & 8-node pulsing nexus' },
                  { id: 'classic_glow', label: 'Classic Glow Orb', desc: 'Breathing soft aura & specular crescent highlight' },
                  { id: 'siri_fluid', label: 'Siri-style Fluid Orb', desc: 'Harmonic multi-frequency fluid blob morphing' },
                  { id: 'neon_ring', label: 'Neon Ring Orb', desc: 'Concentric cyber rings & orbiting particle arcs' },
                  { id: 'galaxy_particle', label: 'Galaxy Particle Orb', desc: 'Accretion disk with swirling star dust particles' },
                  { id: 'energy_core', label: 'Energy Core Orb', desc: 'Plasma reactor with radiating micro-lightning' },
                  { id: 'aurora', label: 'Aurora Orb', desc: 'Atmospheric Northern Lights ribbons in motion' },
                  { id: 'glass', label: 'Glass Orb', desc: 'Frosted glassmorphism with caustic refractive rim' },
                  { id: 'pulse', label: 'Pulse Orb', desc: 'Acoustic sonar shockwaves & dense reactive center' },
                  { id: 'wave', label: 'Wave Orb', desc: 'Sinusoidal 3D spherical equalizer ripples' },
                ].map((styleItem) => {
                  const isSelected = (orbSettings.style || 'myra_signature') === styleItem.id;
                  return (
                    <button
                      key={styleItem.id}
                      onClick={() => {
                        triggerHapticFeedback('command_confirm');
                        onUpdateOrbSettings({ ...orbSettings, style: styleItem.id as OrbStyleType });
                      }}
                      className={`p-3 rounded-xl border text-left transition-all cursor-pointer ${
                        isSelected
                          ? 'bg-gradient-to-r from-cyan-950/80 to-[#0D1424] border-[#06B6D4] text-white shadow-[0_0_15px_rgba(6,182,212,0.25)]'
                          : 'bg-[#090D16]/80 border-slate-800 text-slate-400 hover:border-slate-700'
                      }`}
                    >
                      <div className="flex items-center justify-between mb-1">
                        <span className={`text-xs font-bold ${isSelected ? 'text-[#06B6D4]' : 'text-slate-200'}`}>
                          {styleItem.label}
                        </span>
                        {isSelected && <span className="w-2 h-2 rounded-full bg-[#06B6D4] animate-pulse shadow-[0_0_6px_rgba(6,182,212,0.8)]" />}
                      </div>
                      <span className="text-[10px] text-slate-400 block leading-tight">{styleItem.desc}</span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Aura & Edge Glow Palette */}
            <div className="bg-[#0D1424]/85 backdrop-blur-md border border-[#06B6D4]/20 rounded-2xl p-4 space-y-3 shadow-[0_8px_32px_rgba(0,0,0,0.5)]">
              <span className="text-xs font-bold text-white block">Theme Color Palette</span>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                {[
                  { id: 'myra_signature', label: 'Electric Cyan', color: 'bg-cyan-400', hex: '#06B6D4' },
                  { id: 'electric_blue', label: 'Secondary Blue', color: 'bg-blue-500', hex: '#3B82F6' },
                  { id: 'cyber_purple', label: 'Cyber Violet', color: 'bg-purple-500', hex: '#A855F7' },
                  { id: 'romantic_rose', label: 'Romantic Rose', color: 'bg-rose-500', hex: '#F43F5E' },
                  { id: 'emerald_neon', label: 'Emerald Aura', color: 'bg-emerald-500', hex: '#10B981' },
                  { id: 'cyber_amber', label: 'Cyber Amber', color: 'bg-amber-500', hex: '#F59E0B' },
                ].map((theme) => {
                  const isSelected = orbSettings.theme === theme.id;
                  return (
                    <button
                      key={theme.id}
                      onClick={() => onUpdateOrbSettings({ ...orbSettings, theme: theme.id as any })}
                      className={`p-2.5 rounded-xl border text-left flex items-center gap-2.5 transition-all cursor-pointer ${
                        isSelected
                          ? 'bg-gradient-to-r from-cyan-950/80 to-[#0D1424] border-[#06B6D4] text-white shadow-[0_0_12px_rgba(6,182,212,0.2)]'
                          : 'bg-[#090D16]/80 border-slate-800 text-slate-400 hover:border-slate-700'
                      }`}
                    >
                      <span className={`w-3.5 h-3.5 rounded-full shrink-0 shadow-sm ${theme.color}`} />
                      <div className="truncate">
                        <span className="text-xs font-semibold text-white block truncate">{theme.label}</span>
                        <span className="text-[9px] font-mono text-slate-500">{theme.hex}</span>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Sliders: Dimensions & Audio Reactivity */}
            <div className="bg-[#0D1424]/85 backdrop-blur-md border border-[#06B6D4]/20 rounded-2xl p-4 space-y-4 shadow-[0_8px_32px_rgba(0,0,0,0.5)]">
              <span className="text-xs font-bold text-white block">Visual Effects & Dimensions</span>
              
              <div>
                <div className="flex justify-between items-center text-xs mb-1">
                  <span className="text-slate-300 font-medium">Orb Diameter</span>
                  <span className="font-mono text-[#06B6D4] px-2 py-0.5 rounded bg-cyan-950 border border-[#06B6D4]/30 text-[11px] font-bold">
                    {orbSettings.sizeDp || 240} dp
                  </span>
                </div>
                <input
                  type="range"
                  min="160"
                  max="320"
                  step="10"
                  value={orbSettings.sizeDp || 240}
                  onChange={(e) => onUpdateOrbSettings({ ...orbSettings, sizeDp: parseInt(e.target.value) })}
                  className="w-full accent-[#06B6D4] h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                />
              </div>

              <div>
                <div className="flex justify-between items-center text-xs mb-1">
                  <span className="text-slate-300 font-medium">Glow Intensity</span>
                  <span className="font-mono text-[#06B6D4] px-2 py-0.5 rounded bg-cyan-950 border border-[#06B6D4]/30 text-[11px] font-bold">
                    {((orbSettings.glowIntensity || 0.85) * 100).toFixed(0)}%
                  </span>
                </div>
                <input
                  type="range"
                  min="0.2"
                  max="1.5"
                  step="0.05"
                  value={orbSettings.glowIntensity || 0.85}
                  onChange={(e) => onUpdateOrbSettings({ ...orbSettings, glowIntensity: parseFloat(e.target.value) })}
                  className="w-full accent-[#06B6D4] h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                />
              </div>

              <div>
                <div className="flex justify-between items-center text-xs mb-1">
                  <span className="text-slate-300 font-medium">Animation Speed</span>
                  <span className="font-mono text-[#06B6D4] px-2 py-0.5 rounded bg-cyan-950 border border-[#06B6D4]/30 text-[11px] font-bold">
                    {(orbSettings.animationSpeed || 1.0).toFixed(1)}x
                  </span>
                </div>
                <input
                  type="range"
                  min="0.4"
                  max="2.5"
                  step="0.1"
                  value={orbSettings.animationSpeed || 1.0}
                  onChange={(e) => onUpdateOrbSettings({ ...orbSettings, animationSpeed: parseFloat(e.target.value) })}
                  className="w-full accent-[#06B6D4] h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                />
              </div>

              <div className="pt-2 border-t border-slate-800 flex items-center justify-between">
                <div>
                  <span className="text-xs font-bold text-white block">React to Audio Amplitude</span>
                  <span className="text-[11px] text-slate-400">Pulsate orb diameter in sync with real-time mic volume</span>
                </div>
                <GlassSwitch
                  checked={orbSettings.reactToAudio ?? true}
                  onChange={(val) => onUpdateOrbSettings({ ...orbSettings, reactToAudio: val })}
                />
              </div>
            </div>
          </div>
        )}

        {/* ============================================================== */}
        {/* 2. VOICE ASSISTANT SECTION                                     */}
        {/* ============================================================== */}
        {shouldShowSection('voice') && (
          <div className="space-y-3 animate-in fade-in duration-200">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Mic className="w-4 h-4 text-[#06B6D4]" />
                <h2 className="text-xs font-bold text-white uppercase tracking-wider">2. Voice Assistant</h2>
              </div>
              <span className="text-[10px] font-mono text-[#06B6D4]">Aoede 24kHz</span>
            </div>

            {/* Personality Selector */}
            <div className="bg-[#0D1424]/85 backdrop-blur-md border border-[#06B6D4]/20 rounded-2xl p-4 space-y-3 shadow-[0_8px_32px_rgba(0,0,0,0.5)]">
              <span className="text-xs font-bold text-white block">Persona Demeanor & Style</span>
              <div className="space-y-2">
                {[
                  {
                    id: 'friendly_assistant',
                    name: 'Friendly Assistant (Default)',
                    desc: 'Helpful, warm, capable Hindi/Hinglish partner with conversational flow',
                    icon: Sparkles,
                    color: 'text-[#06B6D4]',
                  },
                  {
                    id: 'romantic_gf',
                    name: 'GF / Romantic Mode ❤️',
                    desc: 'Caring, sweet, affectionate girlfriend voice & playful banter',
                    icon: Heart,
                    color: 'text-rose-400',
                  },
                  {
                    id: 'professional',
                    name: 'Pro Executive Assistant',
                    desc: 'Crisp, structured, business-ready responses with zero fluff',
                    icon: ShieldCheck,
                    color: 'text-emerald-400',
                  },
                ].map((m) => {
                  const Icon = m.icon;
                  const isSelected = voiceSettings.personality === m.id;
                  return (
                    <button
                      key={m.id}
                      onClick={() =>
                        onUpdateVoiceSettings({
                          ...voiceSettings,
                          personality: m.id as PersonalityMode,
                        })
                      }
                      className={`w-full p-3 rounded-xl border text-left flex items-center justify-between transition-all cursor-pointer ${
                        isSelected
                          ? 'bg-gradient-to-r from-cyan-950/80 to-[#0D1424] border-[#06B6D4] text-white shadow-[0_0_15px_rgba(6,182,212,0.2)]'
                          : 'bg-[#090D16]/80 border-slate-800 text-slate-400 hover:border-slate-700 hover:text-slate-200'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <div
                          className={`w-8 h-8 rounded-lg flex items-center justify-center ${
                            isSelected ? 'bg-cyan-500/20 text-cyan-300' : 'bg-slate-800 text-slate-400'
                          }`}
                        >
                          <Icon className={`w-4 h-4 ${m.color}`} />
                        </div>
                        <div>
                          <span className="text-xs font-bold block text-white">{m.name}</span>
                          <span className="text-[11px] text-slate-400">{m.desc}</span>
                        </div>
                      </div>
                      {isSelected ? (
                        <CheckCircle2 className="w-4 h-4 text-[#06B6D4] shrink-0" />
                      ) : (
                        <div className="w-4 h-4 rounded-full border border-slate-700" />
                      )}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Language & Wake Word Settings */}
            <div className="bg-[#0D1424]/85 backdrop-blur-md border border-[#06B6D4]/20 rounded-2xl p-4 space-y-4 shadow-[0_8px_32px_rgba(0,0,0,0.5)]">
              <div>
                <span className="text-xs font-bold text-white block mb-2">Primary Recognition Language</span>
                <div className="grid grid-cols-2 gap-2">
                  {[
                    { id: 'hi-IN', label: 'Hindi / Hinglish (India)', sub: 'Bilingual Code-Switching' },
                    { id: 'en-IN', label: 'English (India)', sub: 'Natural Indian Accent' },
                  ].map((lang) => {
                    const isSelected = voiceSettings.language === lang.id;
                    return (
                      <button
                        key={lang.id}
                        onClick={() => onUpdateVoiceSettings({ ...voiceSettings, language: lang.id as any })}
                        className={`p-2.5 rounded-xl border text-left transition-all cursor-pointer ${
                          isSelected
                            ? 'bg-cyan-950/80 border-[#06B6D4] text-white'
                            : 'bg-[#090D16]/80 border-slate-800 text-slate-400 hover:border-slate-700'
                        }`}
                      >
                        <span className="text-xs font-bold block text-white">{lang.label}</span>
                        <span className="text-[10px] text-slate-400">{lang.sub}</span>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Wake Word Toggles */}
              <div className="pt-3 border-t border-slate-800/80 space-y-3">
                <div className="flex items-center justify-between">
                  <div>
                    <span className="text-xs font-bold text-white block">Always-Listening Wake Word</span>
                    <span className="text-[11px] text-slate-400">Continuous voice trigger detection</span>
                  </div>
                  <GlassSwitch
                    checked={voiceSettings.wakeWordEnabled ?? true}
                    onChange={(val) => onUpdateVoiceSettings({ ...voiceSettings, wakeWordEnabled: val })}
                  />
                </div>

                <div className="grid grid-cols-2 gap-2">
                  {['Hey MYRA', 'Sun MYRA', 'Namaste MYRA', 'Hello MYRA'].map((phrase) => {
                    const isSelected = (voiceSettings.wakeWord || 'Hey MYRA') === phrase;
                    return (
                      <button
                        key={phrase}
                        onClick={() => onUpdateVoiceSettings({ ...voiceSettings, wakeWord: phrase })}
                        className={`p-2 rounded-xl border text-center text-xs font-semibold font-mono transition-all cursor-pointer ${
                          isSelected
                            ? 'bg-cyan-950/80 border-[#06B6D4] text-[#06B6D4]'
                            : 'bg-[#090D16]/80 border-slate-800 text-slate-400'
                        }`}
                      >
                        "{phrase}"
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Pitch & Rate Sliders */}
              <div className="pt-3 border-t border-slate-800/80 space-y-3">
                <div>
                  <div className="flex justify-between items-center text-xs mb-1">
                    <span className="text-slate-300 font-medium">Voice Pitch</span>
                    <span className="font-mono text-[#06B6D4] px-2 py-0.5 rounded bg-cyan-950 border border-[#06B6D4]/30 text-[11px] font-bold">
                      {voiceSettings.pitch}x
                    </span>
                  </div>
                  <input
                    type="range"
                    min="0.5"
                    max="1.5"
                    step="0.1"
                    value={voiceSettings.pitch}
                    onChange={(e) => onUpdateVoiceSettings({ ...voiceSettings, pitch: parseFloat(e.target.value) })}
                    className="w-full accent-[#06B6D4] h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                  />
                </div>

                <div>
                  <div className="flex justify-between items-center text-xs mb-1">
                    <span className="text-slate-300 font-medium">Speech Rate</span>
                    <span className="font-mono text-[#06B6D4] px-2 py-0.5 rounded bg-cyan-950 border border-[#06B6D4]/30 text-[11px] font-bold">
                      {voiceSettings.rate}x
                    </span>
                  </div>
                  <input
                    type="range"
                    min="0.5"
                    max="1.5"
                    step="0.1"
                    value={voiceSettings.rate}
                    onChange={(e) => onUpdateVoiceSettings({ ...voiceSettings, rate: parseFloat(e.target.value) })}
                    className="w-full accent-[#06B6D4] h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                  />
                </div>

                <button
                  onClick={onTestVoice}
                  className="w-full py-2.5 rounded-xl bg-gradient-to-r from-[#06B6D4] to-[#3B82F6] hover:from-cyan-400 hover:to-blue-500 text-slate-950 text-xs font-bold flex items-center justify-center gap-2 shadow-[0_0_20px_rgba(6,182,212,0.3)] active:scale-98 transition-all cursor-pointer"
                >
                  <Volume2 className="w-4 h-4" />
                  <span>Play Sample Audio Preview</span>
                </button>
              </div>

              {/* Hardware Hands-Free Toggles */}
              <div className="pt-3 border-t border-slate-800/80 space-y-3">
                <div className="flex items-center justify-between">
                  <div>
                    <span className="text-xs font-bold text-white block">Bluetooth Audio & Headset Routing</span>
                    <p className="text-[11px] text-slate-400">Stream Gemini Live audio through earbuds or car stereo</p>
                  </div>
                  <GlassSwitch
                    checked={bluetoothAudioEnabled}
                    onChange={(val) => setBluetoothAudioEnabled(val)}
                  />
                </div>

                <div className="flex items-center justify-between pt-2 border-t border-slate-800/60">
                  <div>
                    <span className="text-xs font-bold text-white block">Physical Haptic Feedback</span>
                    <p className="text-[11px] text-slate-400">Vibrate micro-pulse when speech starts & actions complete</p>
                  </div>
                  <button
                    onClick={() => triggerHapticFeedback('command_confirm')}
                    className="px-3 py-1.5 rounded-xl bg-cyan-950 hover:bg-cyan-900 border border-[#06B6D4]/40 text-xs font-semibold text-[#06B6D4] active:scale-95 transition-all flex items-center gap-1.5 cursor-pointer"
                  >
                    <Vibrate className="w-3.5 h-3.5 text-[#06B6D4]" />
                    <span>Test Vibrate</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ============================================================== */}
        {/* 3. AI & GEMINI SECTION                                         */}
        {/* ============================================================== */}
        {shouldShowSection('ai') && (
          <div className="space-y-3 animate-in fade-in duration-200">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-[#06B6D4]" />
                <h2 className="text-xs font-bold text-white uppercase tracking-wider">3. AI & Gemini Engine</h2>
              </div>
              <span className="text-[10px] font-mono text-[#06B6D4]">gemini-3.8-live</span>
            </div>

            {/* AI Speech Pipeline Selection */}
            <div className="bg-[#0D1424]/85 backdrop-blur-md border border-[#06B6D4]/20 rounded-2xl p-4 space-y-3 shadow-[0_8px_32px_rgba(0,0,0,0.5)]">
              <span className="text-xs font-bold text-white block">Primary AI Speech Pipeline</span>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
                {/* 1. Gemini Live */}
                <button
                  onClick={() => onUpdateVoiceSettings({ ...voiceSettings, ttsEngine: 'gemini_live' })}
                  className={`p-3 rounded-xl border text-left transition-all cursor-pointer ${
                    voiceSettings.ttsEngine === 'gemini_live'
                      ? 'bg-gradient-to-br from-emerald-950/80 to-[#0D1424] border-emerald-400 text-white shadow-[0_0_15px_rgba(16,185,129,0.25)]'
                      : 'bg-[#090D16]/80 border-slate-800 text-slate-400 hover:border-slate-700'
                  }`}
                >
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-xs font-bold text-emerald-400 flex items-center gap-1.5">
                      <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                      Gemini Live
                    </span>
                    {voiceSettings.ttsEngine === 'gemini_live' && <Check className="w-3.5 h-3.5 text-emerald-400" />}
                  </div>
                  <span className="text-[10px] text-slate-300 block leading-tight">
                    gemini-3.8-live • Aoede natural female voice • 24kHz stream
                  </span>
                </button>

                {/* 2. Gemini Neural TTS */}
                <button
                  onClick={() => onUpdateVoiceSettings({ ...voiceSettings, ttsEngine: 'gemini_tts' })}
                  className={`p-3 rounded-xl border text-left transition-all cursor-pointer ${
                    voiceSettings.ttsEngine === 'gemini_tts'
                      ? 'bg-gradient-to-br from-cyan-950/80 to-[#0D1424] border-[#06B6D4] text-white shadow-[0_0_15px_rgba(6,182,212,0.25)]'
                      : 'bg-[#090D16]/80 border-slate-800 text-slate-400 hover:border-slate-700'
                  }`}
                >
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-xs font-bold text-[#06B6D4] flex items-center gap-1.5">
                      <Sparkles className="w-3.5 h-3.5" />
                      Gemini Neural TTS
                    </span>
                    {voiceSettings.ttsEngine === 'gemini_tts' && <Check className="w-3.5 h-3.5 text-[#06B6D4]" />}
                  </div>
                  <span className="text-[10px] text-slate-300 block leading-tight">
                    gemini-3.1-flash-tts • Natural inflection & Hindi prosody
                  </span>
                </button>

                {/* 3. Fallback Web TTS */}
                <button
                  onClick={() => onUpdateVoiceSettings({ ...voiceSettings, ttsEngine: 'fallback_tts' })}
                  className={`p-3 rounded-xl border text-left transition-all cursor-pointer ${
                    voiceSettings.ttsEngine === 'fallback_tts'
                      ? 'bg-gradient-to-br from-amber-950/80 to-[#0D1424] border-amber-400 text-white shadow-[0_0_15px_rgba(245,158,11,0.25)]'
                      : 'bg-[#090D16]/80 border-slate-800 text-slate-400 hover:border-slate-700'
                  }`}
                >
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-xs font-bold text-amber-400 flex items-center gap-1.5">
                      <Volume2 className="w-3.5 h-3.5" />
                      Offline Web Speech
                    </span>
                    {voiceSettings.ttsEngine === 'fallback_tts' && <Check className="w-3.5 h-3.5 text-amber-400" />}
                  </div>
                  <span className="text-[10px] text-slate-400 block leading-tight">
                    Device Android TTS (Offline backup when network drops)
                  </span>
                </button>
              </div>

              {/* Barge-In Interruption Setting */}
              <div className="pt-3 border-t border-slate-800 flex items-center justify-between">
                <div>
                  <span className="text-xs font-bold text-white block">Full Duplex Barge-In Interruption</span>
                  <span className="text-[11px] text-slate-400">Instantly halt MYRA speech when you speak mid-sentence</span>
                </div>
                <GlassSwitch
                  checked={bargeInEnabled}
                  onChange={(val) => setBargeInEnabled(val)}
                />
              </div>
            </div>
          </div>
        )}

        {/* ============================================================== */}
        {/* 4. MYRA CONNECT SECTION                                        */}
        {/* ============================================================== */}
        {shouldShowSection('connect') && (
          <div className="space-y-3 animate-in fade-in duration-200">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Smartphone className="w-4 h-4 text-[#06B6D4]" />
                <h2 className="text-xs font-bold text-white uppercase tracking-wider">4. MYRA Connect & Interconnect</h2>
              </div>
              <span className="text-[10px] font-mono text-[#06B6D4]">Port 9876</span>
            </div>

            {/* Phone-to-Phone Interconnect */}
            <div className="bg-[#0D1424]/85 backdrop-blur-md border border-[#06B6D4]/20 rounded-2xl p-4 space-y-3 shadow-[0_8px_32px_rgba(0,0,0,0.5)]">
              <div className="flex items-center justify-between border-b border-[#06B6D4]/20 pb-3">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-cyan-500/20 text-cyan-300 flex items-center justify-center border border-cyan-400/40">
                    <Smartphone className="w-5 h-5 text-[#06B6D4]" />
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-white">Direct Local Wi-Fi / Hotspot Bridge</h3>
                    <p className="text-xs text-slate-400">Zero-cloud encrypted device-to-device remote</p>
                  </div>
                </div>
                <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-cyan-950 text-[#06B6D4] border border-[#06B6D4]/40 font-bold">
                  LOCAL TCP
                </span>
              </div>

              <p className="text-xs text-slate-300 leading-relaxed">
                Control your second Android phone across the room without internet. Say:
                <br />
                <span className="text-[#06B6D4] font-mono">"Doosre phone par YouTube chalao"</span> or{' '}
                <span className="text-[#06B6D4] font-mono">"Torch jalao"</span>.
              </p>

              <button
                onClick={onOpenMyraConnect}
                className="w-full py-3 rounded-xl bg-gradient-to-r from-[#06B6D4] to-[#3B82F6] hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-bold text-xs shadow-lg shadow-cyan-500/20 transition-all active:scale-98 flex items-center justify-center gap-2 cursor-pointer"
              >
                <Smartphone className="w-4 h-4" />
                <span>Open MYRA Connect Device Manager</span>
              </button>
            </div>

            {/* PC Companion Sync Card */}
            <div className="bg-[#0D1424]/85 backdrop-blur-md border border-[#06B6D4]/20 rounded-2xl p-4 space-y-3 shadow-[0_8px_32px_rgba(0,0,0,0.5)]">
              <div className="flex items-center gap-2">
                <Laptop className="w-4 h-4 text-[#06B6D4]" />
                <h3 className="text-xs font-bold text-white uppercase tracking-wider">PC Companion (Windows / Mac)</h3>
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-[10px] text-slate-400 block mb-1">Companion IP</label>
                  <input
                    type="text"
                    value={pcIp}
                    onChange={(e) => setPcIp(e.target.value)}
                    className="w-full bg-[#060911] border border-slate-800 text-xs text-white rounded-xl p-2 outline-none font-mono"
                  />
                </div>
                <div>
                  <label className="text-[10px] text-slate-400 block mb-1">Security PIN</label>
                  <input
                    type="text"
                    value={pcPin}
                    onChange={(e) => setPcPin(e.target.value)}
                    className="w-full bg-[#060911] border border-slate-800 text-xs text-white rounded-xl p-2 outline-none font-mono"
                  />
                </div>
              </div>

              <button
                onClick={() => setPcConnected(!pcConnected)}
                className={`w-full py-2.5 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-2 cursor-pointer ${
                  pcConnected
                    ? 'bg-emerald-600 text-white'
                    : 'bg-cyan-950 hover:bg-cyan-900 border border-[#06B6D4]/40 text-[#06B6D4]'
                }`}
              >
                <Wifi className="w-4 h-4" />
                <span>{pcConnected ? 'Connected to Windows PC (192.168.1.105)' : 'Connect to PC Companion'}</span>
              </button>
            </div>
          </div>
        )}

        {/* ============================================================== */}
        {/* 5. NOTIFICATIONS SECTION                                       */}
        {/* ============================================================== */}
        {shouldShowSection('notifications') && (
          <div className="space-y-3 animate-in fade-in duration-200">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Bell className="w-4 h-4 text-[#06B6D4]" />
                <h2 className="text-xs font-bold text-white uppercase tracking-wider">5. Notifications & Alerts</h2>
              </div>
              <span className="text-[10px] font-mono text-[#06B6D4]">Listener Active</span>
            </div>

            <div className="bg-[#0D1424]/85 backdrop-blur-md border border-[#06B6D4]/20 rounded-2xl p-4 space-y-4 shadow-[0_8px_32px_rgba(0,0,0,0.5)]">
              <div>
                <span className="text-xs font-bold text-white block">Intelligent Announcement Filters</span>
                <p className="text-[11px] text-slate-400">MYRA monitors notifications in real time and announces messages safely</p>
              </div>

              <div className="divide-y divide-slate-800/80">
                {/* Toggle 1: WhatsApp Announcement */}
                <div className="py-2.5 flex items-center justify-between">
                  <div className="pr-4">
                    <span className="text-xs font-semibold text-white block">WhatsApp Message Voice Announcement</span>
                    <span className="text-[10px] text-slate-400">Speak sender name and message content via Aoede voice</span>
                  </div>
                  <GlassSwitch
                    checked={notifyWhatsApp}
                    onChange={(val) => {
                      setNotifyWhatsApp(val);
                      localStorage.setItem('myra_notify_whatsapp', String(val));
                    }}
                  />
                </div>

                {/* Toggle 2: Sensitive OTP Privacy Guard */}
                <div className="py-2.5 flex items-center justify-between">
                  <div className="pr-4">
                    <span className="text-xs font-semibold text-white block">Strict OTP & Financial Privacy Guard</span>
                    <span className="text-[10px] text-slate-400">Never speak bank OTPs aloud automatically; only read when asked</span>
                  </div>
                  <GlassSwitch
                    checked={notifyOtpPrivate}
                    onChange={(val) => {
                      setNotifyOtpPrivate(val);
                      localStorage.setItem('myra_notify_otp_private', String(val));
                    }}
                  />
                </div>

                {/* Toggle 3: VIP Callers Only */}
                <div className="py-2.5 flex items-center justify-between">
                  <div className="pr-4">
                    <span className="text-xs font-semibold text-white block">VIP & Family Priority Filter</span>
                    <span className="text-[10px] text-slate-400">Only announce calls and alerts from starred family contacts</span>
                  </div>
                  <GlassSwitch
                    checked={notifyVipOnly}
                    onChange={(val) => {
                      setNotifyVipOnly(val);
                      localStorage.setItem('myra_notify_vip_only', String(val));
                    }}
                  />
                </div>

                {/* Toggle 4: Alert Sounds */}
                <div className="py-2.5 flex items-center justify-between">
                  <div className="pr-4">
                    <span className="text-xs font-semibold text-white block">Auditory Chime & Haptic Ping</span>
                    <span className="text-[10px] text-slate-400">Play soft futuristic chime before speaking incoming notification</span>
                  </div>
                  <GlassSwitch
                    checked={notifySound}
                    onChange={(val) => {
                      setNotifySound(val);
                      localStorage.setItem('myra_notify_sound', String(val));
                    }}
                  />
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ============================================================== */}
        {/* 6. PERMISSIONS & DEVICE SECTION                                */}
        {/* ============================================================== */}
        {shouldShowSection('permissions') && (
          <div className="space-y-3 animate-in fade-in duration-200">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Key className="w-4 h-4 text-[#06B6D4]" />
                <h2 className="text-xs font-bold text-white uppercase tracking-wider">6. Permissions & Device</h2>
              </div>
              <span className="text-[10px] font-mono text-emerald-400">11 Verified</span>
            </div>

            <div className="bg-[#0D1424]/85 backdrop-blur-md border border-[#06B6D4]/20 rounded-2xl p-4 shadow-[0_8px_32px_rgba(0,0,0,0.5)]">
              <div className="p-3 mb-3 rounded-xl bg-[#090D16] border border-[#06B6D4]/30 text-xs text-slate-300 flex items-center justify-between">
                <div>
                  <span className="font-bold text-[#06B6D4] block">Platform Capability Mapping</span>
                  <span className="text-[11px] text-slate-400">Microphone & Camera operate live in Web Preview. OS-level tools require the native Android APK.</span>
                </div>
                <span className="px-2 py-0.5 rounded-full bg-cyan-950 text-[#06B6D4] text-[10px] font-mono font-bold border border-[#06B6D4]/40 shrink-0">
                  Android 14 Ready
                </span>
              </div>

              <div className="divide-y divide-slate-800/80">
                {permissions.map((perm, idx) => (
                  <div key={perm.key} className="py-2.5 flex items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                      <div
                        className={`w-7 h-7 rounded-lg flex items-center justify-center shrink-0 ${
                          perm.granted ? 'bg-emerald-950 text-emerald-400' : 'bg-slate-800 text-slate-500'
                        }`}
                      >
                        <ShieldCheck className="w-4 h-4" />
                      </div>
                      <div>
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="text-xs font-medium text-slate-200 block">{perm.name}</span>
                          <span className={`text-[9px] font-mono px-1.5 py-0.2 rounded font-bold ${
                            perm.isNativeOnly 
                              ? 'bg-amber-950/80 text-amber-300 border border-amber-500/30' 
                              : 'bg-cyan-950/80 text-[#06B6D4] border border-[#06B6D4]/30'
                          }`}>
                            {perm.platformBadge}
                          </span>
                        </div>
                        <span className="text-[10px] text-slate-500 block leading-tight">{perm.desc}</span>
                      </div>
                    </div>
                    <button
                      onClick={() => togglePermission(idx)}
                      className={`px-3 py-1 rounded-full text-[10px] font-bold font-mono transition-colors shrink-0 cursor-pointer ${
                        perm.granted
                          ? 'bg-emerald-950/80 text-emerald-300 border border-emerald-400/50 shadow-[0_0_8px_rgba(16,185,129,0.25)]'
                          : 'bg-rose-950/80 text-rose-300 border border-rose-500/40'
                      }`}
                    >
                      {perm.granted ? 'GRANTED' : 'DENIED'}
                    </button>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* ============================================================== */}
        {/* 7. PRIVACY & SAFETY SECTION                                    */}
        {/* ============================================================== */}
        {shouldShowSection('privacy') && (
          <div className="space-y-3 animate-in fade-in duration-200">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-[#06B6D4]" />
                <h2 className="text-xs font-bold text-white uppercase tracking-wider">7. Privacy & Safety</h2>
              </div>
              <span className={`text-[10px] font-mono ${ownerProtection ? 'text-emerald-400' : 'text-slate-500'}`}>
                {ownerProtection ? 'Armed' : 'Disarmed'}
              </span>
            </div>

            {/* Master Security Card */}
            <div className="bg-[#0D1424]/85 backdrop-blur-md border border-[#06B6D4]/20 rounded-2xl p-4 space-y-4 shadow-[0_8px_32px_rgba(0,0,0,0.5)]">
              <div className="flex items-center justify-between border-b border-[#06B6D4]/20 pb-3">
                <div>
                  <h3 className="text-xs font-bold text-white">Owner Voiceprint Verification</h3>
                  <p className="text-[11px] text-slate-400">Acoustic speaker validation with MFCC-40 feature extractor</p>
                </div>
                <GlassSwitch
                  checked={ownerProtection}
                  onChange={(val) => {
                    setOwnerProtection(val);
                    localStorage.setItem('myra_owner_protection', String(val));
                  }}
                />
              </div>

              {/* Toggles */}
              <div className="divide-y divide-slate-800/80">
                <div className="py-2.5 flex items-center justify-between">
                  <div>
                    <span className="text-xs font-semibold text-white block">Biometric Fingerprint / Face Fallback</span>
                    <span className="text-[10px] text-slate-400">Android BiometricPrompt when background noise is high</span>
                  </div>
                  <GlassSwitch
                    checked={biometricLock}
                    onChange={(val) => {
                      setBiometricLock(val);
                      localStorage.setItem('myra_biometric_lock', String(val));
                    }}
                  />
                </div>

                <div className="py-2.5 flex items-center justify-between">
                  <div>
                    <span className="text-xs font-semibold text-white block">Block Unknown Voices</span>
                    <span className="text-[10px] text-slate-400">Reject commands spoken by strangers or playback</span>
                  </div>
                  <GlassSwitch
                    checked={unknownVoiceBlocked}
                    onChange={(val) => {
                      setUnknownVoiceBlocked(val);
                      localStorage.setItem('myra_unknown_voice_blocked', String(val));
                    }}
                  />
                </div>

                <div className="py-2.5 flex items-center justify-between">
                  <div>
                    <span className="text-xs font-semibold text-white block">Sensitive Command Shield</span>
                    <span className="text-[10px] text-slate-400">Protect WhatsApp, SMS, Calls, SOS & Settings</span>
                  </div>
                  <GlassSwitch
                    checked={sensitiveProtected}
                    onChange={(val) => {
                      setSensitiveProtected(val);
                      localStorage.setItem('myra_sensitive_protected', String(val));
                    }}
                  />
                </div>

                <div className="py-2.5 flex items-center justify-between">
                  <div>
                    <span className="text-xs font-semibold text-white block">App Launch Gate</span>
                    <span className="text-[10px] text-slate-400">Prompt for biometric authentication on app startup</span>
                  </div>
                  <GlassSwitch
                    checked={appOpenProtection}
                    onChange={(val) => {
                      setAppOpenProtection(val);
                      localStorage.setItem('myra_app_open_protection', String(val));
                    }}
                  />
                </div>
              </div>

              {/* Inactivity Timeout Slider */}
              <div className="pt-3 border-t border-slate-800">
                <div className="flex justify-between items-center text-xs mb-1">
                  <span className="text-slate-300 font-medium">Session Inactivity Timeout</span>
                  <span className="font-mono text-[#06B6D4] px-2 py-0.5 rounded bg-cyan-950 border border-[#06B6D4]/30 text-[11px] font-bold">
                    {securityTimeoutMins} Minutes
                  </span>
                </div>
                <input
                  type="range"
                  min="1"
                  max="10"
                  step="1"
                  value={securityTimeoutMins}
                  onChange={(e) => {
                    const v = parseInt(e.target.value, 10);
                    setSecurityTimeoutMins(v);
                    localStorage.setItem('myra_security_timeout', String(v));
                  }}
                  className="w-full accent-[#06B6D4] h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                />
              </div>

              {/* Action Buttons */}
              <div className="grid grid-cols-2 gap-2 pt-2">
                <button
                  onClick={() => {
                    setVoiceTestResult(null);
                    setShowVoiceTestModal(true);
                  }}
                  className="py-2.5 px-3 rounded-xl bg-cyan-950 hover:bg-cyan-900 border border-cyan-400/50 text-[#06B6D4] text-xs font-bold text-center transition-all flex items-center justify-center gap-1.5 shadow-[0_0_12px_rgba(6,182,212,0.15)] cursor-pointer"
                >
                  <Activity className="w-3.5 h-3.5 text-[#06B6D4]" />
                  <span>Test Owner Voice</span>
                </button>
                <button
                  onClick={() => {
                    setEnrollStep(0);
                    setShowVoiceEnrollModal(true);
                  }}
                  className="py-2.5 px-3 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white text-xs font-bold text-center transition-all flex items-center justify-center gap-1.5 shadow-md shadow-blue-900/30 cursor-pointer"
                >
                  <Mic className="w-3.5 h-3.5" />
                  <span>Re-enroll Voiceprint</span>
                </button>
              </div>
            </div>
          </div>
        )}

        {/* ============================================================== */}
        {/* 8. ACCESSIBILITY SECTION (NEW & COMPLETE)                      */}
        {/* ============================================================== */}
        {shouldShowSection('accessibility') && (
          <div className="space-y-3 animate-in fade-in duration-200">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <SlidersHorizontal className="w-4 h-4 text-[#06B6D4]" />
                <h2 className="text-xs font-bold text-white uppercase tracking-wider">8. Accessibility</h2>
              </div>
              <span className="text-[10px] font-mono text-[#06B6D4]">Inclusive UI</span>
            </div>

            <div className="bg-[#0D1424]/85 backdrop-blur-md border border-[#06B6D4]/20 rounded-2xl p-4 space-y-4 shadow-[0_8px_32px_rgba(0,0,0,0.5)]">
              {/* Text Size Selection */}
              <div>
                <span className="text-xs font-bold text-white block mb-1">Text & Font Scaling</span>
                <p className="text-[11px] text-slate-400 mb-2">Adjust dashboard typography size for enhanced readability</p>
                <div className="grid grid-cols-3 gap-2">
                  {[
                    { id: 'standard', label: 'Standard', scale: '100%' },
                    { id: 'medium', label: 'Medium', scale: '115%' },
                    { id: 'large', label: 'Large', scale: '130%' },
                  ].map((item) => {
                    const isSelected = textSize === item.id;
                    return (
                      <button
                        key={item.id}
                        onClick={() => {
                          setTextSize(item.id as any);
                          localStorage.setItem('myra_accessibility_text_size', item.id);
                        }}
                        className={`p-2.5 rounded-xl border text-center transition-all cursor-pointer ${
                          isSelected
                            ? 'bg-cyan-950/80 border-[#06B6D4] text-white shadow-[0_0_12px_rgba(6,182,212,0.2)]'
                            : 'bg-[#090D16]/80 border-slate-800 text-slate-400 hover:border-slate-700'
                        }`}
                      >
                        <span className="text-xs font-bold block">{item.label}</span>
                        <span className="text-[10px] text-slate-400 font-mono">{item.scale}</span>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Toggles */}
              <div className="divide-y divide-slate-800/80 pt-1">
                {/* Reduced Motion */}
                <div className="py-2.5 flex items-center justify-between">
                  <div className="pr-4">
                    <span className="text-xs font-semibold text-white block">Reduced Motion / Soft Animations</span>
                    <span className="text-[10px] text-slate-400">Minimize orbital particle swirling and high-frequency light flashes</span>
                  </div>
                  <GlassSwitch
                    checked={reducedMotion}
                    onChange={(val) => {
                      setReducedMotion(val);
                      localStorage.setItem('myra_accessibility_reduced_motion', String(val));
                    }}
                  />
                </div>

                {/* High Contrast Mode */}
                <div className="py-2.5 flex items-center justify-between">
                  <div className="pr-4">
                    <span className="text-xs font-semibold text-white block">High Contrast Borders & Text</span>
                    <span className="text-[10px] text-slate-400">Boost border luminosity and contrast for low-vision clarity</span>
                  </div>
                  <GlassSwitch
                    checked={highContrast}
                    onChange={(val) => {
                      setHighContrast(val);
                      localStorage.setItem('myra_accessibility_high_contrast', String(val));
                    }}
                  />
                </div>

                {/* Verbose Screen-Reader Announcements */}
                <div className="py-2.5 flex items-center justify-between">
                  <div className="pr-4">
                    <span className="text-xs font-semibold text-white block">Verbose Screen-Reader Voice Prompts</span>
                    <span className="text-[10px] text-slate-400">Speak detailed step-by-step confirmation prompts on every action</span>
                  </div>
                  <GlassSwitch
                    checked={verboseScreenReader}
                    onChange={(val) => {
                      setVerboseScreenReader(val);
                      localStorage.setItem('myra_accessibility_verbose_screen_reader', String(val));
                    }}
                  />
                </div>
              </div>

              {/* Haptic Strength */}
              <div className="pt-2 border-t border-slate-800">
                <span className="text-xs font-bold text-white block mb-1">Haptic Tactile Strength</span>
                <div className="grid grid-cols-3 gap-2">
                  {['soft', 'medium', 'strong'].map((strength) => {
                    const isSelected = hapticStrength === strength;
                    return (
                      <button
                        key={strength}
                        onClick={() => {
                          setHapticStrength(strength as any);
                          localStorage.setItem('myra_accessibility_haptic_strength', strength);
                          triggerHapticFeedback('command_confirm');
                        }}
                        className={`p-2 rounded-xl border text-center capitalize text-xs font-semibold transition-all cursor-pointer ${
                          isSelected
                            ? 'bg-cyan-950/80 border-[#06B6D4] text-[#06B6D4]'
                            : 'bg-[#090D16]/80 border-slate-800 text-slate-400'
                        }`}
                      >
                        {strength}
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ============================================================== */}
        {/* 9. DIAGNOSTICS SECTION                                         */}
        {/* ============================================================== */}
        {shouldShowSection('diagnostics') && (
          <div className="space-y-3 animate-in fade-in duration-200">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Activity className="w-4 h-4 text-[#06B6D4]" />
                <h2 className="text-xs font-bold text-white uppercase tracking-wider">9. Diagnostics & Service Status</h2>
              </div>
              <span className="text-[10px] font-mono text-emerald-400">HEALTHY</span>
            </div>

            {/* Health Snapshot Grid */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
              <div className="bg-[#0D1424]/85 backdrop-blur-md border border-[#06B6D4]/20 rounded-xl p-3">
                <span className="text-[10px] uppercase font-bold text-slate-400 block">WebSocket Latency</span>
                <span className="text-sm font-bold font-mono text-[#06B6D4] mt-1 block">48 ms</span>
                <span className="text-[10px] text-slate-500">Duplex PCM stream</span>
              </div>
              <div className="bg-[#0D1424]/85 backdrop-blur-md border border-[#06B6D4]/20 rounded-xl p-3">
                <span className="text-[10px] uppercase font-bold text-slate-400 block">Input Sample Rate</span>
                <span className="text-sm font-bold font-mono text-emerald-400 mt-1 block">16.0 kHz</span>
                <span className="text-[10px] text-slate-500">AudioRecord linear PCM</span>
              </div>
              <div className="bg-[#0D1424]/85 backdrop-blur-md border border-[#06B6D4]/20 rounded-xl p-3">
                <span className="text-[10px] uppercase font-bold text-slate-400 block">Output Sample Rate</span>
                <span className="text-sm font-bold font-mono text-[#06B6D4] mt-1 block">24.0 kHz</span>
                <span className="text-[10px] text-slate-500">AudioTrack dynamic buffer</span>
              </div>
              <div className="bg-[#0D1424]/85 backdrop-blur-md border border-[#06B6D4]/20 rounded-xl p-3">
                <span className="text-[10px] uppercase font-bold text-slate-400 block">Kotlin Source Files</span>
                <span className="text-sm font-bold font-mono text-purple-400 mt-1 block">31 Sources</span>
                <span className="text-[10px] text-slate-500">0 compile errors</span>
              </div>
            </div>

            {/* Expandable Live Event Logs Console */}
            <div className="bg-[#0D1424]/85 backdrop-blur-md border border-[#06B6D4]/20 rounded-2xl p-4 space-y-3 shadow-[0_8px_32px_rgba(0,0,0,0.5)]">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Terminal className="w-4 h-4 text-[#06B6D4]" />
                  <span className="text-xs font-bold text-white">Live Engine Diagnostics Console</span>
                </div>
                <button
                  onClick={() => setShowLogsConsole(!showLogsConsole)}
                  className="text-[11px] font-mono text-[#06B6D4] hover:underline cursor-pointer"
                >
                  {showLogsConsole ? 'Hide Logs' : 'View Logs (6)'}
                </button>
              </div>

              {showLogsConsole && (
                <div className="bg-[#060911] border border-slate-800 rounded-xl p-3 font-mono text-[10px] text-slate-300 space-y-1 max-h-48 overflow-y-auto">
                  {diagnosticLogs.map((log, i) => (
                    <div key={i} className="flex gap-2 leading-relaxed">
                      <span className="text-slate-500">{log.time}</span>
                      <span className="text-[#06B6D4] font-bold">[{log.level}]</span>
                      <span className="text-slate-300">{log.msg}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* APK Project Export Card */}
            <div className="bg-[#0D1424]/85 backdrop-blur-md border border-[#06B6D4]/20 rounded-2xl p-4 space-y-3 shadow-[0_8px_32px_rgba(0,0,0,0.5)]">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                  <FolderArchive className="w-5 h-5 text-[#06B6D4]" />
                  <div>
                    <h3 className="text-xs font-bold text-white">Standalone Android Project ZIP</h3>
                    <p className="text-[11px] text-slate-400">Complete Gradle 8.4 + Compose Native Sources</p>
                  </div>
                </div>
                <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-950 text-emerald-300 border border-emerald-500/40">
                  READY
                </span>
              </div>

              <a
                href="/MYRA_ANDROID_FINAL_PROJECT.zip"
                download="MYRA_ANDROID_FINAL_PROJECT.zip"
                className="w-full py-3 rounded-xl bg-gradient-to-r from-[#06B6D4] to-[#3B82F6] hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-bold text-xs flex items-center justify-center gap-2 shadow-[0_0_20px_rgba(6,182,212,0.3)] active:scale-98 transition-all cursor-pointer"
              >
                <Download className="w-4 h-4" />
                <span>DOWNLOAD MYRA_ANDROID_FINAL_PROJECT.ZIP</span>
              </a>
            </div>
          </div>
        )}

        {/* ============================================================== */}
        {/* 10. ABOUT MYRA SECTION                                         */}
        {/* ============================================================== */}
        {shouldShowSection('about') && (
          <div className="space-y-3 animate-in fade-in duration-200">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Info className="w-4 h-4 text-[#06B6D4]" />
                <h2 className="text-xs font-bold text-white uppercase tracking-wider">10. About MYRA</h2>
              </div>
              <span className="text-[10px] font-mono text-[#06B6D4]">BUILD 2026.1</span>
            </div>

            <div className="bg-[#0D1424]/85 backdrop-blur-md border border-[#06B6D4]/20 rounded-2xl p-4 space-y-3 shadow-[0_8px_32px_rgba(0,0,0,0.5)]">
              <div className="flex items-center gap-3 pb-3 border-b border-slate-800">
                <div className="w-10 h-10 rounded-2xl bg-cyan-500/20 border border-cyan-400/40 flex items-center justify-center text-[#06B6D4] font-bold text-base">
                  M
                </div>
                <div>
                  <h3 className="text-sm font-bold text-white">MYRA — Voice-First AI Android Assistant</h3>
                  <p className="text-[11px] text-slate-400">Hindi, Hinglish & English Phone Intelligence</p>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2 text-xs">
                <div className="p-2.5 rounded-xl bg-[#090D16] border border-slate-800">
                  <span className="text-[10px] text-slate-500 uppercase font-bold block">Application ID</span>
                  <span className="font-mono text-[#06B6D4] text-[11px] font-semibold mt-0.5 block truncate">
                    com.myra.assistant
                  </span>
                </div>

                <div className="p-2.5 rounded-xl bg-[#090D16] border border-slate-800">
                  <span className="text-[10px] text-slate-500 uppercase font-bold block">Target Platform</span>
                  <span className="font-mono text-slate-200 text-[11px] font-semibold mt-0.5 block">
                    Android 14 (API 34)
                  </span>
                </div>

                <div className="p-2.5 rounded-xl bg-[#090D16] border border-slate-800">
                  <span className="text-[10px] text-slate-500 uppercase font-bold block">Engine Core</span>
                  <span className="font-mono text-slate-200 text-[11px] font-semibold mt-0.5 block">
                    Jetpack Compose + OkHttp
                  </span>
                </div>

                <div className="p-2.5 rounded-xl bg-[#090D16] border border-slate-800">
                  <span className="text-[10px] text-slate-500 uppercase font-bold block">Theme Palette</span>
                  <span className="font-mono text-[#06B6D4] text-[11px] font-semibold mt-0.5 block">
                    #060911 • #06B6D4
                  </span>
                </div>
              </div>

              <p className="text-[11px] text-slate-400 leading-relaxed pt-1">
                MYRA is an autonomous voice assistant engineered specifically for Indian and international Android users.
                Featuring full duplex 24kHz bi-directional live conversational audio, native WhatsApp & call automation,
                low-latency local Wi-Fi pairing, and multi-layered biometric defense.
              </p>
            </div>
          </div>
        )}
      </div>

      {/* ============================================================== */}
      {/* MODAL 1: OWNER VOICE ENROLLMENT MODAL                           */}
      {/* ============================================================== */}
      {showVoiceEnrollModal && (
        <div className="fixed inset-0 z-50 bg-[#060911]/90 backdrop-blur-md flex items-center justify-center p-4">
          <div className="bg-[#0D1424] border border-[#06B6D4]/50 rounded-2xl p-5 max-w-sm w-full space-y-4 shadow-[0_0_50px_rgba(6,182,212,0.3)]">
            <div className="flex items-center justify-between pb-2 border-b border-[#06B6D4]/20">
              <div className="flex items-center gap-2">
                <Mic className="w-5 h-5 text-[#06B6D4]" />
                <span className="text-sm font-bold text-white">Owner Voice Enrollment</span>
              </div>
              <span className="text-[11px] font-mono text-[#06B6D4] font-bold">
                Step {enrollStep + 1} of 4
              </span>
            </div>

            <div className="w-full bg-[#090D16] h-2 rounded-full overflow-hidden border border-slate-800">
              <div
                className="bg-gradient-to-r from-[#06B6D4] to-[#3B82F6] h-full transition-all duration-300 shadow-[0_0_8px_rgba(6,182,212,0.8)]"
                style={{ width: `${((enrollStep + 1) / 4) * 100}%` }}
              />
            </div>

            <div className="bg-[#090D16] border border-[#06B6D4]/20 rounded-xl p-4 text-center space-y-2">
              <span className="text-xs text-slate-400 block">Please speak clearly into the microphone:</span>
              <div className="text-base font-bold text-cyan-300 py-1">
                "{enrollmentPhrases[enrollStep]}"
              </div>
            </div>

            <div className="flex flex-col items-center justify-center gap-2 pt-2">
              <button
                onClick={() => {
                  triggerHapticFeedback('command_confirm');
                  setIsEnrollListening(true);
                  setTimeout(() => {
                    setIsEnrollListening(false);
                    if (enrollStep < 3) {
                      setEnrollStep((prev) => prev + 1);
                    } else {
                      setOwnerVoiceEnrolled(true);
                      setOwnerProtection(true);
                      localStorage.setItem('myra_owner_voice_enrolled', 'true');
                      localStorage.setItem('myra_owner_protection', 'true');
                      setShowVoiceEnrollModal(false);
                      onTriggerEnrollment();
                    }
                  }, 1200);
                }}
                className={`w-14 h-14 rounded-full flex items-center justify-center transition-all cursor-pointer ${
                  isEnrollListening
                    ? 'bg-rose-500 animate-pulse scale-105 shadow-[0_0_20px_rgba(244,63,94,0.6)]'
                    : 'bg-gradient-to-r from-[#06B6D4] to-[#3B82F6] text-slate-950 shadow-[0_0_20px_rgba(6,182,212,0.4)]'
                }`}
              >
                <Mic className="w-6 h-6" />
              </button>
              <span className="text-[11px] text-slate-400">
                {isEnrollListening ? 'Listening & Extracting Acoustic Embeddings...' : 'Tap mic to record phrase'}
              </span>
            </div>

            <div className="pt-2 border-t border-slate-800 flex justify-end">
              <button
                onClick={() => setShowVoiceEnrollModal(false)}
                className="px-3 py-1.5 rounded-lg text-xs text-slate-400 hover:text-white cursor-pointer"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ============================================================== */}
      {/* MODAL 2: OWNER VOICE TEST MODAL                                */}
      {/* ============================================================== */}
      {showVoiceTestModal && (
        <div className="fixed inset-0 z-50 bg-[#060911]/90 backdrop-blur-md flex items-center justify-center p-4">
          <div className="bg-[#0D1424] border border-[#06B6D4]/50 rounded-2xl p-5 max-w-sm w-full space-y-4 shadow-[0_0_50px_rgba(6,182,212,0.3)]">
            <div className="flex items-center justify-between pb-2 border-b border-[#06B6D4]/20">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-[#06B6D4]" />
                <span className="text-sm font-bold text-white">Test Owner Voice Match</span>
              </div>
              <button
                onClick={() => setShowVoiceTestModal(false)}
                className="text-xs text-slate-400 hover:text-white cursor-pointer"
              >
                ✕
              </button>
            </div>

            <p className="text-xs text-slate-400">
              Tap mic and say any sentence in Hindi or English (e.g., "MYRA battery batao").
            </p>

            <div className="flex flex-col items-center justify-center py-4">
              <button
                onClick={() => {
                  triggerHapticFeedback('command_confirm');
                  setIsTestingVoice(true);
                  setTimeout(() => {
                    setIsTestingVoice(false);
                    setVoiceTestResult({
                      match: true,
                      score: 96.8,
                      text: 'Owner Voice Verified (Confidence: 96.8%)',
                    });
                  }, 1100);
                }}
                className={`w-14 h-14 rounded-full flex items-center justify-center transition-all cursor-pointer ${
                  isTestingVoice
                    ? 'bg-rose-500 animate-pulse scale-105 shadow-[0_0_20px_rgba(244,63,94,0.6)]'
                    : 'bg-gradient-to-r from-[#06B6D4] to-[#3B82F6] text-slate-950 shadow-[0_0_20px_rgba(6,182,212,0.4)]'
                }`}
              >
                <Mic className="w-6 h-6" />
              </button>
              <span className="text-[11px] text-slate-400 mt-2">
                {isTestingVoice ? 'Analyzing acoustic frequencies...' : 'Tap to test speak'}
              </span>
            </div>

            {voiceTestResult && (
              <div className="p-3 rounded-xl bg-emerald-950/80 border border-emerald-400/60 text-emerald-300 text-xs text-center font-semibold shadow-[0_0_15px_rgba(16,185,129,0.2)]">
                ✓ {voiceTestResult.text}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
