package com.myra.assistant.tools

import android.content.Context
import android.util.Log

data class ParsedCommand(
    val intent: CommandIntent,
    val target: String = "",
    val payload: String = "",
    val confidence: Float = 1.0f,
    val isRemote: Boolean = false,
    val remoteTargetDevice: String = ""
)

enum class CommandIntent {
    CALL_PHONE,
    SEND_WHATSAPP,
    SEND_SMS,
    SET_ALARM,
    SET_TIMER,
    ADD_CALENDAR_EVENT,
    OPEN_WIFI_SETTINGS,
    OPEN_BLUETOOTH_SETTINGS,
    CAMERA_VISION,
    SCREEN_UNDERSTANDING,
    EMERGENCY_SOS,
    TOGGLE_TORCH,
    CHECK_BATTERY,
    LOCK_DEVICE,
    OPEN_APP,
    PLAY_MUSIC,
    ADJUST_VOLUME,
    SYSTEM_DIAGNOSTICS,
    EXECUTE_REMOTE_DEVICE,
    CONVERSATIONAL_CHAT,
    UNKNOWN_LLM_FALLBACK
}

/**
 * Fast Hindi/Hinglish & English Command Router for MYRA Assistant.
 * Handles SOV (Subject-Object-Verb, e.g. "WhatsApp खोलो") and SVO ("Open WhatsApp").
 * Normalizes wake words and Devanagari script for instant offline execution.
 */
class FastCommandRouter(private val context: Context) {

    private val tag = "MYRA_Router"

    // Wake words to strip from beginning
    private val wakeWordPrefix = Regex("""(?i)^(hey\s+myra|ok\s+myra|hello\s+myra|myra|mayra|maira|हे\s+मायरा|मायरा|सुनो\s+मायरा)[,\s:]*""")

