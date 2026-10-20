/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from 'react';
import { AlertTriangle, MapPin, PhoneCall, X, ShieldAlert } from 'lucide-react';

interface EmergencySosModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const EmergencySosModal: React.FC<EmergencySosModalProps> = ({ isOpen, onClose }) => {
  const [countdown, setCountdown] = useState(5);
  const [sent, setSent] = useState(false);

  useEffect(() => {
    if (!isOpen) {
      setCountdown(5);
      setSent(false);
      return;
    }

    const timer = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          setSent(true);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div className="absolute inset-0 z-50 bg-rose-950/95 backdrop-blur-md text-white flex flex-col justify-between p-6 animate-in fade-in duration-200">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <AlertTriangle className="w-6 h-6 text-rose-400 animate-bounce" />
          <span className="font-bold text-lg tracking-wider text-rose-300">EMERGENCY SOS ALERT</span>
        </div>
        <button
          onClick={onClose}
          className="p-2 rounded-full bg-slate-900/60 hover:bg-slate-900 text-slate-300 hover:text-white"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      <div className="flex flex-col items-center text-center my-auto">
        <div className="w-24 h-24 rounded-full bg-rose-600/30 border-4 border-rose-500 flex items-center justify-center mb-4 shadow-[0_0_60px_rgba(244,63,94,0.6)] animate-pulse">
          <ShieldAlert className="w-12 h-12 text-rose-400" />
        </div>

        {!sent ? (
          <>
            <h2 className="text-3xl font-extrabold text-white mb-2">Broadcasting in {countdown}s</h2>
            <p className="text-sm text-rose-200 max-w-xs">
              Sending real-time GPS coordinates, audio snippet, and battery status to Papa, Mummy & 112 Emergency Dispatch.
            </p>
          </>
        ) : (
          <>
            <h2 className="text-2xl font-bold text-emerald-400 mb-2">SOS Alert Broadcasted!</h2>
            <div className="bg-slate-900/90 border border-rose-500/50 rounded-xl p-4 text-xs text-left w-full max-w-xs space-y-2 mt-2">
              <div className="flex items-center gap-2 text-rose-300">
                <MapPin className="w-4 h-4 shrink-0" />
                <span>Sector 62, Noida (28.6289° N, 77.3712° E)</span>
              </div>
              <div className="flex items-center gap-2 text-emerald-300">
                <PhoneCall className="w-4 h-4 shrink-0" />
                <span>Dispatched SMS to 2 Trusted Contacts</span>
              </div>
            </div>
          </>
        )}
      </div>

      <div className="flex gap-3">
        <button
          onClick={onClose}
          className="w-full py-3 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-200 font-semibold text-sm transition-all"
        >
          Cancel SOS
        </button>
        <button
          onClick={() => setSent(true)}
          className="w-full py-3 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-sm shadow-lg shadow-rose-900/40 transition-all"
        >
          Dispatch Now
        </button>
      </div>
    </div>
  );
};
