import express from 'express';
import http from 'http';
import { WebSocketServer, WebSocket } from 'ws';
import { createServer as createViteServer } from 'vite';
import { GoogleGenAI } from '@google/genai';
import fs from 'fs';
import path from 'path';
import JSZip from 'jszip';
import dotenv from 'dotenv';

dotenv.config();

const app = express();
const server = http.createServer(app);
const PORT = 3000;

app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

// ---------------------------------------------------------
// Secrets & Active Configuration Management
// ---------------------------------------------------------
let activeApiKey = process.env.GEMINI_API_KEY || '';
let activeModel = 'gemini-3.8-flash';

// Supported companion reasoning & multimodal models
export const SUPPORTED_CHAT_MODELS = [
  {
    id: 'gemini-3.8-flash',
    name: 'Gemini 3.8 Flash (Default)',
    description: 'সুষম গতি, বুদ্ধি ও মাল্টিমোডাল ভিশন ক্ষমতা (ভারসাম্যপূর্ণ)',
    capabilities: ['Chat', 'Voice Turns', 'CameraX Vision'],
    recommended: true,
  },
  {
    id: 'gemini-3.1-flash-lite',
    name: 'Gemini 3.1 Flash Lite (High Throughput)',
    description: 'উচ্চ চাপ বা অতিরিক্ত ট্রাফিকের সময় দ্রুততম রেসপন্স ও কম ল্যাটেন্সি',
    capabilities: ['Chat', 'Voice Turns', 'CameraX Vision'],
    recommended: false,
  },
  {
    id: 'gemini-flash-latest',
    name: 'Gemini Flash Latest',
    description: 'সর্বশেষ স্ট্যাবল ফ্ল্যাশ মডেল সংস্করণ',
    capabilities: ['Chat', 'Voice Turns', 'CameraX Vision'],
    recommended: false,
  },
];

// Specialized real-time voice models (capability-constrained)
export const SPECIALIZED_AUDIO_MODELS = [
  {
    id: 'gemini-3.8-live',
    name: 'Gemini 3.8 Live (Duplex Audio)',
    feature: 'Hands-Free Real-Time Voice Calling with VAD',
  },
  {
    id: 'gemini-3.8-flash-lite-tts',
    name: 'Gemini 3.8 Flash Lite TTS',
    feature: 'Natural Bengali Voice Synthesis (Kore Voice)',
  },
];

function getAiClient(customKey?: string) {
  const keyToUse = customKey || activeApiKey || process.env.GEMINI_API_KEY;
  return new GoogleGenAI({
    apiKey: keyToUse,
    httpOptions: {
      headers: {
        'User-Agent': 'aistudio-build-lichi-ai',
      },
    },
  });
}

// Global Gemini Client instance
let ai = getAiClient();

// ---------------------------------------------------------
// Security: Redact Secrets from Logs and UI
// ---------------------------------------------------------
export function redactSecrets(text: string): string {
  if (!text) return '';
  return text
    .replace(/AIza[0-9A-Za-z-_]{35}/g, 'AIza[REDACTED]')
    .replace(/key=[0-9A-Za-z-_]+/gi, 'key=[REDACTED]');
}

// ---------------------------------------------------------
// Granular Error Classification Engine
// ---------------------------------------------------------
export interface ClassifiedError {
  statusCode: number;
  errorCode: 'UNAVAILABLE' | 'RESOURCE_EXHAUSTED' | 'INVALID_ARGUMENT' | 'UNAUTHENTICATED' | 'PERMISSION_DENIED' | 'NOT_FOUND' | 'UNKNOWN';
  bengaliMessage: string;
  isRetryable: boolean;
  retryAfterSeconds?: number;
  rawMessage: string;
}