    fun route(rawInput: String): ParsedCommand {
        val cleaned = cleanInput(rawInput)
        Log.d(tag, "MYRA_CMD: raw='$rawInput', cleaned='$cleaned'")

        if (cleaned.isEmpty()) {
            return ParsedCommand(CommandIntent.UNKNOWN_LLM_FALLBACK, payload = rawInput)
        }

        // 0. Emergency SOS check (Highest priority safety action)
        if (cleaned.contains("sos") || cleaned.contains("emergency") || 
            (cleaned.contains("help") && (cleaned.contains("bachao") || cleaned.contains("police") || cleaned.contains("karo")))) {
            Log.d(tag, "MYRA_ROUTE: EMERGENCY_SOS")
            return ParsedCommand(CommandIntent.EMERGENCY_SOS, target = "emergency")
        }

        // 0b. Remote Phone-to-Phone Interconnect check (MYRA Connect)
        val remoteResult = checkRemoteDeviceCommand(cleaned, rawInput)
        if (remoteResult != null) {
            Log.d(tag, "MYRA_ROUTE: EXECUTE_REMOTE_DEVICE (${remoteResult.payload})")
            return remoteResult
        }

        // 1. Torch control check (Highest hardware priority)
        val torchResult = checkTorchCommand(cleaned)
        if (torchResult != null) {
            Log.d(tag, "MYRA_ROUTE: TOGGLE_TORCH (${torchResult.payload})")
            return torchResult
        }

        // 2. Music Player / Song playback check
        val musicResult = checkMusicCommand(cleaned)
        if (musicResult != null) {
            Log.d(tag, "MYRA_ROUTE: ${musicResult.intent} (target='${musicResult.target}')")
            return musicResult
        }

        // 3. Battery check
        if (cleaned.contains("battery") || cleaned.contains("charge") || cleaned.contains("बैटरी")) {
            Log.d(tag, "MYRA_ROUTE: CHECK_BATTERY")
            return ParsedCommand(CommandIntent.CHECK_BATTERY)
        }

        // 3a. Camera Vision check
        val visionResult = checkVisionCommand(cleaned)
        if (visionResult != null) {
            Log.d(tag, "MYRA_ROUTE: CAMERA_VISION")
            return visionResult
        }

        // 3b. Screen Understanding check
        val screenResult = checkScreenCommand(cleaned)
        if (screenResult != null) {
            Log.d(tag, "MYRA_ROUTE: SCREEN_UNDERSTANDING")
            return screenResult
        }

        // 3c. Alarms and Timers check
        val alarmResult = checkAlarmCommand(cleaned)
        if (alarmResult != null) {
            Log.d(tag, "MYRA_ROUTE: ${alarmResult.intent}")
            return alarmResult
        }

        val timerResult = checkTimerCommand(cleaned)
        if (timerResult != null) {
            Log.d(tag, "MYRA_ROUTE: SET_TIMER")
            return timerResult
        }

        // 3d. Calendar Event check
        val calendarResult = checkCalendarCommand(cleaned)
        if (calendarResult != null) {
            Log.d(tag, "MYRA_ROUTE: ADD_CALENDAR_EVENT")
            return calendarResult
        }

        // 3e. Wi-Fi and Bluetooth settings panels check
        val connectivityResult = checkWifiBluetoothCommand(cleaned)
        if (connectivityResult != null) {
            Log.d(tag, "MYRA_ROUTE: ${connectivityResult.intent}")
            return connectivityResult
        }

        // 3f. SMS message check
        val smsResult = checkSmsCommand(cleaned)
        if (smsResult != null) {
            Log.d(tag, "MYRA_ROUTE: SEND_SMS (target='${smsResult.target}')")
            return smsResult
        }

        // 4. Lock screen check
        if ((cleaned.contains("lock") || cleaned.contains("लॉक")) && 
            (cleaned.contains("phone") || cleaned.contains("screen") || cleaned.contains("karo") || cleaned.contains("kar do") || cleaned.contains("करो"))) {
            Log.d(tag, "MYRA_ROUTE: LOCK_DEVICE")
            return ParsedCommand(CommandIntent.LOCK_DEVICE)
        }

        // 5. Volume adjustment
        if (cleaned.contains("volume") || cleaned.contains("awaaz") || cleaned.contains("awaz") || cleaned.contains("आवाज़")) {
            Log.d(tag, "MYRA_ROUTE: ADJUST_VOLUME")
            return ParsedCommand(CommandIntent.ADJUST_VOLUME, payload = "80")
        }

        // 6. Diagnostics
        if (cleaned.contains("status") || cleaned.contains("diagnostics") || cleaned.contains("health") || cleaned.contains("स्थिति")) {
            Log.d(tag, "MYRA_ROUTE: SYSTEM_DIAGNOSTICS")
            return ParsedCommand(CommandIntent.SYSTEM_DIAGNOSTICS)
        }

        // 7. WhatsApp message automation check (Must precede simple open app)
        val waMsgResult = checkWhatsAppMessage(cleaned)
        if (waMsgResult != null) {
            Log.d(tag, "MYRA_ROUTE: SEND_WHATSAPP (target='${waMsgResult.target}')")
            return waMsgResult
        }

        // 8. Phone call check
        val callResult = checkPhoneCall(cleaned)
        if (callResult != null) {
            Log.d(tag, "MYRA_ROUTE: CALL_PHONE (target='${callResult.target}')")
            return callResult
        }

        // 9. Known popular App launches (SOV: "WhatsApp kholo", "YouTube chalao", etc.)
        val appResult = checkAppLaunch(cleaned)
        if (appResult != null) {
            Log.d(tag, "MYRA_ROUTE: OPEN_APP (target='${appResult.target}')")
            return appResult
        }

        // 10. Conversational companion dialogue (greetings, identity, capabilities, status)
        val convResult = checkConversationalPhrase(cleaned)
        if (convResult != null) {
            Log.d(tag, "MYRA_ROUTE: CONVERSATIONAL_CHAT (${convResult.payload})")
            return convResult
        }

        Log.d(tag, "MYRA_ROUTE: UNKNOWN_LLM_FALLBACK (payload='$cleaned')")
        return ParsedCommand(CommandIntent.UNKNOWN_LLM_FALLBACK, payload = cleaned)
    }

