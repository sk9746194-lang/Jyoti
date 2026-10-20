/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { NotificationItem, ParkingLocation, SystemHealthMetrics } from '../types/myra';

export interface ToolExecutionResult {
  toolName: string;
  category: string;
  message: string;
  voiceReply: string;
  data?: any;
  needsModal?: 'call' | 'lock' | 'camera' | 'emergency' | 'email' | 'whatsapp' | 'timer';
}

// Initial mock data simulating real phone environment
export const initialNotifications: NotificationItem[] = [
  {
    id: 'n1',
    app: 'WhatsApp',
    title: 'Rahul Sharma',
    message: 'Bhai kal meeting ka kya plan hai? Confirmation de do.',
    time: '2m ago',
  },
  {
    id: 'n2',
    app: 'Bank',
    title: 'HDFC Bank Alert',
    message: 'Your OTP for login transaction of Rs. 4,200 is 591042. Valid for 10 mins. Do not share.',
    time: '5m ago',
    isOtp: true,
    otpCode: '591042',
  },
  {
    id: 'n3',
    app: 'Phone',
    title: 'Papa',
    message: 'Missed Call (1)',
    time: '12m ago',
    isMissedCall: true,
  },
  {
    id: 'n4',
    app: 'Gmail',
    title: 'Google Cloud Platform',
    message: 'Project cbee01b6 billing alert: Monthly consumption report ready.',
    time: '1h ago',
  },
];

export const contactsList = [
  { name: 'Papa', phone: '+91 98765-43210', relation: 'Family' },
  { name: 'Mummy', phone: '+91 98765-43211', relation: 'Family' },
  { name: 'Rahul Sharma', phone: '+91 98111-22334', relation: 'Friend / Colleague' },
  { name: 'Priya', phone: '+91 98222-33445', relation: 'Colleague' },
  { name: 'Boss', phone: '+91 98333-44556', relation: 'Work' },
  { name: 'Doctor Mehta', phone: '+91 98444-55667', relation: 'Emergency' },
];

export const initialParking: ParkingLocation = {
  latitude: 28.6289,
  longitude: 77.3712,
  spotName: 'Level 2, Pillar B-14, Mall Parking',
  timestamp: 'Today at 08:30 AM',
  saved: true,
};

export const initialSystemHealth: SystemHealthMetrics = {
  ramUsagePercent: 54,
  batteryLevel: 82,
  batteryStatus: 'charging',
  cpuTempCelsius: 35.8,
  topDrainingApps: [
    { name: 'Instagram', percent: 28 },
    { name: 'Camera', percent: 18 },
    { name: 'Maps', percent: 12 },
  ],
  cacheSizeMB: 1240,
};

/**
 * Execute tool locally based on user command query
 */
