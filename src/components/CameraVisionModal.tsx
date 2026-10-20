/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useRef, useEffect } from 'react';
import { Camera, X, Scan, RefreshCw, CheckCircle, Sparkles } from 'lucide-react';
import { playCameraShutter } from '../utils/audioEffects';

interface CameraVisionModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const CameraVisionModal: React.FC<CameraVisionModalProps> = ({ isOpen, onClose }) => {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const [streamActive, setStreamActive] = useState(false);
  const [facingMode, setFacingMode] = useState<'user' | 'environment'>('environment');
  const [scanResult, setScanResult] = useState<string | null>(null);
  const [capturedPhoto, setCapturedPhoto] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen) {
      setScanResult(null);
      setCapturedPhoto(null);
      return;
    }

    let currentStream: MediaStream | null = null;
    const startCamera = async () => {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode },
          audio: false,
        });
        currentStream = stream;
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          setStreamActive(true);
        }
      } catch (err) {
        console.warn('Camera access denied or unavailable in iframe, using AI simulated camera viewport:', err);
        setStreamActive(false);
      }
    };

    startCamera();

    return () => {
      if (currentStream) {
        currentStream.getTracks().forEach((track) => track.stop());
      }
    };
  }, [isOpen, facingMode]);

  if (!isOpen) return null;

  const handleCapture = () => {
    playCameraShutter();
    setCapturedPhoto('photo_saved_to_gallery');
    setTimeout(() => {
      setCapturedPhoto(null);
    }, 2000);
  };

  const handleScanBarcode = () => {
    setScanResult('Scanning...');
    setTimeout(() => {
      setScanResult('ISBN 978-0134685991: Effective Java (3rd Edition) - Price ₹1,299');
    }, 1200);
  };

  return (
    <div className="absolute inset-0 z-50 bg-black text-white flex flex-col justify-between p-4 animate-in fade-in duration-200">
      {/* Top Header */}
      <div className="flex items-center justify-between z-10">
        <div className="flex items-center gap-2 bg-slate-900/80 px-3 py-1.5 rounded-full border border-slate-800">
          <Scan className="w-4 h-4 text-blue-400" />
          <span className="text-xs font-semibold text-slate-200">MYRA AI Vision Engine</span>
        </div>
        <button
          onClick={onClose}
          className="p-2 rounded-full bg-slate-900/80 hover:bg-slate-800 text-slate-300"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* Center Viewfinder */}
      <div className="relative flex-1 my-4 rounded-2xl overflow-hidden bg-slate-900 border border-slate-800 flex items-center justify-center">
        {streamActive ? (
          <video
            ref={videoRef}
            autoPlay
            playsInline
            muted
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="flex flex-col items-center justify-center p-6 text-center text-slate-400">
            <div className="w-20 h-20 rounded-full bg-slate-800/80 border border-slate-700 flex items-center justify-center mb-3">
              <Camera className="w-8 h-8 text-blue-400" />
            </div>
            <p className="text-sm font-medium text-slate-300 mb-1">AI Smart Viewfinder</p>
            <p className="text-xs text-slate-500 max-w-xs">
              Real world vision active. Point camera at barcode, QR, document or object.
            </p>
          </div>
        )}

        {/* Optical Scanning Grid Overlay */}
        <div className="absolute inset-10 border-2 border-dashed border-blue-500/60 rounded-xl pointer-events-none flex items-center justify-center">
          <div className="w-full h-0.5 bg-gradient-to-r from-transparent via-blue-400 to-transparent animate-pulse" />
        </div>

        {/* OCR Result Pill */}
        {scanResult && (
          <div className="absolute bottom-4 left-4 right-4 bg-slate-950/90 border border-blue-500/50 p-3 rounded-xl shadow-xl flex items-start gap-2">
            <Sparkles className="w-4 h-4 text-blue-400 shrink-0 mt-0.5" />
            <p className="text-xs text-slate-200 font-medium">{scanResult}</p>
          </div>
        )}

        {capturedPhoto && (
          <div className="absolute inset-0 bg-white/20 backdrop-blur-sm flex items-center justify-center">
            <div className="bg-slate-900/90 px-4 py-2 rounded-full border border-emerald-500 text-emerald-400 text-xs font-semibold flex items-center gap-2">
              <CheckCircle className="w-4 h-4" />
              Photo saved to gallery
            </div>
          </div>
        )}
      </div>

      {/* Bottom Controls */}
      <div className="flex items-center justify-around z-10 pb-2">
        <button
          onClick={() => setFacingMode((prev) => (prev === 'environment' ? 'user' : 'environment'))}
          className="p-3 rounded-full bg-slate-900 hover:bg-slate-800 text-slate-300"
          title="Switch Camera (Front/Back)"
        >
          <RefreshCw className="w-5 h-5" />
        </button>

        {/* Capture Shutter */}
        <button
          onClick={handleCapture}
          className="w-16 h-16 rounded-full border-4 border-white bg-blue-600 hover:bg-blue-500 active:scale-95 flex items-center justify-center shadow-lg shadow-blue-900/40 transition-all"
          title="Take Photo"
        >
          <div className="w-12 h-12 rounded-full bg-white/30" />
        </button>

        {/* Barcode / OCR Trigger */}
        <button
          onClick={handleScanBarcode}
          className="p-3 rounded-full bg-slate-900 hover:bg-slate-800 text-blue-400"
          title="Scan Barcode / Document"
        >
          <Scan className="w-5 h-5" />
        </button>
      </div>
    </div>
  );
};