    /**
     * Strips wake words and maps common Devanagari command words to phonetic English tokens.
     */
    private fun cleanInput(input: String): String {
        var str = input.trim()
        str = wakeWordPrefix.replace(str, "").trim()

        // Normalize Devanagari keywords for uniform matching
        str = str
            .replace("व्हाट्सएप", "whatsapp")
            .replace("व्हाट्सऐप", "whatsapp")
            .replace("यूट्यूब", "youtube")
            .replace("क्रोम", "chrome")
            .replace("कैमरा", "camera")
            .replace("सेटिंग्स", "settings")
            .replace("सेटिंग", "settings")
            .replace("गैलरी", "gallery")
            .replace("फ़ोन", "phone")
            .replace("फोन", "phone")
            .replace("डायलर", "dialer")
            .replace("म्यूजिक", "music")
            .replace("म्यूज़िक", "music")
            .replace("गाना", "gaana")
            .replace("गाने", "gaana")
            .replace("गीत", "gaana")
            .replace("टॉर्च", "torch")
            .replace("लाइट", "torch")
            .replace("फ्लैशलाइट", "torch")
            .replace("खोलो", "kholo")
            .replace("खोलिए", "kholo")
            .replace("खोल दो", "kholo")
            .replace("चलाओ", "chalao")
            .replace("लगाओ", "lagao")
            .replace("बजाओ", "bajao")
            .replace("सुनाओ", "suno")
            .replace("जलाओ", "jalao")
            .replace("चालू करो", "on karo")
            .replace("चालू", "on")
            .replace("बंद करो", "off karo")
            .replace("बंद", "off")
            .replace("बुझाओ", "off")
            .replace("कॉल", "call")
            .replace("करो", "karo")
            .replace("मम्मी", "mummy")
            .replace("पापा", "papa")
            .replace("फेसबुक", "facebook")
            .replace("इंस्टाग्राम", "instagram")
            .replace("इंस्टा", "instagram")
            .replace("टेलीग्राम", "telegram")
            .replace("ट्विटर", "twitter")
            .replace("जीमेल", "gmail")
            .replace("मैप्स", "maps")
            .replace("मैप", "maps")
            .replace("स्पॉटिफ़ाई", "spotify")
            .replace("स्पॉटिफाई", "spotify")
            .replace("एसओएस", "sos")
            .replace("मदद", "help")
            .replace("इमरजेंसी", "emergency")

        return str.lowercase().trim()
    }

    private fun checkTorchCommand(str: String): ParsedCommand? {
        val isTorchMentioned = str.contains("torch") || str.contains("flashlight") || str.contains("light") || str.contains("flash")
        if (!isTorchMentioned) return null

        val isOn = str.contains("on") || str.contains("jalao") || str.contains("chalao") || str.contains("start") || str.contains("jala")
        val isOff = str.contains("off") || str.contains("band") || str.contains("bujhao") || str.contains("stop") || str.contains("bujha")

        return when {
            isOn && !isOff -> ParsedCommand(CommandIntent.TOGGLE_TORCH, payload = "on")
            isOff -> ParsedCommand(CommandIntent.TOGGLE_TORCH, payload = "off")
            else -> null
        }
    }

    private fun checkMusicCommand(str: String): ParsedCommand? {
        // Explicit YouTube Music command
        if (str.contains("youtube music") || str.contains("yt music")) {
            return ParsedCommand(CommandIntent.OPEN_APP, target = "youtube_music")
        }

        val hasMusicKeyword = str.contains("music") || str.contains("gaana") || str.contains("song") || str.contains("geet")
        val hasPlayAction = str.contains("lagao") || str.contains("chalao") || str.contains("bajao") || 
                           str.contains("play") || str.contains("kholo") || str.contains("open") || str.contains("suno")

        if (hasMusicKeyword && hasPlayAction) {
            return ParsedCommand(CommandIntent.PLAY_MUSIC)
        }

        // Just "music lagao", "gaana chalao", "play music"
        if (str == "music" || str == "gaana" || str == "play music") {
            return ParsedCommand(CommandIntent.PLAY_MUSIC)
        }

        return null
    }

