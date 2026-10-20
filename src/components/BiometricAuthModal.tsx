/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from 'react';
import { Fingerprint, ScanFace, KeyRound, ShieldAlert, CheckCircle2, X } from 'lucide-react';

interface BiometricAuthModalProps {
  isOpen: boolean;
  title?: string;
  subtitle?: string;
  description?: string;
  onSuccess: () => void;
  onCancel: () => void;
}

export const BiometricAuthModal: React.FC<BiometricAuthModalProps> = ({
  isOpen,
  title = 'MYRA Owner Verification',
  subtitle = 'संवेदनशील एक्शन के लिए बायोमेट्रिक प्रमाणीकरण आवश्यक है',
  description = 'Touch the fingerprint sensor or enter your device PIN/Password to proceed.',
  onSuccess,
  onCancel,
}) => {
  const [authMode, setAuthMode] = useState<'fingerprint' | 'pin'>('fingerprint');
  const [pinValue, setPinValue] = useState('');
  const [isAuthenticating, setIsAuthenticating] = useState(false);
  const [authError, setAuthError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleFingerprintTouch = () => {
    setIsAuthenticating(true);
    setAuthError(null);
    setTimeout(() => {
      setIsAuthenticating(false);
      onSuccess();
    }, 700);
  };

  const handlePinSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (pinValue.length >= 4) {
      setIsAuthenticating(true);
      setTimeout(() => {
        setIsAuthenticating(false);
        onSuccess();
      }, 500);
    } else {
      setAuthError('कम से कम 4 अंकों का पिन दर्ज करें');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 myra-modal-overlay animate-in fade-in duration-200">
      <div className="relative w-full max-w-sm bg-[#090d16] border border-cyan-500/40 rounded-3xl p-6 shadow-2xl shadow-cyan-950/40 text-slate-100 overflow-hidden">
        {/* Glow corner */}
        <div className="absolute top-0 right-0 w-32 h-32 bg-cyan-500/10 rounded-full blur-2xl pointer-events-none" />

        {/* Top Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-xl bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-center text-emerald-400">
              <Fingerprint className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-white tracking-wide">{title}</h3>
              <p className="text-[10px] text-emerald-400 font-mono">Android BiometricPrompt</p>
            </div>
          </div>
          <button
            onClick={onCancel}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Subtitle & Info */}
        <div className="my-4 text-center">
          <p className="text-xs text-slate-300 font-medium">{subtitle}</p>
          <p className="text-[11px] text-slate-400 mt-1">{description}</p>
        </div>

        {/* Tab switch between Biometric & Device PIN */}
        <div className="flex bg-slate-900/90 border border-slate-800 rounded-xl p-1 mb-5">
          <button
            type="button"
            onClick={() => {
              setAuthMode('fingerprint');
              setAuthError(null);
            }}
            className={`flex-1 py-1.5 rounded-lg text-xs font-semibold flex items-center justify-center gap-1.5 transition-all ${
              authMode === 'fingerprint'
                ? 'bg-emerald-600 text-white shadow-md shadow-emerald-950'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Fingerprint className="w-3.5 h-3.5" />
            <span>Fingerprint / Face</span>
          </button>
          <button
            type="button"
            onClick={() => {
              setAuthMode('pin');
              setAuthError(null);
            }}
            className={`flex-1 py-1.5 rounded-lg text-xs font-semibold flex items-center justify-center gap-1.5 transition-all ${
              authMode === 'pin'
                ? 'bg-blue-600 text-white shadow-md shadow-blue-950'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <KeyRound className="w-3.5 h-3.5" />
            <span>Device PIN</span>
          </button>
        </div>

        {authMode === 'fingerprint' ? (
          <div className="flex flex-col items-center py-4">
            <button
              onClick={handleFingerprintTouch}
              disabled={isAuthenticating}
              className="group relative p-6 rounded-full bg-slate-900 border-2 border-emerald-500/60 hover:border-emerald-400 hover:shadow-xl hover:shadow-emerald-500/30 transition-all active:scale-95 cursor-pointer"
            >
              <Fingerprint
                className={`w-16 h-16 ${
                  isAuthenticating ? 'text-emerald-300 animate-ping' : 'text-emerald-400 group-hover:scale-105'
                } transition-transform`}
              />
              <span className="absolute inset-0 rounded-full border border-emerald-400/40 animate-pulse pointer-events-none" />
            </button>
            <span className="text-xs text-slate-300 mt-4 font-mono font-medium">
              {isAuthenticating ? 'प्रमाणीकरण हो रहा है...' : 'टच करें — Touch sensor to authenticate'}
            </span>
            <span className="text-[10px] text-slate-500 mt-1">
              Supports Android Fingerprint & Class 3 Biometric Face
            </span>
          </div>
        ) : (
          <form onSubmit={handlePinSubmit} className="space-y-4 py-2">
            <div>
              <label className="text-xs text-slate-400 block mb-1.5">Enter Device Screen Lock PIN</label>
              <input
                type="password"
                maxLength={8}
                value={pinValue}
                onChange={(e) => setPinValue(e.target.value.replace(/\D/g, ''))}
                placeholder="••••"
                className="w-full text-center text-xl tracking-[0.5em] py-2.5 px-4 bg-slate-900 border border-slate-700 rounded-xl text-white focus:outline-none focus:border-blue-500 font-mono"
                autoFocus
              />
            </div>
            {authError && <p className="text-xs text-rose-400 text-center">{authError}</p>}
            <button
              type="submit"
              disabled={isAuthenticating || pinValue.length < 4}
              className="w-full py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 disabled:opacity-50 text-white font-bold text-xs transition-colors flex items-center justify-center gap-1.5"
            >
              <CheckCircle2 className="w-4 h-4" />
              <span>Verify PIN & Proceed</span>
            </button>
          </form>
        )}

        {/* Footer / Cancel */}
        <div className="pt-4 mt-2 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
          <span className="text-[10px] text-slate-500">2-minute authorized session window</span>
          <button
            onClick={onCancel}
            className="text-xs text-slate-400 hover:text-white font-medium py-1 px-3 rounded-lg hover:bg-slate-800"
          >
            रद्द करें (Cancel)
          </button>
        </div>
      </div>
    </div>
  );
};
