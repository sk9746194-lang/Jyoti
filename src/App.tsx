/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect, useRef } from 'react';
import { 
  Radio, 
  MessageSquare, 
  Layers, 
  Wrench, 
  Zap, 
  Settings, 
  Sparkles,
  Heart
} from 'lucide-react';

import { 
  AssistantState, 
  ChatMessage, 
  OrbSettings, 
  PersonalityMode, 
  TriggerRule, 
  VoiceSettings,
  VoiceStatus 
} from './types/myra';

import { AndroidFrame } from './components/AndroidFrame';
import { VoiceAssistantTab } from './components/VoiceAssistantTab';
import { ChatTab } from './components/ChatTab';
import { MissionsTab } from './components/MissionsTab';
import { ToolsTab } from './components/ToolsTab';
import { TriggersTab } from './components/TriggersTab';
import { SettingsTab } from './components/SettingsTab';

import { IncomingCallModal } from './components/IncomingCallModal';
import { LockScreenModal } from './components/LockScreenModal';
import { EmergencySosModal } from './components/EmergencySosModal';
import { CameraVisionModal } from './components/CameraVisionModal';
import { DownloadZipModal } from './components/DownloadZipModal';
import { MyraConnectModal } from './components/MyraConnectModal';
import { WallpaperSettingsModal } from './components/WallpaperSettingsModal';
import { BiometricAuthModal } from './components/BiometricAuthModal';

import { naturalVoice } from './utils/naturalVoiceEngine';
import { playWakeChime, playListeningDoneChime, playAlarmTone, triggerHapticFeedback } from './utils/audioEffects';
import { executeTool } from './utils/toolEngine';