    private fun checkWhatsAppMessage(str: String): ParsedCommand? {
        if (!str.contains("whatsapp")) return null

        val isMessageCommand = str.contains("message") || str.contains("msg") || str.contains("bhejo") || str.contains("send") || str.contains("bolo")
        if (!isMessageCommand) return null

        // Pattern 1: "whatsapp par [contact] ko message bhejo [message]"
        val regex1 = Regex("""whatsapp\s+(?:pe|par)?\s*(.+?)\s+(?:ko|pe)?\s*(?:message|msg)?\s*(?:bhejo|send|bolo)?\s*(.*)""")
        val match1 = regex1.find(str)
        if (match1 != null) {
            val rawContact = match1.groupValues[1].replace("ko", "").replace("pe", "").replace("par", "").trim()
            val rawPayload = match1.groupValues[2].trim()
            if (rawContact.isNotEmpty() && rawContact != "kisi" && rawContact != "open" && rawContact != "kholo") {
                return ParsedCommand(CommandIntent.SEND_WHATSAPP, target = rawContact, payload = rawPayload)
            }
        }

        // Pattern 2: "[contact] ko whatsapp par message bhejo [message]"
        val regex2 = Regex("""(.+?)\s+(?:ko|pe)?\s*whatsapp\s*(?:pe|par)?\s*(?:message|msg)?\s*(?:bhejo|send|bolo)?\s*(.*)""")
        val match2 = regex2.find(str)
        if (match2 != null) {
            val rawContact = match2.groupValues[1].trim()
            val rawPayload = match2.groupValues[2].trim()
            if (rawContact.isNotEmpty() && rawContact != "kisi" && rawContact != "open" && rawContact != "kholo") {
                return ParsedCommand(CommandIntent.SEND_WHATSAPP, target = rawContact, payload = rawPayload)
            }
        }

        return ParsedCommand(CommandIntent.SEND_WHATSAPP, target = "")
    }

    private fun checkPhoneCall(str: String): ParsedCommand? {
        val hasCall = str.contains("call") || str.contains("phone") || str.contains("dial")
        if (!hasCall) return null

        // Avoid triggering call for "phone kholo" (which is open dialer app)
        if ((str.contains("kholo") || str.contains("open")) && (str.contains("phone") || str.contains("dialer"))) {
            return null
        }

        // Patterns like "mummy ko call karo", "papa ko phone lagao", "call rahul"
        val patterns = listOf(
            Regex("""(?:call|phone)\s+(?:karo|lagao|milao)?\s*(?:to|ko)?\s*(.+)"""),
            Regex("""(.+?)\s+(?:ko|pe)?\s*(?:call|phone)\s*(?:karo|lagao|milao|laga\s+do)""")
        )

        for (pattern in patterns) {
            val match = pattern.find(str)
            if (match != null) {
                var contact = match.groupValues[1].trim()
                contact = contact.replace("call", "").replace("phone", "").replace("ko", "").trim()
                if (contact.isNotEmpty()) {
                    return ParsedCommand(CommandIntent.CALL_PHONE, target = contact)
                }
            }
        }

        return null
    }

