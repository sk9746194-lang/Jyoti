/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { VoiceSettings } from '../types/myra';

// Type declarations for Web Speech API
interface IWindow extends Window {
  webkitSpeechRecognition?: any;
  SpeechRecognition?: any;
}

export class SpeechEngine {
  private recognition: any = null;
  private isListening: boolean = false;
  private synth: SpeechSynthesis | null = null;
  private currentUtterance: SpeechSynthesisUtterance | null = null;

  constructor() {
    if (typeof window !== 'undefined') {
      const win = window as IWindow;
      const SpeechRec = win.SpeechRecognition || win.webkitSpeechRecognition;
      if (SpeechRec) {
        try {
          this.recognition = new SpeechRec();
          this.recognition.continuous = false;
          this.recognition.interimResults = true;
          this.recognition.lang = 'hi-IN'; // Default to Hindi, also recognizes Hinglish & English
        } catch (e) {
          console.warn('SpeechRecognition initialization error:', e);
        }
      }
      this.synth = window.speechSynthesis || null;
    }
  }

  public isSupported(): boolean {
    return Boolean(this.recognition);
  }

  public startListening(
    onResult: (transcript: string, isFinal: boolean) => void,
    onError: (err: any) => void,
    onEnd: () => void,
    lang: string = 'hi-IN'
  ) {
    if (!this.recognition) {
      onError(new Error('Speech recognition is not supported in this browser.'));
      return;
    }

    if (this.isListening) {
      try {
        this.recognition.abort();
      } catch (e) {}
    }

    this.recognition.lang = lang;

    this.recognition.onstart = () => {
      this.isListening = true;
    };

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
      this.isListening = false;
      onError(event.error);
    };

    this.recognition.onend = () => {
      this.isListening = false;
      onEnd();
    };

    try {
      this.recognition.start();
    } catch (err) {
      this.isListening = false;
      onError(err);
    }
  }

  public stopListening() {
    if (this.recognition && this.isListening) {
      try {
        this.recognition.stop();
      } catch (e) {}
      this.isListening = false;
    }
  }

  public speak(
    text: string,
    settings: VoiceSettings,
    onStart?: () => void,
    onEnd?: () => void
  ) {
    if (!this.synth) {
      onEnd?.();
      return;
    }

    // Cancel any previous speech
    this.synth.cancel();

    const utterance = new SpeechSynthesisUtterance(text);
    this.currentUtterance = utterance;

    // Pick best Hindi or Indian English voice
    const voices = this.synth.getVoices();
    const hindiVoice = voices.find(
      (v) =>
        v.lang.toLowerCase().includes('hi') ||
        v.name.toLowerCase().includes('hindi') ||
        v.lang.toLowerCase() === 'en-in'
    );

    if (hindiVoice) {
      utterance.voice = hindiVoice;
    }

    // Personality adjustments
    if (settings.personality === 'romantic_gf') {
      utterance.pitch = Math.min(settings.pitch * 1.25, 1.8);
      utterance.rate = Math.max(settings.rate * 0.9, 0.7);
    } else {
      utterance.pitch = settings.pitch;
      utterance.rate = settings.rate;
    }

    utterance.onstart = () => {
      onStart?.();
    };

    utterance.onend = () => {
      this.currentUtterance = null;
      onEnd?.();
    };

    utterance.onerror = () => {
      this.currentUtterance = null;
      onEnd?.();
    };

    this.synth.speak(utterance);
  }

  public stopSpeaking() {
    if (this.synth) {
      this.synth.cancel();
    }
  }
}

export const speechEngine = new SpeechEngine();
