/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from 'react';
import { 
  CheckCircle2, 
  Circle, 
  Plus, 
  Flame, 
  Trophy, 
  ChevronDown, 
  ChevronUp, 
  Trash2, 
  Sparkles, 
  Volume2, 
  Calendar,
  X,
  Dumbbell,
  Droplet,
  Sun,
  BrainCircuit,
  Target
} from 'lucide-react';
import { DailyGoal } from '../types/myra';
import { playSuccessChime } from '../utils/audioEffects';

interface DailyGoalTrackerProps {
  onSpeakStatus?: (text: string) => void;
  personality?: string;
}

const DEFAULT_GOALS: DailyGoal[] = [
  {
    id: 'g1',
    title: 'Morning Briefing with MYRA',
    category: 'routine',
    completed: true,
    timeTarget: '07:30 AM',
    icon: '☀️',
  },
  {
    id: 'g2',
    title: 'Drink 2.5L Water',
    category: 'health',
    completed: true,
    timeTarget: '2.5 Liters',
    icon: '💧',
  },
  {
    id: 'g3',
    title: '30 min Evening Walk / Workout',
    category: 'health',
    completed: false,
    timeTarget: '06:00 PM',
    icon: '🏃‍♂️',
  },
  {
    id: 'g4',
    title: 'Complete 1 Deep Research Mission',
    category: 'productivity',
    completed: false,
    timeTarget: '1 Report',
    icon: '⚡',
  },
];

const PRESET_GOALS = [
  { title: 'Drink 500ml Water', category: 'health', icon: '💧', timeTarget: 'Health' },
  { title: '15 min Mindfulness / Meditation', category: 'routine', icon: '🧘', timeTarget: 'Mind' },
  { title: 'Read 10 pages of book', category: 'learning', icon: '📚', timeTarget: 'Read' },
  { title: 'Organize PC Workspace', category: 'productivity', icon: '💻', timeTarget: 'Focus' },
];

const CATEGORY_STYLES = {
  health: {
    bg: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
    dot: 'bg-emerald-400',
    label: 'Health',
  },
  productivity: {
    bg: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
    dot: 'bg-blue-400',
    label: 'Productivity',
  },
  routine: {
    bg: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
    dot: 'bg-amber-400',
    label: 'Routine',
  },
  learning: {
    bg: 'bg-purple-500/10 text-purple-400 border-purple-500/20',
    dot: 'bg-purple-400',
    label: 'Learning',
  },
};

