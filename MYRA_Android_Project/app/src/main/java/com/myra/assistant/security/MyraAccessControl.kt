// MYRA ACCESS CONTROL ACTIVE
package com.myra.assistant.security

/**
 * MYRA Connect - Category-wise Access Control & Privacy Framework
 * 
 * Strict privacy enforcement:
 * - Every category is independently controlled by the user.
 * - No backdoor, keylogger, or hidden monitoring.
 * - If a category is OFF, no remote inspection or command is permitted for that category.
 */
enum class AccessCategory(val id: String, val titleHindi: String, val descriptionHindi: String) {
    DEVICE_INFO(
        id = "device_info",
        titleHindi = "डिवाइस जानकारी",
        descriptionHindi = "बैटरी, स्टोरेज, रैम, नेटवर्क और ओएस वर्ज़न की जानकारी"
    ),
    INSTALLED_APPS(
        id = "installed_apps",
        titleHindi = "इंस्टॉल किए गए ऐप्स",
        descriptionHindi = "केवल यूज़र-अनुमत ऐप्स की सूची देखना और खोलना"
    ),
    SHARED_FILES(
        id = "shared_files",
        titleHindi = "साझा की गई फ़ाइलें",
        descriptionHindi = "केवल यूज़र द्वारा चुने गए फ़ोल्डर्स/फ़ाइलों की सूची और एक्सेस"
    ),
    DIAGNOSTICS(
        id = "diagnostics",
        titleHindi = "हेल्थ और डायग्नोस्टिक्स",
        descriptionHindi = "स्टोरेज भराव, कनेक्शन एरर और सुरक्षित ऐप लॉग्स"
    ),
    NOTIFICATIONS(
        id = "notifications",
        titleHindi = "नोटिफ़िकेशन अलर्ट्स",
        descriptionHindi = "रिमोट फोन पर ज़रूरी सिस्टम अलर्ट्स"
    ),
    CONTACTS_CALLS(
        id = "contacts_calls",
        titleHindi = "कॉल्स और संपर्क",
        descriptionHindi = "कॉल अलर्ट्स और डायलिंग अनुमति"
    ),
    DEVICE_COMMANDS(
        id = "device_commands",
        titleHindi = "डिवाइस कमांड्स",
        descriptionHindi = "टॉर्च, वॉल्यूम और मीडिया नियंत्रण"
    );

    companion object {
        fun fromId(id: String): AccessCategory? = entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }
}

/**
 * Access policy configuration specifying permissions per category for a paired device.
 */
data class MyraAccessPolicy(
    val deviceId: String,
    val isDeviceInfoAllowed: Boolean = true,
    val isInstalledAppsAllowed: Boolean = false,
    val isSharedFilesAllowed: Boolean = false,
    val isDiagnosticsAllowed: Boolean = true,
    val isNotificationsAllowed: Boolean = false,
    val isContactsCallsAllowed: Boolean = false,
    val isDeviceCommandsAllowed: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    /**
     * Checks whether a specific category is permitted under this policy.
     */
    fun isCategoryAllowed(category: AccessCategory): Boolean = when (category) {
        AccessCategory.DEVICE_INFO -> isDeviceInfoAllowed
        AccessCategory.INSTALLED_APPS -> isInstalledAppsAllowed
        AccessCategory.SHARED_FILES -> isSharedFilesAllowed
        AccessCategory.DIAGNOSTICS -> isDiagnosticsAllowed
        AccessCategory.NOTIFICATIONS -> isNotificationsAllowed
        AccessCategory.CONTACTS_CALLS -> isContactsCallsAllowed
        AccessCategory.DEVICE_COMMANDS -> isDeviceCommandsAllowed
    }
}
