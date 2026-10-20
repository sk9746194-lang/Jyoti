/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from 'react';
import { 
  Mic, 
  MicOff, 
  Send, 
  Volume2, 
  Sparkles, 
  Radio, 
  Info, 
  AlertTriangle, 
  Cpu, 
  AudioWaveform, 
  VolumeX, 
  FolderArchive, 
  Download,
  Phone,
  MessageSquare,
  Music,
  Flashlight,
  Camera,
  ShieldAlert,
  Zap,
  ChevronRight,
  Settings
} from 'lucide-react';
import { AssistantState, OrbSettings, PersonalityMode, VoiceSettings, VoiceStatus } from '../types/myra';
import { OrbVisualizer } from './OrbVisualizer';
import { triggerHapticFeedback } from '../utils/audioEffects';

interface VoiceAssistantTabProps {
  assistantState: AssistantState;
  orbSettings: OrbSettings;
  voiceSettings: VoiceSettings;
  voiceStatus: VoiceStatus;
  liveAudioLevel: number;
  liveTranscript: string;
  assistantResponse: string;
  lastCategory: string;
  isListening: boolean;
  onStartListening: () => void;
  onStopListening: () => void;
  onExecuteCommand: (cmd: string) => void;
  onReplayAudio: () => void;
  onOpenToolTab: () => void;
  onOpenDownloadModal?: () => void;
  onOpenMyraConnect?: () => void;
  onOpenSettings?: () => void;
  micPermissionError?: string | null;
  onClearMicPermissionError?: () => void;
}

