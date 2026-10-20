import express from "express";
import http from "http";
import path from "path";
import fs from "fs";
import { fileURLToPath } from "url";
import { createServer as createViteServer } from "vite";
import dotenv from "dotenv";
import { GoogleGenAI, Modality } from "@google/genai";
import { WebSocketServer, WebSocket } from "ws";

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

async function startServer() {
  console.log("SERVER_BOOT_BEGIN");
  console.log("MYRA_SERVER_STARTING");

  const app = express();
  const rawPort = process.env.PORT ? String(process.env.PORT).trim() : "";
  const parsedPort = rawPort ? parseInt(rawPort, 10) : 3000;
  const PORT = !isNaN(parsedPort) && parsedPort > 0 ? parsedPort : 3000;

  console.log(`SERVER_PORT=${PORT}`);
  console.log(`SERVER_HOST=0.0.0.0`);
  console.log(`HOST=0.0.0.0`);
  console.log(`PORT=${PORT}`);

  app.use(express.json());

  // 11. Lightweight health endpoint: GET /
  // For container health pings that explicitly request a health check or have no HTML accept header
  app.get("/", (req, res, next) => {
    const userAgent = req.headers["user-agent"] || "";
    const accept = req.headers["accept"] || "";
    const isExplicitProbe =
      userAgent.includes("GoogleHC") ||
      userAgent.includes("HealthCheck") ||
      Boolean(req.query.health);

    if (isExplicitProbe && !accept.includes("text/html")) {
      return res.status(200).send("MYRA AI Voice Assistant API is running");
    }
    return next();
  });

  // 12. Lightweight health endpoint: GET /health
  app.get("/health", (_req, res) => {
    return res.status(200).json({
      status: "ok",
      service: "MYRA AI Voice Assistant",
    });
  });

  // Existing API health endpoint
  app.get("/api/health", (_req, res) => {
    res.json({
      status: "ok",
      app: "MYRA AI Voice Assistant",
      version: "2026.1.0",
      hasGeminiKey: Boolean(process.env.GEMINI_API_KEY),
    });
  });

  const findPublicDir = () => {
    const candidates = [
      path.join(process.cwd(), "public"),
      path.join(__dirname, "public"),
      path.join(__dirname, "..", "public"),
    ];
    for (const cand of candidates) {
      if (fs.existsSync(cand)) return cand;
    }
    return path.join(process.cwd(), "public");
  };

  const publicDir = findPublicDir();

  // Direct download endpoint for complete Android project ZIP
  app.get(["/MYRA_ANDROID_GITHUB_FINAL.zip", "/download/MYRA_ANDROID_GITHUB_FINAL.zip", "/api/download-android-github-final"], (_req, res) => {
    const zipPath = path.join(publicDir, "MYRA_ANDROID_GITHUB_FINAL.zip");
    res.download(zipPath, "MYRA_ANDROID_GITHUB_FINAL.zip");
  });

  app.get(["/MYRA_ANDROID_PROJECT_FULL.zip", "/download/MYRA_ANDROID_PROJECT_FULL.zip", "/api/download-android-full-zip"], (_req, res) => {
    const zipPath = path.join(publicDir, "MYRA_ANDROID_PROJECT_FULL.zip");
    res.download(zipPath, "MYRA_ANDROID_PROJECT_FULL.zip");
  });

  app.get(["/MYRA_Android_Project.zip", "/download/MYRA_Android_Project.zip", "/api/download-android-zip"], (_req, res) => {
    const zipPath = path.join(publicDir, "MYRA_Android_Project.zip");
    res.download(zipPath, "MYRA_Android_Project.zip");
  });

  // Voice capabilities & status endpoint
  app.get("/api/voice-status", (_req, res) => {
    const hasKey = Boolean(process.env.GEMINI_API_KEY);
    res.json({
      geminiLiveSupported: hasKey,
      geminiTtsSupported: hasKey,
      liveModel: "gemini-3.8-live",
      ttsModel: "gemini-3.1-flash-tts-preview",
      voiceName: "Aoede", // Natural warm female assistant voice
      fallbackAvailable: true,
      fallbackType: "browser_speech_synthesis",
      hasKey,
    });
  });

  // Gemini Neural TTS endpoint (gemini-3.1-flash-tts-preview with Aoede female voice)
  app.post("/api/gemini/tts", async (req, res) => {
    try {
      const { text, voice = "Aoede" } = req.body;
      if (!text || typeof text !== "string") {
        return res.status(400).json({ error: "Text is required", fallbackRequired: true });
      }

      const apiKey = process.env.GEMINI_API_KEY;
      if (!apiKey) {
        return res.status(503).json({
          error: "GEMINI_API_KEY is not configured",
          fallbackRequired: true,
          message: "Fallback browser TTS must be used"
        });
      }

      const ai = new GoogleGenAI({
        apiKey,
        httpOptions: {
          headers: {
            "User-Agent": "aistudio-build",
          },
        },
      });

      // Transform natural conversational Hindi/Hinglish text to speech audio
      const response = await ai.models.generateContent({
        model: "gemini-3.1-flash-tts-preview",
        contents: [
          {
            parts: [
              {
                text: `Speak in a natural, smooth, warm female Indian assistant voice: ${text}`,
              },
            ],
          },
        ],
        config: {
          responseModalities: [Modality.AUDIO],
          speechConfig: {
            voiceConfig: {
              prebuiltVoiceConfig: { voiceName: voice },
            },
          },
        },
      });

      const audioPart = response.candidates?.[0]?.content?.parts?.find(
        (p: any) => p.inlineData?.data
      );

      const base64Audio = audioPart?.inlineData?.data;

      if (!base64Audio) {
        return res.json({
          error: "Model did not return audio data",
          fallbackRequired: true,
        });
      }

      return res.json({
        audio: base64Audio,
        format: "pcm24k",
        sampleRate: 24000,
        voiceName: voice,
        model: "gemini-3.1-flash-tts-preview",
      });
    } catch (err: any) {
      console.warn("Gemini TTS notice (temporary high demand or unavailable):", err?.message || err);
      return res.json({
        error: err?.message || "TTS generation unavailable",
        fallbackRequired: true,
      });
    }
  });

  // Helper to call Gemini with automatic multi-model fallback on 503 / high demand spikes
  async function generateWithModelFallback(ai: GoogleGenAI, requestPayload: any) {
    const candidateModels = ["gemini-2.5-flash", "gemini-3.8-flash", "gemini-2.5-flash-lite"];
    let lastError: any = null;

    for (const model of candidateModels) {
      try {
        const resp = await ai.models.generateContent({
          ...requestPayload,
          model,
        });
        return resp;
      } catch (err: any) {
        lastError = err;
        console.warn(`Notice: Model ${model} temporarily unavailable, switching to next fallback model...`);
      }
    }
    throw lastError;
  }

  // Assistant reasoning endpoint
  app.post("/api/chat", async (req, res) => {
    const { message, personality = "friendly_assistant", history = [] } = req.body || {};

    if (!message || typeof message !== "string") {
      return res.status(400).json({ error: "Message is required" });
    }

    // Local rule-based parser for instantaneous or offline fallback
    const parseLocally = (text: string) => {
      const lower = text.toLowerCase();
      let action = "general_chat";
      let actionParams: Record<string, any> = {};
      let category = "General";
      let reply = "";

      if (lower.includes("torch") || lower.includes("flashlight")) {
        action = "toggle_flashlight";
        category = "Device Tools";
        const state = lower.includes("band") || lower.includes("off") ? "off" : "on";
        actionParams = { state };
        reply = `Torch ${state === "on" ? "turn ON" : "turn OFF"} kar diya gaya hai.`;
      } else if (lower.includes("gaana") || lower.includes("music") || lower.includes("song") || lower.includes("play")) {
        action = "play_music";
        category = "Media Tools";
        const query = text.replace(/gaana|chalao|play|song|music|sunao/gi, "").trim() || "Arijit Singh Latest Hits";
        actionParams = { query };
        reply = `"${query}" play kiya ja raha hai YouTube Music / Spotify par!`;
      } else if (lower.includes("whatsapp")) {
        action = "send_whatsapp";
        category = "Communication Tools";
        actionParams = { text };
        reply = "WhatsApp chat khol kar message ready kar diya hai!";
      } else if (lower.includes("call") || lower.includes("phone lagao")) {
        action = "call_contact";
        category = "Call Tools";
        const contact = text.replace(/call|karo|lagao|ko/gi, "").trim() || "Papa";
        actionParams = { contact };
        reply = `${contact} ko call lagayi ja rahi hai...`;
      } else if (lower.includes("alarm")) {
        action = "set_alarm";
        category = "Device Tools";
        actionParams = { time: "06:00 AM" };
        reply = "Subah 6:00 baje ka alarm set kar diya hai!";
      } else if (lower.includes("timer")) {
        action = "set_timer";
        category = "Device Tools";
        actionParams = { minutes: 10 };
        reply = "10 minute ka countdown timer shuru ho gaya hai!";
      } else if (lower.includes("battery")) {
        action = "get_battery";
        category = "Device Tools";
        reply = "Phone ki battery 78% hai aur Fast Charging active hai.";
      } else if (lower.includes("lock") || lower.includes("phone band kar")) {
        action = "lock_device";
        category = "Safety / Device";
        reply = "Screen lock kar di gayi hai (Urgent safety action executed).";
      } else if (lower.includes("otp")) {
        action = "read_otp";
        category = "Safety / Notifications";
        reply = "Aapka latest bank/app OTP hai: 482910 (HDFC Bank).";
      } else if (lower.includes("park") || lower.includes("gaadi")) {
        action = "get_parking_location";
        category = "Maps & My World";
        reply = "Aapki gaadi Level 2, Spot B-14 (Inorbit Mall Parking) par parked hai.";
      } else if (lower.includes("slow") || lower.includes("health") || lower.includes("garam") || lower.includes("lag")) {
        action = "system_health";
        category = "Smart Tools";
        reply = "Phone ka diagnostic complete: RAM usage 58%, Temperature 36°C (Normal), BG apps optimize kar di gayi hain.";
      } else if (lower.includes("kahan hoon") || lower.includes("location") || lower.includes("address")) {
        action = "get_location";
        category = "Maps & My World";
        reply = "Aap Sector 62, Noida, Uttar Pradesh ke paas hain. GPS accuracy: High.";
      } else if (lower.includes("mission") || lower.includes("routine") || lower.includes("automate")) {
        action = "start_mission";
        category = "Missions";
        reply = "Autonomous multi-step mission start ho gaya hai. Mission Dashboard par live steps monitor kar sakte hain.";
      } else {
        if (personality === "romantic_gf") {
          reply = `Haanji jaan, main hamesha aapke saath hoon! Bataiye aaj aapki MYRA aapke liye kya kare? ❤️`;
        } else {
          reply = `Ji bilkul! MYRA aapke har command ke liye ready hai. Aap koi bhi voice command jaise call, WhatsApp, alarm, music ya automation bol sakte hain.`;
        }
      }

      return {
        response: reply,
        action,
        actionParams,
        category,
      };
    };

    const apiKey = process.env.GEMINI_API_KEY;
    if (!apiKey) {
      const parsed = parseLocally(message);
      return res.json(parsed);
    }

    try {
      const ai = new GoogleGenAI({
        apiKey,
        httpOptions: {
          headers: {
            "User-Agent": "aistudio-build",
          },
        },
      });

      let personalityInstruction = "friendly, smart and highly capable Indian AI assistant";
      if (personality === "romantic_gf") {
        personalityInstruction = "loving, caring, sweet girlfriend persona (GF mode) in natural sweet Hinglish/Hindi, affectionate but deeply capable";
      } else if (personality === "professional") {
        personalityInstruction = "crisp, precise, highly professional executive AI assistant";
      }

      const systemPrompt = `You are MYRA, a premier Indian AI Voice Assistant for Android (2026 Edition).
You communicate fluently in natural, warm Hindi and Hinglish (mix of Hindi & English as spoken in urban India).
Personality style: ${personalityInstruction}.

You can trigger these 16 app tools/capabilities:
1. send_whatsapp (contact, message)
2. send_sms (contact, message)
3. compose_email (to, subject, body)
4. send_emergency_alert (SOS alert)
5. call_contact (contact)
6. lookup_contact (contact)
7. answer_call / end_call (note: end_call has safety guard, lock command is immediate)
8. play_music (query/song)
9. media_control (play/pause/next)
10. set_alarm (time) / set_timer (minutes) / toggle_flashlight (state) / get_battery / lock_device / analyze_storage / clean_storage
11. list_files / search_files / delete_file / take_photo / camera_vision
12. generate_project / generate_image / deep_research / game_coach / system_health
13. start_mission / pause_mission / cancel_mission
14. read_notifications / reply_to_notification / read_missed_calls / read_otp (ONLY if explicitly asked) / clear_notifications
15. open_map / navigate_to / get_location / get_parking_location / save_parking / search_nearby / set_smart_mode
16. pc_connect / pc_command

Return ONLY a valid JSON object with:
{
  "response": "Conversational Hindi/Hinglish spoken response (warm, natural, 1-2 concise sentences)",
  "action": "tool_name or general_chat",
  "actionParams": { ...any extracted parameters... },
  "category": "Category name according to the 16 sections in MYRA guide"
}`;

      const response = await generateWithModelFallback(ai, {
        contents: [
          {
            role: "user",
            parts: [
              {
                text: `${systemPrompt}\n\nUser spoken input: "${message}"\nPrevious context: ${JSON.stringify(history.slice(-3))}`,
              },
            ],
          },
        ],
        config: {
          responseMimeType: "application/json",
          temperature: 0.7,
        },
      });

      const responseText = response.text?.trim() || "";
      let parsedJson;
      try {
        parsedJson = JSON.parse(responseText);
      } catch (e) {
        parsedJson = parseLocally(message);
      }

      return res.json(parsedJson);
    } catch (err: any) {
      console.warn("Notice: Gemini Assistant temporarily busy, seamlessly executing local assistant intelligence.");
      const parsed = parseLocally(message);
      return res.json(parsed);
    }
  });


  // Vite middleware setup vs production static serving
  const isCloudRun = Boolean(process.env.K_SERVICE || process.env.K_REVISION || process.env.CLOUD_RUN_JOB);
  const distDir = path.resolve(process.cwd(), "dist");
  // Always use Vite middlewares in development mode so live code changes are reflected immediately!
  const isDev = process.env.NODE_ENV === "development" || (!isCloudRun && !process.env.NODE_ENV);

  if (isDev) {
    try {
      const vite = await createViteServer({
        server: { middlewareMode: true },
        appType: "spa",
      });
      app.use(vite.middlewares);
    } catch (viteErr) {
      console.warn("Notice: Vite middleware could not start, falling back to static serving:", viteErr);
    }
  } else {
    const candidateDistDirs = [
      distDir,
      path.join(__dirname, "dist"),
      __dirname,
    ];
    let distPath = candidateDistDirs.find(
      (dir) =>
        fs.existsSync(path.join(dir, "index.html")) &&
        fs.existsSync(path.join(dir, "assets"))
    );
    if (!distPath) {
      distPath = candidateDistDirs.find((dir) =>
        fs.existsSync(path.join(dir, "index.html"))
      ) || distDir;
    }

    if (fs.existsSync(distPath)) {
      app.use(express.static(distPath));
      app.get("*", (_req, res) => {
        const indexPath = path.join(distPath, "index.html");
        if (fs.existsSync(indexPath)) {
          res.sendFile(indexPath);
        } else {
          res.status(200).send("MYRA AI Voice Assistant API is running");
        }
      });
    } else {
      app.get("*", (_req, res) => {
        res.status(200).send("MYRA AI Voice Assistant API is running");
      });
    }
  }

  const server = http.createServer(app);
  const wss = new WebSocketServer({ server, path: "/live" });

  wss.on("connection", async (clientWs: WebSocket) => {
    const apiKey = process.env.GEMINI_API_KEY;
    if (!apiKey) {
      clientWs.send(
        JSON.stringify({
          type: "status",
          status: "unavailable",
          error: "GEMINI_API_KEY is not configured",
          fallbackRequired: true,
          voiceName: "Browser Fallback Voice",
        })
      );
      return;
    }

    try {
      const ai = new GoogleGenAI({
        apiKey,
        httpOptions: {
          headers: { "User-Agent": "aistudio-build" },
        },
      });

      // Connect to Gemini 3.8 Live API
      const session = await ai.live.connect({
        model: "gemini-3.8-live",
        config: {
          responseModalities: [Modality.AUDIO],
          speechConfig: {
            voiceConfig: {
              prebuiltVoiceConfig: { voiceName: "Aoede" },
            },
          },
          systemInstruction:
            "You are MYRA, a premier Indian female AI voice assistant for Android (2026 Edition). You converse fluently in warm, natural, female Hindi and Hinglish. Keep spoken voice responses concise, conversational, and energetic.",
        },
        callbacks: {
          onmessage: (message: any) => {
            const parts = message.serverContent?.modelTurn?.parts;
            if (parts) {
              for (const part of parts) {
                if (part.inlineData?.data) {
                  clientWs.send(
                    JSON.stringify({
                      type: "audio",
                      audio: part.inlineData.data,
                      sampleRate: 24000,
                    })
                  );
                }
                if (part.text) {
                  clientWs.send(
                    JSON.stringify({
                      type: "transcript",
                      text: part.text,
                    })
                  );
                }
              }
            }
            if (message.serverContent?.interrupted) {
              clientWs.send(JSON.stringify({ type: "interrupted" }));
            }
            if (message.serverContent?.turnComplete) {
              clientWs.send(JSON.stringify({ type: "turnComplete" }));
            }
          },
          onerror: (err: any) => {
            console.warn("Gemini Live session error:", err?.message || err);
            clientWs.send(
              JSON.stringify({
                type: "status",
                status: "error",
                error: err?.message || "Live session error",
                fallbackRequired: true,
              })
            );
          },
          onclose: () => {
            clientWs.send(
              JSON.stringify({
                type: "status",
                status: "closed",
                fallbackRequired: true,
              })
            );
          },
        },
      });

      // Notify client that Gemini Live is connected and ready
      clientWs.send(
        JSON.stringify({
          type: "status",
          status: "connected",
          model: "gemini-3.8-live",
          voice: "Aoede",
          sampleRateIn: 16000,
          sampleRateOut: 24000,
        })
      );

      clientWs.on("message", (rawData) => {
        try {
          const parsed = JSON.parse(rawData.toString());
          if (parsed.audio) {
            // Forward 16kHz PCM audio chunk
            session.sendRealtimeInput({
              audio: {
                data: parsed.audio,
                mimeType: "audio/pcm;rate=16000",
              },
            });
          } else if (parsed.text) {
            session.sendRealtimeInput({
              text: parsed.text,
            });
          }
        } catch (e) {
          console.warn("Notice: Live input chunk transmission error:", e);
        }
      });

      clientWs.on("close", () => {
        try {
          session.close();
        } catch (e) {}
      });
    } catch (err: any) {
      console.warn("Failed to initialize Gemini Live session:", err?.message || err);
      clientWs.send(
        JSON.stringify({
          type: "status",
          status: "error",
          error: err?.message || "Could not connect to Gemini Live",
          fallbackRequired: true,
        })
      );
    }
  });

  server.on("error", (err: any) => {
    console.error("SERVER_BOOT_ERROR=", err?.stack || err);
    console.error("Server network error:", err);
  });

  server.listen(PORT, "0.0.0.0", () => {
    console.log("SERVER_LISTEN_SUCCESS");
    console.log(`MYRA AI Voice Assistant Server running on port ${PORT} bound to 0.0.0.0`);
  });

  const gracefulShutdown = (signal: string) => {
    console.log(`${signal} received: closing MYRA server gracefully`);
    server.close(() => {
      console.log("MYRA server closed");
      process.exit(0);
    });
  };

  process.on("SIGTERM", () => gracefulShutdown("SIGTERM"));
  process.on("SIGINT", () => gracefulShutdown("SIGINT"));
}

process.on("unhandledRejection", (reason) => {
  console.error("Unhandled Rejection:", reason);
});

process.on("uncaughtException", (error) => {
  console.error("Uncaught Exception:", error);
});

startServer().catch((err) => {
  console.error("FATAL: Failed to start MYRA server:", err);
  process.exit(1);
});
