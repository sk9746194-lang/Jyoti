/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from 'react';
import { 
  Phone, 
  MessageSquare, 
  Music, 
  Cpu, 
  FileText, 
  Sparkles, 
  Bell, 
  MapPin, 
  Laptop, 
  Globe, 
  ShieldAlert, 
  Share2, 
  Volume2, 
  Search,
  ExternalLink,
  ChevronRight,
  Play,
  CheckCircle,
  Copy,
  Flame,
  BarChart3,
  Target,
  Clock,
  Mic,
  Activity,
  Zap
} from 'lucide-react';
import { DailyGoalTracker } from './DailyGoalTracker';

interface ToolsTabProps {
  onExecuteToolCommand: (cmd: string) => void;
  onOpenIncomingCall: () => void;
  onOpenEmergencySos: () => void;
  onOpenVisionCamera: () => void;
  onLockScreen: () => void;
  torchOn: boolean;
  onToggleTorch: () => void;
  personality?: string;
  onOpenMyraConnect?: () => void;
  onOpenWallpaper?: () => void;
}

const POPULAR_HINDI_COMMANDS = [
  { text: 'Live Wallpaper set karo', category: 'Wallpaper', icon: '🌌' },
  { text: 'दूसरे फोन पर YouTube खोलो', category: 'MYRA Connect', icon: '📱' },
  { text: 'घर वाले फोन पर Camera खोलो', category: 'MYRA Connect', icon: '📷' },
  { text: 'दूसरे फोन पर टॉर्च जलाओ', category: 'MYRA Connect', icon: '🔦' },
  { text: 'Arijit Singh ka gaana chalao', category: 'Media', icon: '🎵' },
  { text: 'Rahul ko WhatsApp pe bolo main late hoon', category: 'WhatsApp', icon: '💬' },
  { text: 'Papa ko call karo', category: 'Call', icon: '📞' },
  { text: '10 minute ka timer laga do', category: 'Device', icon: '⏱️' },
  { text: 'Torch on karo', category: 'Device', icon: '🔦' },
  { text: 'Phone slow kyun hai', category: 'Health', icon: '⚡' },
  { text: 'Gaadi kahan park ki thi', category: 'Maps', icon: '📍' },
  { text: 'OTP batao', category: 'Safety Guard', icon: '🛡️' },
  { text: 'Lock kar do', category: 'Safety', icon: '🔒' },
  { text: 'Aaj ke goals kya hain', category: 'Habits', icon: '🎯' },
  { text: 'Plan my trip to Delhi', category: 'Missions', icon: '🚀' },
  { text: 'Electric cars pe research karo', category: 'Smart Tools', icon: '🔬' },
];

