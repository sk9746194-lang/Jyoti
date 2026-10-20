/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from 'react';
import { 
  Zap, 
  Plus, 
  Clock, 
  Bell, 
  BatteryCharging, 
  Smartphone, 
  Headphones, 
  Wifi, 
  Check, 
  Sparkles, 
  Power,
  Trash2,
  BrainCircuit
} from 'lucide-react';
import { TriggerRule } from '../types/myra';

interface TriggersTabProps {
  onTestTrigger: (rule: TriggerRule) => void;
}

export const TriggersTab: React.FC<TriggersTabProps> = ({ onTestTrigger }) => {
  const [triggers, setTriggers] = useState<TriggerRule[]>([
    {
      id: 'tr-1',
      name: 'Morning Wakeup Briefing',
      type: 'scheduled',
      condition: 'Every weekday at 07:00 AM',
      actionType: 'speak_line',
      actionValue: 'Good morning boss! Delhi weather 24°C hai, aur 3 unread WhatsApp messages hain.',
      enabled: true,
    },
    {
      id: 'tr-2',
      name: 'Charger Plugged In Alert',
      type: 'charging',
      condition: 'When charger plugged in',
      actionType: 'speak_line',
      actionValue: 'Fast Charging started. Est. full charge in 32 minutes.',
      enabled: true,
    },
    {
      id: 'tr-3',
      name: 'Headphones Connected Music',
      type: 'headphones',
      condition: 'When Bluetooth headphones connect',
      actionType: 'run_mission',
      actionValue: 'Play recent Spotify playlist & set volume to 60%',
      enabled: true,
    },
    {
      id: 'tr-4',
      name: 'Low Battery Auto Power Save',
      type: 'battery',
      condition: 'Battery drops below 20%',
      actionType: 'speak_line',
      actionValue: 'Battery 20% se kam hai. Ultra Power Saving mode activate kar diya gaya hai.',
      enabled: true,
    },
  ]);

  const [isCreating, setIsCreating] = useState(false);
  const [newRuleName, setNewRuleName] = useState('');
  const [newRuleType, setNewRuleType] = useState<TriggerRule['type']>('charging');
  const [newRuleAction, setNewRuleAction] = useState('');

  const handleToggle = (id: string) => {
    setTriggers((prev) =>
      prev.map((t) => (t.id === id ? { ...t, enabled: !t.enabled } : t))
    );
  };

  const handleDelete = (id: string) => {
    setTriggers((prev) => prev.filter((t) => t.id !== id));
  };

  const handleAdd = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newRuleName.trim() || !newRuleAction.trim()) return;

    const newTrigger: TriggerRule = {
      id: `tr-${Date.now()}`,
      name: newRuleName.trim(),
      type: newRuleType,
      condition:
        newRuleType === 'scheduled'
          ? 'Daily at specified time'
          : newRuleType === 'charging'
          ? 'When charging state changes'
          : newRuleType === 'wifi'
          ? 'When home Wi-Fi connects'
          : 'Trigger condition met',
      actionType: 'speak_line',
      actionValue: newRuleAction.trim(),
      enabled: true,
    };

    setTriggers([...triggers, newTrigger]);
    setNewRuleName('');
    setNewRuleAction('');
    setIsCreating(false);
  };

  return (
    <div className="flex-1 flex flex-col p-4 bg-[#090d16] text-slate-100 select-none overflow-y-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-3">
        <div>
          <h2 className="text-base font-bold text-white tracking-tight flex items-center gap-2">
            <Zap className="w-5 h-5 text-amber-400" />
            <span>Automation & Triggers</span>
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            "Agar X ho to Y karo" rules engine (Section 15).
          </p>
        </div>
        <button
          onClick={() => setIsCreating(!isCreating)}
          className="p-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white flex items-center gap-1 text-xs font-semibold shadow-md active:scale-95 transition-all"
        >
          <Plus className="w-4 h-4" />
          <span>Naya Trigger</span>
        </button>
      </div>

      {/* Habit Tracking Proactive Card (Page 11) */}
      <div className="bg-gradient-to-r from-blue-950/60 to-indigo-950/60 border border-blue-500/40 rounded-2xl p-3.5 mb-4 shadow-lg flex items-start gap-3">
        <div className="p-2 rounded-xl bg-blue-500/20 text-blue-400 mt-0.5 shrink-0">
          <BrainCircuit className="w-5 h-5" />
        </div>
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="text-xs font-bold text-white">Autonomous Habit Tracking (AI Learning)</span>
            <span className="text-[9px] bg-emerald-950 text-emerald-300 font-mono px-1.5 py-0.2 rounded border border-emerald-500/30">
              ALWAYS ON
            </span>
          </div>
          <p className="text-xs text-slate-300 leading-snug">
            MYRA aapke daily patterns automatically seekhti hai:
          </p>
          <div className="mt-2 bg-black/40 border border-slate-800 rounded-xl p-2 text-xs italic text-blue-300">
            "Boss, aap roughly 9 baje Maps kholte ho, kuch chahiye?"
          </div>
        </div>
      </div>

      {/* New Rule Modal/Form */}
      {isCreating && (
        <form
          onSubmit={handleAdd}
          className="bg-[#111726] border border-blue-500/40 rounded-2xl p-4 mb-4 space-y-3 shadow-xl animate-in fade-in"
        >
          <h3 className="text-xs font-bold text-white uppercase tracking-wider">
            Naya Trigger Create Karein
          </h3>

          <div>
            <label className="text-xs text-slate-400 block mb-1">Trigger Ka Naam:</label>
            <input
              type="text"
              value={newRuleName}
              onChange={(e) => setNewRuleName(e.target.value)}
              placeholder="e.g. Work Location Reached"
              className="w-full bg-slate-900 border border-slate-700 text-xs text-white rounded-xl p-2.5 outline-none focus:border-blue-500"
            />
          </div>

          <div>
            <label className="text-xs text-slate-400 block mb-1">Trigger Type (Event):</label>
            <select
              value={newRuleType}
              onChange={(e) => setNewRuleType(e.target.value as any)}
              className="w-full bg-slate-900 border border-slate-700 text-xs text-white rounded-xl p-2.5 outline-none focus:border-blue-500"
            >
              <option value="scheduled">Scheduled Time (Fix time pe)</option>
              <option value="charging">Charging State (Charger plug/unplug)</option>
              <option value="battery">Battery Level (Threshold cross)</option>
              <option value="headphones">Headphones Connect/Disconnect</option>
              <option value="wifi">Wi-Fi Connect/Disconnect</option>
              <option value="notification">Notification Arrival</option>
              <option value="screen">Screen On / Unlock</option>
            </select>
          </div>

          <div>
            <label className="text-xs text-slate-400 block mb-1">Action (MYRA kya bole ya kare):</label>
            <input
              type="text"
              value={newRuleAction}
              onChange={(e) => setNewRuleAction(e.target.value)}
              placeholder="e.g. Welcome to office! Sound profile silent kar di hai."
              className="w-full bg-slate-900 border border-slate-700 text-xs text-white rounded-xl p-2.5 outline-none focus:border-blue-500"
            />
          </div>

          <div className="flex gap-2 pt-1">
            <button
              type="button"
              onClick={() => setIsCreating(false)}
              className="flex-1 py-2 rounded-xl bg-slate-800 text-slate-300 text-xs font-semibold"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="flex-1 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold"
            >
              Save Rule
            </button>
          </div>
        </form>
      )}

      {/* Rules List */}
      <div className="space-y-3">
        {triggers.map((rule) => (
          <div
            key={rule.id}
            className={`p-3.5 rounded-2xl border transition-all ${
              rule.enabled
                ? 'bg-[#111726] border-slate-800/80 shadow-md'
                : 'bg-slate-900/40 border-slate-900 opacity-60'
            }`}
          >
            <div className="flex items-start justify-between mb-1.5">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg bg-slate-800 text-amber-400">
                  {rule.type === 'scheduled' && <Clock className="w-4 h-4" />}
                  {rule.type === 'charging' && <BatteryCharging className="w-4 h-4" />}
                  {rule.type === 'headphones' && <Headphones className="w-4 h-4" />}
                  {rule.type === 'wifi' && <Wifi className="w-4 h-4" />}
                  {rule.type === 'battery' && <Power className="w-4 h-4" />}
                </div>
                <div>
                  <h4 className="text-xs font-bold text-white">{rule.name}</h4>
                  <span className="text-[10px] text-slate-400 font-mono">{rule.condition}</span>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => onTestTrigger(rule)}
                  className="px-2 py-0.5 rounded text-[10px] bg-blue-600/30 text-blue-300 border border-blue-500/40 hover:bg-blue-600 hover:text-white transition-colors"
                >
                  Test Run
                </button>
                <button
                  onClick={() => handleToggle(rule.id)}
                  className={`w-9 h-5 rounded-full p-0.5 transition-colors ${
                    rule.enabled ? 'bg-blue-600' : 'bg-slate-700'
                  }`}
                >
                  <div
                    className={`w-4 h-4 rounded-full bg-white transition-transform ${
                      rule.enabled ? 'translate-x-4' : 'translate-x-0'
                    }`}
                  />
                </button>
                <button
                  onClick={() => handleDelete(rule.id)}
                  className="p-1 text-slate-600 hover:text-rose-400 transition-colors"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            <div className="bg-slate-900/80 rounded-xl p-2.5 mt-2 text-xs text-slate-300 flex items-start gap-2 border border-slate-800/60">
              <span className="text-[10px] font-mono text-emerald-400 bg-emerald-950/60 px-1 rounded shrink-0 mt-0.5">
                ACTION
              </span>
              <span className="text-[11px] leading-relaxed italic">"{rule.actionValue}"</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