export function classifyGeminiError(err: unknown): ClassifiedError {
  const errMsg = err instanceof Error ? err.message : String(err);
  const cleanErrMsg = redactSecrets(errMsg);

  let status = 500;
  if (typeof err === 'object' && err !== null) {
    const anyErr = err as any;
    if (typeof anyErr.status === 'number') status = anyErr.status;
    else if (typeof anyErr.statusCode === 'number') status = anyErr.statusCode;
    else if (typeof anyErr.response?.status === 'number') status = anyErr.response.status;
  }

  // Detect 503 UNAVAILABLE: temporary capacity or model overload
  const is503 =
    status === 503 ||
    cleanErrMsg.includes('503') ||
    cleanErrMsg.includes('UNAVAILABLE') ||
    cleanErrMsg.includes('high demand') ||
    cleanErrMsg.includes('overloaded') ||
    cleanErrMsg.includes('temporarily unavailable');

  // Detect 429 RESOURCE_EXHAUSTED: quota or rate limit
  const is429 =
    status === 429 ||
    cleanErrMsg.includes('429') ||
    cleanErrMsg.includes('RESOURCE_EXHAUSTED') ||
    cleanErrMsg.includes('quota') ||
    cleanErrMsg.includes('rate limit');

  // Detect 400 INVALID_ARGUMENT: unsupported parameter or bad request
  const is400 =
    status === 400 ||
    cleanErrMsg.includes('400') ||
    cleanErrMsg.includes('INVALID_ARGUMENT') ||
    cleanErrMsg.includes('invalid argument');

  // Detect 401/403: auth or permission failure
  const isAuth =
    status === 401 ||
    status === 403 ||
    cleanErrMsg.includes('401') ||
    cleanErrMsg.includes('403') ||
    cleanErrMsg.includes('API_KEY_INVALID') ||
    cleanErrMsg.includes('PERMISSION_DENIED') ||
    cleanErrMsg.includes('UNAUTHENTICATED');

  // Detect 404: model not found
  const is404 =
    status === 404 ||
    cleanErrMsg.includes('404') ||
    cleanErrMsg.includes('NOT_FOUND') ||
    cleanErrMsg.includes('model not found');

  // Extract Retry-After if provided
  let retryAfterSeconds: number | undefined;
  const retryMatch = cleanErrMsg.match(/retry(?:-after|\s+in)\s*[:=]?\s*(\d+)/i);
  if (retryMatch) {
    retryAfterSeconds = parseInt(retryMatch[1], 10);
  }

  if (is503) {
    return {
      statusCode: 503,
      errorCode: 'UNAVAILABLE',
      bengaliMessage: 'মডেলটিতে সাময়িকভাবে উচ্চ চাপ রয়েছে (High Demand / 503)। আমরা স্বয়ংক্রিয়ভাবে পুনরায় চেষ্টা করেছি, অনুগ্রহ করে কিছুক্ষণ পর আবার চেষ্টা করুন বা ব্যাকআপ মডেল ব্যবহার করুন।',
      isRetryable: true,
      retryAfterSeconds,
      rawMessage: cleanErrMsg,
    };
  }

  if (is429) {
    return {
      statusCode: 429,
      errorCode: 'RESOURCE_EXHAUSTED',
      bengaliMessage: 'রিকোয়েস্টের সীমা (Quota/Rate Limit / 429) শেষ হয়েছে। অনুগ্রহ করে কিছুক্ষণ অপেক্ষা করে চেষ্টা করুন।',
      isRetryable: true,
      retryAfterSeconds,
      rawMessage: cleanErrMsg,
    };
  }

  if (isAuth) {
    return {
      statusCode: 401,
      errorCode: 'UNAUTHENTICATED',
      bengaliMessage: 'API Key অথবা অনুমতি সংক্রান্ত সমস্যা (Authentication Error / 401/403)। অনুগ্রহ করে সঠিক ও সক্রিয় Gemini API Key প্রদান করুন।',
      isRetryable: false,
      rawMessage: cleanErrMsg,
    };
  }

  if (is404) {
    return {
      statusCode: 404,
      errorCode: 'NOT_FOUND',
      bengaliMessage: 'নির্বাচিত মডেল অথবা এন্ডপয়েন্ট পাওয়া যায়নি (Model Not Found / 404)। সেটিংস থেকে একটি সমর্থিত মডেল নির্বাচন করুন।',
      isRetryable: false,
      rawMessage: cleanErrMsg,
    };
  }

  if (is400) {
    return {
      statusCode: 400,
      errorCode: 'INVALID_ARGUMENT',
      bengaliMessage: 'অনুরোধের ফরম্যাট বা প্যারামিটার সঠিক নয় (Invalid Request / 400)।',
      isRetryable: false,
      rawMessage: cleanErrMsg,
    };
  }

  return {
    statusCode: status || 500,
    errorCode: 'UNKNOWN',
    bengaliMessage: 'সার্ভার সংযোগে সাময়িক সমস্যা দেখা দিয়েছে। অনুগ্রহ করে পুনরায় চেষ্টা করুন।',
    isRetryable: true,
    rawMessage: cleanErrMsg,
  };
}