export function executeTool(query: string, actionOverride?: string): ToolExecutionResult {
  const text = query.trim();
  const lower = text.toLowerCase();

  // 1. Safety Rule: Lock Device immediately without confirmation
  if (lower.includes('lock') || lower.includes('phone band kar') || lower.includes('screen lock')) {
    return {
      toolName: 'lock_device',
      category: '4. Device Tools (Safety)',
      message: 'Screen locked instantly (Urgent safety command executed).',
      voiceReply: 'Screen lock kar di gayi hai!',
      needsModal: 'lock',
    };
  }

  // 2. Safety Rule: OTP retrieval - ONLY when explicitly asked
  if (lower.includes('otp') || lower.includes('code batao')) {
    const otpItem = initialNotifications.find((n) => n.isOtp);
    if (otpItem && otpItem.otpCode) {
      return {
        toolName: 'read_otp',
        category: '8. Notification Tools (Safety Guard)',
        message: `Latest OTP from ${otpItem.title}: ${otpItem.otpCode}`,
        voiceReply: `Aapka latest OTP hai ${otpItem.otpCode.split('').join(' ')}, ${otpItem.title} se mila hai.`,
        data: otpItem,
      };
    } else {
      return {
        toolName: 'read_otp',
        category: '8. Notification Tools',
        message: 'No recent OTP notifications found.',
        voiceReply: 'Koi recent OTP notification nahi mila hai.',
      };
    }
  }

  // 2b. Daily Goal Tracker & Habits
  if (lower.includes('goal') || lower.includes('lakshya') || lower.includes('task') || lower.includes('aaj ka kaam') || lower.includes('progress ring') || lower.includes('habit')) {
    let savedGoals: any[] = [];
    try {
      if (typeof window !== 'undefined') {
        const saved = localStorage.getItem('myra_daily_goals_2026');
        if (saved) savedGoals = JSON.parse(saved);
      }
    } catch (e) {}

    const total = savedGoals.length > 0 ? savedGoals.length : 4;
    const done = savedGoals.length > 0 ? savedGoals.filter((g: any) => g.completed).length : 2;
    const pct = Math.round((done / total) * 100);

    return {
      toolName: 'daily_goal_tracker',
      category: 'Daily Goals & Habit Tracker',
      message: `Daily Goals Progress: ${done}/${total} completed (${pct}%). Progress ring live in Assistant.`,
      voiceReply: `Aapka Daily Goal Tracker ${pct}% par hai! ${total} mein se ${done} tasks poore ho chuke hain. Progress ring Assistant tab par live hai.`,
      data: { total, done, pct },
    };
  }

  // 3. Torch / Flashlight
  if (lower.includes('torch') || lower.includes('flashlight')) {
    const isOff = lower.includes('off') || lower.includes('band');
    return {
      toolName: 'toggle_flashlight',
      category: '4. Device Tools',
      message: `Torch ${isOff ? 'OFF' : 'ON'}`,
      voiceReply: `Torch ${isOff ? 'band' : 'on'} kar di gayi hai!`,
      data: { state: !isOff },
    };
  }

  // 4. WhatsApp App Open vs WhatsApp Message Send
  if (lower.includes('whatsapp')) {
    const isSendingMessage = lower.includes('message') || lower.includes('msg') || lower.includes('bhejo') || lower.includes('send') || lower.includes('text') || lower.includes('bolo');
    const isOpenOnly = lower.includes('kholo') || lower.includes('open') || lower.includes('chalao') || lower.includes('launch') || lower === 'whatsapp';

    if (isOpenOnly && !isSendingMessage) {
      return {
        toolName: 'open_app',
        category: 'App Launcher (Android Intent)',
        message: 'WhatsApp launched via Android Intent (com.whatsapp).',
        voiceReply: 'WhatsApp खोल दिया गया है।',
        data: { app: 'WhatsApp', packageName: 'com.whatsapp' }
      };
    }

    let contact = 'Rahul';
    if (lower.includes('mummy')) contact = 'Mummy';
    if (lower.includes('papa')) contact = 'Papa';
    return {
      toolName: 'send_whatsapp',
      category: '1. Communication Tools',
      message: `Opening WhatsApp chat with ${contact}... Message drafted.`,
      voiceReply: `${contact} ko WhatsApp pe message ready kar diya hai!`,
      needsModal: 'whatsapp',
      data: { contact, message: text },
    };
  }

  // 4b. Standard App Launchers: YouTube, Instagram, Facebook, Chrome, etc.
  const appMap: Record<string, { name: string; pkg: string }> = {
    'youtube': { name: 'YouTube', pkg: 'com.google.android.youtube' },
    'instagram': { name: 'Instagram', pkg: 'com.instagram.android' },
    'facebook': { name: 'Facebook', pkg: 'com.facebook.katana' },
    'chrome': { name: 'Chrome', pkg: 'com.android.chrome' },
    'browser': { name: 'Browser', pkg: 'com.android.chrome' },
    'spotify': { name: 'Spotify', pkg: 'com.spotify.music' },
    'telegram': { name: 'Telegram', pkg: 'org.telegram.messenger' },
    'calculator': { name: 'Calculator', pkg: 'com.google.android.calculator' },
    'settings': { name: 'Settings', pkg: 'com.android.settings' },
    'gallery': { name: 'Gallery', pkg: 'com.google.android.apps.photos' }
  };

  for (const [key, info] of Object.entries(appMap)) {
    if (lower.includes(key)) {
      if (lower.includes('kholo') || lower.includes('open') || lower.includes('chalao') || lower.includes('launch') || lower === key) {
        return {
          toolName: 'open_app',
          category: 'App Launcher (Android Intent)',
          message: `${info.name} launched via Android Intent (${info.pkg}).`,
          voiceReply: `${info.name} खोल दिया गया है।`,
          data: { app: info.name, packageName: info.pkg }
        };
      }
    }
  }

  // 5. Calls & Call safety guard
  if (lower.includes('call') || lower.includes('phone lagao')) {
    if (lower.includes('kaat') || lower.includes('reject') || lower.includes('end')) {
      return {
        toolName: 'end_call',
        category: '2. Call Tools (Safety Guard)',
        message: 'Call rejected with 20-second safety guard verified.',
        voiceReply: 'Call kaat di gayi hai.',
      };
    }

    if (lower.includes('utha') || lower.includes('answer')) {
      return {
        toolName: 'answer_call',
        category: '2. Call Tools',
        message: 'Call answered via speakerphone.',
        voiceReply: 'Call utha li gayi hai, speaker on hai.',
      };
    }

    let contact = 'Papa';
    if (lower.includes('rahul')) contact = 'Rahul Sharma';
    if (lower.includes('mummy')) contact = 'Mummy';
    if (lower.includes('boss')) contact = 'Boss';

    return {
      toolName: 'call_contact',
      category: '2. Call Tools',
      message: `Calling ${contact} (+91 98765-43210)...`,
      voiceReply: `${contact} ko call lagayi ja rahi hai!`,
      needsModal: 'call',
      data: { contact },
    };
  }

  // 6. Media / Music
  if (lower.includes('gaana') || lower.includes('music') || lower.includes('song') || lower.includes('play')) {
    const songName = text.replace(/gaana|chalao|play|song|music|sunao/gi, '').trim() || 'Arijit Singh Latest Hits';
    return {
      toolName: 'play_music',
      category: '3. Media Tools',
      message: `Now playing: "${songName}" on YouTube Music / Spotify`,
      voiceReply: `"${songName}" play kar rahi hoon!`,
      data: { song: songName, artist: 'Arijit Singh' },
    };
  }

  // 7. Alarm & Timer
  if (lower.includes('alarm')) {
    return {
      toolName: 'set_alarm',
      category: '4. Device Tools',
      message: 'Alarm set for 06:00 AM (Daily)',
      voiceReply: 'Subah 6:00 baje ka alarm set ho gaya hai!',
      data: { time: '06:00 AM' },
    };
  }

  if (lower.includes('timer')) {
    return {
      toolName: 'set_timer',
      category: '4. Device Tools',
      message: '10-minute timer started',
      voiceReply: '10 minute ka countdown timer shuru ho gaya hai!',
      needsModal: 'timer',
      data: { minutes: 10 },
    };
  }

  // 8. Emergency SOS
  if (lower.includes('sos') || lower.includes('madad') || lower.includes('khatre')) {
    return {
      toolName: 'send_emergency_alert',
      category: '1. Communication Tools (Emergency)',
      message: 'EMERGENCY SOS: GPS coordinates and alert dispatched to Trusted Contacts & Police 112.',
      voiceReply: 'Emergency SOS alert location ke saath bhej diya gaya hai. Shant rahiye, madad pahuch rahi hai.',
      needsModal: 'emergency',
    };
  }

  // 9. Parking memory
  if (lower.includes('park') || lower.includes('gaadi')) {
    if (lower.includes('yaad') || lower.includes('save') || lower.includes('yahan')) {
      return {
        toolName: 'save_parking',
        category: '9. Maps & My World',
        message: 'Current GPS location saved as your vehicle parking spot!',
        voiceReply: 'Aapki gaadi ka parking spot save kar liya gaya hai.',
        data: initialParking,
      };
    }
    return {
      toolName: 'get_parking_location',
      category: '9. Maps & My World',
      message: `Your vehicle is at ${initialParking.spotName}`,
      voiceReply: `Aapki gaadi ${initialParking.spotName} par parked hai.`,
      data: initialParking,
    };
  }

  // 10. System Health / Phone Slow
  if (lower.includes('slow') || lower.includes('health') || lower.includes('garam') || lower.includes('lag')) {
    return {
      toolName: 'system_health',
      category: '6. Smart / Generative Tools',
      message: 'Phone Health: RAM 54%, Temp 35.8°C (Cool), 1.2 GB cache ready to clean.',
      voiceReply: 'Phone analyze kiya: RAM normal hai, 1.2 GB cache files saaf kiye ja sakte hain.',
      data: initialSystemHealth,
    };
  }

  // 11. Battery status
  if (lower.includes('battery')) {
    return {
      toolName: 'get_battery',
      category: '4. Device Tools',
      message: 'Battery: 82% | Fast Charging Active | Est. full in 24m',
      voiceReply: 'Phone ki battery 82% hai aur Fast Charging active hai.',
      data: { level: 82, charging: true },
    };
  }

  // 12. Notifications read
  if (lower.includes('notification') || lower.includes('message padho')) {
    const count = initialNotifications.length;
    return {
      toolName: 'read_notifications',
      category: '8. Notification Tools',
      message: `${count} new notifications from WhatsApp, Bank, and Gmail`,
      voiceReply: `Aapke paas ${count} naye notifications hain. Sabse pehla WhatsApp par Rahul ka hai.`,
      data: initialNotifications,
    };
  }

  // 13. Camera & Vision
  if (lower.includes('camera') || lower.includes('photo') || lower.includes('scan') || lower.includes('barcode')) {
    return {
      toolName: 'camera_vision',
      category: '5. Files & Photos / Vision',
      message: 'Camera viewfinder active: Ready for photo capture & real-time OCR/Barcode scan.',
      voiceReply: 'Camera khol diya hai, scan ya photo lene ke liye ready hai.',
      needsModal: 'camera',
    };
  }

  // 14. Maps & Navigation
  if (lower.includes('rasta') || lower.includes('navigate') || lower.includes('map') || lower.includes('kahan hoon')) {
    return {
      toolName: 'open_map',
      category: '9. Maps & My World',
      message: 'MYRA Personal AI Map UI loaded with turn-by-turn navigation.',
      voiceReply: 'Map khol diya hai. Current location Sector 62, Noida hai.',
    };
  }

  // 15. Missions
  if (lower.includes('mission') || lower.includes('routine') || lower.includes('trip')) {
    return {
      toolName: 'start_mission',
      category: '7. Missions (Autonomous Execution)',
      message: 'Multi-step autonomous mission started: Step 1 of 4 running in background.',
      voiceReply: 'Mission shuru ho gaya hai! Mission Dashboard mein live status dekh sakte hain.',
    };
  }

  // 16. Default Fallback
  return {
    toolName: 'general_chat',
    category: 'Conversational Assistant',
    message: `Processed: "${text}"`,
    voiceReply: `Ji! "${text}" samajh liya hai. MYRA aapki poori madad ke liye tayyar hai.`,
  };
}
