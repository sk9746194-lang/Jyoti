/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { Component, ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('[MYRA Preview ErrorBoundary caught]:', error, errorInfo);
  }

  private handleReset = () => {
    this.setState({ hasError: false, error: null });
    window.location.reload();
  };

  public render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-[#070b13] text-white flex flex-col items-center justify-center p-6 text-center select-none font-sans">
          <div className="w-16 h-16 rounded-2xl bg-amber-500/20 border border-amber-500/40 flex items-center justify-center text-amber-400 mb-4 shadow-lg shadow-amber-950/50">
            <AlertTriangle className="w-8 h-8" />
          </div>

          <h2 className="text-xl font-bold text-slate-100 mb-2">MYRA Preview Safe Mode</h2>
          <p className="text-sm text-slate-400 max-w-md mb-4 leading-relaxed">
            Preview environment me runtime error detect hua. MYRA ki UI safety boundary ne crash prevent kiya.
          </p>

          <div className="w-full max-w-md bg-slate-900/90 border border-slate-800 rounded-xl p-3 text-left mb-6 overflow-x-auto text-xs font-mono text-rose-300">
            {this.state.error?.message || 'Unknown runtime error'}
          </div>

          <button
            onClick={this.handleReset}
            className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-xs flex items-center gap-2 shadow-lg active:scale-95 transition-all"
          >
            <RefreshCw className="w-4 h-4" />
            <span>Reload MYRA</span>
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}
