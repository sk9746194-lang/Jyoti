package com.myra.assistant.security

import android.content.Context

/**
 * Unified SecurityManager alias and factory for MYRA Assistant.
 * Provides direct access to requireOwnerAuthentication(), speaker verification,
 * and AndroidX BiometricPrompt security boundary.
 */
typealias SecurityManager = OwnerSecurityManager

object SecurityManagerFactory {
    fun getInstance(context: Context): OwnerSecurityManager {
        return OwnerSecurityManager.getInstance(context)
    }
}