    private fun checkAppLaunch(str: String): ParsedCommand? {
        val knownApps = listOf(
            "whatsapp", "youtube", "chrome", "camera", "settings", "gallery",
            "phone", "dialer", "calculator", "maps", "spotify", "instagram", "telegram",
            "facebook", "twitter", "gmail"
        )

        val openVerbs = listOf("kholo", "open", "chalao", "launch", "start", "khol do", "open karo", "kholna")

        // 1. Direct app name + open verb (SOV): "whatsapp kholo", "camera open karo", "chrome chalao"
        for (app in knownApps) {
            for (verb in openVerbs) {
                if (str == "$app $verb" || str == "$app $verb karo" || str == "$app app $verb" || 
                    str.startsWith("$app ") && str.endsWith(verb)) {
                    val resolvedTarget = if (app == "dialer") "phone" else app
                    return ParsedCommand(CommandIntent.OPEN_APP, target = resolvedTarget)
                }
            }
        }

        // 2. Open verb + app name (SVO): "open whatsapp", "kholo youtube"
        for (app in knownApps) {
            for (verb in openVerbs) {
                if (str == "$verb $app" || str == "$verb $app app" || str.startsWith("$verb ") && str.endsWith(app)) {
                    val resolvedTarget = if (app == "dialer") "phone" else app
                    return ParsedCommand(CommandIntent.OPEN_APP, target = resolvedTarget)
                }
            }
        }

        // 3. Fallback: If string is strictly just the known app name e.g. "whatsapp", "camera"
        for (app in knownApps) {
            if (str == app) {
                val resolvedTarget = if (app == "dialer") "phone" else app
                return ParsedCommand(CommandIntent.OPEN_APP, target = resolvedTarget)
            }
        }

        // 4. Special camera colloquial phrases
        if (str.contains("camera") && (str.contains("kholo") || str.contains("open") || str.contains("chalao") || str.contains("start"))) {
            return ParsedCommand(CommandIntent.OPEN_APP, target = "camera")
        }
        if (str.contains("photo") && (str.contains("khicho") || str.contains("lo") || str.contains("lena"))) {
            return ParsedCommand(CommandIntent.OPEN_APP, target = "camera")
        }

        // 5. Special browser phrases
        if (str.contains("browser") && (str.contains("kholo") || str.contains("open"))) {
            return ParsedCommand(CommandIntent.OPEN_APP, target = "chrome")
        }

        // 6. Generic regex for any other installed app: "X kholo" or "open X"
        val genericPatterns = listOf(
            Regex("""(?i)(?:open|kholo|chalao|launch)\s+(?:app)?\s*(.+)"""),
            Regex("""(?i)(.+?)\s+(?:app)?\s*(?:kholo|chalao|open\s+karo|khol\s+do)""")
        )

        for (pattern in genericPatterns) {
            val match = pattern.find(str)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (candidate.isNotEmpty() && candidate != "app") {
                    return ParsedCommand(CommandIntent.OPEN_APP, target = candidate)
                }
            }
        }