export default function App() {
  // Navigation Tab
  const [currentTab, setCurrentTab] = useState<
    'assistant' | 'chat' | 'missions' | 'tools' | 'triggers' | 'settings'
  >(() => {
    try {
      const urlParams = new URLSearchParams(window.location.search);
      const tabParam = urlParams.get('tab');
      if (tabParam && ['assistant', 'chat', 'missions', 'tools', 'triggers', 'settings'].includes(tabParam)) {
        return tabParam as any;
      }
      const savedTab = localStorage.getItem('myra_active_tab');
      if (savedTab && ['assistant', 'chat', 'missions', 'tools', 'triggers', 'settings'].includes(savedTab)) {
        return savedTab as any;
      }
    } catch (e) {
      // ignore
    }
    return 'assistant';
  });

  const handleSetTab = (tab: 'assistant' | 'chat' | 'missions' | 'tools' | 'triggers' | 'settings') => {
    setCurrentTab(tab);
    try {
      localStorage.setItem('myra_active_tab', tab);
    } catch (e) {
      // ignore
    }
  };

  const [isConnectModalOpen, setIsConnectModalOpen] = useState<boolean>(false);

  // Assistant State & Engine
  const [assistantState, setAssistantState] = useState<AssistantState>('idle');
  const [liveTranscript, setLiveTranscript] = useState<string>('');
  const [assistantResponse, setAssistantResponse] = useState<string>(
    'Namaste! Main MYRA hoon — aapki 2026 Edition Hindi AI Voice Assistant. Kuch bhi bol kar poocho!'
  );
  const [lastCategory, setLastCategory] = useState<string>('MYRA AI 2026');
  const [isListening, setIsListening] = useState<boolean>(false);

  // Live Audio pipeline state for animated orb and transparent diagnostics
  const [voiceStatus, setVoiceStatus] = useState<VoiceStatus>(naturalVoice.status);
  const [liveAudioLevel, setLiveAudioLevel] = useState<number>(0);

  // Settings
  const [voiceSettings, setVoiceSettings] = useState<VoiceSettings>({
    ttsEngine: 'gemini_live',
    language: 'hi-IN',
    pitch: 1.0,
    rate: 1.0,
    personality: 'friendly_assistant',
    wakeWordEnabled: true,
    wakeWord: 'Hey MYRA',
    preferredVoice: 'Aoede',
  });

  const [orbSettings, setOrbSettings] = useState<OrbSettings>({
    theme: 'electric_blue',
    particleCount: 48,
    glowIntensity: 1.2,
    reactToAudio: true,
  });

  // System Controls
  const [torchOn, setTorchOn] = useState<boolean>(false);
  const [isMuted, setIsMuted] = useState<boolean>(false);
  const [batteryLevel, setBatteryLevel] = useState<number>(84);
  const [isCharging, setIsCharging] = useState<boolean>(true);

  // Active Modals
  const [incomingCallOpen, setIncomingCallOpen] = useState<boolean>(false);
  const [lockScreenOpen, setLockScreenOpen] = useState<boolean>(false);
  const [emergencySosOpen, setEmergencySosOpen] = useState<boolean>(false);
  const [cameraVisionOpen, setCameraVisionOpen] = useState<boolean>(false);
  const [isDownloadModalOpen, setIsDownloadModalOpen] = useState<boolean>(false);
  const [isWallpaperModalOpen, setIsWallpaperModalOpen] = useState<boolean>(false);
  const [micPermissionError, setMicPermissionError] = useState<string | null>(null);

  // Biometric Authentication State & 2-Minute Session Window
  const [isBiometricPromptOpen, setIsBiometricPromptOpen] = useState<boolean>(false);
  const [pendingSensitiveCommand, setPendingSensitiveCommand] = useState<string | null>(null);
  const [lastBiometricSuccessTime, setLastBiometricSuccessTime] = useState<number>(0);

  // 10. APP OPEN PROTECTION
  useEffect(() => {
    const isAppOpenProtected = localStorage.getItem('myra_app_open_protection') === 'true';
    const isOwnerProtectionOn = localStorage.getItem('myra_owner_protection') !== 'false';
    if (isOwnerProtectionOn && isAppOpenProtected) {
      setIsBiometricPromptOpen(true);
    }
  }, []);

  // Chat History
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'm1',
      sender: 'myra',
      text: 'Namaste! Main MYRA hoon — 2026 Edition Hindi & Hinglish voice assistant. Aap mujhe phone call, WhatsApp, gaane chalane, camera scan ya kisi bhi automation ke liye bol sakte hain.',
      timestamp: '09:40 AM',
      category: 'System Briefing',
    },
  ]);

  // Read actual battery if available in browser
  useEffect(() => {
    if (typeof navigator !== 'undefined' && 'getBattery' in navigator) {
      (navigator as any).getBattery?.().then((batt: any) => {
        setBatteryLevel(Math.round(batt.level * 100));
        setIsCharging(batt.charging);
        batt.addEventListener('levelchange', () =>
          setBatteryLevel(Math.round(batt.level * 100))
        );
        batt.addEventListener('chargingchange', () =>
          setIsCharging(batt.charging)
        );
      }).catch(() => {});
    }
  }, []);

  // Subscribe to Natural AI Voice Engine (Status, Real-time Orb Reactivity, Audio)
  useEffect(() => {
    const unsub = naturalVoice.onStatusChange((status) => {
      setVoiceStatus(status);
    });

    naturalVoice.setAudioLevelCallback((level) => {
      setLiveAudioLevel(level);
    });

    naturalVoice.setSpeakingChangeCallback((speaking) => {
      setAssistantState((prev) => {
        if (speaking) return 'speaking';
        return prev === 'speaking' ? 'idle' : prev;
      });
    });

    return () => {
      unsub();
      naturalVoice.stopPlayback();
      naturalVoice.stopRealtimeMic();
    };
  }, []);

  // Update Orb theme if personality changes to GF mode
  const handlePersonalityChange = (mode: PersonalityMode) => {
    setVoiceSettings((prev) => ({ ...prev, personality: mode }));
    if (mode === 'romantic_gf') {
      setOrbSettings((prev) => ({ ...prev, theme: 'romantic_rose' }));
      const greeting = 'Haanji mere hero! Main aapki MYRA hoon, boliye aaj aapke liye kya karun? ❤️';
      setAssistantResponse(greeting);
      speakResponse(greeting);
    } else if (mode === 'professional') {
      setOrbSettings((prev) => ({ ...prev, theme: 'emerald_neon' }));
      const greeting = 'Executive mode enabled. Ready for high-priority task delegation.';
      setAssistantResponse(greeting);
      speakResponse(greeting);
    } else {
      setOrbSettings((prev) => ({ ...prev, theme: 'electric_blue' }));
      const greeting = 'Friendly assistant mode active. How can I help you?';
      setAssistantResponse(greeting);
      speakResponse(greeting);
    }
  };

  // Speak assistant response with natural voice architecture
  const speakResponse = (text: string) => {
    if (isMuted) return;
    triggerHapticFeedback('speech_start');
    setAssistantState('speaking');
    naturalVoice.speakText(
      text,
      voiceSettings,
      () => setAssistantState('speaking'),
      () => setAssistantState('idle')
    );
  };

  // Start Realtime Voice Recognition & Audio Stream
  const handleStartListening = () => {
    if (naturalVoice.shouldThrottleTrigger()) return;
    playWakeChime();
    setIsListening(true);
    setAssistantState('listening');
    setLiveTranscript('');
    setMicPermissionError(null);

    naturalVoice.startRealtimeMic(
      (transcript, isFinal) => {
        setLiveTranscript(transcript);
        setMicPermissionError(null);
        if (isFinal) {
          setIsListening(false);
          handleExecuteCommand(transcript);
        }
      },
      (err) => {
        console.warn('Speech recognition notice:', err);
        setIsListening(false);
        setAssistantState('idle');
        const errStr = typeof err === 'string' ? err : err?.error || '';
        if (errStr.includes('not-allowed') || errStr.includes('permission') || errStr.includes('denied') || errStr.includes('security')) {
          setMicPermissionError('Browser/Preview iframe me microphone permission restricted hai. Niche text box me likhkar koi bhi command execute karein.');
        } else if (errStr.includes('no-speech')) {
          // Normal timeout when nothing said, ignore
        } else {
          setMicPermissionError('Microphone connect nahi ho paya. Aap niche text box me likhkar MYRA ke saare tools use kar sakte hain.');
        }
      },
      () => {
        setIsListening(false);
        if (assistantState === 'listening') {
          setAssistantState('idle');
        }
      },
      voiceSettings.language
    );
  };

  const handleStopListening = () => {
    playListeningDoneChime();
    setIsListening(false);
    naturalVoice.stopRealtimeMic();
    if (liveTranscript.trim()) {
      handleExecuteCommand(liveTranscript.trim());
    } else {
      setAssistantState('idle');
    }
  };

  // Helper to determine if an action is sensitive and requires biometric authorization
  const checkIsSensitiveAction = (commandText: string): boolean => {
    const lower = commandText.toLowerCase().trim();

    // Normal App Launch commands (YouTube, WhatsApp, Instagram, Facebook, Chrome, etc.) must NEVER require biometric authentication
    const isOpenAppOnly = (lower.includes('kholo') || lower.includes('open') || lower.includes('chalao') || lower.includes('launch')) &&
                          !lower.includes('message') && !lower.includes('msg') && !lower.includes('bhejo') &&
                          !lower.includes('send') && !lower.includes('sos') && !lower.includes('otp') &&
                          !lower.includes('password') && !lower.includes('call lagao') && !lower.includes('phone lagao');
    if (isOpenAppOnly) return false;

    if (lower.includes('whatsapp') && (lower.includes('send') || lower.includes('bhejo') || lower.includes('message') || lower.includes('msg') || lower.includes('text') || lower.includes('bolo'))) return true;
    if (lower.includes('sms') || lower.includes('text message') || lower.includes('मैसेज भेजो') || lower.includes('एसएमएस')) return true;
    if ((lower.includes('email') || lower.includes('mail') || lower.includes('ईमेल') || lower.includes('मेल')) && (lower.includes('send') || lower.includes('bhejo') || lower.includes('karo') || lower.includes('write'))) return true;
    if (lower.includes('call lagao') || lower.includes('phone lagao') || lower.includes('कॉल लगाओ') || lower.includes('फोन लगाओ') || lower.includes('कॉल करो')) return true;
    if (lower.includes('sos') || lower.includes('emergency') || lower.includes('मदद') || lower.includes('help me') || lower.includes('बचाओ') || lower.includes('आपातकाल')) return true;
    if (lower.includes('share location') || lower.includes('location share') || lower.includes('live location') || lower.includes('लोकेशन भेजो') || lower.includes('लाइव लोकेशन')) return true;
    if (lower.includes('otp') || lower.includes('password') || lower.includes('पासवर्ड') || lower.includes('bank') || lower.includes('बैंक') || lower.includes('balance') || lower.includes('read message') || lower.includes('message padho') || lower.includes('मैसेज पढ़ो') || lower.includes('notification')) return true;
    if (lower.includes('दूसरे फोन') || lower.includes('दूसरा फोन') || lower.includes('pc connect') || lower.includes('remote phone') || lower.includes('connect device')) return true;
    if (lower.includes('security') || lower.includes('सुरक्षा') || lower.includes('re-enroll') || lower.includes('voice change') || lower.includes('export') || lower.includes('credential') || lower.includes('api key') || lower.includes('reset owner')) return true;
    return false;
  };

  const isBiometricSessionValid = (): boolean => {
    const timeoutMins = Number(localStorage.getItem('myra_security_timeout_mins')) || 2;
    const elapsed = Date.now() - lastBiometricSuccessTime;
    return elapsed >= 0 && elapsed <= timeoutMins * 60 * 1000;
  };

  // Core Command Dispatcher (Handles voice or text across all 16 tools)
  const handleExecuteCommand = async (commandText: string) => {
    setLiveTranscript('');
    setAssistantState('thinking');

    // 1. Add user message to chat history
    const userMsg: ChatMessage = {
      id: `usr-${Date.now()}`,
      sender: 'user',
      text: commandText,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };
    setMessages((prev) => [...prev, userMsg]);

    const lowerCmd = commandText.toLowerCase();

    // 1.4 MYRA OWNER SECURITY CHECK
    const isOwnerProtectionOn = localStorage.getItem('myra_owner_protection') !== 'false';
    const isUnknownVoiceBlocked = localStorage.getItem('myra_unknown_voice_blocked') !== 'false';

    // Check for simulated unknown speaker or failed speaker verification test
    if (isOwnerProtectionOn && (lowerCmd.includes('unknown speaker') || lowerCmd.includes('dusri awaz') || lowerCmd.includes('paraya'))) {
      const rejectReply = isUnknownVoiceBlocked ? 'Owner verification required.' : "Sorry, I couldn't verify the owner.";
      setAssistantResponse(rejectReply);
      setLastCategory('MYRA Security');
      const rejectMsg: ChatMessage = {
        id: `myra-${Date.now()}`,
        sender: 'myra',
        text: rejectReply,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        category: 'MYRA Security',
        action: 'owner_verification_rejected',
      };
      setMessages((prev) => [...prev, rejectMsg]);
      speakResponse(rejectReply);
      return;
    }

    // Voice-Only Command Authorization: Commands execute directly upon Owner Voice verification
    // BiometricPrompt removed from command pipeline per requirement 8
    await proceedWithResolvedCommand(commandText, lowerCmd);
  };

  const proceedWithResolvedCommand = async (commandText: string, lowerCmd: string) => {

    const isRemoteIntent =
      commandText.includes('दूसरे फोन') ||
      commandText.includes('दूसरे मोबाइल') ||
      commandText.includes('घर वाले फोन') ||
      commandText.includes('घर के फोन') ||
      lowerCmd.includes('doosre phone') ||
      lowerCmd.includes('dusre phone') ||
      lowerCmd.includes('remote phone') ||
      lowerCmd.includes('second phone') ||
      lowerCmd.includes('connected phone');

    if (isRemoteIntent) {
      let pairedDevicesList: any[] = [];
      let permSettings: any = { openApps: true, mediaControls: true, deviceCommands: true, notifications: true, deviceStatus: true };
      try {
        const savedDevs = localStorage.getItem('myra_paired_devices');
        if (savedDevs) pairedDevicesList = JSON.parse(savedDevs);
        const savedPermsMap = localStorage.getItem('myra_device_permissions_map');
        if (savedPermsMap) {
          const map = JSON.parse(savedPermsMap);
          permSettings = map['dev_phone_2'] || permSettings;
        }
      } catch (_) {}

      const onlineDevice = pairedDevicesList.find((d) => d.status === 'connected') || pairedDevicesList[0];
      let remoteReply = '';
      if (!onlineDevice) {
        remoteReply = 'दूसरा फोन अभी MYRA Connect से कनेक्टेड नहीं है। कृपया पहले डिवाइस पेयर या कनेक्ट करें।';
      } else {
        if (lowerCmd.includes('continue') || lowerCmd.includes('कंटिन्यू') || lowerCmd.includes('handoff') || lowerCmd.includes('हैंडऑफ')) {
          setIsConnectModalOpen(true);
          remoteReply = `✓ MYRA AI Task Handoff: वर्तमान कार्य को दूसरे फोन (${onlineDevice.name}) पर हैंडऑफ कर दिया गया है। दूसरे फोन पर "Task received from MYRA Phone [OPEN]" प्रॉम्प्ट दिखेगा।`;
        } else if (lowerCmd.includes('drop') || lowerCmd.includes('ड्रॉप') || lowerCmd.includes('फाइल शेयर')) {
          setIsConnectModalOpen(true);
          remoteReply = `✓ MYRA Drop स्क्रीन खोल दी गई है। आप दूसरे फोन (${onlineDevice.name}) को सुरक्षित फाइल भेज सकते हैं।`;
        } else if (lowerCmd.includes('trust center') || lowerCmd.includes('ट्रस्ट सेंटर')) {
          setIsConnectModalOpen(true);
          remoteReply = `✓ MYRA Trust Center खोल दिया गया है। यहाँ आप सभी कनेक्टेड डिवाइसेस की परमिशन, एक्टिव सेशन्स और सिक्यूरिटी ऑडिट देख सकते हैं।`;
        } else if (lowerCmd.includes('दिखाओ') || lowerCmd.includes('dikhao') || lowerCmd.includes('dashboard') || lowerCmd.includes('open connect') || lowerCmd.includes('connect')) {
          setIsConnectModalOpen(true);
          remoteReply = `MYRA Connect स्क्रीन खोल दी गई है। ${onlineDevice.name} (${onlineDevice.model}) ऑनलाइन कनेक्टेड है।`;
        } else if (lowerCmd.includes('battery') || lowerCmd.includes('बैटरी')) {
          remoteReply = `दूसरे फोन (${onlineDevice.name}) की बैटरी ${onlineDevice.batteryLevel || 84}% है और यह ${onlineDevice.isCharging ? 'चार्ज हो रहा है' : 'सामान्य स्थिति में है'}।`;
        } else if (lowerCmd.includes('ring') || lowerCmd.includes('रिंग') || lowerCmd.includes('बजाओ')) {
          if (!permSettings.deviceCommands) {
            remoteReply = `PERMISSION REQUIRED: दूसरे फोन (${onlineDevice.name}) पर Hardware Controls / Ringer की अनुमति बंद (OFF) है।`;
          } else {
            playAlarmTone();
            remoteReply = `✓ दूसरे फोन (${onlineDevice.name}) पर लाउड रिंगर बजा दिया गया है ताकि आप उसे आसानी से खोज सकें।`;
          }
        } else if (lowerCmd.includes('स्थिति') || lowerCmd.includes('status') || lowerCmd.includes('sthiti') || lowerCmd.includes('haal')) {
          remoteReply = `दूसरे फोन (${onlineDevice.name}) की स्थिति: ऑनलाइन, मॉडल ${onlineDevice.model}, बैटरी ${onlineDevice.batteryLevel || 84}%, स्टोरेज ${onlineDevice.storageUsedGB || 114} GB प्रयुक्त है।`;
        } else if (lowerCmd.includes('youtube') || lowerCmd.includes('यूट्यूब')) {
          if (!permSettings.openApps) {
            remoteReply = `PERMISSION REQUIRED: दूसरे फोन (${onlineDevice.name}) पर App Launching अनुमति बंद (OFF) है।`;
          } else {
            remoteReply = `✓ दूसरे फोन (${onlineDevice.name}) पर YouTube खोल दिया गया है।`;
          }
        } else if (lowerCmd.includes('camera') || lowerCmd.includes('कैमरा')) {
          if (!permSettings.openApps) {
            remoteReply = `PERMISSION REQUIRED: दूसरे फोन (${onlineDevice.name}) पर Camera खोलने की अनुमति बंद (OFF) है।`;
          } else {
            remoteReply = `✓ दूसरे फोन (${onlineDevice.name}) पर Camera खोल दिया गया है।`;
          }
        } else if (lowerCmd.includes('torch') || lowerCmd.includes('flashlight') || lowerCmd.includes('टॉर्च')) {
          if (!permSettings.deviceCommands) {
            remoteReply = `PERMISSION REQUIRED: दूसरे फोन (${onlineDevice.name}) पर Hardware Controls अनुमति बंद (OFF) है।`;
          } else {
            remoteReply = `✓ दूसरे फोन (${onlineDevice.name}) पर टॉर्च चालू कर दी गई है।`;
          }
        } else if (lowerCmd.includes('chrome') || lowerCmd.includes('क्रोम') || lowerCmd.includes('browser')) {
          if (!permSettings.openApps) {
            remoteReply = `PERMISSION REQUIRED: दूसरे फोन पर ब्राउज़र लॉन्च अनुमति बंद (OFF) है।`;
          } else {
            remoteReply = `✓ दूसरे फोन (${onlineDevice.name}) पर Chrome ब्राउज़र खोल दिया गया है।`;
          }
        } else {
          remoteReply = `✓ दूसरे फोन (${onlineDevice.name}) पर सुरक्षित रिमोट कमांड भेज दिया गया है।`;
        }
      }

      setAssistantResponse(remoteReply);
      setLastCategory('MYRA Connect Remote');

      const myraMsg: ChatMessage = {
        id: `myra-${Date.now()}`,
        sender: 'myra',
        text: remoteReply,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        category: 'MYRA Connect Remote',
        action: 'remote_device_execute',
      };
      setMessages((prev) => [...prev, myraMsg]);
      speakResponse(remoteReply);
      return;
    }

    // 2. Immediate local rule execution for instant tactile feedback
    const localResult = executeTool(commandText);

    // Check if immediate modal is needed (Safety lock, call, emergency, etc.)
    if (lowerCmd.includes('wallpaper') || lowerCmd.includes('वॉलपेपर')) {
      setIsWallpaperModalOpen(true);
      const wpReply = 'MYRA 9:16 Futuristic AI Live Wallpaper screen open kar di gayi hai. Aap Earth, HUD rings, aur blue-red neon settings customize kar sakte hain!';
      setAssistantResponse(wpReply);
      setLastCategory('Live Wallpaper');
      speakResponse(wpReply);
      return;
    }

    if (localResult.needsModal === 'lock') {
      setLockScreenOpen(true);
    } else if (localResult.needsModal === 'call') {
      setIncomingCallOpen(true);
    } else if (localResult.needsModal === 'emergency') {
      setEmergencySosOpen(true);
    } else if (localResult.needsModal === 'camera') {
      setCameraVisionOpen(true);
    } else if (localResult.toolName === 'toggle_flashlight') {
      setTorchOn((prev) => !prev);
    } else if (localResult.toolName === 'set_timer') {
      playAlarmTone();
    } else if (localResult.toolName === 'open_app' || localResult.toolName === 'open_map') {
      // Browser Preview Web Fallback for app launches & navigation
      const appName = (localResult.data?.app || (localResult.toolName === 'open_map' ? 'maps' : '')).toLowerCase();
      let webUrl = '';
      if (appName.includes('youtube')) {
        webUrl = 'https://www.youtube.com/';
      } else if (appName.includes('whatsapp')) {
        webUrl = 'https://web.whatsapp.com/';
      } else if (appName.includes('spotify')) {
        webUrl = 'https://open.spotify.com/';
      } else if (appName.includes('maps') || localResult.toolName === 'open_map') {
        webUrl = 'https://www.google.com/maps/';
      } else if (appName.includes('chrome') || appName.includes('browser') || appName.includes('search')) {
        webUrl = 'https://www.google.com/';
      } else if (appName.includes('instagram')) {
        webUrl = 'https://www.instagram.com/';
      } else if (appName.includes('facebook')) {
        webUrl = 'https://www.facebook.com/';
      }

      if (webUrl && typeof window !== 'undefined') {
        try {
          window.open(webUrl, '_blank', 'noopener,noreferrer');
        } catch (e) {
          console.warn('Could not launch web URL fallback:', e);
        }
      }
    }

    // 3. Query Express backend with Gemini API for deep natural Hindi conversational intelligence
    let finalVoiceReply = localResult.voiceReply;
    let finalCategory = localResult.category;

    try {
      const response = await fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: commandText,
          personality: voiceSettings.personality,
          history: messages.slice(-4),
        }),
      });

      if (response.ok) {
        const data = await response.json();
        if (data && data.response) {
          finalVoiceReply = data.response;
          if (data.category) finalCategory = data.category;
        }
      }
    } catch (e) {
      // Gracefully fall back to local rule result
    }

    // 4. Update Assistant Display & Speak
    triggerHapticFeedback('command_confirm');
    setAssistantResponse(finalVoiceReply);
    setLastCategory(finalCategory);

    const myraMsg: ChatMessage = {
      id: `myra-${Date.now()}`,
      sender: 'myra',
      text: finalVoiceReply,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      category: finalCategory,
      action: localResult.toolName,
    };
    setMessages((prev) => [...prev, myraMsg]);

    speakResponse(finalVoiceReply);
  };

  const handleTestTrigger = (rule: TriggerRule) => {
    setAssistantResponse(rule.actionValue);
    setLastCategory(`Trigger: ${rule.name}`);
    speakResponse(rule.actionValue);
  };

  return (
    <AndroidFrame
      personality={voiceSettings.personality}
      onPersonalityChange={handlePersonalityChange}
      torchOn={torchOn}
      onToggleTorch={() => setTorchOn(!torchOn)}
      onLockScreen={() => setLockScreenOpen(true)}
      isMuted={isMuted}
      onToggleMute={() => {
        if (!isMuted) naturalVoice.stopPlayback();
        setIsMuted(!isMuted);
      }}
      batteryLevel={batteryLevel}
      isCharging={isCharging}
      onOpenDownloadModal={() => setIsDownloadModalOpen(true)}
      onOpenSettings={() => handleSetTab('settings')}
    >
      {/* Active Tab Screen */}
      <div className="flex-1 min-h-0 flex flex-col relative overflow-hidden">
        {currentTab === 'assistant' && (
          <VoiceAssistantTab
            assistantState={assistantState}
            orbSettings={orbSettings}
            voiceSettings={voiceSettings}
            voiceStatus={voiceStatus}
            liveAudioLevel={liveAudioLevel}
            liveTranscript={liveTranscript}
            assistantResponse={assistantResponse}
            lastCategory={lastCategory}
            isListening={isListening}
            onStartListening={handleStartListening}
            onStopListening={handleStopListening}
            onExecuteCommand={handleExecuteCommand}
            onReplayAudio={() => speakResponse(assistantResponse)}
            onOpenToolTab={() => handleSetTab('tools')}
            onOpenDownloadModal={() => setIsDownloadModalOpen(true)}
            onOpenMyraConnect={() => setIsConnectModalOpen(true)}
            onOpenSettings={() => handleSetTab('settings')}
            micPermissionError={micPermissionError}
            onClearMicPermissionError={() => setMicPermissionError(null)}
          />
        )}

        {currentTab === 'chat' && (
          <ChatTab
            messages={messages}
            onSendMessage={handleExecuteCommand}
            personality={voiceSettings.personality}
          />
        )}

        {currentTab === 'missions' && (
          <MissionsTab onRunVoiceCommand={handleExecuteCommand} />
        )}

        {currentTab === 'tools' && (
          <ToolsTab
            onExecuteToolCommand={handleExecuteCommand}
            onOpenIncomingCall={() => setIncomingCallOpen(true)}
            onOpenEmergencySos={() => setEmergencySosOpen(true)}
            onOpenVisionCamera={() => setCameraVisionOpen(true)}
            onLockScreen={() => setLockScreenOpen(true)}
            torchOn={torchOn}
            onToggleTorch={() => setTorchOn(!torchOn)}
            personality={voiceSettings.personality}
            onOpenMyraConnect={() => setIsConnectModalOpen(true)}
            onOpenWallpaper={() => setIsWallpaperModalOpen(true)}
          />
        )}

        {currentTab === 'triggers' && (
          <TriggersTab onTestTrigger={handleTestTrigger} />
        )}

        {currentTab === 'settings' && (
          <SettingsTab
            voiceSettings={voiceSettings}
            onUpdateVoiceSettings={setVoiceSettings}
            orbSettings={orbSettings}
            onUpdateOrbSettings={setOrbSettings}
            onOpenMyraConnect={() => setIsConnectModalOpen(true)}
            onOpenWallpaper={() => setIsWallpaperModalOpen(true)}
            onTestVoice={() =>
              speakResponse(
                'Namaste! Main MYRA hoon. Aapki aawaz aur saare voice tools bilkul sahi kaam kar rahe hain.'
              )
            }
            onTriggerEnrollment={() => {
              setAssistantResponse(
                'Voiceprint enrollment mode: Kripya 3 sentences clearly bolein — "Hey MYRA unlock", "Main ghar pahuch gaya", "OTP batao".'
              );
              speakResponse(
                'Voiceprint enrollment shuru ho gaya hai. Sentence bolein.'
              );
            }}
            onBack={() => handleSetTab('assistant')}
          />
        )}
      </div>

      {/* Android Bottom App Navigation Bar */}
      <nav className="w-full shrink-0 bg-[#090d16] border-t border-cyan-500/20 px-2 py-1.5 flex items-center justify-around text-[10px] select-none z-20">
        <button
          id="nav-tab-assistant"
          onClick={() => handleSetTab('assistant')}
          className={`flex flex-col items-center gap-1 py-1 px-2.5 rounded-xl transition-all cursor-pointer ${
            currentTab === 'assistant'
              ? 'text-cyan-400 font-bold scale-105'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <div
            className={`w-6 h-6 rounded-full flex items-center justify-center ${
              currentTab === 'assistant'
                ? 'bg-cyan-500/20 text-cyan-400 shadow-[0_0_12px_rgba(6,182,212,0.5)]'
                : ''
            }`}
          >
            <Radio className="w-4 h-4" />
          </div>
          <span>Assistant</span>
        </button>

        <button
          id="nav-tab-chat"
          onClick={() => handleSetTab('chat')}
          className={`flex flex-col items-center gap-1 py-1 px-2.5 rounded-xl transition-all cursor-pointer ${
            currentTab === 'chat'
              ? 'text-cyan-400 font-bold scale-105'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <div
            className={`w-6 h-6 rounded-full flex items-center justify-center ${
              currentTab === 'chat'
                ? 'bg-cyan-500/20 text-cyan-400 shadow-[0_0_12px_rgba(6,182,212,0.5)]'
                : ''
            }`}
          >
            <MessageSquare className="w-4 h-4" />
          </div>
          <span>Chat</span>
        </button>

        <button
          id="nav-tab-missions"
          onClick={() => handleSetTab('missions')}
          className={`flex flex-col items-center gap-1 py-1 px-2.5 rounded-xl transition-all cursor-pointer ${
            currentTab === 'missions'
              ? 'text-cyan-400 font-bold scale-105'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <div
            className={`w-6 h-6 rounded-full flex items-center justify-center ${
              currentTab === 'missions'
                ? 'bg-cyan-500/20 text-cyan-400 shadow-[0_0_12px_rgba(6,182,212,0.5)]'
                : ''
            }`}
          >
            <Layers className="w-4 h-4" />
          </div>
          <span>Missions</span>
        </button>

        <button
          id="nav-tab-tools"
          onClick={() => handleSetTab('tools')}
          className={`flex flex-col items-center gap-1 py-1 px-2.5 rounded-xl transition-all cursor-pointer ${
            currentTab === 'tools'
              ? 'text-cyan-400 font-bold scale-105'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <div
            className={`w-6 h-6 rounded-full flex items-center justify-center ${
              currentTab === 'tools'
                ? 'bg-cyan-500/20 text-cyan-400 shadow-[0_0_12px_rgba(6,182,212,0.5)]'
                : ''
            }`}
          >
            <Wrench className="w-4 h-4" />
          </div>
          <span>16 Tools</span>
        </button>

        <button
          id="nav-tab-triggers"
          onClick={() => handleSetTab('triggers')}
          className={`flex flex-col items-center gap-1 py-1 px-2.5 rounded-xl transition-all cursor-pointer ${
            currentTab === 'triggers'
              ? 'text-cyan-400 font-bold scale-105'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <div
            className={`w-6 h-6 rounded-full flex items-center justify-center ${
              currentTab === 'triggers'
                ? 'bg-cyan-500/20 text-cyan-400 shadow-[0_0_12px_rgba(6,182,212,0.5)]'
                : ''
            }`}
          >
            <Zap className="w-4 h-4" />
          </div>
          <span>Triggers</span>
        </button>

        <button
          id="nav-tab-settings"
          onClick={() => handleSetTab('settings')}
          className={`flex flex-col items-center gap-1 py-1 px-2.5 rounded-xl transition-all cursor-pointer ${
            currentTab === 'settings'
              ? 'text-cyan-400 font-bold scale-105'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <div
            className={`w-6 h-6 rounded-full flex items-center justify-center ${
              currentTab === 'settings'
                ? 'bg-cyan-500/20 text-cyan-400 shadow-[0_0_12px_rgba(6,182,212,0.5)]'
                : ''
            }`}
          >
            <Settings className="w-4 h-4" />
          </div>
          <span>Settings</span>
        </button>
      </nav>

      {/* Global Interactive Overlays */}
      <IncomingCallModal
        callerName="Papa"
        callerNumber="+91 98765-43210"
        isOpen={incomingCallOpen}
        onAnswer={() => {
          handleExecuteCommand('answer_call');
          setIncomingCallOpen(false);
        }}
        onReject={() => {
          handleExecuteCommand('end_call');
          setIncomingCallOpen(false);
        }}
        onClose={() => setIncomingCallOpen(false)}
      />

      <LockScreenModal
        isOpen={lockScreenOpen}
        onUnlock={() => setLockScreenOpen(false)}
      />

      <EmergencySosModal
        isOpen={emergencySosOpen}
        onClose={() => setEmergencySosOpen(false)}
      />

      <CameraVisionModal
        isOpen={cameraVisionOpen}
        onClose={() => setCameraVisionOpen(false)}
      />

      <DownloadZipModal
        isOpen={isDownloadModalOpen}
        onClose={() => setIsDownloadModalOpen(false)}
      />

      <MyraConnectModal
        isOpen={isConnectModalOpen}
        onClose={() => setIsConnectModalOpen(false)}
      />

      <WallpaperSettingsModal
        isOpen={isWallpaperModalOpen}
        onClose={() => setIsWallpaperModalOpen(false)}
        onApplyAssistantTrigger={() => {
          setIsWallpaperModalOpen(false);
          handleStartListening();
        }}
      />

      {/* 4 & 5. Real Android BiometricPrompt Boundary & Session Window */}
      <BiometricAuthModal
        isOpen={isBiometricPromptOpen}
        title="MYRA Owner Verification"
        subtitle="संवेदनशील एक्शन के लिए बायोमेट्रिक प्रमाणीकरण आवश्यक है"
        description="Touch sensor or enter device PIN to authorize this sensitive action."
        onSuccess={() => {
          setLastBiometricSuccessTime(Date.now());
          setIsBiometricPromptOpen(false);
          if (pendingSensitiveCommand) {
            const cmd = pendingSensitiveCommand;
            setPendingSensitiveCommand(null);
            proceedWithResolvedCommand(cmd, cmd.toLowerCase());
          }
        }}
        onCancel={() => {
          setIsBiometricPromptOpen(false);
          setPendingSensitiveCommand(null);
          const cancelReply = 'Owner verification required.';
          setAssistantResponse(cancelReply);
          setLastCategory('MYRA Security');
          const cancelMsg: ChatMessage = {
            id: `myra-${Date.now()}`,
            sender: 'myra',
            text: cancelReply,
            timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
            category: 'MYRA Security',
            action: 'biometric_cancelled',
          };
          setMessages((prev) => [...prev, cancelMsg]);
          speakResponse(cancelReply);
        }}
      />
    </AndroidFrame>
  );
}
