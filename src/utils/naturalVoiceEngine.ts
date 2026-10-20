/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { VoiceSettings, VoiceStatus } from '../types/myra';
import { triggerHapticFeedback } from './audioEffects';

// Helper: Decode 16-bit little-endian PCM base64 string to Float32Array
function pcm16Base64ToFloat32(base64: string): Float32Array {
  const binaryString = atob(base64);
  const len = binaryString.length;
  const bytes = new Uint8Array(len);
  for (let i = 0; i < len; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }
  const int16Array = new Int16Array(bytes.buffer);
  const float32Array = new Float32Array(int16Array.length);
  for (let i = 0; i < int16Array.length; i++) {
    float32Array[i] = int16Array[i] / 32768.0;
  }
  return float32Array;
}

// Helper: Convert Float32 microphone data to 16kHz 16-bit PCM base64
function float32To16kPcmBase64(inputData: Float32Array, inputSampleRate: number): string {
  // Downsample to 16000 if necessary
  const targetSampleRate = 16000;
  let resampled: Float32Array;
  
  if (inputSampleRate === targetSampleRate) {
    resampled = inputData;
  } else {
    const ratio = inputSampleRate / targetSampleRate;
    const newLen = Math.round(inputData.length / ratio);
    resampled = new Float32Array(newLen);
    for (let i = 0; i < newLen; i++) {
      const originalIdx = Math.floor(i * ratio);
      resampled[i] = inputData[originalIdx] || 0;
    }
  }

  const int16 = new Int16Array(resampled.length);
  for (let i = 0; i < resampled.length; i++) {
    const s = Math.max(-1, Math.min(1, resampled[i]));
    int16[i] = s < 0 ? s * 0x8000 : s * 0x7fff;
  }

  const bytes = new Uint8Array(int16.buffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}

export class NaturalVoiceEngine {
  private ws: WebSocket | null = null;
  private audioCtx: AudioContext | null = null;
  private analyser: AnalyserNode | null = null;
  private micStream: MediaStream | null = null;
  private micAudioCtx: AudioContext | null = null;
  private micAnalyser: AnalyserNode | null = null;
  private scriptProcessor: ScriptProcessorNode | null = null;

  // Single Audio Owner & Sequential Queue Management
  private nextStartTime: number = 0;
  private activeSources: AudioBufferSourceNode[] = [];
  private isSpeaking: boolean = false;
  private animFrameId: number | null = null;
  private fallbackSpeechActive: boolean = false;

  // Global Speech Request ID tracking (strictly prevents stale/delayed playback)
  private currentSpeechRequestId: number = 0;
  private currentActivePlaybackId: number = 0;

  // Debounce guard for Orb touch / mic triggers (300ms window)
  private lastTriggerTime: number = 0;

  // Recognition fallback
  private recognition: any = null;

  // Status state
  public status: VoiceStatus = {
    activeMode: 'gemini_live',
    geminiLiveConnected: false,
    model: 'gemini-3.8-live',
    voiceName: 'Aoede',
    fallbackActive: false,
    fallbackReason: undefined,
    sampleRateIn: 16000,
    sampleRateOut: 24000,
  };

  private onStatusChangeCallbacks: ((status: VoiceStatus) => void)[] = [];
  private onAudioLevelCallback?: (level: number) => void;
  private onTranscriptCallback?: (text: string) => void;
  private onSpeakingChangeCallback?: (isSpeaking: boolean) => void;

  constructor() {
    this.initRecognition();
    this.initWebSocket();
  }

  // Diagnostic logging helper
  private logVoiceEvent(event: string, details?: Record<string, any>) {
    console.log(`[VOICE_PIPELINE] ${event}`, details || {});
  }

  private updateStatus(newPartial: Partial<VoiceStatus>) {
    this.status = { ...this.status, ...newPartial };
    this.onStatusChangeCallbacks.forEach((cb) => cb(this.status));
  }

  public onStatusChange(callback: (status: VoiceStatus) => void) {
    this.onStatusChangeCallbacks.push(callback);
    callback(this.status);
    return () => {
      this.onStatusChangeCallbacks = this.onStatusChangeCallbacks.filter((c) => c !== callback);
    };
  }

  public setAudioLevelCallback(callback: (level: number) => void) {
    this.onAudioLevelCallback = callback;
  }

  public setTranscriptCallback(callback: (text: string) => void) {
    this.onTranscriptCallback = callback;
  }

  public setSpeakingChangeCallback(callback: (isSpeaking: boolean) => void) {
    this.onSpeakingChangeCallback = callback;
  }

  // Single AudioContext manager
  private getAudioContext(): AudioContext {
    if (!this.audioCtx || this.audioCtx.state === 'closed') {
      const AudioCtxClass = window.AudioContext || (window as any).webkitAudioContext;
      this.audioCtx = new AudioCtxClass({ sampleRate: 24000 });
      this.analyser = this.audioCtx.createAnalyser();
      this.analyser.fftSize = 256;
      this.analyser.smoothingTimeConstant = 0.4;
      this.analyser.connect(this.audioCtx.destination);
    }
    if (this.audioCtx.state === 'suspended') {
      this.audioCtx.resume().catch(() => {});
    }
    return this.audioCtx;
  }

  // Initialize WebSocket for Gemini Live (Primary Realtime Voice)
  public initWebSocket() {
    if (typeof window === 'undefined') return;

    try {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const wsUrl = `${protocol}//${window.location.host}/live`;

      if (this.ws) {
        try {
          this.ws.close();
        } catch (e) {}
      }

      this.ws = new WebSocket(wsUrl);

      this.ws.onopen = () => {
        this.logVoiceEvent('VOICE_ENGINE_SELECTED', {
          engine: 'GEMINI_LIVE',
          voiceName: 'Aoede',
        });
      };

      this.ws.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data);

          if (msg.type === 'status') {
            if (msg.status === 'connected') {
              this.updateStatus({
                activeMode: 'gemini_live',
                geminiLiveConnected: true,
                fallbackActive: false,
                fallbackReason: undefined,
                model: msg.model || 'gemini-3.8-live',
                voiceName: 'Aoede',
                sampleRateIn: msg.sampleRateIn || 16000,
                sampleRateOut: msg.sampleRateOut || 24000,
              });
              this.logVoiceEvent('LIVE_WS_STATUS_CONNECTED', {
                model: msg.model || 'gemini-3.8-live',
                voiceName: 'Aoede',
              });
            } else if (msg.status === 'unavailable' || msg.status === 'error') {
              this.updateStatus({
                activeMode: 'gemini_tts',
                geminiLiveConnected: false,
                fallbackActive: true,
                fallbackReason: msg.error || 'Gemini Live session unavailable',
                voiceName: 'Aoede (Neural TTS)',
              });
            }
          } else if (msg.type === 'audio' && msg.audio) {
            // Live audio chunk arrives: only play if this is the active request
            this.updateStatus({
              activeMode: 'gemini_live',
              fallbackActive: false,
              voiceName: 'Aoede',
            });
            this.playPcmChunk(msg.audio, 24000, this.currentActivePlaybackId);
          } else if (msg.type === 'transcript' && msg.text) {
            if (this.onTranscriptCallback) {
              this.onTranscriptCallback(msg.text);
            }
          } else if (msg.type === 'interrupted') {
            this.logVoiceEvent('VOICE_BARGE_IN_INTERRUPTED');
            this.stopPlayback();
          } else if (msg.type === 'turnComplete') {
            // Turn finished
          }
        } catch (e) {
          console.error('[NaturalVoiceEngine] Error parsing WS message:', e);
        }
      };

      this.ws.onerror = (err) => {
        console.warn('[NaturalVoiceEngine] WebSocket error notice:', err);
        this.updateStatus({
          geminiLiveConnected: false,
          fallbackActive: true,
          fallbackReason: 'Gemini Live connection dropped or unavailable. Using Neural TTS.',
        });
      };

      this.ws.onclose = () => {
        this.updateStatus({
          geminiLiveConnected: false,
        });
      };
    } catch (e) {
      console.warn('[NaturalVoiceEngine] Could not initialize Live WebSocket:', e);
      this.updateStatus({
        geminiLiveConnected: false,
        fallbackActive: true,
        fallbackReason: 'WebSocket connection failed. Using Neural TTS.',
      });
    }
  }

  // Play a 24kHz PCM chunk with gapless scheduling and Request-ID validation
  public playPcmChunk(base64Pcm: string, sampleRate: number = 24000, playbackId?: number) {
    // If a specific playbackId is provided, ignore if it is stale
    if (playbackId !== undefined && playbackId !== this.currentActivePlaybackId) {
      this.logVoiceEvent('VOICE_PLAYBACK_CANCEL_STALE', {
        playbackId,
        activePlaybackId: this.currentActivePlaybackId,
      });
      return;
    }

    try {
      const ctx = this.getAudioContext();
      const float32 = pcm16Base64ToFloat32(base64Pcm);
      const buffer = ctx.createBuffer(1, float32.length, sampleRate);
      buffer.getChannelData(0).set(float32);

      const source = ctx.createBufferSource();
      source.buffer = buffer;

      if (this.analyser) {
        source.connect(this.analyser);
      } else {
        source.connect(ctx.destination);
      }

      const now = ctx.currentTime;
      const startTime = Math.max(now, this.nextStartTime);
      source.start(startTime);
      this.nextStartTime = startTime + buffer.duration;
      this.activeSources.push(source);

      this.setSpeakingState(true);

      source.onended = () => {
        const idx = this.activeSources.indexOf(source);
        if (idx !== -1) this.activeSources.splice(idx, 1);
        if (this.activeSources.length === 0 && ctx.currentTime >= this.nextStartTime - 0.05) {
          this.setSpeakingState(false);
          this.logVoiceEvent('VOICE_PLAYBACK_STOP', {
            requestId: this.currentActivePlaybackId,
          });
        }
      };

      this.startVisualizationLoop();
    } catch (e) {
      console.warn('[NaturalVoiceEngine] Notice playing PCM chunk:', e);
    }
  }

  // Continuous frequency measurement for Animated Orb
  private startVisualizationLoop() {
    if (this.animFrameId !== null) return;

    const update = () => {
      if ((this.isSpeaking && this.analyser) || (this.micAnalyser && this.micStream)) {
        const targetAnalyser = this.isSpeaking && this.analyser ? this.analyser : this.micAnalyser;
        if (targetAnalyser) {
          const dataArray = new Uint8Array(targetAnalyser.frequencyBinCount);
          targetAnalyser.getByteFrequencyData(dataArray);

          let sum = 0;
          for (let i = 0; i < dataArray.length; i++) {
            sum += dataArray[i];
          }
          const avg = sum / dataArray.length;
          const level = Math.min(1.0, Math.max(0, (avg / 128) * 1.3));

          if (this.onAudioLevelCallback) {
            this.onAudioLevelCallback(level);
          }
        }
        this.animFrameId = requestAnimationFrame(update);
      } else if (this.fallbackSpeechActive) {
        const t = Date.now() / 150;
        const simulated = 0.4 + 0.35 * Math.sin(t) + 0.15 * Math.cos(t * 2.3);
        if (this.onAudioLevelCallback) {
          this.onAudioLevelCallback(Math.max(0.1, simulated));
        }
        this.animFrameId = requestAnimationFrame(update);
      } else {
        if (this.onAudioLevelCallback) {
          this.onAudioLevelCallback(0);
        }
        this.animFrameId = null;
      }
    };

    this.animFrameId = requestAnimationFrame(update);
  }

  private setSpeakingState(speaking: boolean) {
    if (speaking && !this.isSpeaking) {
      triggerHapticFeedback('speech_start');
    }
    this.isSpeaking = speaking;
    if (this.onSpeakingChangeCallback) {
      this.onSpeakingChangeCallback(speaking);
    }
  }

  // ABSOLUTE SINGLE-AUDIO-OWNER: Stop any ongoing audio playback immediately
  public stopPlayback() {
    this.logVoiceEvent('VOICE_PLAYBACK_CANCEL', {
      activePlaybackId: this.currentActivePlaybackId,
    });

    this.activeSources.forEach((s) => {
      try {
        s.stop();
        s.disconnect();
      } catch (e) {}
    });
    this.activeSources = [];
    this.nextStartTime = 0;
    this.setSpeakingState(false);

    // Cancel browser TTS if active
    if (typeof window !== 'undefined' && window.speechSynthesis) {
      try {
        window.speechSynthesis.cancel();
      } catch (e) {}
      this.fallbackSpeechActive = false;
    }
  }

  // Debounce check for Orb touch / clicks (prevents duplicate triggers within 300ms)
  public shouldThrottleTrigger(): boolean {
    const now = Date.now();
    if (now - this.lastTriggerTime < 300) {
      this.logVoiceEvent('VOICE_TRIGGER_THROTTLED');
      return true;
    }
    this.lastTriggerTime = now;
    return false;
  }

  // Speak text using EXACTLY ONE audio output pipeline with Request ID Protection
  public async speakText(
    text: string,
    voiceSettings: VoiceSettings,
    onStart?: () => void,
    onEnd?: () => void
  ) {
    // 1. Increment Request ID & Stop previous playback completely
    const requestId = ++this.currentSpeechRequestId;
    this.currentActivePlaybackId = requestId;

    this.logVoiceEvent('VOICE_REQUEST_START', {
      requestId,
      voiceName: 'Aoede',
      textPreview: text.substring(0, 30),
    });

    this.stopPlayback();
    onStart?.();

    // 2. PRIMARY PIPELINE: If Gemini Live is connected, use real-time Live voice stream
    if (this.ws && this.ws.readyState === WebSocket.OPEN && this.status.geminiLiveConnected) {
      try {
        this.logVoiceEvent('VOICE_ENGINE_SELECTED', {
          engine: 'GEMINI_LIVE',
          voiceName: 'Aoede',
          requestId,
        });

        this.updateStatus({
          activeMode: 'gemini_live',
          voiceName: 'Aoede',
          fallbackActive: false,
          fallbackReason: undefined,
        });

        this.logVoiceEvent('VOICE_PLAYBACK_START', {
          engine: 'GEMINI_LIVE',
          voiceName: 'Aoede',
          requestId,
        });

        this.ws.send(JSON.stringify({ text }));
        return;
      } catch (e) {
        console.warn('Gemini Live WS send failed, evaluating next tier:', e);
      }
    }

    // Check if this request was superseded while evaluating
    if (this.currentActivePlaybackId !== requestId) {
      this.logVoiceEvent('VOICE_PLAYBACK_CANCEL_SUPERSEDED', { requestId });
      return;
    }

    // 3. SECONDARY TIER: Gemini Neural TTS (/api/gemini/tts with voice=Aoede)
    try {
      this.logVoiceEvent('VOICE_ENGINE_SELECTED', {
        engine: 'GEMINI_NEURAL_TTS',
        voiceName: 'Aoede',
        requestId,
      });

      const res = await fetch('/api/gemini/tts', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          text,
          voice: 'Aoede', // Strictly Primary Voice
          personality: voiceSettings.personality,
        }),
      });

      // Stale check
      if (this.currentActivePlaybackId !== requestId) {
        this.logVoiceEvent('VOICE_PLAYBACK_CANCEL_STALE', { requestId });
        return;
      }

      if (res.ok) {
        const data = await res.json();
        if (data.audio && this.currentActivePlaybackId === requestId) {
          this.updateStatus({
            activeMode: 'gemini_tts',
            fallbackActive: false,
            fallbackReason: undefined,
            model: data.model || 'gemini-3.1-flash-tts-preview',
            voiceName: 'Aoede (Neural Voice)',
            sampleRateOut: 24000,
          });

          this.logVoiceEvent('VOICE_PLAYBACK_START', {
            engine: 'GEMINI_NEURAL_TTS',
            voiceName: 'Aoede',
            requestId,
          });

          this.playPcmChunk(data.audio, data.sampleRate || 24000, requestId);
          return;
        }
      }
    } catch (e) {
      console.warn('Gemini TTS fetch notice:', e);
    }

    // Stale check before fallback
    if (this.currentActivePlaybackId !== requestId) {
      return;
    }

    // 4. FALLBACK ONLY: ONLY when primary and neural TTS are genuinely unavailable. Never parallel!
    this.logVoiceEvent('VOICE_FALLBACK', {
      reason: 'Gemini Live and Neural TTS offline',
      requestId,
    });

    this.updateStatus({
      activeMode: 'fallback_tts',
      fallbackActive: true,
      fallbackReason: 'Gemini Live & Neural audio unavailable. Fallback speech active.',
      voiceName: 'Browser TTS (Fallback)',
    });

    this.speakFallbackWebSpeech(text, voiceSettings, requestId, onStart, onEnd);
  }

  // Fallback speech synthesis (strictly sequential, guarded by requestId)
  private speakFallbackWebSpeech(
    text: string,
    settings: VoiceSettings,
    requestId: number,
    onStart?: () => void,
    onEnd?: () => void
  ) {
    if (typeof window === 'undefined' || !window.speechSynthesis) {
      onEnd?.();
      return;
    }

    if (this.currentActivePlaybackId !== requestId) return;

    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);

    // Best Hindi/Indian female voice
    const voices = window.speechSynthesis.getVoices();
    const femaleHindi = voices.find(
      (v) =>
        (v.lang.toLowerCase().includes('hi') || v.name.toLowerCase().includes('hindi')) &&
        (v.name.toLowerCase().includes('female') ||
          v.name.toLowerCase().includes('google') ||
          v.name.toLowerCase().includes('zira') ||
          v.name.toLowerCase().includes('natural'))
    );
    const anyHindiOrIndian = voices.find(
      (v) =>
        v.lang.toLowerCase().includes('hi') ||
        v.name.toLowerCase().includes('hindi') ||
        v.lang.toLowerCase() === 'en-in'
    );

    if (femaleHindi) {
      utterance.voice = femaleHindi;
    } else if (anyHindiOrIndian) {
      utterance.voice = anyHindiOrIndian;
    }

    if (settings.personality === 'romantic_gf') {
      utterance.pitch = 1.25;
      utterance.rate = 0.92;
    } else {
      utterance.pitch = 1.05;
      utterance.rate = 0.98;
    }

    utterance.onstart = () => {
      if (this.currentActivePlaybackId !== requestId) {
        window.speechSynthesis.cancel();
        return;
      }
      this.fallbackSpeechActive = true;
      this.setSpeakingState(true);
      this.startVisualizationLoop();
      onStart?.();
    };

    utterance.onend = () => {
      this.fallbackSpeechActive = false;
      this.setSpeakingState(false);
      onEnd?.();
    };

    utterance.onerror = () => {
      this.fallbackSpeechActive = false;
      this.setSpeakingState(false);
      onEnd?.();
    };

    window.speechSynthesis.speak(utterance);
  }

  // Microphone capture with 16kHz PCM streaming and real-time audio level monitoring
  public async startRealtimeMic(
    onResult: (transcript: string, isFinal: boolean) => void,
    onError: (err: any) => void,
    onEnd: () => void,
    lang: string = 'hi-IN'
  ) {
    // Barge-in: User speaking immediately cancels any ongoing assistant speech
    this.stopPlayback();

    try {
      this.micStream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const AudioCtxClass = window.AudioContext || (window as any).webkitAudioContext;
      this.micAudioCtx = new AudioCtxClass();
      const source = this.micAudioCtx.createMediaStreamSource(this.micStream);

      this.micAnalyser = this.micAudioCtx.createAnalyser();
      this.micAnalyser.fftSize = 256;
      source.connect(this.micAnalyser);

      // Script processor to send 16kHz PCM chunks to Gemini Live if active
      const processor = this.micAudioCtx.createScriptProcessor(4096, 1, 1);
      source.connect(processor);
      processor.connect(this.micAudioCtx.destination);
      this.scriptProcessor = processor;

      processor.onaudioprocess = (e) => {
        if (this.ws && this.ws.readyState === WebSocket.OPEN && this.status.geminiLiveConnected) {
          const inputData = e.inputBuffer.getChannelData(0);
          const base64 = float32To16kPcmBase64(inputData, this.micAudioCtx?.sampleRate || 48000);
          try {
            this.ws.send(JSON.stringify({ audio: base64 }));
          } catch (err) {}
        }
      };

      this.startVisualizationLoop();
    } catch (err) {
      console.warn('Microphone AudioContext error:', err);
    }

    this.startSpeechRecognition(onResult, onError, onEnd, lang);
  }

  public stopRealtimeMic() {
    if (this.scriptProcessor) {
      try {
        this.scriptProcessor.disconnect();
      } catch (e) {}
      this.scriptProcessor = null;
    }
    if (this.micStream) {
      this.micStream.getTracks().forEach((track) => track.stop());
      this.micStream = null;
    }
    if (this.micAudioCtx && this.micAudioCtx.state !== 'closed') {
      try {
        this.micAudioCtx.close();
      } catch (e) {}
      this.micAudioCtx = null;
    }
    this.micAnalyser = null;
    this.stopSpeechRecognition();
  }

  // Web Speech recognition for transcription
  private initRecognition() {
    if (typeof window === 'undefined') return;
    const win = window as any;
    const SpeechRec = win.SpeechRecognition || win.webkitSpeechRecognition;
    if (SpeechRec) {
      try {
        this.recognition = new SpeechRec();
        this.recognition.continuous = false;
        this.recognition.interimResults = true;
        this.recognition.lang = 'hi-IN';
      } catch (e) {}
    }
  }

  private startSpeechRecognition(
    onResult: (transcript: string, isFinal: boolean) => void,
    onError: (err: any) => void,
    onEnd: () => void,
    lang: string = 'hi-IN'
  ) {
    if (!this.recognition) return;

    this.recognition.lang = lang;

    this.recognition.onresult = (event: any) => {
      let interim = '';
      let final = '';
      for (let i = event.resultIndex; i < event.results.length; ++i) {
        if (event.results[i].isFinal) {
          final += event.results[i][0].transcript;
        } else {
          interim += event.results[i][0].transcript;
        }
      }
      if (final) {
        onResult(final.trim(), true);
      } else if (interim) {
        onResult(interim.trim(), false);
      }
    };

    this.recognition.onerror = (event: any) => {
      onError(event.error);
    };

    this.recognition.onend = () => {
      onEnd();
    };

    try {
      this.recognition.start();
    } catch (e) {}
  }

  private stopSpeechRecognition() {
    if (this.recognition) {
      try {
        this.recognition.stop();
      } catch (e) {}
    }
  }
}

export const naturalVoice = new NaturalVoiceEngine();
