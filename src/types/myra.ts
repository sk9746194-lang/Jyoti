/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

export type AssistantState = 'idle' | 'listening' | 'thinking' | 'speaking';

export type PersonalityMode = 'friendly_assistant' | 'romantic_gf' | 'professional' | 'gaming';

export type VoiceEngineMode = 'gemini_live' | 'gemini_tts' | 'fallback_tts';

export interface VoiceStatus {
  activeMode: VoiceEngineMode;
  geminiLiveConnected: boolean;
  model: string;
  voiceName: string;
  fallbackActive: boolean;
  fallbackReason?: string;
  sampleRateIn: number;
  sampleRateOut: number;
}

export interface VoiceSettings {
  ttsEngine: VoiceEngineMode;
  language: 'hi-IN' | 'en-IN';
  pitch: number;
  rate: number;
  personality: PersonalityMode;
  wakeWordEnabled: boolean;
  wakeWord: string;
  preferredVoice: string; // e.g. 'Kore' (Female Natural AI)
}

export type OrbStyleType =
  | 'classic_glow'
  | 'siri_fluid'
  | 'neon_ring'
  | 'galaxy_particle'
  | 'energy_core'
  | 'aurora'
  | 'glass'
  | 'pulse'
  | 'wave'
  | 'myra_signature';

export type BackgroundStyleType =
  | 'solid'
  | 'gradient'
  | 'animated_gradient'
  | 'particles'
  | 'galaxy'
  | 'aurora'
  | 'digital'
  | 'myra_signature';

export type EdgeEffectModeType =
  | 'off'
  | 'bottom_only'
  | 'left_right'
  | 'top_bottom'
  | 'all_4_sides';

export interface OrbSettings {
  style?: OrbStyleType;
  theme: 'electric_blue' | 'cyber_purple' | 'romantic_rose' | 'emerald_neon' | 'cyber_amber' | 'myra_signature';
  sizeDp?: number;
  particleCount: number;
  glowIntensity: number;
  animationSpeed?: number;
  ringThickness?: number;
  opacity?: number;
  position?: 'top_third' | 'center' | 'bottom_offset';
  reactToAudio: boolean;
  backgroundStyle?: BackgroundStyleType;
  backgroundSpeed?: number;
  backgroundOpacity?: number;
  edgeEffectMode?: EdgeEffectModeType;
  edgeGlowIntensity?: number;
}

export interface ChatMessage {
  id: string;
  sender: 'user' | 'myra';
  text: string;
  timestamp: string;
  action?: string;
  category?: string;
  isVoiceNote?: boolean;
  audioDuration?: number;
  quickReplies?: string[];
}

export interface MissionStep {
  id: string;
  title: string;
  description: string;
  status: 'pending' | 'in_progress' | 'completed' | 'failed';
  app?: string;
}

export interface Mission {
  id: string;
  title: string;
  goal: string;
  status: 'running' | 'paused' | 'completed' | 'cancelled';
  progress: number;
  steps: MissionStep[];
}

export interface ScreenElement {
  id: number;
  text: string;
  type: 'button' | 'input' | 'text' | 'image' | 'container';
  x: number;
  y: number;
  clickable: boolean;
}

export interface TriggerRule {
  id: string;
  name: string;
  type: 'scheduled' | 'notification' | 'charging' | 'battery' | 'screen' | 'headphones' | 'app' | 'wifi';
  condition: string;
  actionType: 'speak_line' | 'run_mission' | 'live_conversation';
  actionValue: string;
  enabled: boolean;
}

export interface ParkingLocation {
  latitude: number;
  longitude: number;
  spotName: string;
  timestamp: string;
  saved: boolean;
}

export interface SystemHealthMetrics {
  ramUsagePercent: number;
  batteryLevel: number;
  batteryStatus: 'charging' | 'discharging';
  cpuTempCelsius: number;
  topDrainingApps: { name: string; percent: number }[];
  cacheSizeMB: number;
}

export interface NotificationItem {
  id: string;
  app: 'WhatsApp' | 'Telegram' | 'Gmail' | 'Bank' | 'Phone';
  title: string;
  message: string;
  time: string;
  isOtp?: boolean;
  otpCode?: string;
  isMissedCall?: boolean;
}

export interface DailyGoal {
  id: string;
  title: string;
  category: 'health' | 'productivity' | 'routine' | 'learning';
  completed: boolean;
  timeTarget?: string;
  icon?: string;
}

// MYRA Connect — Phone-to-Phone Interconnect Types
export type ConnectStatusType = 'connected' | 'connecting' | 'disconnected' | 'pairing_required';
export type ConnectTransportType = 'wifi_local' | 'hotspot' | 'bluetooth' | 'internet_tunnel' | 'firebase_cloud';
export type DevicePresenceStatus = 'online' | 'syncing' | 'waiting_approval' | 'offline';

export interface GoogleAccountProfile {
  uid: string;
  email: string;
  displayName: string;
  photoURL?: string;
}

export interface SharedFileItem {
  id: string;
  name: string;
  size: string;
  type: string;
  date: string;
}

export interface PairedDevice {
  id: string;
  name: string;
  model: string;
  androidVersion: string;
  ipAddress: string;
  port: number;
  authToken: string;
  status: ConnectStatusType;
  presenceStatus?: DevicePresenceStatus;
  connectionType: ConnectTransportType;
  lastConnectedTime: number;
  batteryLevel: number;
  isCharging: boolean;
  isOnline: boolean;
  storageTotalGB?: number;
  storageUsedGB?: number;
  networkType?: string;
  isHost?: boolean;
  userEmail?: string;
  pairingCode?: string;
  approvedApps?: string[];
  sharedFiles?: SharedFileItem[];
}

export interface DevicePermissions {
  openApps: boolean;
  mediaControls: boolean;
  deviceCommands: boolean;
  notifications: boolean;
  files: boolean;
  contactsCalls: boolean;
  deviceStatus?: boolean;
  accessibilityActions?: boolean;
}

export type RemoteResultStatus =
  | 'SUCCESS'
  | 'FAILED'
  | 'PERMISSION REQUIRED'
  | 'DEVICE OFFLINE'
  | 'NOT SUPPORTED'
  | 'success'
  | 'failed'
  | 'not_connected'
  | 'permission_required'
  | 'app_not_installed'
  | 'unsupported';

export interface RemoteCommandResult {
  commandId: string;
  status: RemoteResultStatus;
  message: string;
  payload?: string;
  timestamp: number;
}

export interface ConnectAuditLog {
  id: string;
  timestamp: string;
  action: string;
  deviceName: string;
  status: RemoteResultStatus;
  details: string;
}

export interface DropTransferItem {
  id: string;
  senderDeviceId: string;
  senderDeviceName: string;
  receiverDeviceId: string;
  receiverDeviceName: string;
  fileName: string;
  fileSize: string;
  fileCategory: 'Photos' | 'Videos' | 'Documents' | 'Audio' | 'Archives';
  fileIcon?: string;
  status: 'pending_approval' | 'transferring' | 'completed' | 'declined' | 'failed';
  progress: number;
  timestamp: number;
}

export interface TaskHandoffItem {
  id: string;
  sourceDeviceId: string;
  sourceDeviceName: string;
  targetDeviceId: string;
  targetDeviceName: string;
  taskTitle: string;
  taskCategory: 'Web Browsing' | 'Document' | 'Voice Task' | 'App Deep-Link' | 'Conversation';
  payload: string;
  status: 'ready' | 'received' | 'opened';
  timestamp: number;
}


