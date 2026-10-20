/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from 'react';
import { 
  Play, 
  Pause, 
  RotateCcw, 
  CheckCircle2, 
  Clock, 
  Terminal, 
  Smartphone, 
  Eye, 
  MousePointer, 
  Type, 
  ArrowDownCircle, 
  ShieldCheck,
  AlertCircle,
  Sparkles,
  Layers
} from 'lucide-react';
import { Mission, ScreenElement } from '../types/myra';

interface MissionsTabProps {
  onRunVoiceCommand: (cmd: string) => void;
}

export const MissionsTab: React.FC<MissionsTabProps> = ({ onRunVoiceCommand }) => {
  const [activeTab, setActiveTab] = useState<'missions' | 'screen_agent' | 'task_logs'>('missions');

  // Sample active multi-step mission
  const [activeMission, setActiveMission] = useState<Mission>({
    id: 'm-101',
    title: 'Plan Delhi Trip & Itinerary',
    goal: 'Flight comparison, Hotel booking check, Weather alert, and Metro route caching',
    status: 'running',
    progress: 65,
    steps: [
      {
        id: 's1',
        title: 'Check Weather Forecast for Delhi',
        description: '28°C sunny, moderate AQI, light evening breeze expected',
        status: 'completed',
        app: 'Google Weather',
      },
      {
        id: 's2',
        title: 'Find Best Morning Flights',
        description: 'Selected IndiGo 6E-214 departs 07:15 AM at ₹4,150',
        status: 'completed',
        app: 'MakeMyTrip / Browser',
      },
      {
        id: 's3',
        title: 'Draft WhatsApp Itinerary to Rahul',
        description: 'Preparing flight and timing breakdown for final confirmation',
        status: 'in_progress',
        app: 'WhatsApp',
      },
      {
        id: 's4',
        title: 'Cache Delhi Metro Route Offline',
        description: 'Airport Express Line to Rajiv Chowk download',
        status: 'pending',
        app: 'Metro Guide',
      },
    ],
  });

  // Simulated accessibility screen inspector elements
  const [screenElements] = useState<ScreenElement[]>([
    { id: 101, text: 'Zomato Search "Paneer Tikka"', type: 'input', x: 200, y: 120, clickable: true },
    { id: 102, text: 'Haldiram’s Express (4.3★)', type: 'button', x: 190, y: 240, clickable: true },
    { id: 103, text: 'Add to Cart — ₹299', type: 'button', x: 310, y: 450, clickable: true },
    { id: 104, text: 'Apply Promo Coupon "TRYNEW"', type: 'button', x: 160, y: 560, clickable: true },
  ]);

  const [lastActionLog, setLastActionLog] = useState<string>(
    'Accessibility Tree parsed: 4 interactive nodes identified. Numeric ID 103 clicked.'
  );

  return (
    <div className="flex-1 flex flex-col p-4 bg-[#090d16] text-slate-100 select-none overflow-y-auto">
      {/* Header */}
      <div className="mb-4">
        <div className="flex items-center justify-between mb-1">
          <div className="flex items-center gap-2">
            <Layers className="w-5 h-5 text-blue-400" />
            <h2 className="text-base font-bold text-white tracking-tight">
              Missions & Screen Automation
            </h2>
          </div>
          <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-blue-950 text-blue-300 border border-blue-500/30">
            Autonomous Agent
          </span>
        </div>
        <p className="text-xs text-slate-400">
          Background execution & phone automation jab seedha tool na ho.
        </p>
      </div>

      {/* Sub-tab navigation */}
      <div className="flex rounded-xl bg-slate-900 p-1 border border-slate-800 text-xs font-medium mb-4">
        <button
          onClick={() => setActiveTab('missions')}
          className={`flex-1 py-1.5 rounded-lg transition-colors ${
            activeTab === 'missions' ? 'bg-blue-600 text-white font-semibold' : 'text-slate-400 hover:text-white'
          }`}
        >
          Active Mission (Sec 7)
        </button>
        <button
          onClick={() => setActiveTab('screen_agent')}
          className={`flex-1 py-1.5 rounded-lg transition-colors ${
            activeTab === 'screen_agent' ? 'bg-blue-600 text-white font-semibold' : 'text-slate-400 hover:text-white'
          }`}
        >
          Screen Agent (Sec 12)
        </button>
        <button
          onClick={() => setActiveTab('task_logs')}
          className={`flex-1 py-1.5 rounded-lg transition-colors ${
            activeTab === 'task_logs' ? 'bg-blue-600 text-white font-semibold' : 'text-slate-400 hover:text-white'
          }`}
        >
          Task Logs
        </button>
      </div>

      {/* Tab 1: Multi-Step Missions */}
      {activeTab === 'missions' && (
        <div className="space-y-4">
          <div className="bg-[#111726] border border-blue-500/30 rounded-2xl p-4 shadow-lg">
            <div className="flex items-start justify-between mb-2">
              <div>
                <span className="text-[10px] font-mono uppercase tracking-wider text-blue-400 bg-blue-950 px-2 py-0.5 rounded">
                  Mission Dashboard
                </span>
                <h3 className="text-sm font-bold text-white mt-1">{activeMission.title}</h3>
                <p className="text-xs text-slate-400 mt-0.5">{activeMission.goal}</p>
              </div>

              <div className="flex items-center gap-1.5">
                {activeMission.status === 'running' ? (
                  <button
                    onClick={() =>
                      setActiveMission({ ...activeMission, status: 'paused' })
                    }
                    className="p-1.5 rounded-lg bg-amber-500/20 text-amber-300 hover:bg-amber-500/30 text-xs flex items-center gap-1"
                    title="Pause Mission"
                  >
                    <Pause className="w-3.5 h-3.5" />
                    <span>Pause</span>
                  </button>
                ) : (
                  <button
                    onClick={() =>
                      setActiveMission({ ...activeMission, status: 'running' })
                    }
                    className="p-1.5 rounded-lg bg-emerald-500/20 text-emerald-300 hover:bg-emerald-500/30 text-xs flex items-center gap-1"
                    title="Resume Mission"
                  >
                    <Play className="w-3.5 h-3.5" />
                    <span>Resume</span>
                  </button>
                )}
              </div>
            </div>

            {/* Progress bar */}
            <div className="my-3">
              <div className="flex justify-between text-xs text-slate-400 mb-1">
                <span>Autonomous Progress</span>
                <span className="font-mono font-semibold text-blue-400">{activeMission.progress}%</span>
              </div>
              <div className="h-2 bg-slate-800 rounded-full overflow-hidden">
                <div
                  className="h-full bg-gradient-to-r from-blue-500 to-indigo-500 transition-all duration-500"
                  style={{ width: `${activeMission.progress}%` }}
                />
              </div>
            </div>

            {/* Steps list */}
            <div className="space-y-2 mt-4">
              {activeMission.steps.map((step, idx) => (
                <div
                  key={step.id}
                  className={`p-2.5 rounded-xl border flex items-start gap-3 transition-colors ${
                    step.status === 'completed'
                      ? 'bg-slate-900/60 border-slate-800 text-slate-300'
                      : step.status === 'in_progress'
                      ? 'bg-blue-950/40 border-blue-500/50 text-white'
                      : 'bg-slate-900/30 border-slate-800/60 text-slate-500'
                  }`}
                >
                  <div className="mt-0.5">
                    {step.status === 'completed' ? (
                      <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                    ) : step.status === 'in_progress' ? (
                      <span className="w-4 h-4 rounded-full border-2 border-blue-400 border-t-transparent animate-spin inline-block" />
                    ) : (
                      <Clock className="w-4 h-4 text-slate-600" />
                    )}
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-semibold">{step.title}</span>
                      {step.app && (
                        <span className="text-[10px] font-mono text-slate-400 bg-slate-800 px-1.5 py-0.2 rounded">
                          {step.app}
                        </span>
                      )}
                    </div>
                    <p className="text-[11px] text-slate-400 mt-0.5">{step.description}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Quick Mission Launch Buttons */}
          <div>
            <span className="text-xs font-semibold text-slate-400 block mb-2">
              Start Pre-built Autonomous Goals:
            </span>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              <button
                onClick={() => onRunVoiceCommand('Meri subah ki routine automate kar do')}
                className="p-3 bg-slate-900 hover:bg-slate-800 border border-slate-800 hover:border-blue-500/40 rounded-xl text-left transition-all group"
              >
                <span className="text-xs font-semibold text-white group-hover:text-blue-300 block">
                  🌅 Morning Routine Automator
                </span>
                <span className="text-[11px] text-slate-400 block mt-0.5">
                  Alarm, Weather, News & WhatsApp briefing
                </span>
              </button>

              <button
                onClick={() => onRunVoiceCommand('Zomato se pizza order kar do')}
                className="p-3 bg-slate-900 hover:bg-slate-800 border border-slate-800 hover:border-blue-500/40 rounded-xl text-left transition-all group"
              >
                <span className="text-xs font-semibold text-white group-hover:text-blue-300 block">
                  🍕 Zomato Food Order Handoff
                </span>
                <span className="text-[11px] text-slate-400 block mt-0.5">
                  Screen automation background agent
                </span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Tab 2: Screen Agent Inspector (Sec 12) */}
      {activeTab === 'screen_agent' && (
        <div className="space-y-4">
          <div className="bg-[#111726] border border-slate-800 rounded-2xl p-4">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <Smartphone className="w-4 h-4 text-blue-400" />
                <span className="text-xs font-bold text-white">Accessibility Tree & Numeric IDs</span>
              </div>
              <span className="text-[10px] text-emerald-400 font-mono bg-emerald-950/60 px-2 py-0.5 rounded border border-emerald-500/30">
                read_screen active
              </span>
            </div>

            <p className="text-xs text-slate-400 mb-3">
              MYRA parses the Android Accessibility Hierarchy so it can autonomously tap, type, and navigate third-party apps like Zomato, Ola, or Instagram.
            </p>

            {/* Elements Table */}
            <div className="border border-slate-800 rounded-xl overflow-hidden text-xs">
              <div className="bg-slate-900 px-3 py-2 grid grid-cols-4 font-semibold text-slate-400">
                <span>ID</span>
                <span className="col-span-2">Element Text</span>
                <span className="text-right">Action</span>
              </div>
              <div className="divide-y divide-slate-800/80 bg-slate-950/50">
                {screenElements.map((el) => (
                  <div key={el.id} className="px-3 py-2.5 grid grid-cols-4 items-center">
                    <span className="font-mono text-blue-400 font-bold">#{el.id}</span>
                    <span className="col-span-2 text-slate-200 truncate">{el.text}</span>
                    <div className="text-right">
                      <button
                        onClick={() =>
                          setLastActionLog(`tap_element executed on #${el.id}: "${el.text}"`)
                        }
                        className="px-2 py-1 bg-slate-800 hover:bg-blue-600 text-slate-300 hover:text-white rounded text-[10px] font-mono transition-colors"
                      >
                        tap_element
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Live Terminal Log */}
            <div className="mt-3 bg-black/80 border border-slate-800 rounded-xl p-3 font-mono text-[11px] text-emerald-400 flex items-start gap-2">
              <Terminal className="w-3.5 h-3.5 shrink-0 mt-0.5 text-slate-500" />
              <span>{lastActionLog}</span>
            </div>
          </div>
        </div>
      )}

      {/* Tab 3: Task Logs */}
      {activeTab === 'task_logs' && (
        <div className="space-y-3">
          <div className="text-xs text-slate-400 mb-1">
            "MYRA ne X kyun kiya" samajhne ke liye complete audit trail:
          </div>

          <div className="space-y-2">
            {[
              {
                time: '10:04 AM',
                tool: 'send_whatsapp',
                action: 'Drafted WhatsApp to Rahul',
                reason: 'Triggered by user voice command: "Rahul ko WhatsApp pe bolo..."',
                status: 'Success',
              },
              {
                time: '09:48 AM',
                tool: 'toggle_flashlight',
                action: 'Turned ON Torch',
                reason: 'Voice command "Torch on karo"',
                status: 'Success',
              },
              {
                time: '09:15 AM',
                tool: 'lock_device',
                action: 'Immediate screen lock triggered',
                reason: 'Safety critical priority execution without dialog confirmation',
                status: 'Success',
              },
            ].map((log, i) => (
              <div
                key={i}
                className="bg-[#111726] border border-slate-800/80 p-3 rounded-xl text-xs space-y-1"
              >
                <div className="flex items-center justify-between">
                  <span className="font-mono text-blue-400 font-semibold">{log.tool}</span>
                  <span className="text-[10px] text-slate-500">{log.time}</span>
                </div>
                <p className="text-white font-medium">{log.action}</p>
                <p className="text-[11px] text-slate-400">{log.reason}</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
