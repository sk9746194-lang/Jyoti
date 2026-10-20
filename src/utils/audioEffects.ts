/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

// Web Audio API sound generator for system audio effects without external MP3 files
let audioCtx: AudioContext | null = null;

function getAudioContext(): AudioContext {
  if (!audioCtx) {
    const AudioCtxClass = window.AudioContext || (window as any).webkitAudioContext;
    audioCtx = new AudioCtxClass();
  }
  if (audioCtx.state === 'suspended') {
    audioCtx.resume();
  }
  return audioCtx;
}

export function playWakeChime() {
  try {
    const ctx = getAudioContext();
    const now = ctx.currentTime;
    
    // Two gentle sine beeps ascending (F5 -> A5 -> C6)
    const osc1 = ctx.createOscillator();
    const gain1 = ctx.createGain();
    osc1.type = 'sine';
    osc1.frequency.setValueAtTime(659.25, now); // E5
    osc1.frequency.exponentialRampToValueAtTime(880, now + 0.12); // A5
    
    gain1.gain.setValueAtTime(0.01, now);
    gain1.gain.linearRampToValueAtTime(0.18, now + 0.04);
    gain1.gain.exponentialRampToValueAtTime(0.001, now + 0.28);
    
    osc1.connect(gain1);
    gain1.connect(ctx.destination);
    
    osc1.start(now);
    osc1.stop(now + 0.28);
  } catch (e) {
    // Ignore audio autoplay restrictions before first user gesture
  }
}

export function playListeningDoneChime() {
  try {
    const ctx = getAudioContext();
    const now = ctx.currentTime;
    
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(880, now);
    osc.frequency.exponentialRampToValueAtTime(587.33, now + 0.15); // D5
    
    gain.gain.setValueAtTime(0.15, now);
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.22);
    
    osc.connect(gain);
    gain.connect(ctx.destination);
    
    osc.start(now);
    osc.stop(now + 0.22);
  } catch (e) {
    // Ignore
  }
}

export function playAlarmTone() {
  try {
    const ctx = getAudioContext();
    const now = ctx.currentTime;
    
    // 3 rapid alarm beeps
    for (let i = 0; i < 3; i++) {
      const startTime = now + i * 0.2;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(1046.5, startTime); // C6
      
      gain.gain.setValueAtTime(0.2, startTime);
      gain.gain.exponentialRampToValueAtTime(0.001, startTime + 0.12);
      
      osc.connect(gain);
      gain.connect(ctx.destination);
      
      osc.start(startTime);
      osc.stop(startTime + 0.12);
    }
  } catch (e) {
    // Ignore
  }
}

export function playCameraShutter() {
  try {
    const ctx = getAudioContext();
    const now = ctx.currentTime;
    
    // White noise burst click
    const bufferSize = ctx.sampleRate * 0.05;
    const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
    const data = buffer.getChannelData(0);
    for (let i = 0; i < bufferSize; i++) {
      data[i] = Math.random() * 2 - 1;
    }
    
    const noise = ctx.createBufferSource();
    noise.buffer = buffer;
    
    const filter = ctx.createBiquadFilter();
    filter.type = 'highpass';
    filter.frequency.value = 1000;
    
    const gain = ctx.createGain();
    gain.gain.setValueAtTime(0.2, now);
    gain.gain.exponentialRampToValueAtTime(0.01, now + 0.05);
    
    noise.connect(filter);
    filter.connect(gain);
    gain.connect(ctx.destination);
    
    noise.start(now);
  } catch (e) {
    // Ignore
  }
}

export function playSuccessChime() {
  try {
    const ctx = getAudioContext();
    const now = ctx.currentTime;
    
    // Joyful ascending arpeggio (C5 -> E5 -> G5 -> C6) with smooth decay
    const notes = [523.25, 659.25, 783.99, 1046.5];
    notes.forEach((freq, idx) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      
      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, now + idx * 0.07);
      
      gain.gain.setValueAtTime(0.01, now + idx * 0.07);
      gain.gain.linearRampToValueAtTime(0.12, now + idx * 0.07 + 0.02);
      gain.gain.exponentialRampToValueAtTime(0.001, now + idx * 0.07 + 0.22);
      
      osc.connect(gain);
      gain.connect(ctx.destination);
      
      osc.start(now + idx * 0.07);
      osc.stop(now + idx * 0.07 + 0.22);
    });
  } catch (e) {
    // Ignore
  }
}

/**
 * Subtle haptic feedback sequence when assistant initiates speech or confirms an interaction.
 * Uses navigator.vibrate if available (Android/PWA) and generates a subtle, low-frequency
 * physical pulse in the Web Audio context so the user feels the tactile click even on desktop/iOS/preview.
 */
export function triggerHapticFeedback(pattern: 'speech_start' | 'command_confirm' | 'tap' = 'speech_start') {
  // 1. Hardware vibration sequence via Vibration API
  if (typeof navigator !== 'undefined' && 'vibrate' in navigator) {
    try {
      if (pattern === 'speech_start') {
        // Double micro-tick: [15ms pulse, 30ms gap, 10ms pulse]
        navigator.vibrate([15, 30, 10]);
      } else if (pattern === 'command_confirm') {
        // Confirmation pulse: [20ms firm tick, 40ms gap, 25ms tick]
        navigator.vibrate([20, 40, 25]);
      } else {
        // Single micro-tap
        navigator.vibrate(10);
      }
    } catch (e) {
      // Ignore vibration errors on unsupported environments
    }
  }

  // 2. Physical Sub-bass Tactile Click (65Hz -> 40Hz damped thud) so the user senses a physical bounce
  try {
    const ctx = getAudioContext();
    const now = ctx.currentTime;
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    osc.type = 'sine';
    if (pattern === 'command_confirm') {
      osc.frequency.setValueAtTime(80, now);
      osc.frequency.exponentialRampToValueAtTime(45, now + 0.06);
      gain.gain.setValueAtTime(0.08, now);
      gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.06);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(now);
      osc.stop(now + 0.06);
    } else {
      osc.frequency.setValueAtTime(65, now);
      osc.frequency.exponentialRampToValueAtTime(35, now + 0.04);
      gain.gain.setValueAtTime(0.06, now);
      gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.04);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(now);
      osc.stop(now + 0.04);
    }
  } catch (e) {
    // Ignore audio context autoplay or device quirks
  }
}