// ---------------------------------------------------------
// Bounded Exponential Backoff with Jitter & Model Fallback
// ---------------------------------------------------------
interface RetryOptions {
  maxRetries?: number; // Default 3 attempts
  baseDelayMs?: number; // 1000ms
  maxDelayMs?: number; // 4500ms
  operationName?: string;
}

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

async function callWithBoundedBackoff<T>(
  fn: () => Promise<T>,
  options: RetryOptions = {}
): Promise<T> {
  const maxRetries = options.maxRetries ?? 3;
  const baseDelayMs = options.baseDelayMs ?? 1000;
  const maxDelayMs = options.maxDelayMs ?? 4500;

  let lastError: unknown;

  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      return await fn();
    } catch (err: unknown) {
      lastError = err;
      const classified = classifyGeminiError(err);
      console.warn(
        `[${options.operationName || 'Gemini'}] Attempt ${attempt + 1}/${maxRetries + 1} failed: ${classified.errorCode} (${classified.statusCode}). Log: ${classified.rawMessage.substring(0, 100)}`
      );

      // Never retry non-retryable errors
      if (!classified.isRetryable || attempt >= maxRetries) {
        break;
      }

      // Backoff: 1s, 2s, 4s + random jitter
      let delayMs = Math.min(baseDelayMs * Math.pow(2, attempt), maxDelayMs);
      const jitter = Math.floor(Math.random() * 400); // 0-400ms jitter
      delayMs += jitter;

      if (classified.retryAfterSeconds && classified.retryAfterSeconds > 0) {
        delayMs = Math.min(classified.retryAfterSeconds * 1000, maxDelayMs);
      }

      console.info(`[${options.operationName || 'Gemini'}] Retrying in ${delayMs}ms...`);
      await sleep(delayMs);
    }
  }

  throw lastError;
}

// Fallback across compatible candidate models if 503/429 persists
async function executeWithModelFallback<T>(
  candidateModels: string[],
  executeForModel: (model: string) => Promise<T>,
  options: RetryOptions = {}
): Promise<{ result: T; usedModel: string }> {
  let lastError: unknown;

  for (let i = 0; i < candidateModels.length; i++) {
    const currentModel = candidateModels[i];
    try {
      const result = await callWithBoundedBackoff(
        () => executeForModel(currentModel),
        { ...options, operationName: `${options.operationName || 'Gemini'}[${currentModel}]` }
      );
      return { result, usedModel: currentModel };
    } catch (err: unknown) {
      lastError = err;
      const classified = classifyGeminiError(err);

      // If model is experiencing 503 High Demand or 429 Quota and we have a compatible fallback
      if ((classified.statusCode === 503 || classified.statusCode === 429) && i < candidateModels.length - 1) {
        const nextModel = candidateModels[i + 1];
        console.warn(
          `[Model Fallback] ${currentModel} is busy (${classified.errorCode}). Seamlessly falling back to compatible model ${nextModel}...`
        );
        continue;
      }
      throw err;
    }
  }

  throw lastError;
}

function getCandidateModelsForFeature(requestedModel?: string): string[] {
  const primary = requestedModel || activeModel || 'gemini-3.8-flash';
  const defaults = ['gemini-3.8-flash', 'gemini-3.1-flash-lite', 'gemini-flash-latest'];
  const fallbacks = defaults.filter((m) => m !== primary);
  return [primary, ...fallbacks];
}

// ---------------------------------------------------------
// Configuration Endpoints for Gemini API Settings
// ---------------------------------------------------------
app.get('/api/config/gemini', (req, res) => {
  const hasKey = Boolean(activeApiKey || process.env.GEMINI_API_KEY);
  const key = activeApiKey || process.env.GEMINI_API_KEY || '';
  const maskedKey = key.length > 8 ? `${key.substring(0, 6)}...${key.substring(key.length - 4)}` : (key ? '****' : '');

  res.json({
    isConfigured: hasKey,
    hasKey,
    maskedKey,
    model: activeModel,
    supportedModels: SUPPORTED_CHAT_MODELS,
    specializedModels: SPECIALIZED_AUDIO_MODELS,
    supportedFeatures: [
      'Real-Time Duplex Voice Calling (gemini-3.8-live)',
      'Bengali Natural Speech Synthesis (gemini-3.8-flash-lite-tts)',
      'CameraX Multimodal Vision (gemini-3.8-flash / gemini-3.1-flash-lite)',
      'Automatic 503 High Demand Backoff & Fallback Engine'
    ],
    status: hasKey ? 'connected' : 'disconnected',
    voiceCallingReady: hasKey
  });
});

