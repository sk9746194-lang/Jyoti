/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { PersonalityMode } from '../types/myra';

/**
 * Generates smart context-aware quick reply chips based on MYRA's last response,
 * the command category/action, user query, and active personality mode.
 */
export function generateQuickReplies(
  lastText: string,
  category?: string,
  action?: string,
  personality: PersonalityMode = 'friendly_assistant'
): string[] {
  const lower = (lastText + ' ' + (category || '') + ' ' + (action || '')).toLowerCase();

  // Romantic GF Mode specific quick reactions
  if (personality === 'romantic_gf') {
    if (lower.includes('kaise ho') || lower.includes('kesi ho') || lower.includes('hero') || lower.includes('love') || lower.includes('pyar') || lower.includes('sweet')) {
      return ['Aapki bahut yaad aa rahi thi ❤️', 'Aap mere sabse favorite ho! 😘', 'Chalo ek romantic gaana sunao 🎵', 'Main theek hoon jaan, aap batao?'];
    }
    if (lower.includes('gaana') || lower.includes('song') || lower.includes('music')) {
      return ['Next romantic gaana lagao 🎶', 'Volume badhao please 🔊', 'Pause karo meri jaan ⏸️'];
    }
  }

  // Calls & Phone
  if (lower.includes('call') || lower.includes('phone') || lower.includes('dial') || action === 'call_contact') {
    return [
      'Speaker ON karo 📢',
      'Call kaat do ❌',
      'Mummy ko call lagao 📞',
      'WhatsApp par message bhejo 💬'
    ];
  }

  // Music & Media
  if (lower.includes('music') || lower.includes('gaana') || lower.includes('song') || lower.includes('play') || action === 'play_music') {
    return [
      'Next song play karo ⏭️',
      'Arijit Singh ke sad songs lagao 🎶',
      'Volume 80% karo 🔊',
      'Music pause kar do ⏸️'
    ];
  }

  // WhatsApp & Messaging
  if (lower.includes('whatsapp') || lower.includes('message') || lower.includes('sms') || action === 'send_whatsapp' || action === 'send_sms') {
    return [
      'Papa ko WhatsApp bhejo 👨',
      'Rahul ko bolo "Main 10 min me aa raha hoon" 💬',
      'Latest WhatsApp notifications padho 📩',
      'Voice note record karo 🎙️'
    ];
  }

  // Flashlight / Torch
  if (lower.includes('torch') || lower.includes('flashlight')) {
    if (lower.includes('on') || lower.includes('jala')) {
      return ['Torch band kar do 🔦', 'Screen brightness badhao ☀️', 'Battery status check karo 🔋'];
    } else {
      return ['Torch on karo 🔦', 'Flashlight blink mode ✨', 'System health scan karo 📱'];
    }
  }

  // Camera & Visual Scanner
  if (lower.includes('camera') || lower.includes('photo') || lower.includes('scan') || action === 'camera_vision') {
    return [
      'Photo click karo 📸',
      'Front camera open karo 🤳',
      'Document scan karo 📄',
      'QR code scan karo 🔍'
    ];
  }

  // Alarm & Timers
  if (lower.includes('alarm') || lower.includes('timer')) {
    return [
      'Subah 6 baje ka alarm lagao ⏰',
      '10 minute ka timer start karo ⏳',
      'Alarms list dikhao 📋',
      'Saare alarm cancel karo 🔕'
    ];
  }

  // Parking & Navigation
  if (lower.includes('parking') || lower.includes('location') || lower.includes('map') || lower.includes('navigate')) {
    return [
      'Meri gaadi kahan park hai? 📍',
      'Yahan ki parking location save karo 🚗',
      'Ghar ka rasta dikhao 🗺️',
      'Nearby petrol pump dhoondo ⛽'
    ];
  }

  // Battery & System Health
  if (lower.includes('battery') || lower.includes('charging') || lower.includes('storage') || lower.includes('health') || lower.includes('ram')) {
    return [
      'Battery saver ON karo 🔋',
      'Junk files clean karo 🧹',
      'RAM optimize karo 🚀',
      'Phone lock kar do 🔒'
    ];
  }

  // Emergency & SOS
  if (lower.includes('sos') || lower.includes('emergency') || lower.includes('police') || lower.includes('alert')) {
    return [
      'Police ko call lagao 🚨',
      'Emergency SOS cancel karo 🛑',
      'Live location family ko bhejo 📍',
      'Flashlight SOS strobe on karo ⚡'
    ];
  }

  // Notifications & Briefing
  if (lower.includes('notification') || lower.includes('briefing') || lower.includes('namaste') || lower.includes('schedule') || lower.includes('weather')) {
    return [
      'Aaj ka weather kaisa hai? ⛅',
      'Unread notifications padho 📬',
      'Papa ko call lagao 📞',
      'Arijit Singh ke gaane chalao 🎵'
    ];
  }

  // Default smart assistant suggestions
  if (personality === 'romantic_gf') {
    return [
      'Haanji meri jaan, bolo ❤️',
      'Ek pyara sa gaana sunao 🎶',
      'Aapko kya pasand hai? 😊',
      'Mujhe ek pyari shayari sunao 🌹'
    ];
  }

  return [
    'Papa ko call lagao 📞',
    'Arijit Singh ke gaane chalao 🎵',
    'Torch on karo 🔦',
    'Unread messages padho 💬'
  ];
}
