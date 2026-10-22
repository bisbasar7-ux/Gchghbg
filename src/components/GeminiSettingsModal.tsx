import React, { useState, useEffect } from 'react';
import {
  Key,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  PhoneCall,
  ExternalLink,
  ShieldCheck,
  Cpu,
  Clock,
  Sparkles,
  Eye,
  EyeOff,
  Save,
  Trash2,
  X,
  Layers,
  ArrowRight,
  ShieldAlert,
} from 'lucide-react';

interface GeminiSettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  onStartVoiceCall: () => void;
}

interface SupportedModel {
  id: string;
  name: string;
  description: string;
  capabilities: string[];
  recommended: boolean;
}

export const GeminiSettingsModal: React.FC<GeminiSettingsModalProps> = ({
  isOpen,
  onClose,
  onStartVoiceCall,
}) => {
  const [connectionStatus, setConnectionStatus] = useState<'connected' | 'disconnected' | 'testing'>('disconnected');
  const [latencyMs, setLatencyMs] = useState<number | null>(null);
  const [isTesting, setIsTesting] = useState(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [errorCode, setErrorCode] = useState<string | null>(null);
  const [canRetry, setCanRetry] = useState(false);
  const [maskedKey, setMaskedKey] = useState<string>('');
  const [customKeyInput, setCustomKeyInput] = useState<string>('');
  const [showKeyText, setShowKeyText] = useState(false);
  const [selectedModel, setSelectedModel] = useState<string>('gemini-3.8-flash');
  const [activeServerModel, setActiveServerModel] = useState<string>('gemini-3.8-flash');
  const [supportedModels, setSupportedModels] = useState<SupportedModel[]>([
    {
      id: 'gemini-3.8-flash',
      name: 'Gemini 3.8 Flash (ডিফল্ট)',
      description: 'সুষম গতি, বুদ্ধি ও মাল্টিমোডাল ভিশন',
      capabilities: ['Chat', 'Voice Turns', 'CameraX Vision'],
      recommended: true,
    },
    {
      id: 'gemini-3.1-flash-lite',
      name: 'Gemini 3.1 Flash Lite (হাই থ্রুপুট)',
      description: 'উচ্চ ট্রাফিকের সময় অতি দ্রুত রেসপন্স ও কম ল্যাটেন্সি',
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
  ]);

  // Fetch current backend configuration
  const fetchStatus = async () => {
    try {
      const res = await fetch('/api/config/gemini');
      if (res.ok) {
        const data = await res.json();
        setConnectionStatus(data.status === 'connected' ? 'connected' : 'disconnected');
        setMaskedKey(data.maskedKey || '');
        if (data.model) {
          setSelectedModel(data.model);
          setActiveServerModel(data.model);
        }
        if (data.supportedModels && Array.isArray(data.supportedModels)) {
          setSupportedModels(data.supportedModels);
        }
      }
    } catch {
      setConnectionStatus('disconnected');
    }
  };

  useEffect(() => {
    if (isOpen) {
      fetchStatus();
    }
  }, [isOpen]);

  // Model change handler
  const handleModelChange = async (newModel: string) => {
    setSelectedModel(newModel);
    try {
      const res = await fetch('/api/config/gemini/model', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ model: newModel }),
      });
      if (res.ok) {
        setActiveServerModel(newModel);
      }
    } catch {
      // Ignore network error on model switch
    }
  };

  // Run Real Connection Test with minimal valid payload
  const runTestConnection = async (keyOverride?: string, modelOverride?: string) => {
    setIsTesting(true);
    setConnectionStatus('testing');
    setSuccessMsg(null);
    setErrorMsg(null);
    setErrorCode(null);
    setCanRetry(false);

    const targetModel = modelOverride || selectedModel;

    try {
      const res = await fetch('/api/config/gemini/test', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          apiKey: keyOverride || customKeyInput.trim() || undefined,
          model: targetModel,
        }),
      });

      const data = await res.json();
      if (res.ok && data.success) {
        setConnectionStatus('connected');
        setLatencyMs(data.latencyMs || 180);
        setSuccessMsg(data.message || 'Gemini API সফলভাবে যাচাই হয়েছে! রিয়েল-টাইম ভয়েস কল এখন প্রস্তুত।');
        if (data.model) {
          setActiveServerModel(data.model);
        }
      } else {
        setConnectionStatus('disconnected');
        setErrorCode(data.errorCode || 'ERROR');
        setErrorMsg(data.error || 'সংযোগ পরীক্ষা ব্যর্থ হয়েছে।');
        setCanRetry(Boolean(data.canRetry));
      }
    } catch (err: unknown) {
      setConnectionStatus('disconnected');
      const msg = err instanceof Error ? err.message : 'Network test error';
      setErrorMsg(`নেটওয়ার্ক সংযোগ ব্যর্থ: ${msg}। সার্ভার চালু আছে কিনা পরীক্ষা করুন।`);
      setCanRetry(true);
    } finally {
      setIsTesting(false);
    }
  };

  // Switch to Flash Lite fallback directly on 503
  const handleQuickSwitchToLite = async () => {
    await handleModelChange('gemini-3.1-flash-lite');
    await runTestConnection(undefined, 'gemini-3.1-flash-lite');
  };

  // Save Key to Backend
  const handleSaveKey = async () => {
    if (!customKeyInput.trim()) return;
    setIsTesting(true);
    setErrorMsg(null);
    setSuccessMsg(null);
    setErrorCode(null);
    setCanRetry(false);

    try {
      const res = await fetch('/api/config/gemini/save', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          apiKey: customKeyInput.trim(),
          model: selectedModel,
        }),
      });

      const data = await res.json();
      if (res.ok && data.success) {
        setConnectionStatus('connected');
        setSuccessMsg(data.message || 'Gemini API কী সফলভাবে সংরক্ষিত হয়েছে!');
        setCustomKeyInput('');
        fetchStatus();
      } else {
        setErrorMsg(data.error || 'সংরক্ষণ ব্যর্থ হয়েছে।');
        setErrorCode(data.errorCode || null);
        setCanRetry(Boolean(data.canRetry));
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Save error';
      setErrorMsg(`সংরক্ষণ ব্যর্থ: ${msg}`);
      setCanRetry(true);
    } finally {
      setIsTesting(false);
    }
  };

  // Delete Custom Key
  const handleDeleteKey = async () => {
    try {
      const res = await fetch('/api/config/gemini/delete', { method: 'POST' });
      if (res.ok) {
        setSuccessMsg('কাস্টম API Key মুছে ফেলা হয়েছে। ডিফল্ট কী রিস্টোর করা হয়েছে।');
        setCustomKeyInput('');
        fetchStatus();
      }
    } catch {
      setErrorMsg('কী মুছতে সমস্যা হয়েছে।');
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-fade-in">
      <div className="w-full max-w-xl rounded-3xl bg-slate-900 border border-slate-700/60 p-6 shadow-2xl flex flex-col max-h-[92vh] overflow-y-auto">
        {/* Modal Header */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-cyan-500/10 border border-cyan-500/30">
              <Key className="w-5 h-5 text-cyan-400" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white">Gemini API সেটিংস ও মডেল কনফিগারেশন</h2>
              <p className="text-[11px] text-cyan-400/80 font-mono">
                Real-Time Voice, Vision & 503 Resiliency Engine
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-full text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* 1. Connection Status Card */}
        <div className="mt-4 p-4 rounded-2xl bg-slate-950 border border-slate-800 flex items-center justify-between">
          <div>
            <span className="text-[11px] text-slate-400 block font-medium">সংযোগ অবস্থা (Status)</span>
            <div className="flex items-center gap-2 mt-1">
              <span
                className={`w-2.5 h-2.5 rounded-full ${
                  connectionStatus === 'connected'
                    ? 'bg-emerald-400 animate-pulse'
                    : connectionStatus === 'testing'
                    ? 'bg-cyan-400 animate-ping'
                    : 'bg-rose-500'
                }`}
              />
              <span
                className={`text-sm font-extrabold capitalize ${
                  connectionStatus === 'connected'
                    ? 'text-emerald-400'
                    : connectionStatus === 'testing'
                    ? 'text-cyan-400'
                    : 'text-rose-400'
                }`}
              >
                {connectionStatus === 'connected'
                  ? 'Connected (সংযুক্ত)'
                  : connectionStatus === 'testing'
                  ? 'Testing (পরীক্ষা চলছে...)'
                  : 'Disconnected (সংযোগ বিচ্ছিন্ন)'}
              </span>
            </div>
            {connectionStatus === 'connected' && (
              <span className="text-[10px] text-slate-400 font-mono mt-0.5 block">
                Model: <strong className="text-cyan-300">{activeServerModel}</strong> • Latency: {latencyMs || '~150'}ms
              </span>
            )}
          </div>

          {/* Quick Voice Call Trigger if Connected */}
          {connectionStatus === 'connected' && (
            <button
              onClick={() => {
                onClose();
                onStartVoiceCall();
              }}
              className="flex items-center gap-2 py-2 px-3.5 rounded-xl bg-gradient-to-r from-pink-500 to-rose-500 hover:from-pink-400 hover:to-rose-400 text-white text-xs font-bold shadow-lg shadow-pink-500/20 transition-all hover:scale-105 active:scale-95"
            >
              <PhoneCall className="w-4 h-4 text-white" />
              <span>লিচুর সঙ্গে কথা বলো</span>
            </button>
          )}
        </div>

        {/* 2. Model Selection Dropdown (Requirement 5) */}
        <div className="mt-3 p-4 rounded-2xl bg-slate-950 border border-slate-800 space-y-2.5">
          <div className="flex items-center justify-between">
            <label className="text-xs font-bold text-slate-200 flex items-center gap-1.5">
              <Cpu className="w-4 h-4 text-cyan-400" />
              <span>Gemini মডেল নির্বাচন (Model Selection):</span>
            </label>
            <span className="text-[10px] text-cyan-400/90 font-mono bg-cyan-500/10 border border-cyan-500/30 px-2 py-0.5 rounded-full">
              503 Auto-Fallback Active
            </span>
          </div>

          <p className="text-[11px] text-slate-400 leading-snug">
            সরাসরি এআই স্টুডিও সমর্থিত মডেল নির্বাচন করুন। অতিরিক্ত চাপের সময় স্বয়ংক্রিয় ব্যাকঅফ ও ফলব্যাক সক্রিয় থাকে:
          </p>

          <div className="grid grid-cols-1 gap-2 pt-1">
            {supportedModels.map((m) => {
              const isSelected = selectedModel === m.id;
              return (
                <div
                  key={m.id}
                  onClick={() => handleModelChange(m.id)}
                  className={`p-3 rounded-xl border cursor-pointer transition-all flex items-start justify-between ${
                    isSelected
                      ? 'bg-cyan-500/10 border-cyan-500/50 shadow-md shadow-cyan-500/10'
                      : 'bg-slate-900/80 border-slate-800 hover:border-slate-700'
                  }`}
                >
                  <div className="space-y-0.5">
                    <div className="flex items-center gap-2">
                      <span className={`text-xs font-bold ${isSelected ? 'text-cyan-300' : 'text-slate-200'}`}>
                        {m.name}
                      </span>
                      {m.recommended && (
                        <span className="text-[9px] px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-300 font-semibold border border-emerald-500/30">
                          Recommended
                        </span>
                      )}
                      {m.id === 'gemini-3.1-flash-lite' && (
                        <span className="text-[9px] px-1.5 py-0.5 rounded bg-amber-500/20 text-amber-300 font-semibold border border-amber-500/30">
                          503 Safe / Fast
                        </span>
                      )}
                    </div>
                    <p className="text-[11px] text-slate-400">{m.description}</p>
                    <div className="flex items-center gap-1.5 pt-1 text-[9px] text-slate-500">
                      <span>Capabilities:</span>
                      {m.capabilities.map((c) => (
                        <span key={c} className="bg-slate-800 px-1.5 py-0.5 rounded text-slate-300">
                          {c}
                        </span>
                      ))}
                    </div>
                  </div>
                  <div
                    className={`w-4 h-4 rounded-full border mt-0.5 flex items-center justify-center ${
                      isSelected ? 'border-cyan-400 bg-cyan-400' : 'border-slate-600'
                    }`}
                  >
                    {isSelected && <div className="w-1.5 h-1.5 rounded-full bg-slate-950" />}
                  </div>
                </div>
              );
            })}
          </div>

          <div className="p-2 rounded-lg bg-slate-900 border border-slate-800/80 flex items-center gap-2 text-[10px] text-slate-400">
            <Layers className="w-3.5 h-3.5 text-cyan-400 shrink-0" />
            <span>
              স্বয়ংক্রিয় ফলব্যাক চেইন: <strong className="text-slate-300">{selectedModel}</strong> →{' '}
              <span className="text-slate-300">gemini-3.1-flash-lite</span> →{' '}
              <span className="text-slate-300">gemini-flash-latest</span>
            </span>
          </div>
        </div>

        {/* 3. API Key Password-Style Input Form */}
        <div className="mt-3 p-4 rounded-2xl bg-slate-950 border border-slate-800 space-y-3">
          <div>
            <label className="text-xs font-bold text-slate-200 block">
              Gemini API Key ইনপুট (Password-style)
            </label>
            <p className="text-[11px] text-slate-400 leading-snug">
              গুগল এআই স্টুডিও থেকে প্রাপ্ত গোপন কী পরীক্ষা বা সংরক্ষণ করতে পারেন:
            </p>
          </div>

          <div className="relative">
            <input
              type={showKeyText ? 'text' : 'password'}
              value={customKeyInput}
              onChange={(e) => setCustomKeyInput(e.target.value)}
              placeholder={maskedKey ? `Current Key: ${maskedKey}` : 'AIzaSy... আপনার Gemini API Key'}
              className="w-full pl-3 pr-10 py-2.5 rounded-xl bg-slate-900 border border-slate-700 text-xs text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-cyan-400 font-mono"
            />
            <button
              type="button"
              onClick={() => setShowKeyText((prev) => !prev)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-cyan-400"
            >
              {showKeyText ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            </button>
          </div>

          {/* Action Buttons: Test Connection, Save, Delete */}
          <div className="flex items-center gap-2 pt-1">
            <button
              onClick={() => runTestConnection(customKeyInput.trim() || undefined)}
              disabled={isTesting}
              className="flex-1 flex items-center justify-center gap-1.5 py-2.5 px-3 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 text-xs font-bold transition-all shadow-lg shadow-cyan-500/20 disabled:opacity-50 cursor-pointer"
            >
              {isTesting ? (
                <>
                  <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                  <span>যাচাই চলছে (Minimal Ping)...</span>
                </>
              ) : (
                <>
                  <RefreshCw className="w-3.5 h-3.5" />
                  <span>Test Connection</span>
                </>
              )}
            </button>

            <button
              onClick={handleSaveKey}
              disabled={isTesting || !customKeyInput.trim()}
              className="flex items-center gap-1.5 py-2.5 px-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium border border-slate-700 disabled:opacity-40 cursor-pointer"
              title="Save Key to Backend"
            >
              <Save className="w-3.5 h-3.5" />
              <span>Save</span>
            </button>

            <button
              onClick={handleDeleteKey}
              className="p-2.5 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 cursor-pointer"
              title="Delete Custom Key"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Success Banner */}
        {successMsg && (
          <div className="mt-3 p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>{successMsg}</span>
          </div>
        )}

        {/* Error Banner with Categorized Bengali Message & Manual Retry (Requirements 2, 3, 4, 8) */}
        {errorMsg && (
          <div className="mt-3 p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs space-y-2">
            <div className="flex items-start gap-2">
              <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
              <div className="flex-1 space-y-1">
                <div className="flex items-center gap-2">
                  <span className="font-bold text-rose-200">
                    {errorCode === 'UNAVAILABLE'
                      ? 'HTTP 503 UNAVAILABLE (মডেল ওভারলোড)'
                      : errorCode === 'RESOURCE_EXHAUSTED'
                      ? 'HTTP 429 RESOURCE EXHAUSTED (কোটা শেষ)'
                      : errorCode === 'UNAUTHENTICATED'
                      ? 'HTTP 401/403 AUTHENTICATION ERROR'
                      : errorCode === 'NOT_FOUND'
                      ? 'HTTP 404 MODEL NOT FOUND'
                      : 'সংযোগ ত্রুটি'}
                  </span>
                </div>
                <p className="leading-relaxed">{errorMsg}</p>
              </div>
            </div>

            {/* If 503 or retryable, offer Retry & Fast Switch */}
            <div className="flex flex-wrap items-center gap-2 pt-1 border-t border-rose-500/20">
              {canRetry && (
                <button
                  onClick={() => runTestConnection(customKeyInput.trim() || undefined)}
                  disabled={isTesting}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-rose-500 hover:bg-rose-600 text-white font-bold text-[11px] shadow transition-all cursor-pointer"
                >
                  <RefreshCw className="w-3 h-3" />
                  <span>পুনরায় চেষ্টা করুন (Retry)</span>
                </button>
              )}

              {errorCode === 'UNAVAILABLE' && selectedModel !== 'gemini-3.1-flash-lite' && (
                <button
                  onClick={handleQuickSwitchToLite}
                  className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-cyan-600 hover:bg-cyan-500 text-slate-950 font-bold text-[11px] transition-all cursor-pointer"
                >
                  <span>Gemini 3.1 Flash Lite এ পরিবর্তন করুন</span>
                  <ArrowRight className="w-3 h-3" />
                </button>
              )}
            </div>
          </div>
        )}

        {/* 4. Google AI Studio Setup Guide */}
        <div className="mt-4 p-4 rounded-2xl bg-slate-950 border border-slate-800 space-y-2.5 text-xs text-slate-300 leading-relaxed">
          <div className="flex items-center justify-between text-cyan-400 font-bold">
            <span className="flex items-center gap-1.5">
              <Sparkles className="w-4 h-4" />
              <span>গুগল এআই স্টুডিও থেকে কী পাওয়ার নিয়ম</span>
            </span>
            <a
              href="https://aistudio.google.com/app/apikey"
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1 text-[11px] text-cyan-300 hover:underline"
            >
              <span>Get API Key</span>
              <ExternalLink className="w-3 h-3" />
            </a>
          </div>
          <ol className="list-decimal list-inside space-y-1 text-slate-400 text-[11px]">
            <li>
              ব্রাউজারে <code className="text-cyan-300 font-mono">aistudio.google.com/app/apikey</code> খুলুন।
            </li>
            <li>গুগল অ্যাকাউন্ট দিয়ে সাইন-ইন করে <strong>Create API key</strong> নির্বাচন করুন।</li>
            <li>
              প্রাপ্ত কী কপি করে ব্যাকএন্ডের <code className="text-cyan-300 font-mono">.env</code> ফাইলে{' '}
              <code className="text-cyan-300 font-mono">GEMINI_API_KEY=&quot;YOUR_KEY&quot;</code> সেট করুন।
            </li>
            <li>উপরের <strong>Test Connection</strong> বাটনে ট্যাপ করে লাইভ সংযোগ যাচাই করুন।</li>
          </ol>
          <div className="pt-2 border-t border-slate-800/80 flex items-center gap-2 text-[10px] text-slate-400">
            <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>API কী কখনো অ্যান্ড্রয়েড APK তে এম্বেড হবে না; ব্যাকএন্ড প্রক্সিতে সংরক্ষিত থাকে।</span>
          </div>
        </div>
      </div>
    </div>
  );
};