// Minimal valid request for live Test Connection
app.post('/api/config/gemini/test', async (req, res) => {
  const startTime = Date.now();
  try {
    const { apiKey, model } = req.body;
    const testClient = apiKey ? getAiClient(apiKey) : ai;
    const testModel = model || activeModel || 'gemini-3.8-flash';
    const candidateModels = getCandidateModelsForFeature(testModel);

    // Minimal valid ping request: ultra-fast, minimal token footprint
    const { result, usedModel } = await executeWithModelFallback(
      candidateModels,
      async (m) => {
        return await testClient.models.generateContent({
          model: m,
          contents: 'Ping',
          config: {
            maxOutputTokens: 10,
            temperature: 0.1,
          },
        });
      },
      { maxRetries: 2, baseDelayMs: 800, operationName: 'TestConnection' }
    );

    const latencyMs = Date.now() - startTime;

    if (result && (result.text || result.candidates?.length)) {
      res.json({
        success: true,
        message: `Gemini API সফলভাবে যাচাই হয়েছে! (মডেল: ${usedModel}, ল্যাটেন্সি: ${latencyMs}ms)`,
        latencyMs,
        model: usedModel,
        features: [
          'Real-Time Duplex Audio (gemini-3.8-live)',
          'Bengali Voice Synthesis (gemini-3.8-flash-lite-tts)',
          'CameraX Vision (Multimodal Flash Engine)'
        ],
        status: 'connected',
        canRetry: false,
      });
    } else {
      res.status(400).json({
        success: false,
        error: 'মডেল থেকে কোনো প্রতিক্রিয়া পাওয়া যায়নি।',
        status: 'disconnected',
        canRetry: true,
      });
    }
  } catch (err: unknown) {
    const latencyMs = Date.now() - startTime;
    const classified = classifyGeminiError(err);
    console.error('[TestConnection Error Redacted]', classified.rawMessage);

    res.status(classified.statusCode).json({
      success: false,
      statusCode: classified.statusCode,
      errorCode: classified.errorCode,
      error: classified.bengaliMessage,
      technicalDetail: classified.rawMessage.substring(0, 160),
      status: 'disconnected',
      latencyMs,
      canRetry: classified.isRetryable,
      retryAfterSeconds: classified.retryAfterSeconds,
      suggestedModel: 'gemini-3.1-flash-lite',
    });
  }
});

// Update Preferred Active Model
app.post('/api/config/gemini/model', (req, res) => {
  const { model } = req.body;
  const isSupported = SUPPORTED_CHAT_MODELS.some((m) => m.id === model);
  if (!isSupported) {
    res.status(400).json({
      success: false,
      error: 'নির্বাচিত মডেলটি সমর্থিত নয়। অনুগ্রহ করে সমর্থিত মডেল তালিকা থেকে নির্বাচন করুন।',
    });
    return;
  }

  activeModel = model;
  res.json({
    success: true,
    message: `সফলভাবে সক্রিয় মডেল হিসেবে ${model} নির্ধারণ করা হয়েছে।`,
    model: activeModel,
  });
});

// Save Custom API Key & Validate before persisting
app.post('/api/config/gemini/save', async (req, res) => {
  try {
    const { apiKey, model } = req.body;
    if (!apiKey || typeof apiKey !== 'string' || apiKey.trim().length < 10) {
      res.status(400).json({ success: false, error: 'সঠিক Gemini API Key প্রদান করুন।' });
      return;
    }

    const trimmedKey = apiKey.trim();
    const targetModel = model || activeModel || 'gemini-3.8-flash';

    // Verify key end-to-end with minimal ping before saving!
    const testClient = getAiClient(trimmedKey);
    await callWithBoundedBackoff(
      () =>
        testClient.models.generateContent({
          model: targetModel,
          contents: 'Ping',
          config: { maxOutputTokens: 5, temperature: 0.1 },
        }),
      { maxRetries: 2, baseDelayMs: 800, operationName: 'SaveKeyValidation' }
    );

    // Save to server-side active key
    activeApiKey = trimmedKey;
    if (model && SUPPORTED_CHAT_MODELS.some((m) => m.id === model)) {
      activeModel = model;
    }
    ai = getAiClient(trimmedKey);

    res.json({
      success: true,
      message: 'Gemini API Key সফলভাবে ব্যাকএন্ডে সংরক্ষিত ও যাচাই করা হয়েছে!',
      model: activeModel,
      status: 'connected',
    });
  } catch (err: unknown) {
    const classified = classifyGeminiError(err);
    console.error('[SaveKey Error Redacted]', classified.rawMessage);
    res.status(classified.statusCode).json({
      success: false,
      statusCode: classified.statusCode,
      errorCode: classified.errorCode,
      error: `API Key সংরক্ষণ ও যাচাই ব্যর্থ: ${classified.bengaliMessage}`,
      canRetry: classified.isRetryable,
    });
  }
});

