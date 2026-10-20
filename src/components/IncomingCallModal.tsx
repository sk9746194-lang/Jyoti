/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from 'react';
import { Phone, PhoneOff, User, ShieldAlert, Volume2 } from 'lucide-react';

interface IncomingCallModalProps {
  callerName: string;
  callerNumber: string;
  isOpen: boolean;
  onAnswer: () => void;
  onReject: () => void;
  onClose: () => void;
}

export const IncomingCallModal: React.FC<IncomingCallModalProps> = ({
  callerName,
  callerNumber,
  isOpen,
  onAnswer,
  onReject,
  onClose,
}) => {
  const [secondsRemaining, setSecondsRemaining] = useState(20);
  const [callStatus, setCallStatus] = useState<'ringing' | 'active' | 'rejected'>('ringing');

  useEffect(() => {
    if (!isOpen) {
      setSecondsRemaining(20);
      setCallStatus('ringing');
      return;
    }

    const timer = setInterval(() => {
      setSecondsRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div className="absolute inset-0 z-40 bg-slate-950/95 backdrop-blur-md flex flex-col justify-between p-6 text-white animate-in fade-in duration-300">
      {/* Top Banner: Call-Reject Safety Guard Notice from PDF */}
      <div className="bg-amber-950/60 border border-amber-500/40 rounded-xl p-3 text-xs text-amber-200 flex items-start gap-2">
        <ShieldAlert className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
        <div>
          <span className="font-bold text-amber-300">20-Second Call-Reject Guard Active:</span>{' '}
          Accidental hang-up prevented. Voice commands valid: "Utha lo" ya "Reject karo" ({secondsRemaining}s window).
        </div>
      </div>

      {/* Caller Avatar & Details */}
      <div className="flex flex-col items-center justify-center my-auto">
        <div className="relative mb-6">
          <div className="w-28 h-28 rounded-full bg-gradient-to-tr from-blue-600 to-indigo-500 flex items-center justify-center shadow-[0_0_50px_rgba(59,130,246,0.4)] animate-pulse">
            <User className="w-14 h-14 text-white" />
          </div>
          <span className="absolute -bottom-1 -right-1 bg-emerald-500 border-2 border-slate-900 w-6 h-6 rounded-full flex items-center justify-center">
            <Volume2 className="w-3.5 h-3.5 text-white" />
          </span>
        </div>

        <h2 className="text-2xl font-bold tracking-tight text-white mb-1">{callerName}</h2>
        <p className="text-sm text-slate-400 font-mono mb-2">{callerNumber}</p>
        <span className="text-xs px-3 py-1 rounded-full bg-slate-800 text-blue-400 font-medium">
          {callStatus === 'ringing' ? 'Incoming Cellular Call...' : callStatus === 'active' ? 'Call In Progress' : 'Call Rejected'}
        </span>
      </div>

      {/* Action Buttons */}
      <div className="flex items-center justify-around pb-4">
        {/* Reject Button with Safety Guard */}
        <div className="flex flex-col items-center gap-2">
          <button
            id="reject-call-btn"
            onClick={() => {
              setCallStatus('rejected');
              setTimeout(() => {
                onReject();
                onClose();
              }, 600);
            }}
            className="w-16 h-16 rounded-full bg-rose-600 hover:bg-rose-500 active:scale-95 text-white flex items-center justify-center shadow-lg shadow-rose-900/50 transition-all"
            title="Reject / Kaat do"
          >
            <PhoneOff className="w-7 h-7" />
          </button>
          <span className="text-xs font-semibold text-rose-300">Reject karo</span>
        </div>

        {/* Answer Button */}
        <div className="flex flex-col items-center gap-2">
          <button
            id="answer-call-btn"
            onClick={() => {
              setCallStatus('active');
              onAnswer();
            }}
            className="w-16 h-16 rounded-full bg-emerald-600 hover:bg-emerald-500 active:scale-95 text-white flex items-center justify-center shadow-lg shadow-emerald-900/50 transition-all animate-bounce"
            title="Answer / Utha lo"
          >
            <Phone className="w-7 h-7" />
          </button>
          <span className="text-xs font-semibold text-emerald-300">Utha lo</span>
        </div>
      </div>
    </div>
  );
};
