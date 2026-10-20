/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from 'react';
import { Fingerprint, Lock, ShieldCheck, ChevronUp } from 'lucide-react';

interface LockScreenModalProps {
  isOpen: boolean;
  onUnlock: () => void;
}

export const LockScreenModal: React.FC<LockScreenModalProps> = ({ isOpen, onUnlock }) => {
  const [time, setTime] = useState('09:41');
  const [dateStr, setDateStr] = useState('');

  useEffect(() => {
    const update = () => {
      const now = new Date();
      setTime(now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false }));
      setDateStr(now.toLocaleDateString('en-IN', { weekday: 'long', month: 'short', day: 'numeric' }));
    };
    update();
    const interval = setInterval(update, 1000);
    return () => clearInterval(interval);
  }, []);

  if (!isOpen) return null;

  return (
    <div className="absolute inset-0 z-50 bg-[#060911] text-white flex flex-col justify-between p-6 select-none animate-in fade-in duration-200">
      {/* Top Lock Icon & Notice */}
      <div className="flex flex-col items-center pt-6">
        <div className="flex items-center gap-1.5 text-xs text-cyan-300 bg-[#090d16] px-3 py-1 rounded-full border border-cyan-500/30 mb-8 shadow-sm">
          <Lock className="w-3.5 h-3.5 text-cyan-400" />
          <span>Device Secured via MYRA Urgent Command</span>
        </div>

        <h1 className="text-6xl font-light tracking-tight text-white mb-2 font-mono">{time}</h1>
        <p className="text-sm font-medium text-slate-400 tracking-wide">{dateStr}</p>
      </div>

      {/* Center Security Shield */}
      <div className="flex flex-col items-center">
        <div className="w-16 h-16 rounded-2xl bg-indigo-950/40 border border-indigo-500/30 flex items-center justify-center mb-2 shadow-[0_0_30px_rgba(99,102,241,0.2)]">
          <ShieldCheck className="w-8 h-8 text-indigo-400" />
        </div>
        <p className="text-xs text-slate-400">MYRA Voice Authentication Ready</p>
      </div>

      {/* Bottom Unlock Button / Biometric */}
      <div className="flex flex-col items-center pb-6">
        <button
          id="unlock-device-btn"
          onClick={onUnlock}
          className="group flex flex-col items-center gap-2 text-slate-400 hover:text-white transition-all cursor-pointer"
        >
          <div className="w-16 h-16 rounded-full bg-slate-900 border border-slate-700 group-hover:border-indigo-500 flex items-center justify-center shadow-lg group-hover:shadow-indigo-500/20 active:scale-95 transition-all">
            <Fingerprint className="w-8 h-8 text-indigo-400 group-hover:scale-110 transition-transform" />
          </div>
          <span className="text-xs font-medium tracking-wide flex items-center gap-1 mt-1">
            <ChevronUp className="w-3.5 h-3.5 animate-bounce" />
            Tap or Swipe to Unlock
          </span>
        </button>
      </div>
    </div>
  );
};