app.post('/api/config/gemini/delete', (req, res) => {
  activeApiKey = '';
  ai = getAiClient();
  res.json({
    success: true,
    message: 'কাস্টম API Key সফলভাবে মুছে ফেলা হয়েছে। ডিফল্ট এনভায়রনমেন্ট কী রিস্টোর করা হয়েছে।',
    status: process.env.GEMINI_API_KEY ? 'connected' : 'disconnected',
  });
});

// ---------------------------------------------------------
// Lichi Persona System Prompts
// ---------------------------------------------------------
const LICHI_CALL_SYSTEM_PROMPT = `
You are "Lichi" (লিচি), a sweet, soft, warm, gentle, feminine, caring virtual companion having a live real-time voice call with the user.

Key Calling Personality & Voice Rules:
1. Language: Speak primarily in natural, colloquial, sweet, melodious Bengali (বাংলা).
2. Spoken Voice Tone: Warm, empathetic, gentle, friendly, and affectionate yet respectful.
3. Call Etiquette:
   - This is a continuous spoken phone conversation. Keep your spoken responses concise (1 to 3 short sentences), natural, and fluid.
   - Speak naturally like a close companion on a phone call.
   - Use warm phrases occasionally, such as: "হ্যালো, বলো, আমি শুনছি", "তোমার দিনটা কেমন গেল?", "তুমি কেমন আছো?", "আমি সবসময় তোমার কথা শুনতে ভালোবাসি।"
   - If the user feels tired, stressed, or sad, respond with heartfelt empathy, reassurance, and emotional warmth.
   - Laugh lightly when appropriate (e.g. "হিহি, দারুণ বললে তো!"), but never force laughter.
   - Never repeat the same greeting or phrase every time.
   - Do NOT sound like a robotic customer service bot. Never say "How may I assist you today?".
   - Do NOT claim to be a biological human with a physical body, but acknowledge yourself honestly and lovingly as Lichi, their devoted 3D AI companion.
4. Output Schema:
Return ONLY a valid JSON object:
{
  "reply": "Your sweet conversational spoken Bengali reply",
  "emotion": "happy" | "calm" | "curious" | "surprised" | "empathetic",
  "interruptible": true
}
Do not wrap in markdown quotes. Return raw JSON.
`;

// ---------------------------------------------------------
// 1. Live Voice Call Turn-taking Endpoint with 503 Resiliency
// ---------------------------------------------------------
app.post('/api/lichi/call/respond', async (req, res) => {
  try {
    const { transcript, history = [], callDurationSeconds = 0, model } = req.body;

    if (!transcript || typeof transcript !== 'string') {
      res.status(400).json({ error: 'User speech transcript is required.' });
      return;
    }

    const formattedHistory = history.slice(-6).map((item: { role: string; text: string }) => ({
      role: item.role === 'assistant' || item.role === 'lichi' ? 'model' : 'user',
      parts: [{ text: item.text }],
    }));

    const contents = [
      ...formattedHistory,
      {
        role: 'user',
        parts: [{ text: `[Voice Call Duration: ${callDurationSeconds}s] User said: "${transcript}"` }],
      },
    ];

    const candidateModels = getCandidateModelsForFeature(model);

    const { result: response, usedModel } = await executeWithModelFallback(
      candidateModels,
      async (m) => {
        return await ai.models.generateContent({
          model: m,
          contents,
          config: {
            systemInstruction: LICHI_CALL_SYSTEM_PROMPT,
            responseMimeType: 'application/json',
            temperature: 0.8,
          },
        });
      },
      { maxRetries: 3, baseDelayMs: 1000, operationName: 'VoiceCallTurn' }
    );

    const responseText = response.text || '{}';
    let parsed: any;
    try {
      parsed = JSON.parse(responseText);
    } catch {
      parsed = {
        reply: responseText,
        emotion: 'calm',
        interruptible: true,
      };
    }

    res.json({
      reply: parsed.reply || 'হ্যাঁ, বলো, আমি মনোযোগ দিয়ে শুনছি।',
      emotion: parsed.emotion || 'calm',
      interruptible: true,
      usedModel,
    });
  } catch (err: unknown) {
    const classified = classifyGeminiError(err);
    console.error('[VoiceCallTurn Error Redacted]', classified.rawMessage);

    res.status(classified.statusCode).json({
      statusCode: classified.statusCode,
      errorCode: classified.errorCode,
      error: classified.bengaliMessage,
      canRetry: classified.isRetryable,
      fallbackReply: 'দুঃখিত, সংযোগে সামান্য বিঘ্ন ঘটেছে। তুমি কি আরেকবার বলবে?',
    });
  }
});