        return null
    }

    private fun checkRemoteDeviceCommand(cleaned: String, raw: String): ParsedCommand? {
        val lower = cleaned.lowercase()
        val remoteKeywords = listOf(
            "दूसरे फोन पर", "दूसरे फोन में", "दूसरे मोबाइल पर", "दूसरे मोबाइल में",
            "दूसरे फोन", "दूसरे मोबाइल", "दूसरे डिवाइस पर",
            "घर वाले फोन पर", "घर के फोन पर", "घर वाले फोन", "घर के मोबाइल पर",
            "doosre phone par", "dusre phone par", "doosre phone", "dusre phone",
            "remote phone par", "remote phone", "second phone par", "second phone",
            "home phone par", "home phone"
        )

        val matchedKeyword = remoteKeywords.find { lower.contains(it) } ?: return null

        val targetDeviceName = if (matchedKeyword.contains("घर") || matchedKeyword.contains("home")) {
            "Home Phone"
        } else {
            "दूसरा फोन"
        }

        val extractedSubCmd = cleaned
            .replace(matchedKeyword, "", ignoreCase = true)
            .replace("पर", "", ignoreCase = true)
            .replace("par", "", ignoreCase = true)
            .replace("pe", "", ignoreCase = true)
            .trim()

        return ParsedCommand(
            intent = CommandIntent.EXECUTE_REMOTE_DEVICE,
            target = targetDeviceName,
            payload = extractedSubCmd.ifBlank { cleaned },
            isRemote = true,
            remoteTargetDevice = targetDeviceName
        )
    }

    private fun checkConversationalPhrase(str: String): ParsedCommand? {
        val s = str.lowercase().trim()

        // 1. Greetings
        if (s == "namaste" || s == "hello" || s == "hi" || s == "helo" || s == "pranam" || s == "ram ram" ||
            s.startsWith("namaste") || s.startsWith("hello") || s.startsWith("hi ")
        ) {
            return ParsedCommand(
                intent = CommandIntent.CONVERSATIONAL_CHAT,
                payload = "नमस्ते! मैं MYRA हूँ। बताइए, आज मैं आपकी क्या मदद कर सकती हूँ?"
            )
        }

        // 2. How are you / Status
        if (s.contains("kaise ho") || s.contains("kaisi ho") || s.contains("kya haal") || s.contains("how are you") || s.contains("sab theek")) {
            return ParsedCommand(
                intent = CommandIntent.CONVERSATIONAL_CHAT,
                payload = "मैं बिल्कुल ठीक और तैयार हूँ! आप बताइए, आपका दिन कैसा चल रहा है?"
            )
        }

        // 3. Identity / Name / Who are you
        if (s.contains("kaun ho") || s.contains("who are you") || s.contains("naam kya") || s.contains("what is your name")) {
            return ParsedCommand(
                intent = CommandIntent.CONVERSATIONAL_CHAT,
                payload = "मैं MYRA हूँ — आपकी 2026 Edition पर्सनल AI वॉइस कम्पैनियन! मैं कॉल, वॉट्सऐप, म्यूज़िक, टॉर्च और स्मार्ट सवालों में आपकी मदद करती हूँ।"
            )
        }

        // 4. Capabilities / What can you do
        if (s.contains("kya kar sakti ho") || s.contains("kya karti ho") || s.contains("what can you do") || (s.contains("madad") && s.contains("kaise"))) {
            return ParsedCommand(
                intent = CommandIntent.CONVERSATIONAL_CHAT,
                payload = "मैं आपके लिए फोन कॉल लगा सकती हूँ, वॉट्सऐप मैसेज भेज सकती हूँ, टॉर्च और म्यूज़िक चला सकती हूँ, फोन लॉक कर सकती हूँ और आपके किसी भी सवाल का जवाब दे सकती हूँ!"
            )
        }

        // 5. Gratitude / Thanks
        if (s.contains("dhanyawad") || s.contains("shukriya") || s.contains("thank you") || s.contains("thanks")) {
            return ParsedCommand(
                intent = CommandIntent.CONVERSATIONAL_CHAT,
                payload = "आपका बहुत स्वागत है! हमेशा आपकी सेवा में उपस्थित हूँ।"
            )
        }

        // 6. Good bye / Good night
        if (s.contains("bye") || s.contains("alvida") || s.contains("good night") || s.contains("shubh ratri")) {
            return ParsedCommand(
                intent = CommandIntent.CONVERSATIONAL_CHAT,
                payload = "अलविडा! अपना ध्यान रखिएगा। जब भी ज़रूरत हो, बस 'Hey MYRA' कहिए।"
            )
        }

        return null
    }

    /**
     * Parses Alarm creation commands (e.g. "7 baje ka alarm lagao", "set alarm for 6 30", "wake me up at 8").
     */
    private fun checkAlarmCommand(s: String): ParsedCommand? {
        if (!s.contains("alarm") && !s.contains("अलार्म") && !s.contains("wake me up")) {
            return null
        }

        var hour = 7
        var minute = 0
        var label = "MYRA Alarm"

        // Search for time pattern e.g. "7:30", "7 30", "7 baje", "19:00"
        val timeRegex = Regex("""(\d{1,2})[:\s](\d{2})""")
        val matchTime = timeRegex.find(s)
        if (matchTime != null) {
            hour = matchTime.groupValues[1].toIntOrNull() ?: 7
            minute = matchTime.groupValues[2].toIntOrNull() ?: 0
        } else {
            val singleHourRegex = Regex("""(\d{1,2})\s*(baje|am|pm|o'clock|बजे)?""")
            val matchHour = singleHourRegex.find(s)
            if (matchHour != null) {
                hour = matchHour.groupValues[1].toIntOrNull() ?: 7
            }
        }

        if (s.contains("pm") && hour < 12) hour += 12
        if (s.contains("am") && hour == 12) hour = 0

        return ParsedCommand(
            intent = CommandIntent.SET_ALARM,
            target = "$hour:$minute",
            payload = label
        )
    }

    /**
     * Parses Countdown Timer commands (e.g. "5 minute ka timer lagao", "set timer for 10 minutes", "10 second timer").
     */
    private fun checkTimerCommand(s: String): ParsedCommand? {
        if (!s.contains("timer") && !s.contains("टाइमर")) return null

        var seconds = 300 // default 5 mins
        val numMatch = Regex("""(\d+)\s*(minute|min|minut|second|sec|मिनट|सेकंड)?""").find(s)
        if (numMatch != null) {
            val num = numMatch.groupValues[1].toIntOrNull() ?: 5
            val unit = numMatch.groupValues[2]
            seconds = if (unit.contains("sec") || unit.contains("सेकंड")) {
                num
            } else {
                num * 60
            }
        }

        return ParsedCommand(
            intent = CommandIntent.SET_TIMER,
            target = seconds.toString(),
            payload = "MYRA Timer"
        )
    }

    /**
     * Parses Calendar event creation (e.g. "calendar me meeting add karo", "schedule doctor appointment").
     */
    private fun checkCalendarCommand(s: String): ParsedCommand? {
        if (!s.contains("calendar") && !s.contains("कैलेंडर") && !s.contains("schedule") && !s.contains("meeting")) {
            return null
        }

        var title = "Meeting"
        val prefixes = listOf("meeting", "appointment", "event", "doctor", "interview")
        for (p in prefixes) {
            if (s.contains(p)) {
                title = p.replaceFirstChar { it.uppercase() }
                break
            }
        }

        return ParsedCommand(
            intent = CommandIntent.ADD_CALENDAR_EVENT,
            target = title,
            payload = s
        )
    }

    /**
     * Parses Wi-Fi and Bluetooth settings panels.
     */
    private fun checkWifiBluetoothCommand(s: String): ParsedCommand? {
        if (s.contains("wifi") || s.contains("wi-fi") || s.contains("वाईफाई")) {
            return ParsedCommand(intent = CommandIntent.OPEN_WIFI_SETTINGS, target = "wifi")
        }
        if (s.contains("bluetooth") || s.contains("ब्लूटूथ")) {
            return ParsedCommand(intent = CommandIntent.OPEN_BLUETOOTH_SETTINGS, target = "bluetooth")
        }
        return null
    }

    /**
     * Parses SMS composition & send requests.
     */
    private fun checkSmsCommand(s: String): ParsedCommand? {
        val isSms = s.contains("sms") || s.contains("एसएमएस") || 
                    (s.contains("message") && !s.contains("whatsapp") && (s.contains("bhejo") || s.contains("send") || s.contains("karo"))) ||
                    (s.contains("text") && !s.contains("whatsapp") && (s.contains("karo") || s.contains("bhejo")))
        if (!isSms) return null

        var target = ""
        var msg = ""

        val koSplit = s.split(Regex("""\s+(ko|to|par)\s+"""), limit = 2)
        if (koSplit.size == 2) {
            val candidate = koSplit[0].replace(Regex("""(?i)(sms|message|text|bhejo|send|karo)\s*"""), "").trim()
            val after = koSplit[1].trim()
            if (candidate.isNotBlank()) target = candidate

            val msgTokens = after.split(Regex("""\s+(likho|bolo|bhejo|send|saying|text)\s+"""), limit = 2)
            if (msgTokens.size == 2) {
                msg = msgTokens[1].trim()
            } else {
                msg = after
            }
        }

        return ParsedCommand(
            intent = CommandIntent.SEND_SMS,
            target = target,
            payload = msg
        )
    }

    /**
     * Parses Camera Vision inspection requests ("dekh kar batao", "look at this", "camera se dekho", "what is in front").
     */
    private fun checkVisionCommand(s: String): ParsedCommand? {
        if (s.contains("dekh kar batao") || s.contains("camera se dekho") || s.contains("look at this") ||
            s.contains("kya dikh raha hai") || s.contains("scan this") || s.contains("dekh ke batao") ||
            (s.contains("camera") && (s.contains("dekho") || s.contains("batao") || s.contains("look")))) {
            return ParsedCommand(
                intent = CommandIntent.CAMERA_VISION,
                payload = s
            )
        }
        return null
    }

    /**
     * Parses Screen Understanding requests ("screen par kya hai", "read screen", "screen padho", "summarize screen").
     */
    private fun checkScreenCommand(s: String): ParsedCommand? {
        if (s.contains("screen par kya") || s.contains("read screen") || s.contains("screen padho") ||
            s.contains("summarize screen") || s.contains("what is on my screen") || s.contains("screen samjhao")) {
            return ParsedCommand(
                intent = CommandIntent.SCREEN_UNDERSTANDING,
                payload = s
            )
        }
        return null
    }
}