export const DailyGoalTracker: React.FC<DailyGoalTrackerProps> = ({
  onSpeakStatus,
  personality,
}) => {
  const [goals, setGoals] = useState<DailyGoal[]>(() => {
    try {
      const saved = localStorage.getItem('myra_daily_goals_2026');
      if (saved) return JSON.parse(saved);
    } catch (e) {}
    return DEFAULT_GOALS;
  });

  const [isExpanded, setIsExpanded] = useState<boolean>(false);
  const [isAdding, setIsAdding] = useState<boolean>(false);
  const [newTitle, setNewTitle] = useState<string>('');
  const [newCategory, setNewCategory] = useState<DailyGoal['category']>('health');
  const [newTarget, setNewTarget] = useState<string>('');
  const [streakDays, setStreakDays] = useState<number>(5);

  // Save to localStorage
  useEffect(() => {
    try {
      localStorage.setItem('myra_daily_goals_2026', JSON.stringify(goals));
    } catch (e) {}
  }, [goals]);

  const totalGoals = goals.length;
  const completedGoals = goals.filter((g) => g.completed).length;
  const percentage = totalGoals > 0 ? Math.round((completedGoals / totalGoals) * 100) : 0;
  const allCompleted = totalGoals > 0 && completedGoals === totalGoals;

  // Toggle Goal completion
  const handleToggle = (id: string) => {
    setGoals((prev) =>
      prev.map((g) => {
        if (g.id === id) {
          const nextState = !g.completed;
          if (nextState) {
            playSuccessChime();
            // Voice encouragement
            if (onSpeakStatus) {
              const praise = personality === 'romantic_gf'
                ? `Wah mere hero! "${g.title}" complete ho gaya! Bahut proud hoon aap par ❤️`
                : `Shabaash! Daily Goal "${g.title}" complete ho gaya! Aaj ke ${completedGoals + 1}/${totalGoals} tasks poore ho chuke hain.`;
              onSpeakStatus(praise);
            }
          }
          return { ...g, completed: nextState };
        }
        return g;
      })
    );
  };

  // Add new Goal
  const handleAddGoal = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim()) return;

    const newGoal: DailyGoal = {
      id: `goal-${Date.now()}`,
      title: newTitle.trim(),
      category: newCategory,
      completed: false,
      timeTarget: newTarget.trim() || undefined,
      icon: newCategory === 'health' ? '🏃‍♂️' : newCategory === 'productivity' ? '⚡' : newCategory === 'learning' ? '📚' : '☀️',
    };

    setGoals((prev) => [...prev, newGoal]);
    setNewTitle('');
    setNewTarget('');
    setIsAdding(false);
  };

  // Add quick preset
  const handleAddPreset = (preset: typeof PRESET_GOALS[0]) => {
    const newGoal: DailyGoal = {
      id: `goal-${Date.now()}`,
      title: preset.title,
      category: preset.category as any,
      completed: false,
      timeTarget: preset.timeTarget,
      icon: preset.icon,
    };
    setGoals((prev) => [...prev, newGoal]);
  };

  // Delete Goal
  const handleDelete = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setGoals((prev) => prev.filter((g) => g.id !== id));
  };

  // Announce Status via Voice
  const handleAnnounceStatus = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (!onSpeakStatus) return;
    const remaining = totalGoals - completedGoals;
    let text = '';
    if (allCompleted) {
      text = `Badhaai ho! Aapne aaj ke sabhi ${totalGoals} daily goals poore kar liye hain. 100% completion achieved!`;
    } else {
      text = `Aapke aaj ${totalGoals} mein se ${completedGoals} goals poore hain (${percentage}% done). ${remaining} tasks bache hain: ${goals.filter(g => !g.completed).map(g => g.title).join(', ')}.`;
    }
    onSpeakStatus(text);
  };

  // Circular Ring SVG Calculations (radius 24, stroke 5)
  const ringRadius = 26;
  const strokeWidth = 5;
  const circumference = 2 * Math.PI * ringRadius;
  const strokeDashoffset = circumference - (percentage / 100) * circumference;

  return (
    <div className="w-full bg-[#111726]/90 border border-blue-500/25 rounded-2xl p-3 shadow-xl backdrop-blur-md transition-all duration-300">
      {/* Header Bar with Progress Ring & Summary */}
      <div 
        onClick={() => setIsExpanded(!isExpanded)}
        className="flex items-center justify-between cursor-pointer group select-none"
      >
        <div className="flex items-center gap-3">
          {/* Progress Ring */}
          <div className="relative w-14 h-14 shrink-0 flex items-center justify-center">
            <svg className="w-14 h-14 -rotate-90 transform" viewBox="0 0 64 64">
              {/* Background track circle */}
              <circle
                cx="32"
                cy="32"
                r={ringRadius}
                fill="none"
                stroke="#1e293b"
                strokeWidth={strokeWidth}
              />
              {/* Animated Progress circle */}
              <circle
                cx="32"
                cy="32"
                r={ringRadius}
                fill="none"
                stroke={allCompleted ? '#10b981' : percentage > 50 ? '#38bdf8' : '#6366f1'}
                strokeWidth={strokeWidth}
                strokeDasharray={circumference}
                strokeDashoffset={strokeDashoffset}
                strokeLinecap="round"
                className="transition-all duration-700 ease-out"
              />
            </svg>
            
            {/* Center Content */}
            <div className="absolute inset-0 flex flex-col items-center justify-center">
              {allCompleted ? (
                <Trophy className="w-4 h-4 text-emerald-400 animate-bounce" />
              ) : (
                <span className="text-[11px] font-bold font-mono text-white leading-none">
                  {percentage}%
                </span>
              )}
            </div>
          </div>

          {/* Title & Streak info */}
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold text-white tracking-wide group-hover:text-blue-300 transition-colors">
                Daily Goals & Habit Tracker
              </span>
              <span className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-amber-950/80 border border-amber-500/30 text-amber-300 flex items-center gap-0.5">
                <Flame className="w-3 h-3 text-amber-400 fill-amber-400" />
                {streakDays}d Streak
              </span>
            </div>

            <div className="flex items-center gap-2 mt-0.5 text-[11px] text-slate-400">
              <span className="font-medium text-slate-300 font-mono">
                {completedGoals}/{totalGoals} Completed
              </span>
              <span>•</span>
              <span className={allCompleted ? 'text-emerald-400 font-semibold' : 'text-slate-400'}>
                {allCompleted ? '🎉 All tasks done!' : `${totalGoals - completedGoals} tasks remaining`}
              </span>
            </div>
          </div>
        </div>

        {/* Action icons: Speak & Expand */}
        <div className="flex items-center gap-1.5">
          <button
            onClick={handleAnnounceStatus}
            className="p-1.5 rounded-lg bg-slate-800/80 hover:bg-slate-700 text-slate-300 hover:text-blue-300 transition-colors"
            title="Speak Goals Status with MYRA"
          >
            <Volume2 className="w-3.5 h-3.5" />
          </button>
          <div className="p-1 text-slate-400 group-hover:text-white transition-colors">
            {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </div>
        </div>
      </div>

      {/* Expanded Task Checklist & Management */}
      {isExpanded && (
        <div className="mt-3 pt-3 border-t border-slate-800/80 space-y-2.5 animate-in fade-in slide-in-from-top-2 duration-200">
          {/* Quick Preset Chips */}
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-none no-scrollbar">
            <span className="text-[10px] text-slate-500 whitespace-nowrap uppercase font-semibold">
              + Quick Add:
            </span>
            {PRESET_GOALS.map((preset, idx) => (
              <button
                key={idx}
                onClick={() => handleAddPreset(preset)}
                className="shrink-0 text-[10px] bg-slate-900/90 hover:bg-slate-800 text-slate-300 border border-slate-800 hover:border-blue-500/40 px-2 py-0.5 rounded-full flex items-center gap-1 transition-all active:scale-95"
              >
                <span>{preset.icon}</span>
                <span>{preset.title}</span>
              </button>
            ))}
          </div>

          {/* Goal Items List */}
          <div className="space-y-1.5 max-h-52 overflow-y-auto pr-1">
            {goals.map((goal) => {
              const catStyle = CATEGORY_STYLES[goal.category] || CATEGORY_STYLES.health;
              return (
                <div
                  key={goal.id}
                  onClick={() => handleToggle(goal.id)}
                  className={`flex items-center justify-between p-2 rounded-xl border transition-all cursor-pointer select-none group ${
                    goal.completed
                      ? 'bg-emerald-950/20 border-emerald-500/20 opacity-80'
                      : 'bg-slate-900/60 border-slate-800/80 hover:border-blue-500/40 hover:bg-slate-900'
                  }`}
                >
                  <div className="flex items-center gap-2.5 min-w-0">
                    <button
                      className="shrink-0 transition-transform group-hover:scale-110 active:scale-90"
                      title={goal.completed ? 'Mark pending' : 'Mark completed'}
                    >
                      {goal.completed ? (
                        <CheckCircle2 className="w-4 h-4 text-emerald-400 fill-emerald-400/20" />
                      ) : (
                        <Circle className="w-4 h-4 text-slate-500 group-hover:text-blue-400" />
                      )}
                    </button>

                    <span className="text-xs shrink-0">{goal.icon || '🎯'}</span>

                    <div className="truncate">
                      <span
                        className={`text-xs block truncate ${
                          goal.completed
                            ? 'line-through text-slate-400 font-normal'
                            : 'text-slate-200 font-medium'
                        }`}
                      >
                        {goal.title}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 shrink-0 ml-2">
                    {goal.timeTarget && (
                      <span className="text-[9px] font-mono text-slate-400 bg-black/40 px-1.5 py-0.5 rounded">
                        {goal.timeTarget}
                      </span>
                    )}
                    <span
                      className={`text-[9px] font-semibold px-1.5 py-0.2 rounded border ${catStyle.bg}`}
                    >
                      {catStyle.label}
                    </span>
                    <button
                      onClick={(e) => handleDelete(goal.id, e)}
                      className="text-slate-600 hover:text-rose-400 p-0.5 rounded transition-colors opacity-0 group-hover:opacity-100"
                      title="Delete goal"
                    >
                      <Trash2 className="w-3 h-3" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Add Custom Goal Toggle Form */}
          {isAdding ? (
            <form
              onSubmit={handleAddGoal}
              className="bg-slate-900/90 border border-blue-500/40 rounded-xl p-2.5 space-y-2 animate-in fade-in duration-150"
            >
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-bold text-white uppercase tracking-wider">
                  Naya Daily Goal Jodein
                </span>
                <button
                  type="button"
                  onClick={() => setIsAdding(false)}
                  className="text-slate-400 hover:text-slate-200"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              </div>

              <input
                type="text"
                value={newTitle}
                onChange={(e) => setNewTitle(e.target.value)}
                placeholder="e.g. 45 min Gym workout, 2 chapter padho..."
                className="w-full bg-slate-950 border border-slate-700 focus:border-blue-500 text-xs text-white rounded-lg p-2 outline-none"
                autoFocus
              />

              <div className="grid grid-cols-2 gap-2 text-xs">
                <div>
                  <label className="text-[10px] text-slate-400 block mb-0.5">Category:</label>
                  <select
                    value={newCategory}
                    onChange={(e) => setNewCategory(e.target.value as any)}
                    className="w-full bg-slate-950 border border-slate-700 text-slate-200 text-[11px] rounded-lg p-1.5 outline-none"
                  >
                    <option value="health">Health & Fitness</option>
                    <option value="productivity">Productivity</option>
                    <option value="routine">Daily Routine</option>
                    <option value="learning">Learning & Skill</option>
                  </select>
                </div>
                <div>
                  <label className="text-[10px] text-slate-400 block mb-0.5">Target / Time (Opt):</label>
                  <input
                    type="text"
                    value={newTarget}
                    onChange={(e) => setNewTarget(e.target.value)}
                    placeholder="e.g. 08:00 PM, 30 min"
                    className="w-full bg-slate-950 border border-slate-700 text-slate-200 text-[11px] rounded-lg p-1.5 outline-none"
                  />
                </div>
              </div>

              <div className="flex gap-2 pt-1">
                <button
                  type="button"
                  onClick={() => setIsAdding(false)}
                  className="flex-1 py-1.5 bg-slate-800 text-slate-300 rounded-lg text-xs font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 py-1.5 bg-blue-600 hover:bg-blue-500 text-white rounded-lg text-xs font-semibold"
                >
                  Save Goal
                </button>
              </div>
            </form>
          ) : (
            <button
              onClick={() => setIsAdding(true)}
              className="w-full py-1.5 bg-slate-900/70 hover:bg-slate-900 border border-dashed border-slate-800 hover:border-blue-500/50 text-slate-400 hover:text-blue-300 rounded-xl text-xs font-medium flex items-center justify-center gap-1.5 transition-all"
            >
              <Plus className="w-3.5 h-3.5 text-blue-400" />
              <span>Naya Goal Add Karein</span>
            </button>
          )}
        </div>
      )}
    </div>
  );
};