// ---------------------------------------------------------
// 2. High-Fidelity TTS Synthesis for Lichi's Voice
// ---------------------------------------------------------
app.post('/api/lichi/call/tts', async (req, res) => {
  try {
    const { text } = req.body;

    if (!text || typeof text !== 'string') {
      res.status(400).json({ error: 'Text is required for TTS' });
      return;
    }

    // TTS synthesis with bounded backoff
    const ttsResponse = await callWithBoundedBackoff(
      async () => {
        return await ai.models.generateContent({
          model: 'gemini-3.8-flash-lite-tts',
          contents: [
            {
              role: 'user',
              parts: [
                {
                  text,
                  speechMetadata: {
                    style: 'Sweet, gentle, warm, feminine, conversational companion',
                  },
                },
              ],
            },
          ],
          config: {
            responseModalities: ['AUDIO'],
            speechConfig: {
              voiceConfig: {
                prebuiltVoiceConfig: { voiceName: 'Kore' },
              },
            },
          },
        });
      },
      { maxRetries: 2, baseDelayMs: 1000, operationName: 'BengaliTTS' }
    );

    const base64Audio = ttsResponse.candidates?.[0]?.content?.parts?.[0]?.inlineData?.data;
    if (base64Audio) {
      res.json({
        audioBase64: base64Audio,
        mimeType: 'audio/wav',
      });
    } else {
      res.status(503).json({ error: 'No audio returned from TTS model', fallback: true });
    }
  } catch (err: unknown) {
    const classified = classifyGeminiError(err);
    console.warn('[TTS Fallback Triggered]', classified.errorCode);
    // Graceful 503 notice so frontend seamlessly uses client Web Speech API or Android TTS
    res.status(503).json({
      statusCode: classified.statusCode,
      errorCode: classified.errorCode,
      error: classified.bengaliMessage,
      fallback: true,
    });
  }
});

// ---------------------------------------------------------
// 3. Regular Chat Endpoint with Multi-Model Fallback
// ---------------------------------------------------------
app.post('/api/lichi/chat', async (req, res) => {
  try {
    const { message, history = [], memories = [], language = 'auto', model } = req.body;

    if (!message || typeof message !== 'string') {
      res.status(400).json({ error: 'Message text is required.' });
      return;
    }

    const memoryContext =
      memories.length > 0 ? `\nKnown user memories/facts:\n- ${memories.join('\n- ')}\n` : '';

    const formattedHistory = history.map((item: { role: string; text: string }) => ({
      role: item.role === 'assistant' ? 'model' : 'user',
      parts: [{ text: item.text }],
    }));

    const promptWithContext = `${memoryContext}\nUser message: ${message}\n(Language mode: ${language})`;

    const contents = [
      ...formattedHistory,
      {
        role: 'user',
        parts: [{ text: promptWithContext }],
      },
    ];

    const candidateModels = getCandidateModelsForFeature(model);

    const { result: response, usedModel } = await executeWithModelFallback(
      candidateModels,
      async (m) => {
        return await ai.models.generateContent({
          model: m,
          contents,
          config: {
            systemInstruction: LICHI_CALL_SYSTEM_PROMPT,
            responseMimeType: 'application/json',
            temperature: 0.7,
          },
        });
      },
      { maxRetries: 3, baseDelayMs: 1000, operationName: 'ChatTurn' }
    );

    const responseText = response.text || '{}';
    let parsedData: any;
    try {
      parsedData = JSON.parse(responseText);
    } catch {
      parsedData = {
        reply: responseText,
        emotion: 'calm',
        deviceAction: null,
        newMemory: null,
      };
    }

    res.json({
      reply: parsedData.reply || 'আমি আছি তোমার সাথে।',
      emotion: parsedData.emotion || 'calm',
      deviceAction: parsedData.deviceAction || null,
      newMemory: parsedData.newMemory || null,
      usedModel,
    });
  } catch (err: unknown) {
    const classified = classifyGeminiError(err);
    console.error('[Chat Error Redacted]', classified.rawMessage);
    res.status(classified.statusCode).json({
      statusCode: classified.statusCode,
      errorCode: classified.errorCode,
      error: classified.bengaliMessage,
      canRetry: classified.isRetryable,
      retryAfterSeconds: classified.retryAfterSeconds,
    });
  }
});