export const VoiceAssistantTab: React.FC<VoiceAssistantTabProps> = ({
  assistantState,
  orbSettings,
  voiceSettings,
  voiceStatus,
  liveAudioLevel,
  liveTranscript,
  assistantResponse,
  lastCategory,
  isListening,
  onStartListening,
  onStopListening,
  onExecuteCommand,
  onReplayAudio,
  onOpenToolTab,
  onOpenDownloadModal,
  onOpenMyraConnect,
  onOpenSettings,
  micPermissionError,
  onClearMicPermissionError,
}) => {
  const [textInput, setTextInput] = useState('');
  const [showVoiceDiag, setShowVoiceDiag] = useState(false);

  const handleTextSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!textInput.trim()) return;
    onExecuteCommand(textInput.trim());
    setTextInput('');
  };

  return (
    <div className="flex-1 flex flex-col justify-between p-4 relative select-none overflow-hidden">
      {/* Full-Screen Dynamic Edge Speaking / Listening Effect Overlay */}
      {orbSettings.edgeEffectMode !== 'off' && (assistantState === 'speaking' || assistantState === 'listening' || assistantState === 'thinking') && (
        <div className="absolute inset-0 pointer-events-none z-30 transition-opacity duration-300">
          {(orbSettings.edgeEffectMode === 'all_4_sides' || orbSettings.edgeEffectMode === 'top_bottom') && (
            <div className={`absolute top-0 left-0 right-0 h-10 bg-gradient-to-b ${
              assistantState === 'speaking' ? 'from-emerald-500/40' : assistantState === 'listening' ? 'from-rose-500/40' : 'from-amber-500/40'
            } to-transparent animate-pulse`} />
          )}
          {(orbSettings.edgeEffectMode === 'all_4_sides' || orbSettings.edgeEffectMode === 'top_bottom' || orbSettings.edgeEffectMode === 'bottom_only') && (
            <div className={`absolute bottom-0 left-0 right-0 h-12 bg-gradient-to-t ${
              assistantState === 'speaking' ? 'from-emerald-500/50' : assistantState === 'listening' ? 'from-rose-500/50' : 'from-amber-500/50'
            } to-transparent animate-pulse`} />
          )}
          {(orbSettings.edgeEffectMode === 'all_4_sides' || orbSettings.edgeEffectMode === 'left_right') && (
            <>
              <div className={`absolute top-0 bottom-0 left-0 w-8 bg-gradient-to-r ${
                assistantState === 'speaking' ? 'from-emerald-500/35' : assistantState === 'listening' ? 'from-rose-500/35' : 'from-amber-500/35'
              } to-transparent animate-pulse`} />
              <div className={`absolute top-0 bottom-0 right-0 w-8 bg-gradient-to-l ${
                assistantState === 'speaking' ? 'from-emerald-500/35' : assistantState === 'listening' ? 'from-rose-500/35' : 'from-amber-500/35'
              } to-transparent animate-pulse`} />
            </>
          )}
        </div>
      )}

      {/* 1. Header: Branding, Connection, & Gemini Live Voice Status */}
      <div className="space-y-2 z-10">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="px-2.5 py-1 rounded-full bg-blue-950/90 border border-blue-500/40 text-blue-300 text-xs font-bold tracking-wide flex items-center gap-1.5 shadow-sm shadow-blue-950/40">
              <Radio className="w-3.5 h-3.5 text-blue-400 animate-pulse" />
              <span>MYRA 2026 Edition</span>
            </div>
            <span className="text-[11px] text-slate-400 font-medium">Hinglish / Hindi</span>
          </div>

          <div className="flex items-center gap-2">
            {onOpenMyraConnect && (
              <button
                onClick={onOpenMyraConnect}
                className="flex items-center gap-1 text-[10px] font-mono px-2 py-0.5 rounded-full bg-cyan-950/80 border border-cyan-500/40 text-cyan-300 hover:border-cyan-400 transition-colors cursor-pointer"
                title="MYRA Connect Phone-to-Phone"
              >
                <span className="w-1.5 h-1.5 rounded-full bg-cyan-400 animate-pulse" />
                <span>📱 Remote</span>
              </button>
            )}
            {onOpenSettings && (
              <button
                onClick={onOpenSettings}
                className="flex items-center gap-1 text-[10px] font-mono px-2 py-0.5 rounded-full bg-[#0D1424] border border-cyan-500/30 text-slate-300 hover:text-cyan-300 hover:border-cyan-400 transition-colors cursor-pointer"
                title="Open Settings"
              >
                <Settings className="w-3 h-3 text-cyan-400" />
                <span className="hidden sm:inline">Settings</span>
              </button>
            )}
            <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
              {voiceSettings.personality === 'romantic_gf' ? 'GF Mode ❤️' : 'Assistant'}
            </span>
            <span className="flex items-center gap-1.5 text-xs text-emerald-400 font-medium">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
              Online
            </span>
          </div>
        </div>

        {/* Gemini Live AI Voice Status Badge */}
        <div className="flex items-center justify-between gap-2">
          <button
            onClick={() => setShowVoiceDiag(!showVoiceDiag)}
            className={`text-xs font-medium px-3 py-1 rounded-full border flex items-center gap-2 transition-all active:scale-95 ${
              voiceStatus.activeMode === 'gemini_live'
                ? 'bg-emerald-950/70 border-emerald-500/40 text-emerald-300 shadow-sm shadow-emerald-950/40'
                : voiceStatus.activeMode === 'gemini_tts'
                ? 'bg-blue-950/70 border-blue-500/40 text-blue-300 shadow-sm'
                : 'bg-amber-950/70 border-amber-500/40 text-amber-300 shadow-sm'
            }`}
          >
            {voiceStatus.activeMode === 'gemini_live' ? (
              <>
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                <span>Gemini Live AI Voice (Aoede • 24kHz)</span>
              </>
            ) : voiceStatus.activeMode === 'gemini_tts' ? (
              <>
                <span className="w-2 h-2 rounded-full bg-blue-400" />
                <span>Gemini Neural AI (Aoede Female Voice)</span>
              </>
            ) : (
              <>
                <AlertTriangle className="w-3.5 h-3.5 text-amber-400" />
                <span>Fallback Browser TTS Active</span>
              </>
            )}
            <Info className="w-3 h-3 text-slate-400 ml-0.5" />
          </button>

          {/* Real-time Audio Reactivity Visual Indicator */}
          {(assistantState === 'speaking' || isListening) && (
            <div className="flex items-center gap-1.5 bg-slate-900/90 border border-slate-700/70 px-2.5 py-1 rounded-full">
              <span className="text-[10px] font-mono text-slate-300">
                {assistantState === 'speaking' ? 'Voice Out' : 'Mic In'}
              </span>
              <div className="w-14 h-2 bg-slate-800 rounded-full overflow-hidden flex items-center">
                <div 
                  className="h-full bg-gradient-to-r from-blue-500 via-indigo-400 to-emerald-400 transition-all duration-75 rounded-full"
                  style={{ width: `${Math.min(100, Math.round(liveAudioLevel * 100))}%` }}
                />
              </div>
            </div>
          )}
        </div>

        {/* Real Downloadable File Card: MYRA_ANDROID_PROJECT_FULL.zip */}
        <div 
          onClick={onOpenDownloadModal}
          className="p-2.5 rounded-xl bg-gradient-to-r from-blue-950/80 via-slate-900/90 to-indigo-950/80 border border-blue-500/40 hover:border-blue-400 flex items-center justify-between shadow-lg shadow-blue-950/30 cursor-pointer transition-all active:scale-99 group"
        >
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-blue-600/20 border border-blue-500/30 flex items-center justify-center text-blue-400 group-hover:scale-105 transition-transform">
              <FolderArchive className="w-4 h-4" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xs font-bold text-white font-mono">MYRA_ANDROID_PROJECT_FULL.zip</span>
                <span className="text-[9px] font-mono px-1.5 py-0.2 rounded bg-emerald-950 text-emerald-400 border border-emerald-500/30">
                  51 Files • 84.5 KB Uncompressed
                </span>
              </div>
              <p className="text-[10px] text-slate-400">
                Verified: 31 Kotlin sources, SettingsActivity, FastCommandRouter & AudioEngine
              </p>
            </div>
          </div>
          <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-blue-600 group-hover:bg-blue-500 text-white text-xs font-bold shadow-md">
            <Download className="w-3.5 h-3.5" />
            <span>Download</span>
          </div>
        </div>

        {/* Fallback TTS Notice (Honest Architecture) */}
        {voiceStatus.fallbackActive && (
          <div className="p-2.5 rounded-xl bg-amber-950/30 border border-amber-500/30 text-amber-200 text-xs flex items-start gap-2 animate-in fade-in duration-200">
            <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
            <div className="leading-snug text-[11px]">
              <span className="font-semibold text-amber-300">Notice: </span>
              {voiceStatus.fallbackReason || 'Gemini Live real-time audio is unavailable.'} Operating in honest Fallback Text-to-Speech mode (Female Assistant Hindi voice).
            </div>
          </div>
        )}

        {/* Voice Architecture Diagnostics Drawer */}
        {showVoiceDiag && (
          <div className="p-3 bg-[#0e1422] border border-blue-500/30 rounded-2xl space-y-1.5 text-xs text-slate-300 shadow-2xl animate-in fade-in duration-150">
            <div className="flex items-center justify-between font-semibold text-white border-b border-slate-800 pb-1.5">
              <span className="flex items-center gap-1.5 text-blue-400">
                <Cpu className="w-4 h-4" />
                MYRA Voice Architecture 2026
              </span>
              <button onClick={() => setShowVoiceDiag(false)} className="text-slate-400 hover:text-white p-0.5">✕</button>
            </div>
            <div className="grid grid-cols-2 gap-2 pt-1 text-[11px] font-mono">
              <div>
                <span className="text-slate-500 block">Voice Engine:</span>
                <span className="text-slate-200 font-semibold">{voiceStatus.activeMode}</span>
              </div>
              <div>
                <span className="text-slate-500 block">Voice Persona:</span>
                <span className="text-slate-200 font-semibold">{voiceStatus.voiceName}</span>
              </div>
              <div>
                <span className="text-slate-500 block">Input Spec:</span>
                <span className="text-slate-200">{voiceStatus.sampleRateIn} Hz PCM Mono</span>
              </div>
              <div>
                <span className="text-slate-500 block">Output Spec:</span>
                <span className="text-slate-200">{voiceStatus.sampleRateOut} Hz 16-bit Float</span>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* 2. Center Stage: बड़ा Animated MYRA Orb & Voice State */}
      <div className="flex-1 flex flex-col items-center justify-center relative my-auto py-2">
        {/* Animated AI Orb Visualizer */}
        <div className="relative flex flex-col items-center justify-center">
          <OrbVisualizer
            state={assistantState}
            settings={orbSettings}
            audioLevel={liveAudioLevel}
            onClick={() => {
              triggerHapticFeedback('tap');
              if (isListening) onStopListening();
              else onStartListening();
            }}
          />

          {/* Prominent "TAP ORB TO SPEAK" / Voice State Indicator */}
          <button
            onClick={() => {
              triggerHapticFeedback('tap');
              if (isListening) onStopListening();
              else onStartListening();
            }}
            className={`mt-4 px-4 py-1.5 rounded-full text-xs font-bold tracking-wider uppercase border transition-all duration-200 flex items-center gap-2 shadow-lg active:scale-95 ${
              assistantState === 'listening'
                ? 'bg-rose-950/80 border-rose-500/60 text-rose-300 shadow-rose-950/50 ring-2 ring-rose-500/30'
                : assistantState === 'speaking'
                ? 'bg-emerald-950/80 border-emerald-500/60 text-emerald-300 shadow-emerald-950/50'
                : assistantState === 'thinking'
                ? 'bg-amber-950/80 border-amber-500/60 text-amber-300 shadow-amber-950/50'
                : 'bg-slate-900/90 hover:bg-slate-800 border-blue-500/40 text-blue-300 hover:border-blue-400 hover:text-white shadow-blue-950/30'
            }`}
          >
            <span
              className={`w-2 h-2 rounded-full ${
                assistantState === 'listening'
                  ? 'bg-rose-400 animate-ping'
                  : assistantState === 'speaking'
                  ? 'bg-emerald-400 animate-bounce'
                  : assistantState === 'thinking'
                  ? 'bg-amber-400 animate-pulse'
                  : 'bg-blue-400'
              }`}
            />
            <span>
              {assistantState === 'idle' && 'TAP ORB TO SPEAK'}
              {assistantState === 'listening' && 'Listening to you...'}
              {assistantState === 'thinking' && 'Processing...'}
              {assistantState === 'speaking' && 'MYRA Speaking...'}
            </span>
          </button>
        </div>

        {/* Dynamic Voice Speech Bubble / Transcript / छोटा Welcome Message */}
        <div className="w-full max-w-sm mt-5 px-1">
          {liveTranscript ? (
            /* Live Transcript while user is speaking */
            <div className="bg-slate-900/95 border border-indigo-500/50 rounded-2xl p-3.5 shadow-2xl backdrop-blur-md animate-in fade-in slide-in-from-bottom-2 duration-200">
              <div className="flex items-center gap-2 text-indigo-400 text-xs font-semibold mb-1">
                <span className="w-2 h-2 rounded-full bg-indigo-500 animate-ping" />
                Listening voice input:
              </div>
              <p className="text-sm font-medium text-slate-100 italic">
                "{liveTranscript}"
              </p>
            </div>
          ) : assistantResponse ? (
            /* Assistant Spoken Response Card */
            <div className="bg-[#111726]/95 border border-blue-500/30 rounded-2xl p-3.5 shadow-2xl backdrop-blur-md animate-in fade-in zoom-in-95 duration-200">
              <div className="flex items-center justify-between mb-1.5">
                <span className="text-[10px] font-semibold tracking-wider text-blue-400 uppercase bg-blue-950/60 px-2.5 py-0.5 rounded-full border border-blue-500/20">
                  {lastCategory || 'MYRA Voice Assistant'}
                </span>
                <button
                  onClick={onReplayAudio}
                  className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
                  title="Speak again"
                >
                  <Volume2 className="w-4 h-4 text-blue-300" />
                </button>
              </div>
              <p className="text-sm font-medium text-slate-100 leading-snug">
                {assistantResponse}
              </p>
            </div>
          ) : (
            /* छोटा Welcome Message */
            <div className="bg-[#111726]/80 border border-slate-800 rounded-2xl p-3.5 text-center shadow-lg backdrop-blur-sm">
              <p className="text-xs font-medium text-slate-200 flex items-center justify-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-blue-400" />
                <span>Namaste! Main MYRA hoon — aapki 2026 Edition Hindi AI Voice Assistant.</span>
              </p>
              <p className="text-[11px] text-slate-400 mt-1">
                Boliye, aaj aapke liye kya karun?
              </p>
            </div>
          )}

          {/* Quick Action Grid (Calls, WhatsApp, Media, Torch, Connect) */}
          <div className="grid grid-cols-4 gap-1.5 mt-3">
            <button
              onClick={() => onExecuteCommand('Papa ko call karo')}
              className="p-2 rounded-xl bg-slate-900/80 hover:bg-slate-800 border border-slate-800 hover:border-emerald-500/40 flex flex-col items-center justify-center text-center transition-all group active:scale-95 cursor-pointer"
              title="Papa ko call karo"
            >
              <Phone className="w-4 h-4 text-emerald-400 group-hover:scale-110 transition-transform mb-1" />
              <span className="text-[10px] text-slate-300 font-medium leading-none">Call</span>
            </button>
            <button
              onClick={() => onExecuteCommand('Rahul ko WhatsApp message bhejo')}
              className="p-2 rounded-xl bg-slate-900/80 hover:bg-slate-800 border border-slate-800 hover:border-emerald-500/40 flex flex-col items-center justify-center text-center transition-all group active:scale-95 cursor-pointer"
              title="WhatsApp Message"
            >
              <MessageSquare className="w-4 h-4 text-emerald-400 group-hover:scale-110 transition-transform mb-1" />
              <span className="text-[10px] text-slate-300 font-medium leading-none">WhatsApp</span>
            </button>
            <button
              onClick={() => onExecuteCommand('Arijit Singh ka gaana chalao')}
              className="p-2 rounded-xl bg-slate-900/80 hover:bg-slate-800 border border-slate-800 hover:border-indigo-500/40 flex flex-col items-center justify-center text-center transition-all group active:scale-95 cursor-pointer"
              title="Play Music"
            >
              <Music className="w-4 h-4 text-indigo-400 group-hover:scale-110 transition-transform mb-1" />
              <span className="text-[10px] text-slate-300 font-medium leading-none">Music</span>
            </button>
            <button
              onClick={() => onExecuteCommand('Torch on karo')}
              className="p-2 rounded-xl bg-slate-900/80 hover:bg-slate-800 border border-slate-800 hover:border-amber-500/40 flex flex-col items-center justify-center text-center transition-all group active:scale-95 cursor-pointer"
              title="Torch on karo"
            >
              <Flashlight className="w-4 h-4 text-amber-400 group-hover:scale-110 transition-transform mb-1" />
              <span className="text-[10px] text-slate-300 font-medium leading-none">Torch</span>
            </button>
          </div>

          {/* Hindi Voice Suggestions Horizontal Scroll */}
          <div className="flex items-center gap-1.5 mt-2.5 overflow-x-auto pb-1 scrollbar-none text-[11px]">
            <span className="text-[10px] text-slate-500 font-semibold uppercase tracking-wider shrink-0 mr-0.5">Try:</span>
            {[
              'पापा को कॉल करो',
              'टॉर्च ऑन करो',
              'दस मिनट का टाइमर लगाओ',
              'अरिजीत सिंह का गाना चलाओ',
              'दूसरे फोन पर YouTube खोलो',
              'घर वाले फोन पर कैमरा खोलो',
              'आज के गोल क्या हैं?',
              'मेरा फोन धीमा क्यों है?',
              'मेरी गाड़ी कहाँ पार्क है?',
              'MYRA Connect की स्थिति बताओ'
            ].map((cmd, idx) => (
              <button
                key={idx}
                onClick={() => onExecuteCommand(cmd)}
                className="px-2.5 py-1 rounded-full bg-[#111726] hover:bg-blue-950/70 border border-slate-800 hover:border-blue-500/40 text-slate-300 hover:text-blue-300 whitespace-nowrap shrink-0 transition-all text-[11px] active:scale-95 cursor-pointer"
              >
                {cmd}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* 3. Bottom Voice-First Interaction Bar: "Hey MYRA" / Microphone */}
      <div className="pt-2 z-10 space-y-2">
        {/* Microphone Permission Fallback Notice (Only visible when mic fails/blocked) */}
        {micPermissionError && (
          <div className="p-2.5 rounded-xl bg-amber-950/40 border border-amber-500/40 text-amber-200 text-xs flex items-center justify-between gap-2 shadow-lg animate-in fade-in slide-in-from-bottom-1 duration-200">
            <div className="flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0" />
              <span className="text-[11px] leading-tight font-medium">
                {micPermissionError}
              </span>
            </div>
            {onClearMicPermissionError && (
              <button
                onClick={onClearMicPermissionError}
                className="text-amber-400 hover:text-white px-1.5 py-0.5 rounded text-[11px] font-bold"
                title="Dismiss"
              >
                ✕
              </button>
            )}
          </div>
        )}

        {/* Wake Word Hint Pill */}
        <div className="flex items-center justify-center gap-2 text-xs text-slate-400">
          <Sparkles className="w-3 h-3 text-indigo-400 animate-pulse" />
          <span>Say <strong className="text-slate-200 font-semibold">"{voiceSettings.wakeWord || 'Hey MYRA'}"</strong> or tap the microphone</span>
        </div>

        {/* Physical Microphone + Text Fallback Bar */}
        <div className="flex items-center gap-2">
          {/* Silent Text Fallback Input */}
          <form onSubmit={handleTextSubmit} className="flex-1 relative">
            <input
              type="text"
              value={textInput}
              onChange={(e) => setTextInput(e.target.value)}
              placeholder="Bol kar ya likh kar poocho (e.g. Papa ko call karo)..."
              className="w-full bg-[#111726] border border-slate-800 focus:border-blue-500 text-slate-100 text-xs rounded-2xl py-3.5 pl-4 pr-10 outline-none transition-all placeholder:text-slate-500 shadow-inner"
            />
            {textInput.trim() && (
              <button
                type="submit"
                className="absolute right-2.5 top-1/2 -translate-y-1/2 p-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl transition-transform active:scale-90"
              >
                <Send className="w-3.5 h-3.5" />
              </button>
            )}
          </form>

          {/* Big Glowing Push-to-Talk Microphone Button */}
          <div className="relative shrink-0">
            {isListening && (
              <span className="absolute -inset-2 rounded-2xl bg-rose-500/40 animate-ping pointer-events-none" />
            )}
            <button
              id="main-voice-mic-btn"
              onClick={() => {
                triggerHapticFeedback('tap');
                if (isListening) onStopListening();
                else onStartListening();
              }}
              className={`w-14 h-14 rounded-2xl flex items-center justify-center transition-all shadow-xl active:scale-95 ${
                isListening
                  ? 'bg-rose-600 text-white shadow-rose-900/50 ring-4 ring-rose-500/30 animate-pulse'
                  : 'bg-gradient-to-tr from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white shadow-blue-900/40'
              }`}
              title={isListening ? 'Stop Listening' : 'Tap to Speak'}
            >
              {isListening ? <MicOff className="w-6 h-6" /> : <Mic className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