export const ToolsTab: React.FC<ToolsTabProps> = ({
  onExecuteToolCommand,
  onOpenIncomingCall,
  onOpenEmergencySos,
  onOpenVisionCamera,
  onLockScreen,
  torchOn,
  onToggleTorch,
  personality,
  onOpenMyraConnect,
  onOpenWallpaper,
}) => {
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [copiedCmd, setCopiedCmd] = useState<string | null>(null);

  const handleCopy = (text: string) => {
    navigator.clipboard?.writeText(text);
    setCopiedCmd(text);
    setTimeout(() => setCopiedCmd(null), 1500);
  };

  const categories = [
    { id: 'all', label: 'All 16 Tools' },
    { id: 'wallpaper', label: '🌌 Live Wallpaper' },
    { id: 'goals', label: '🎯 Daily Goals & Habits' },
    { id: 'popular', label: '🔥 Popular Hindi Commands' },
    { id: 'analytics', label: '📊 Voice Analytics' },
    { id: 'comm', label: '1. Communication' },
    { id: 'call', label: '2. Calls & Safety' },
    { id: 'media', label: '3. Media' },
    { id: 'device', label: '4. Device' },
    { id: 'files', label: '5. Files & Vision' },
    { id: 'smart', label: '6. Smart / GenAI' },
    { id: 'notif', label: '8. Notifications' },
    { id: 'maps', label: '9. Maps & World' },
    { id: 'pc', label: '10. PC Connect' },
    { id: 'connect', label: '13. Connectors' },
  ];

  return (
    <div className="flex-1 flex flex-col p-4 bg-[#090d16] text-slate-100 select-none overflow-y-auto">
      {/* Header */}
      <div className="mb-3">
        <h2 className="text-base font-bold text-white tracking-tight flex items-center gap-2">
          <span>MYRA Tools & Features Catalog</span>
          <span className="text-[10px] font-mono bg-blue-900/60 border border-blue-500/30 text-blue-300 px-2 py-0.5 rounded">
            2026 Edition
          </span>
        </h2>
        <p className="text-xs text-slate-400 mt-0.5">
          Daily habits, popular commands, voice analytics aur 16 core phone automation tools.
        </p>
      </div>

      {/* Category Filter Pills */}
      <div className="flex gap-1.5 overflow-x-auto pb-2 scrollbar-none mb-3">
        {categories.map((cat) => (
          <button
            key={cat.id}
            onClick={() => setSelectedCategory(cat.id)}
            className={`px-3 py-1 rounded-full text-xs font-medium whitespace-nowrap transition-all ${
              selectedCategory === cat.id
                ? 'bg-blue-600 text-white shadow-md shadow-blue-900/40'
                : 'bg-slate-900 text-slate-400 hover:text-white border border-slate-800'
            }`}
          >
            {cat.label}
          </button>
        ))}
      </div>

      {/* Tool Cards Grid */}
      <div className="space-y-4 pb-6">
        {/* MYRA Connect — Phone-to-Phone Interconnect Feature Card */}
        <div
          onClick={onOpenMyraConnect}
          className="cursor-pointer rounded-2xl p-4 bg-gradient-to-r from-slate-900 via-cyan-950/40 to-slate-900 border border-cyan-500/40 hover:border-cyan-400 transition-all shadow-lg shadow-cyan-500/10 group"
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-xl bg-cyan-500/20 border border-cyan-500/40 flex items-center justify-center text-cyan-400 group-hover:scale-105 transition-transform">
                <Laptop className="w-6 h-6" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-sm font-bold text-white group-hover:text-cyan-300 transition-colors">
                    MYRA Connect — Phone to Phone
                  </h3>
                  <span className="text-[9px] font-mono font-bold uppercase px-2 py-0.5 rounded-full bg-cyan-500/20 text-cyan-400 border border-cyan-500/40">
                    Active
                  </span>
                </div>
                <p className="text-xs text-slate-300 mt-0.5">
                  घर के दूसरे Android फोन से सुरक्षित जोड़ें और वॉइस कमांड से WhatsApp, YouTube, Camera चलाएं।
                </p>
                <div className="flex items-center gap-2 mt-1.5 text-[11px] text-cyan-400 font-medium">
                  <span>Pair with 6-Digit Code / QR</span>
                  <span>•</span>
                  <span>Independent Permissions</span>
                </div>
              </div>
            </div>
            <ChevronRight className="w-5 h-5 text-cyan-400 group-hover:translate-x-1 transition-transform shrink-0" />
          </div>
        </div>

        {/* MYRA Live Wallpaper Feature Card */}
        <div
          onClick={onOpenWallpaper}
          className="cursor-pointer rounded-2xl p-4 bg-gradient-to-r from-slate-900 via-purple-950/40 to-slate-900 border border-purple-500/40 hover:border-purple-400 transition-all shadow-lg shadow-purple-500/10 group"
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-xl bg-purple-500/20 border border-purple-500/40 flex items-center justify-center text-purple-400 group-hover:scale-105 transition-transform">
                <Sparkles className="w-6 h-6" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-sm font-bold text-white group-hover:text-purple-300 transition-colors">
                    MYRA Live Wallpaper
                  </h3>
                  <span className="text-[9px] font-mono font-bold uppercase px-2 py-0.5 rounded-full bg-purple-500/20 text-purple-300 border border-purple-500/40">
                    9:16 3D Hologram
                  </span>
                </div>
                <p className="text-xs text-slate-300 mt-0.5">
                  Rotating Holographic Earth, HUD rings, neon AI orb, particle sparks aur radial app galaxy.
                </p>
                <div className="flex items-center gap-2 mt-1.5 text-[11px] text-purple-400 font-medium">
                  <span>Native WallpaperService</span>
                  <span>•</span>
                  <span>Dual Blue + Red Neon</span>
                  <span>•</span>
                  <span>60 FPS</span>
                </div>
              </div>
            </div>
            <ChevronRight className="w-5 h-5 text-purple-400 group-hover:translate-x-1 transition-transform shrink-0" />
          </div>
        </div>

        {/* Module A: Daily Goals & Habit Tracker */}
        {(selectedCategory === 'all' || selectedCategory === 'goals') && (
          <div className="space-y-2">
            <div className="flex items-center justify-between px-1">
              <span className="text-xs font-bold text-white flex items-center gap-1.5">
                <Target className="w-4 h-4 text-emerald-400" />
                Daily Goals & Habit Tracker
              </span>
              <span className="text-[10px] text-emerald-400 font-mono">Real-time Progress</span>
            </div>
            <DailyGoalTracker
              onSpeakStatus={onExecuteToolCommand}
              personality={personality}
            />
          </div>
        )}

        {/* Module B: Popular Hindi Commands Hub */}
        {(selectedCategory === 'all' || selectedCategory === 'popular') && (
          <div className="bg-[#111726] border border-slate-800/80 rounded-2xl p-4 space-y-3">
            <div className="flex items-center justify-between border-b border-slate-800/60 pb-2.5">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg bg-amber-500/10 text-amber-400">
                  <Flame className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-bold text-xs text-white">Popular Hindi Commands (2026 Guide)</h3>
                  <p className="text-[10px] text-slate-400">Tap any command to test or speak it with MYRA</p>
                </div>
              </div>
              <span className="text-[10px] text-amber-400 font-mono">12 Quick Commands</span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {POPULAR_HINDI_COMMANDS.map((cmd, idx) => (
                <button
                  key={idx}
                  onClick={() => onExecuteToolCommand(cmd.text)}
                  className="bg-slate-900/80 hover:bg-slate-800/90 active:scale-98 border border-slate-800 hover:border-blue-500/40 p-2.5 rounded-xl flex items-center justify-between text-left transition-all group"
                >
                  <div className="flex items-center gap-2 overflow-hidden">
                    <span className="text-base shrink-0">{cmd.icon}</span>
                    <div className="truncate">
                      <span className="text-xs font-medium text-slate-200 group-hover:text-blue-300 block truncate">
                        "{cmd.text}"
                      </span>
                      <span className="text-[10px] text-slate-500">{cmd.category}</span>
                    </div>
                  </div>
                  <Play className="w-3.5 h-3.5 text-blue-400 opacity-0 group-hover:opacity-100 shrink-0 transition-opacity" />
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Module C: Voice Analytics & Latency Dashboard */}
        {(selectedCategory === 'all' || selectedCategory === 'analytics') && (
          <div className="bg-[#111726] border border-slate-800/80 rounded-2xl p-4 space-y-3">
            <div className="flex items-center justify-between border-b border-slate-800/60 pb-2.5">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg bg-indigo-500/10 text-indigo-400">
                  <BarChart3 className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-bold text-xs text-white">Voice Analytics & Diagnostics</h3>
                  <p className="text-[10px] text-slate-400">Active session specs, recognition accuracy & system health</p>
                </div>
              </div>
              <span className="text-[10px] text-indigo-400 font-mono">Live Engine</span>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-center">
              <div className="bg-slate-900/80 border border-slate-800 p-2.5 rounded-xl">
                <span className="text-[10px] text-slate-400 block">Voice Latency</span>
                <span className="text-sm font-bold text-emerald-400 font-mono">Real-time Live</span>
              </div>
              <div className="bg-slate-900/80 border border-slate-800 p-2.5 rounded-xl">
                <span className="text-[10px] text-slate-400 block">Recognition Accuracy</span>
                <span className="text-sm font-bold text-blue-400 font-mono">Measured on Device</span>
              </div>
              <div className="bg-slate-900/80 border border-slate-800 p-2.5 rounded-xl">
                <span className="text-[10px] text-slate-400 block">Audio Pipeline</span>
                <span className="text-sm font-bold text-indigo-400 font-mono">24 kHz PCM</span>
              </div>
              <div className="bg-slate-900/80 border border-slate-800 p-2.5 rounded-xl">
                <span className="text-[10px] text-slate-400 block">Battery Overhead</span>
                <span className="text-sm font-bold text-teal-400 font-mono">Active Monitoring</span>
              </div>
            </div>
          </div>
        )}

        {/* 1. Communication Tools */}
        {(selectedCategory === 'all' || selectedCategory === 'comm') && (
          <div className="bg-[#111726] border border-slate-800/80 rounded-2xl p-3.5 space-y-3">
            <div className="flex items-center justify-between border-b border-slate-800/60 pb-2">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg bg-emerald-500/10 text-emerald-400">
                  <MessageSquare className="w-4 h-4" />
                </div>
                <h3 className="font-bold text-xs text-white">1. Communication Tools</h3>
              </div>
              <span className="text-[10px] text-slate-400 font-mono">WhatsApp, SMS, SOS</span>
            </div>

            {/* send_whatsapp */}
            <div className="bg-slate-900/70 p-2.5 rounded-xl border border-slate-800/60 flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <span className="font-mono text-xs text-blue-400 font-semibold">send_whatsapp</span>
                <button
                  onClick={() => onExecuteToolCommand('Rahul ko WhatsApp pe bolo main late hoon')}
                  className="px-2 py-0.5 bg-blue-600 hover:bg-blue-500 text-white rounded text-[10px] font-semibold flex items-center gap-1 active:scale-95"
                >
                  <Play className="w-2.5 h-2.5" /> Test
                </button>
              </div>
              <p className="text-[11px] text-slate-300">
                Contact ka WhatsApp chat kholta hai, message likhta hai aur confirmation mangta hai.
              </p>
              <div className="flex items-center justify-between bg-black/40 px-2.5 py-1 rounded text-[11px] font-mono text-emerald-400">
                <span>"Rahul ko WhatsApp pe bolo main late hoon"</span>
                <button onClick={() => handleCopy('Rahul ko WhatsApp pe bolo main late hoon')} className="text-slate-500 hover:text-slate-300">
                  <Copy className="w-3 h-3" />
                </button>
              </div>
            </div>

            {/* Emergency SOS Alert */}
            <div className="bg-rose-950/20 p-2.5 rounded-xl border border-rose-500/30 flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <span className="font-mono text-xs text-rose-400 font-semibold">send_emergency_alert</span>
                <button
                  onClick={onOpenEmergencySos}
                  className="px-2 py-0.5 bg-rose-600 hover:bg-rose-500 text-white rounded text-[10px] font-semibold flex items-center gap-1 active:scale-95"
                >
                  <ShieldAlert className="w-2.5 h-2.5" /> Trigger SOS
                </button>
              </div>
              <p className="text-[11px] text-rose-200">
                Aapke trusted contact ko location ke saath SOS alert bhejta hai — sirf emergency ke liye.
              </p>
              <div className="bg-black/40 px-2.5 py-1 rounded text-[11px] font-mono text-rose-300">
                "SOS", "Main khatre mein hoon", "Madad chahiye"
              </div>
            </div>
          </div>
        )}

        {/* 2. Call Tools */}
        {(selectedCategory === 'all' || selectedCategory === 'call') && (
          <div className="bg-[#111726] border border-slate-800/80 rounded-2xl p-3.5 space-y-3">
            <div className="flex items-center justify-between border-b border-slate-800/60 pb-2">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg bg-blue-500/10 text-blue-400">
                  <Phone className="w-4 h-4" />
                </div>
                <h3 className="font-bold text-xs text-white">2. Call Tools & Safety Guard</h3>
              </div>
              <span className="text-[10px] text-amber-400 font-semibold font-mono">20s Reject Guard</span>
            </div>

            {/* call_contact */}
            <div className="bg-slate-900/70 p-2.5 rounded-xl border border-slate-800/60 flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <span className="font-mono text-xs text-blue-400 font-semibold">call_contact</span>
                <button
                  onClick={() => onExecuteToolCommand('Papa ko call karo')}
                  className="px-2 py-0.5 bg-blue-600 hover:bg-blue-500 text-white rounded text-[10px] font-semibold flex items-center gap-1 active:scale-95"
                >
                  <Play className="w-2.5 h-2.5" /> Call Papa
                </button>
              </div>
              <p className="text-[11px] text-slate-300">Saved contact ya phone number pe call lagata hai.</p>
              <div className="bg-black/40 px-2.5 py-1 rounded text-[11px] font-mono text-emerald-400">
                "Papa ko call karo"
              </div>
            </div>

            {/* answer_call / end_call SAFETY GUARD */}
            <div className="bg-amber-950/20 p-2.5 rounded-xl border border-amber-500/30 flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5">
                  <span className="font-mono text-xs text-amber-300 font-semibold">end_call</span>
                  <span className="text-[9px] bg-rose-900/80 text-rose-200 px-1.5 py-0.2 rounded font-bold">
                    SAFETY GUARD
                  </span>
                </div>
                <button
                  onClick={onOpenIncomingCall}
                  className="px-2 py-0.5 bg-amber-600 hover:bg-amber-500 text-white rounded text-[10px] font-semibold flex items-center gap-1 active:scale-95"
                >
                  Simulate Call
                </button>
              </div>
              <p className="text-[11px] text-amber-200">
                Call kaatta/reject karta hai. Ringing call sirf tabhi kaatega jab aapne 20 second ke andar clearly bola ho — galti se misheard word pe hang up nahi karega.
              </p>
              <div className="bg-black/40 px-2.5 py-1 rounded text-[11px] font-mono text-amber-300">
                "Utha lo" ya "Call kaat do" / "Reject karo"
              </div>
            </div>
          </div>
        )}

        {/* 3. Media Tools */}
        {(selectedCategory === 'all' || selectedCategory === 'media') && (
          <div className="bg-[#111726] border border-slate-800/80 rounded-2xl p-3.5 space-y-3">
            <div className="flex items-center justify-between border-b border-slate-800/60 pb-2">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg bg-indigo-500/10 text-indigo-400">
                  <Music className="w-4 h-4" />
                </div>
                <h3 className="font-bold text-xs text-white">3. Media Tools</h3>
              </div>
              <span className="text-[10px] text-slate-400 font-mono">YouTube Music / Spotify</span>
            </div>

            <div className="bg-slate-900/70 p-2.5 rounded-xl border border-slate-800/60 flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <span className="font-mono text-xs text-blue-400 font-semibold">play_music</span>
                <button
                  onClick={() => onExecuteToolCommand('Arijit Singh ka gaana chalao')}
                  className="px-2 py-0.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded text-[10px] font-semibold flex items-center gap-1 active:scale-95"
                >
                  <Play className="w-2.5 h-2.5" /> Play Song
                </button>
              </div>
              <p className="text-[11px] text-slate-300">
                Song/album/artist search karke auto-play karta hai.
              </p>
              <div className="bg-black/40 px-2.5 py-1 rounded text-[11px] font-mono text-emerald-400">
                "Arijit Singh ka gaana chalao", "Agla gaana", "Volume 50% kar do"
              </div>
            </div>
          </div>
        )}

        {/* 4. Device Tools & Urgent Lock */}
        {(selectedCategory === 'all' || selectedCategory === 'device') && (
          <div className="bg-[#111726] border border-slate-800/80 rounded-2xl p-3.5 space-y-3">
            <div className="flex items-center justify-between border-b border-slate-800/60 pb-2">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg bg-amber-500/10 text-amber-400">
                  <Cpu className="w-4 h-4" />
                </div>
                <h3 className="font-bold text-xs text-white">4. Device Tools</h3>
              </div>
              <span className="text-[10px] text-slate-400 font-mono">Torch, Lock, Timer, Storage</span>
            </div>

            {/* lock_device (Immediate No Confirm) */}
            <div className="bg-slate-900/70 p-2.5 rounded-xl border border-slate-800/60 flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5">
                  <span className="font-mono text-xs text-rose-400 font-semibold">lock_device</span>
                  <span className="text-[9px] bg-rose-950 text-rose-300 px-1 rounded font-bold border border-rose-500/30">
                    NO CONFIRM
                  </span>
                </div>
                <button
                  onClick={onLockScreen}
                  className="px-2 py-0.5 bg-rose-600 hover:bg-rose-500 text-white rounded text-[10px] font-semibold active:scale-95"
                >
                  Test Lock
                </button>
              </div>
              <p className="text-[11px] text-slate-300">
                Turant screen lock kar deta hai — urgent safety command, bina poochhe execute hota hai.
              </p>
              <div className="bg-black/40 px-2.5 py-1 rounded text-[11px] font-mono text-rose-300">
                "Lock kar do", "Phone band kar do"
              </div>
            </div>

            {/* Flashlight toggle */}
            <div className="bg-slate-900/70 p-2.5 rounded-xl border border-slate-800/60 flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <span className="font-mono text-xs text-blue-400 font-semibold">toggle_flashlight</span>
                <button
                  onClick={onToggleTorch}
                  className={`px-2 py-0.5 rounded text-[10px] font-semibold active:scale-95 ${
                    torchOn ? 'bg-amber-500 text-black' : 'bg-slate-800 text-slate-300'
                  }`}
                >
                  {torchOn ? 'Torch OFF' : 'Torch ON'}
                </button>
              </div>
              <p className="text-[11px] text-slate-300">Torch on/off karta hai.</p>
              <div className="bg-black/40 px-2.5 py-1 rounded text-[11px] font-mono text-emerald-400">
                "Torch on karo", "Torch band karo"
              </div>
            </div>

            {/* Timer & Alarm */}
            <div className="bg-slate-900/70 p-2.5 rounded-xl border border-slate-800/60 flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <span className="font-mono text-xs text-blue-400 font-semibold">set_timer & set_alarm</span>
                <button
                  onClick={() => onExecuteToolCommand('10 minute ka timer laga do')}
                  className="px-2 py-0.5 bg-blue-600 hover:bg-blue-500 text-white rounded text-[10px] font-semibold active:scale-95"
                >
                  10m Timer
                </button>
              </div>
              <div className="bg-black/40 px-2.5 py-1 rounded text-[11px] font-mono text-emerald-400">
                "10 minute ka timer laga do", "Subah 6 baje alarm laga do"
              </div>
            </div>
          </div>
        )}

        {/* 5. Files, Photos & Camera Vision */}
        {(selectedCategory === 'all' || selectedCategory === 'files') && (
          <div className="bg-[#111726] border border-slate-800/80 rounded-2xl p-3.5 space-y-3">
            <div className="flex items-center justify-between border-b border-slate-800/60 pb-2">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg bg-teal-500/10 text-teal-400">
                  <FileText className="w-4 h-4" />
                </div>
                <h3 className="font-bold text-xs text-white">5. Files, Photos & Vision</h3>
              </div>
              <span className="text-[10px] text-slate-400 font-mono">OCR, Barcode, Camera</span>
            </div>

            <div className="bg-slate-900/70 p-2.5 rounded-xl border border-slate-800/60 flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <span className="font-mono text-xs text-teal-400 font-semibold">camera_vision</span>
                <button
                  onClick={onOpenVisionCamera}
                  className="px-2 py-0.5 bg-teal-600 hover:bg-teal-500 text-white rounded text-[10px] font-semibold active:scale-95"
                >
                  Open Viewfinder
                </button>
              </div>
              <p className="text-[11px] text-slate-300">
                Camera viewfinder khol kar real duniya dekhta hai — barcode scan, document padhna.
              </p>
              <div className="bg-black/40 px-2.5 py-1 rounded text-[11px] font-mono text-teal-300">
                "Ye barcode scan karo", "Ek photo le lo"
              </div>
            </div>
          </div>
        )}

        {/* 6. Smart / Generative Tools */}
        {(selectedCategory === 'all' || selectedCategory === 'smart') && (
          <div className="bg-[#111726] border border-slate-800/80 rounded-2xl p-3.5 space-y-3">
            <div className="flex items-center justify-between border-b border-slate-800/60 pb-2">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg bg-purple-500/10 text-purple-400">
                  <Sparkles className="w-4 h-4" />
                </div>
                <h3 className="font-bold text-xs text-white">6. Smart & Generative Tools</h3>
              </div>
              <span className="text-[10px] text-purple-300 font-mono">Web, Image, Health</span>
            </div>

            {/* system_health */}
            <div className="bg-slate-900/70 p-2.5 rounded-xl border border-slate-800/60 flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <span className="font-mono text-xs text-purple-400 font-semibold">system_health</span>
                <button
                  onClick={() => onExecuteToolCommand('Phone slow kyun hai')}
                  className="px-2 py-0.5 bg-purple-600 hover:bg-purple-500 text-white rounded text-[10px] font-semibold active:scale-95"
                >
                  Diagnose
                </button>
              </div>
              <p className="text-[11px] text-slate-300">
                Phone slow/hot/battery drain kyun hai — RAM, thermal, battery khane wali app diagnose karta hai.
              </p>
              <div className="bg-black/40 px-2.5 py-1 rounded text-[11px] font-mono text-purple-300">
                "Phone slow kyun hai"
              </div>
            </div>

            {/* deep_research */}
            <div className="bg-slate-900/70 p-2.5 rounded-xl border border-slate-800/60 flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <span className="font-mono text-xs text-purple-400 font-semibold">deep_research</span>
                <button
                  onClick={() => onExecuteToolCommand('Electric cars pe research karo')}
                  className="px-2 py-0.5 bg-purple-600 hover:bg-purple-500 text-white rounded text-[10px] font-semibold active:scale-95"
                >
                  Research
                </button>
              </div>
              <p className="text-[11px] text-slate-300">Web research karke summary report taiyar karta hai.</p>
              <div className="bg-black/40 px-2.5 py-1 rounded text-[11px] font-mono text-purple-300">
                "Electric cars pe research karo"
              </div>
            </div>
          </div>
        )}

        {/* 8. Notification Tools & OTP Safety */}
        {(selectedCategory === 'all' || selectedCategory === 'notif') && (
          <div className="bg-[#111726] border border-slate-800/80 rounded-2xl p-3.5 space-y-3">
            <div className="flex items-center justify-between border-b border-slate-800/60 pb-2">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg bg-orange-500/10 text-orange-400">
                  <Bell className="w-4 h-4" />
                </div>
                <h3 className="font-bold text-xs text-white">8. Notification Tools & OTP</h3>
              </div>
              <span className="text-[10px] text-rose-400 font-mono font-semibold">Explicit OTP Only</span>
            </div>

            {/* read_otp */}
            <div className="bg-rose-950/20 p-2.5 rounded-xl border border-rose-500/30 flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5">
                  <span className="font-mono text-xs text-rose-300 font-semibold">read_otp</span>
                  <span className="text-[9px] bg-rose-900 text-rose-200 px-1.5 py-0.2 rounded font-bold">
                    EXPLICIT ONLY
                  </span>
                </div>
                <button
                  onClick={() => onExecuteToolCommand('OTP batao')}
                  className="px-2 py-0.5 bg-rose-600 hover:bg-rose-500 text-white rounded text-[10px] font-semibold active:scale-95"
                >
                  Get OTP
                </button>
              </div>
              <p className="text-[11px] text-rose-200">
                Latest OTP dhund kar padhta hai — kabhi khud se automatically nahi bolta, sirf poochne pe.
              </p>
              <div className="bg-black/40 px-2.5 py-1 rounded text-[11px] font-mono text-rose-300">
                "OTP batao"
              </div>
            </div>
          </div>
        )}

        {/* 9. Maps & My World */}
        {(selectedCategory === 'all' || selectedCategory === 'maps') && (
          <div className="bg-[#111726] border border-slate-800/80 rounded-2xl p-3.5 space-y-3">
            <div className="flex items-center justify-between border-b border-slate-800/60 pb-2">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg bg-cyan-500/10 text-cyan-400">
                  <MapPin className="w-4 h-4" />
                </div>
                <h3 className="font-bold text-xs text-white">9. Maps & "My World"</h3>
              </div>
              <span className="text-[10px] text-cyan-300 font-mono">Parking, Location</span>
            </div>

            {/* get_parking_location */}
            <div className="bg-slate-900/70 p-2.5 rounded-xl border border-slate-800/60 flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <span className="font-mono text-xs text-cyan-400 font-semibold">get_parking_location</span>
                <button
                  onClick={() => onExecuteToolCommand('Gaadi kahan park ki thi')}
                  className="px-2 py-0.5 bg-cyan-600 hover:bg-cyan-500 text-white rounded text-[10px] font-semibold active:scale-95"
                >
                  Locate Car
                </button>
              </div>
              <p className="text-[11px] text-slate-300">
                Aapne gaadi kahan park ki thi, wo bataata hai (Level 2, Pillar B-14).
              </p>
              <div className="bg-black/40 px-2.5 py-1 rounded text-[11px] font-mono text-cyan-300">
                "Gaadi kahan park ki thi", "Yahan park kiya hai, yaad rakho"
              </div>
            </div>
          </div>
        )}

        {/* 10. PC Connect */}
        {(selectedCategory === 'all' || selectedCategory === 'pc') && (
          <div className="bg-[#111726] border border-slate-800/80 rounded-2xl p-3.5 space-y-3">
            <div className="flex items-center justify-between border-b border-slate-800/60 pb-2">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg bg-blue-500/10 text-blue-400">
                  <Laptop className="w-4 h-4" />
                </div>
                <h3 className="font-bold text-xs text-white">10. PC Connect (MYRA Companion)</h3>
              </div>
              <span className="text-[10px] text-slate-400 font-mono">IP + PIN Pairing</span>
            </div>

            <div className="bg-slate-900/70 p-2.5 rounded-xl border border-slate-800/60 flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <span className="font-mono text-xs text-blue-400 font-semibold">pc_command</span>
                <button
                  onClick={() => onExecuteToolCommand('PC pe ye kar do...')}
                  className="px-2 py-0.5 bg-blue-600 hover:bg-blue-500 text-white rounded text-[10px] font-semibold active:scale-95"
                >
                  Send to PC
                </button>
              </div>
              <p className="text-[11px] text-slate-300">
                MYRA Companion app chal rahe PC ko phone se control command aur files bhejta hai.
              </p>
              <div className="bg-black/40 px-2.5 py-1 rounded text-[11px] font-mono text-blue-300">
                "PC pe ye kar do...", "Ye file PC pe bhej do"
              </div>
            </div>
          </div>
        )}

        {/* 13. Connector Tools */}
        {(selectedCategory === 'all' || selectedCategory === 'connect') && (
          <div className="bg-[#111726] border border-slate-800/80 rounded-2xl p-3.5 space-y-3">
            <div className="flex items-center justify-between border-b border-slate-800/60 pb-2">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg bg-pink-500/10 text-pink-400">
                  <Globe className="w-4 h-4" />
                </div>
                <h3 className="font-bold text-xs text-white">13. Connector Tools</h3>
              </div>
              <span className="text-[10px] text-slate-400 font-mono">Google, Drive, GitHub, Canva</span>
            </div>

            <div className="grid grid-cols-2 gap-2 text-xs">
              <div className="bg-slate-900/80 p-2 rounded-xl border border-slate-800">
                <span className="font-semibold text-white block">Google Calendar</span>
                <span className="text-[10px] text-emerald-400 font-mono">Connected</span>
              </div>
              <div className="bg-slate-900/80 p-2 rounded-xl border border-slate-800">
                <span className="font-semibold text-white block">Google Drive</span>
                <span className="text-[10px] text-emerald-400 font-mono">Connected</span>
              </div>
              <div className="bg-slate-900/80 p-2 rounded-xl border border-slate-800">
                <span className="font-semibold text-white block">GitHub Repos</span>
                <span className="text-[10px] text-emerald-400 font-mono">Active</span>
              </div>
              <div className="bg-slate-900/80 p-2 rounded-xl border border-slate-800">
                <span className="font-semibold text-white block">Canva Designs</span>
                <span className="text-[10px] text-emerald-400 font-mono">Ready</span>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