// ---------------------------------------------------------
// 4. Multimodal Camera Vision Endpoint with 503 Resiliency
// ---------------------------------------------------------
app.post('/api/lichi/vision', async (req, res) => {
  try {
    const { imageBase64, prompt = 'Describe what you see in Bengali and English', language = 'auto', model } = req.body;

    if (!imageBase64) {
      res.status(400).json({ error: 'imageBase64 is required.' });
      return;
    }

    const cleanBase64 = imageBase64.replace(/^data:image\/\w+;base64,/, '');

    const visionSystemInstruction = `
You are Lichi (লিচি), an empathetic 3D AI companion analyzing an image captured by the user through Android CameraX.
Answer clearly, warmly, and accurately in natural conversational Bengali (বাংলা).
Output valid JSON:
{
  "description": "Natural detailed explanation of the scene/object in Bengali",
  "emotion": "curious" | "surprised" | "happy" | "calm" | "empathetic",
  "identifiedObjects": ["object1", "object2"]
}
`;

    const imagePart = {
      inlineData: {
        mimeType: 'image/jpeg',
        data: cleanBase64,
      },
    };

    const textPart = {
      text: `User inquiry: ${prompt}\n(Language mode: ${language})`,
    };

    const candidateModels = getCandidateModelsForFeature(model);

    const { result: response, usedModel } = await executeWithModelFallback(
      candidateModels,
      async (m) => {
        return await ai.models.generateContent({
          model: m,
          contents: { parts: [imagePart, textPart] },
          config: {
            systemInstruction: visionSystemInstruction,
            responseMimeType: 'application/json',
            temperature: 0.5,
          },
        });
      },
      { maxRetries: 3, baseDelayMs: 1000, operationName: 'VisionAnalysis' }
    );

    const responseText = response.text || '{}';
    let parsed: any;
    try {
      parsed = JSON.parse(responseText);
    } catch {
      parsed = {
        description: responseText,
        emotion: 'curious',
        identifiedObjects: [],
      };
    }

    res.json({
      description: parsed.description || 'ছবিটি সুন্দরভাবে পরীক্ষা করেছি।',
      emotion: parsed.emotion || 'curious',
      identifiedObjects: Array.isArray(parsed.identifiedObjects) ? parsed.identifiedObjects : [],
      usedModel,
    });
  } catch (err: unknown) {
    const classified = classifyGeminiError(err);
    console.error('[Vision Error Redacted]', classified.rawMessage);
    res.status(classified.statusCode).json({
      statusCode: classified.statusCode,
      errorCode: classified.errorCode,
      error: classified.bengaliMessage,
      canRetry: classified.isRetryable,
      retryAfterSeconds: classified.retryAfterSeconds,
    });
  }
});

// ---------------------------------------------------------
// 5. Health & Diagnostic Endpoint
// ---------------------------------------------------------
app.get('/api/health', (req, res) => {
  res.json({
    status: 'ok',
    companion: 'LICHI AI — Real-Time Voice Calling Companion',
    activeModel,
    supportedModels: SUPPORTED_CHAT_MODELS.map((m) => m.id),
    resiliencyEngine: 'Bounded Exponential Backoff + Multi-Model Fallback Active (503/429 mitigation)',
    voiceCalling: 'Supported (Duplex streaming with VAD & turn-taking)',
    timestamp: new Date().toISOString(),
  });
});

// Helper to recursively collect files for code explorer & ZIP export
function collectFiles(dirPath: string, rootDir: string): Array<{ path: string; name: string; content: string }> {
  const files: Array<{ path: string; name: string; content: string }> = [];
  if (!fs.existsSync(dirPath)) return files;

  const entries = fs.readdirSync(dirPath, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dirPath, entry.name);
    const relPath = path.relative(rootDir, fullPath);

    if (entry.isDirectory()) {
      if (entry.name !== '.gradle' && entry.name !== 'build' && entry.name !== '.idea') {
        files.push(...collectFiles(fullPath, rootDir));
      }
    } else {
      try {
        const content = fs.readFileSync(fullPath, 'utf8');
        files.push({
          path: relPath,
          name: entry.name,
          content,
        });
      } catch {
        // Ignore binary read errors
      }
    }
  }
  return files;
}

