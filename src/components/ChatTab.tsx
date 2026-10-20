/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useRef, useEffect, useMemo } from 'react';
import { 
  Send, 
  Mic, 
  Play, 
  Pause, 
  User, 
  Sparkles, 
  Bot, 
  Users, 
  Search, 
  CheckCheck,
  Volume2,
  Zap,
  CornerDownLeft
} from 'lucide-react';
import { ChatMessage, PersonalityMode } from '../types/myra';
import { generateQuickReplies } from '../utils/quickReplies';
import { triggerHapticFeedback } from '../utils/audioEffects';

interface ChatTabProps {
  messages: ChatMessage[];
  onSendMessage: (text: string) => void;
  personality: PersonalityMode;
}

export const ChatTab: React.FC<ChatTabProps> = ({
  messages,
  onSendMessage,
  personality,
}) => {
  const [inputText, setInputText] = useState('');
  const [activeChat, setActiveChat] = useState<'myra' | 'rahul' | 'tech_group'>('myra');
  const [playingVoiceId, setPlayingVoiceId] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputText.trim()) return;
    triggerHapticFeedback('command_confirm');
    onSendMessage(inputText.trim());
    setInputText('');
  };

  const handleSendVoiceNote = () => {
    triggerHapticFeedback('command_confirm');
    onSendMessage('🎤 [Voice Note recorded in Hindi: "MYRA aaj ka schedule batao"]');
  };

  const handleQuickReplyTap = (replyText: string) => {
    triggerHapticFeedback('tap');
    onSendMessage(replyText);
  };

  // Derive contextual suggested quick replies based on the latest message from MYRA
  const activeQuickReplies = useMemo(() => {
    if (messages.length === 0) return [];
    // Find the latest message from MYRA
    const lastMyraMsg = [...messages].reverse().find((m) => m.sender === 'myra');
    if (!lastMyraMsg) return [];
    
    // If the message specifically supplied custom quickReplies, use them
    if (lastMyraMsg.quickReplies && lastMyraMsg.quickReplies.length > 0) {
      return lastMyraMsg.quickReplies;
    }

    return generateQuickReplies(
      lastMyraMsg.text,
      lastMyraMsg.category,
      lastMyraMsg.action,
      personality
    );
  }, [messages, personality]);

  return (
    <div className="flex-1 flex flex-col h-full bg-[#090d16] select-none">
      {/* Top Chat Channel Selector (MYRA vs Contacts) */}
      <div className="p-3 border-b border-slate-800/80 bg-[#0d121f] flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="relative">
            <div className="w-9 h-9 rounded-full bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center text-white shadow-md">
              <Bot className="w-5 h-5" />
            </div>
            <span className="absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full bg-emerald-500 border-2 border-slate-900" />
          </div>
          <div>
            <div className="flex items-center gap-1.5">
              <h3 className="font-bold text-sm text-white">MYRA AI</h3>
              <span className="text-[10px] px-1.5 py-0.2 bg-blue-950 text-blue-400 rounded font-mono border border-blue-500/20">
                {personality === 'romantic_gf' ? 'GF Mode' : 'Assistant'}
              </span>
            </div>
            <p className="text-[11px] text-slate-400">Always active • 16 tools ready</p>
          </div>
        </div>

        {/* Tab Switcher */}
        <div className="flex gap-1 bg-slate-900 p-1 rounded-xl border border-slate-800 text-xs font-medium">
          <button
            onClick={() => setActiveChat('myra')}
            className={`px-2.5 py-1 rounded-lg transition-colors ${
              activeChat === 'myra' ? 'bg-blue-600 text-white font-semibold' : 'text-slate-400 hover:text-white'
            }`}
          >
            AI Assistant
          </button>
          <button
            onClick={() => setActiveChat('rahul')}
            className={`px-2.5 py-1 rounded-lg transition-colors ${
              activeChat === 'rahul' ? 'bg-blue-600 text-white font-semibold' : 'text-slate-400 hover:text-white'
            }`}
          >
            Users & Notes
          </button>
        </div>
      </div>

      {/* Messages Scrollable List */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3 scrollbar-thin scrollbar-thumb-slate-800">
        {activeChat === 'rahul' ? (
          <div className="text-center py-8 text-slate-400">
            <Users className="w-10 h-10 mx-auto mb-2 text-slate-600" />
            <h4 className="text-sm font-semibold text-slate-300">P2P User Chat & Voice Notes</h4>
            <p className="text-xs text-slate-500 max-w-xs mx-auto mt-1">
              Encrypted 1-on-1 chats with other MYRA devices. Connected to Rahul (+91 98111-22334).
            </p>
            <div className="mt-4 bg-slate-900/80 border border-slate-800 rounded-xl p-3 max-w-xs mx-auto text-left">
              <div className="flex items-center gap-2 mb-1">
                <User className="w-4 h-4 text-blue-400" />
                <span className="text-xs font-semibold text-white">Rahul Sharma</span>
                <span className="text-[10px] text-slate-500 ml-auto">10:14 AM</span>
              </div>
              <p className="text-xs text-slate-300">"Bhai MYRA se trip itinerary share kar do"</p>
            </div>
          </div>
        ) : (
          <>
            {messages.map((msg, index) => {
              const isUser = msg.sender === 'user';
              const isLatestMessage = index === messages.length - 1;

              return (
                <div
                  key={msg.id}
                  className={`flex flex-col ${isUser ? 'items-end' : 'items-start'} animate-in fade-in duration-150`}
                >
                  <div
                    className={`max-w-[85%] rounded-2xl p-3 shadow-md ${
                      isUser
                        ? 'bg-blue-600 text-white rounded-br-xs'
                        : 'bg-[#111726] border border-slate-800 text-slate-100 rounded-bl-xs'
                    }`}
                  >
                    {!isUser && (
                      <div className="flex items-center justify-between gap-2 mb-1 text-[10px] text-blue-400 font-semibold tracking-wide uppercase">
                        <span className="flex items-center gap-1">
                          <Sparkles className="w-2.5 h-2.5" />
                          {msg.category || 'MYRA'}
                        </span>
                        {msg.action && (
                          <span className="bg-slate-900/60 px-1.5 py-0.5 rounded text-[9px] text-slate-400">
                            {msg.action}
                          </span>
                        )}
                      </div>
                    )}

                    {/* Text or Voice Note UI */}
                    {msg.isVoiceNote ? (
                      <div className="flex items-center gap-3 py-1">
                        <button
                          onClick={() =>
                            setPlayingVoiceId(playingVoiceId === msg.id ? null : msg.id)
                          }
                          className="w-8 h-8 rounded-full bg-blue-500/20 text-blue-400 flex items-center justify-center"
                        >
                          {playingVoiceId === msg.id ? (
                            <Pause className="w-4 h-4" />
                          ) : (
                            <Play className="w-4 h-4 ml-0.5" />
                          )}
                        </button>
                        <div className="flex-1">
                          <div className="h-1.5 bg-slate-700 rounded-full overflow-hidden w-28">
                            <div
                              className={`h-full bg-blue-400 ${
                                playingVoiceId === msg.id ? 'w-2/3 animate-pulse' : 'w-1/3'
                              }`}
                            />
                          </div>
                          <span className="text-[10px] text-slate-400 mt-1 block">
                            0:14 • Hindi Voice Note
                          </span>
                        </div>
                      </div>
                    ) : (
                      <p className="text-xs sm:text-sm leading-relaxed whitespace-pre-wrap">
                        {msg.text}
                      </p>
                    )}
                  </div>

                  <div className="flex items-center gap-1 mt-1 px-1 text-[10px] text-slate-500 font-mono">
                    <span>{msg.timestamp}</span>
                    {isUser && <CheckCheck className="w-3 h-3 text-blue-400" />}
                  </div>
                </div>
              );
            })}
            <div ref={messagesEndRef} />
          </>
        )}
      </div>

      {/* Suggested Quick Replies Bar */}
      {activeChat === 'myra' && activeQuickReplies.length > 0 && (
        <div className="px-3 py-1.5 bg-[#0b101c] border-t border-slate-800/60 flex items-center gap-1.5 overflow-x-auto scrollbar-none animate-in fade-in duration-200">
          <div className="flex items-center gap-1 text-[10px] text-blue-400 font-semibold uppercase tracking-wider shrink-0 pl-1 pr-1.5">
            <Zap className="w-3 h-3 text-blue-400" />
            <span>Quick:</span>
          </div>
          {activeQuickReplies.map((reply, i) => (
            <button
              key={i}
              onClick={() => handleQuickReplyTap(reply)}
              className="px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap bg-slate-800/80 hover:bg-blue-600/30 text-slate-200 hover:text-white border border-slate-700/80 hover:border-blue-500/60 transition-all duration-150 active:scale-95 flex items-center gap-1 shadow-sm shrink-0"
            >
              <span>{reply}</span>
            </button>
          ))}
        </div>
      )}

      {/* Bottom Message Input Bar */}
      <div className="p-3 bg-[#0d121f] border-t border-slate-800/80">
        <form onSubmit={handleSubmit} className="flex items-center gap-2">
          <button
            type="button"
            onClick={handleSendVoiceNote}
            className="p-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-blue-400 active:scale-95 transition-all"
            title="Record Voice Note"
          >
            <Mic className="w-4 h-4" />
          </button>

          <input
            type="text"
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            placeholder="Type in Hindi, Hinglish or English..."
            className="flex-1 bg-slate-900 border border-slate-700 focus:border-blue-500 text-xs text-white rounded-xl py-2.5 px-3.5 outline-none transition-colors"
          />

          <button
            type="submit"
            disabled={!inputText.trim()}
            className="p-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 disabled:opacity-40 text-white active:scale-95 transition-all"
          >
            <Send className="w-4 h-4" />
          </button>
        </form>
      </div>
    </div>
  );
};
