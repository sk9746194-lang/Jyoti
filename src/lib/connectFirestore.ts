/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import {
  doc,
  setDoc,
  getDoc,
  getDocs,
  updateDoc,
  deleteDoc,
  collection,
  query,
  onSnapshot,
} from 'firebase/firestore';
import { db, auth, handleFirestoreError, OperationType } from './firebase';
import {
  PairedDevice,
  DevicePermissions,
  ConnectAuditLog,
  RemoteCommandResult,
  DropTransferItem,
  TaskHandoffItem,
} from '../types/myra';

/**
 * Save or update a connected device in Firestore under the authenticated user
 */
export async function syncDeviceToFirestore(userId: string, device: PairedDevice): Promise<void> {
  const path = `users/${userId}/devices/${device.id}`;
  try {
    const deviceRef = doc(db, 'users', userId, 'devices', device.id);
    await setDoc(
      deviceRef,
      {
        id: device.id,
        userId: userId,
        name: device.name,
        model: device.model,
        androidVersion: device.androidVersion,
        batteryLevel: device.batteryLevel,
        isCharging: device.isCharging,
        isOnline: device.isOnline,
        lastActive: new Date(device.lastConnectedTime).toISOString(),
        isPrimary: device.isHost || false,
        pairingStatus: device.status === 'connected' ? 'paired' : 'pending',
        presenceStatus: device.presenceStatus || (device.isOnline ? 'online' : 'offline'),
        networkType: device.networkType || 'Wi-Fi',
        storageTotalGB: device.storageTotalGB || 256,
        storageUsedGB: device.storageUsedGB || 114,
        approvedApps: device.approvedApps || [],
        sharedFilesCount: device.sharedFiles?.length || 0,
      },
      { merge: true }
    );
  } catch (error) {
    console.warn(`Firestore syncDevice error for ${path}:`, error);
  }
}

/**
 * Save or update granular device permissions in Firestore
 */
export async function syncPermissionsToFirestore(
  userId: string,
  deviceId: string,
  permissions: DevicePermissions
): Promise<void> {
  const path = `users/${userId}/permissions/${deviceId}`;
  try {
    const permRef = doc(db, 'users', userId, 'permissions', deviceId);
    await setDoc(
      permRef,
      {
        deviceId,
        userId,
        openApps: permissions.openApps,
        mediaControls: permissions.mediaControls,
        deviceCommands: permissions.deviceCommands,
        notifications: permissions.notifications,
        files: permissions.files,
        contactsCalls: permissions.contactsCalls,
        deviceStatus: permissions.deviceStatus ?? true,
        accessibilityActions: permissions.accessibilityActions ?? false,
        updatedAt: new Date().toISOString(),
      },
      { merge: true }
    );
  } catch (error) {
    console.warn(`Firestore syncPermissions error for ${path}:`, error);
  }
}

/**
 * Unlink and delete device from Firestore
 */
export async function removeDeviceFromFirestore(userId: string, deviceId: string): Promise<void> {
  const path = `users/${userId}/devices/${deviceId}`;
  try {
    const devRef = doc(db, 'users', userId, 'devices', deviceId);
    await deleteDoc(devRef);
    const permRef = doc(db, 'users', userId, 'permissions', deviceId);
    await deleteDoc(permRef);
  } catch (error) {
    console.warn(`Firestore removeDevice error for ${path}:`, error);
  }
}

/**
 * Record an immutable audit log entry in Firestore
 */
export async function recordAuditLogInFirestore(
  userId: string,
  log: ConnectAuditLog,
  deviceId?: string
): Promise<void> {
  const path = `users/${userId}/auditLogs/${log.id}`;
  try {
    const logRef = doc(db, 'users', userId, 'auditLogs', log.id);
    await setDoc(logRef, {
      id: log.id,
      userId,
      deviceId: deviceId || 'global',
      action: log.action,
      details: log.details,
      status: log.status,
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    console.warn(`Firestore recordAuditLog error for ${path}:`, error);
  }
}

/**
 * Record a remote command invocation in Firestore
 */
export async function logRemoteCommandToFirestore(
  userId: string,
  targetDeviceId: string,
  action: string,
  result: RemoteCommandResult
): Promise<void> {
  const commandId = `cmd_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
  const path = `users/${userId}/commands/${commandId}`;
  try {
    const cmdRef = doc(db, 'users', userId, 'commands', commandId);
    await setDoc(cmdRef, {
      id: commandId,
      userId,
      sourceDeviceId: 'myra_primary_phone',
      targetDeviceId,
      action,
      parameters: result.payload || '',
      status: result.status,
      resultMessage: result.message,
      createdAt: new Date(result.timestamp).toISOString(),
      executedAt: new Date().toISOString(),
    });
  } catch (error) {
    console.warn(`Firestore logRemoteCommand error for ${path}:`, error);
  }
}

/**
 * Record MYRA Drop file transfer in Firestore
 */
export async function recordDropTransferInFirestore(
  userId: string,
  transfer: DropTransferItem
): Promise<void> {
  const path = `users/${userId}/dropTransfers/${transfer.id}`;
  try {
    const transferRef = doc(db, 'users', userId, 'dropTransfers', transfer.id);
    await setDoc(
      transferRef,
      {
        id: transfer.id,
        userId,
        senderDeviceId: transfer.senderDeviceId,
        receiverDeviceId: transfer.receiverDeviceId,
        fileName: transfer.fileName,
        fileSize: transfer.fileSize,
        fileCategory: transfer.fileCategory,
        status: transfer.status.toUpperCase(),
        progress: transfer.progress,
        timestamp: new Date(transfer.timestamp).toISOString(),
      },
      { merge: true }
    );
  } catch (error) {
    console.warn(`Firestore recordDropTransfer error for ${path}:`, error);
  }
}

/**
 * Record AI Task Handoff in Firestore
 */
export async function recordTaskHandoffInFirestore(
  userId: string,
  handoff: TaskHandoffItem
): Promise<void> {
  const path = `users/${userId}/taskHandoffs/${handoff.id}`;
  try {
    const handoffRef = doc(db, 'users', userId, 'taskHandoffs', handoff.id);
    await setDoc(
      handoffRef,
      {
        id: handoff.id,
        userId,
        sourceDeviceId: handoff.sourceDeviceId,
        targetDeviceId: handoff.targetDeviceId,
        taskTitle: handoff.taskTitle,
        taskType: handoff.taskCategory,
        payload: handoff.payload,
        status: handoff.status.toUpperCase(),
        timestamp: new Date(handoff.timestamp).toISOString(),
      },
      { merge: true }
    );
  } catch (error) {
    console.warn(`Firestore recordTaskHandoff error for ${path}:`, error);
  }
}

/**
 * Create pairing session code in Firestore
 */
export async function createPairingSessionInFirestore(
  userId: string,
  code: string,
  requesterDeviceId: string,
  requesterDeviceName: string
): Promise<string> {
  const sessionId = `pair_${Date.now()}`;
  const path = `users/${userId}/pairingSessions/${sessionId}`;
  try {
    const sessionRef = doc(db, 'users', userId, 'pairingSessions', sessionId);
    await setDoc(sessionRef, {
      id: sessionId,
      userId,
      code,
      requesterDeviceId,
      requesterDeviceName,
      status: 'pending',
      expiresAt: new Date(Date.now() + 5 * 60 * 1000).toISOString(),
      createdAt: new Date().toISOString(),
    });
    return sessionId;
  } catch (error) {
    console.warn(`Firestore createPairingSession error for ${path}:`, error);
    return sessionId;
  }
}