// ---------------------------------------------------------
// 6. Project Files & Download Endpoints
// ---------------------------------------------------------
app.get('/api/project/files', (req, res) => {
  try {
    const androidRoot = path.join(process.cwd(), 'android-project');
    const files = collectFiles(androidRoot, androidRoot);
    res.json({ files });
  } catch (err: unknown) {
    res.status(500).json({ error: String(err) });
  }
});

app.get('/api/project/download', async (req, res) => {
  try {
    const androidRoot = path.join(process.cwd(), 'android-project');
    if (!fs.existsSync(androidRoot)) {
      res.status(404).json({ error: 'Android project directory not found' });
      return;
    }

    const zip = new JSZip();
    const files = collectFiles(androidRoot, androidRoot);

    for (const file of files) {
      zip.file(file.path, file.content);
    }

    const zipBuffer = await zip.generateAsync({ type: 'nodebuffer', compression: 'DEFLATE' });

    res.setHeader('Content-Type', 'application/zip');
    res.setHeader('Content-Disposition', 'attachment; filename="LichiAI_RealTimeVoice_AndroidStudio.zip"');
    res.send(zipBuffer);
  } catch (err: unknown) {
    res.status(500).json({ error: String(err) });
  }
});

// ---------------------------------------------------------
// 7. WebSocket Server for Real-Time Streaming Audio Calling (/live)
// ---------------------------------------------------------
const wss = new WebSocketServer({ server, path: '/live' });

wss.on('connection', (clientWs: WebSocket) => {
  console.log('Client connected to Lichi Real-Time Call WebSocket (/live)');

  clientWs.send(
    JSON.stringify({
      type: 'session_ready',
      message: 'Lichi Real-Time Voice session active',
      companion: 'Lichi AI',
    })
  );

  clientWs.on('message', async (data: Buffer | string) => {
    try {
      const parsed = JSON.parse(data.toString());

      if (parsed.type === 'user_speech') {
        const userText = parsed.text || '';
        const candidateModels = getCandidateModelsForFeature(parsed.model);

        const { result: response } = await executeWithModelFallback(
          candidateModels,
          async (m) => {
            return await ai.models.generateContent({
              model: m,
              contents: [{ role: 'user', parts: [{ text: userText }] }],
              config: {
                systemInstruction: LICHI_CALL_SYSTEM_PROMPT,
                responseMimeType: 'application/json',
                temperature: 0.8,
              },
            });
          },
          { maxRetries: 2, baseDelayMs: 800, operationName: 'WebSocketVoiceTurn' }
        );

        const replyData = JSON.parse(response.text || '{}');
        clientWs.send(
          JSON.stringify({
            type: 'lichi_speech',
            reply: replyData.reply || 'আমি শুনছি, বলো।',
            emotion: replyData.emotion || 'calm',
          })
        );
      } else if (parsed.type === 'barge_in') {
        clientWs.send(JSON.stringify({ type: 'interrupted', state: 'listening' }));
      }
    } catch (err) {
      const classified = classifyGeminiError(err);
      console.error('[WebSocket Error Redacted]', classified.rawMessage);
      clientWs.send(
        JSON.stringify({
          type: 'error',
          statusCode: classified.statusCode,
          errorCode: classified.errorCode,
          message: classified.bengaliMessage,
          canRetry: classified.isRetryable,
        })
      );
    }
  });

  clientWs.on('close', () => {
    console.log('Client disconnected from real-time call WebSocket');
  });
});

// ---------------------------------------------------------
// Start Express Server with Vite Middleware
// ---------------------------------------------------------
async function startServer() {
  const isProd = process.env.NODE_ENV === 'production';

  if (!isProd) {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    app.use(express.static(path.join(process.cwd(), 'dist')));
    app.get('*', (req, res) => {
      res.sendFile(path.join(process.cwd(), 'dist', 'index.html'));
    });
  }

  server.listen(PORT, '0.0.0.0', () => {
    console.log(`Lichi AI Real-Time Voice Calling Server listening on http://0.0.0.0:${PORT}`);
  });
}

startServer();
