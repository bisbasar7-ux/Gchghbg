/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect, useRef } from 'react';
import {
  EmotionState,
  ChatMessage,
  DeviceActionRequest,
  MemoryItem,
} from './types';
import { VoiceCallMode } from './components/VoiceCallMode';
import { AudioWaveform } from './components/AudioWaveform';
import { ActionConfirmModal } from './components/ActionConfirmModal';
import { CameraVisionModal } from './components/CameraVisionModal';
import { ProjectCodeExplorer } from './components/ProjectCodeExplorer';
import { GeminiSettingsModal } from './components/GeminiSettingsModal';
import {
  PhoneCall,
  Smartphone,
  FolderCode,
  Download,
  Trash2,
  Sparkles,
  Wifi,
  Battery,
  Settings,
  Clock,
  CheckCircle2,
  Send,
  Camera,
  Mic,
  Sliders,
  Flashlight,
  Shield,
  Maximize2,
  Heart,
  Key,
} from 'lucide-react';

export default function App() {
  // Navigation & View Mode
  const [activeView, setActiveView] = useState<'simulator' | 'project_hub' | 'fullscreen_call'>('simulator');
  const [activePhoneTab, setActivePhoneTab] = useState<'call' | 'chat' | 'actions' | 'memory' | 'settings'>('call');
  const [isGeminiModalOpen, setIsGeminiModalOpen] = useState(false);

  // Real-time Voice Call State
  const [isCallActive, setIsCallActive] = useState(false);
  const [emotion, setEmotion] = useState<EmotionState>('happy');

  // Chat & Memory
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputText, setInputText] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [memories, setMemories] = useState<MemoryItem[]>([
    { id: '1', fact: 'ব্যবহারকারী বাংলায় কথা বলতে ভালোবাসেন', category: 'language', timestamp: Date.now() - 3600000 },
    { id: '2', fact: 'লিচির মিষ্টি কণ্ঠস্বর ব্যবহারকারীর পছন্দ', category: 'preference', timestamp: Date.now() - 1800000 },
  ]);

  // Device Action Sandbox & Modals
  const [pendingAction, setPendingAction] = useState<DeviceActionRequest | null>(null);
  const [actionFeedback, setActionFeedback] = useState<string | null>(null);
  const [isVisionModalOpen, setIsVisionModalOpen] = useState(false);
  const [isFlashlightOn, setIsFlashlightOn] = useState(false);
  const [simulatedVolume, setSimulatedVolume] = useState(70);

  const chatBottomRef = useRef<HTMLDivElement>(null);

  // Initial Welcome Message
  useEffect(() => {
    const welcome = 'স্বাগতম! আমি লিচি, আপনার এআই সঙ্গী। সরাসরি রিয়েল-টাইম ভয়েস কলে কথা বলতে "কল শুরু করুন" চাপুন।';
    setMessages([
      {
        id: 'welcome-msg',
        sender: 'lichi',
        text: welcome,
        timestamp: Date.now(),
        emotion: 'happy',
      },
    ]);
  }, []);

  // Send Message in Text Chat
  const sendChatMessage = async (textToSend: string) => {
    const text = textToSend.trim();
    if (!text) return;

    const userMessage: ChatMessage = {
      id: `user-${Date.now()}`,
      sender: 'user',
      text,
      timestamp: Date.now(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInputText('');
    setIsProcessing(true);

    try {
      const history = messages.slice(-6).map((m) => ({
        role: m.sender === 'user' ? 'user' : 'assistant',
        text: m.text,
      }));

      const res = await fetch('/api/lichi/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: text,
          history,
          memories: memories.map((m) => m.fact),
          language: 'bn',
        }),
      });

      if (!res.ok) throw new Error(`Server returned code ${res.status}`);

      const data = await res.json();
      const reply = data.reply || 'আমি আছি তোমার সাথে।';
      if (data.emotion) setEmotion(data.emotion);

      const assistantMsg: ChatMessage = {
        id: `lichi-${Date.now()}`,
        sender: 'lichi',
        text: reply,
        timestamp: Date.now(),
        emotion: data.emotion || 'calm',
        actionRequest: data.deviceAction || undefined,
      };

      setMessages((prev) => [...prev, assistantMsg]);

      if (data.deviceAction) {
        handleTriggerDeviceAction(data.deviceAction);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Communication error';
      setMessages((prev) => [
        ...prev,
        { id: `sys-${Date.now()}`, sender: 'system', text: `সার্ভার ত্রুটি: ${msg}`, timestamp: Date.now() },
      ]);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleTriggerDeviceAction = (action: DeviceActionRequest) => {
    if (action.requiresConfirmation) {
      setPendingAction(action);
    } else {
      executeActionNow(action);
    }
  };

  const executeActionNow = (action: DeviceActionRequest) => {
    setPendingAction(null);
    switch (action.type) {
      case 'OPEN_APP':
        setActionFeedback(`অ্যাপ্লিকেশন লঞ্চ করা হয়েছে: ${action.target || action.label}`);
        break;
      case 'OPEN_SETTINGS':
        setActionFeedback(`সিস্টেম সেটিংস খোলা হয়েছে: ${action.target}`);
        break;
      case 'TOGGLE_FLASHLIGHT':
        setIsFlashlightOn((prev) => !prev);
        setActionFeedback(`ফ্ল্যাশলাইট ${!isFlashlightOn ? 'চালু' : 'বন্ধ'} হয়েছে।`);
        break;
      case 'SET_VOLUME': {
        const lvl = parseInt(action.parameters?.level || '70', 10);
        setSimulatedVolume(lvl);
        setActionFeedback(`ভলিউম ${lvl}% এ সেট করা হয়েছে।`);
        break;
      }
      case 'SET_ALARM':
        setActionFeedback(`অ্যালার্ম তৈরি করা হয়েছে।`);
        break;
      default:
        setActionFeedback(`অ্যাকশন সফল: ${action.label}`);
    }
    setTimeout(() => setActionFeedback(null), 4000);
  };

  return (
    <div className="min-h-screen bg-[#070913] text-slate-100 flex flex-col font-sans selection:bg-cyan-500/30">
      {/* Top Universal App Header */}
      <header className="sticky top-0 z-40 bg-slate-950/80 backdrop-blur-md border-b border-slate-800/80 px-4 md:px-8 py-3 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-cyan-500 via-indigo-600 to-pink-500 p-0.5 shadow-lg shadow-cyan-500/20">
            <div className="w-full h-full bg-slate-950 rounded-[10px] flex items-center justify-center">
              <span className="text-cyan-400 font-extrabold text-xs tracking-wider">LC</span>
            </div>
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="font-extrabold text-sm md:text-base text-white tracking-wide">
                LICHI AI
              </h1>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-pink-500/10 text-pink-400 border border-pink-500/30 flex items-center gap-1">
                <Heart className="w-2.5 h-2.5 fill-pink-400" />
                <span>Real-Time Voice Companion</span>
              </span>
            </div>
            <p className="text-[11px] text-slate-400 hidden sm:block">
              One-Tap Continuous Calling • Gemini 3.8 Live & Flash • Bengali Voice • Android Studio Project
            </p>
          </div>
        </div>

        {/* View Switcher Controls */}
        <div className="flex items-center gap-2">
          <div className="flex items-center bg-slate-900 border border-slate-800 rounded-xl p-1 text-xs">
            <button
              onClick={() => setActiveView('simulator')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-medium transition-all ${
                activeView === 'simulator'
                  ? 'bg-cyan-500 text-slate-950 font-bold shadow-md shadow-cyan-500/20'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <Smartphone className="w-3.5 h-3.5" />
              <span>3D Phone Call</span>
            </button>
            <button
              onClick={() => setActiveView('fullscreen_call')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-medium transition-all ${
                activeView === 'fullscreen_call'
                  ? 'bg-pink-500 text-slate-950 font-bold shadow-md shadow-pink-500/20'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <Maximize2 className="w-3.5 h-3.5" />
              <span>Fullscreen Call</span>
            </button>
            <button
              onClick={() => setActiveView('project_hub')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-medium transition-all ${
                activeView === 'project_hub'
                  ? 'bg-cyan-500 text-slate-950 font-bold shadow-md shadow-cyan-500/20'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <FolderCode className="w-3.5 h-3.5" />
              <span>Android Files</span>
            </button>
          </div>

          <button
            onClick={() => setIsGeminiModalOpen(true)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-900 hover:bg-slate-800 border border-cyan-500/40 text-cyan-300 text-xs font-semibold transition-all hover:scale-105"
            title="Gemini API Settings"
          >
            <Key className="w-3.5 h-3.5 text-cyan-400" />
            <span className="hidden sm:inline">Gemini API</span>
          </button>

          <a
            href="/api/project/download"
            download="LichiAI_RealTimeVoice_AndroidStudio.zip"
            className="hidden md:flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-cyan-400 text-xs font-semibold transition-all"
            title="Download full project ZIP"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Download ZIP</span>
          </a>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-3 sm:p-6 flex flex-col">
        {activeView === 'project_hub' && (
          <div className="flex-1">
            <ProjectCodeExplorer />
          </div>
        )}

        {activeView === 'fullscreen_call' && (
          <div className="flex-1 w-full max-w-4xl mx-auto h-[740px] rounded-3xl overflow-hidden border border-slate-800 shadow-2xl">
            <VoiceCallMode
              emotion={emotion}
              setEmotion={setEmotion}
              isCallActive={isCallActive}
              setIsCallActive={setIsCallActive}
              isFullscreen={true}
              onToggleFullscreen={() => setActiveView('simulator')}
              onOpenGeminiSettings={() => setIsGeminiModalOpen(true)}
            />
          </div>
        )}

        {activeView === 'simulator' && (
          /* Phone Simulator View */
          <div className="flex-1 flex flex-col lg:flex-row items-center justify-center gap-8 py-2">
            {/* Left Column: Realistic Android Mobile Device Shell */}
            <div className="relative w-full max-w-[390px] h-[780px] bg-slate-950 rounded-[44px] border-[10px] border-slate-800/90 shadow-2xl shadow-cyan-950/30 flex flex-col overflow-hidden ring-1 ring-slate-700/50">
              {/* Phone Status Bar */}
              <div className="h-7 bg-slate-950 flex items-center justify-between px-6 text-[11px] text-slate-400 select-none z-20">
                <div className="flex items-center gap-1.5 font-semibold text-slate-200">
                  <Clock className="w-3 h-3 text-cyan-400" />
                  <span>10:24 AM</span>
                </div>
                {/* Camera punch hole */}
                <div className="w-3.5 h-3.5 rounded-full bg-slate-900 border border-slate-800 flex items-center justify-center">
                  <div className="w-1.5 h-1.5 rounded-full bg-pink-500/60" />
                </div>
                <div className="flex items-center gap-2">
                  <Wifi className="w-3 h-3 text-cyan-400" />
                  <span className="font-mono text-[10px]">5G</span>
                  <Battery className="w-3.5 h-3.5 text-emerald-400" />
                </div>
              </div>

              {/* Action Feedback Toast */}
              {actionFeedback && (
                <div className="absolute top-10 inset-x-4 z-30 p-2.5 rounded-xl bg-cyan-950/90 border border-cyan-400/60 text-cyan-200 text-xs font-medium shadow-xl flex items-center gap-2 animate-bounce">
                  <CheckCircle2 className="w-4 h-4 text-cyan-400 shrink-0" />
                  <span>{actionFeedback}</span>
                </div>
              )}

              {/* In-Phone Screen Views */}
              <div className="flex-1 flex flex-col overflow-hidden relative">
                {/* TAB 1: DEDICATED FULL-SCREEN CALL SCREEN */}
                {activePhoneTab === 'call' && (
                  <VoiceCallMode
                    emotion={emotion}
                    setEmotion={setEmotion}
                    isCallActive={isCallActive}
                    setIsCallActive={setIsCallActive}
                    isFullscreen={false}
                    onToggleFullscreen={() => setActiveView('fullscreen_call')}
                    onOpenGeminiSettings={() => setIsGeminiModalOpen(true)}
                  />
                )}

                {/* TAB 2: CHAT CONVERSATION */}
                {activePhoneTab === 'chat' && (
                  <div className="flex-1 flex flex-col bg-[#0b0d19] overflow-hidden">
                    <div className="px-4 py-2.5 border-b border-slate-800 flex items-center justify-between">
                      <div>
                        <h2 className="text-xs font-bold text-white">চ্যাট স্ক্রিন (Text Chat)</h2>
                        <p className="text-[10px] text-slate-400">Bengali & English Dialogue</p>
                      </div>
                      <button
                        onClick={() => setMessages([])}
                        className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400"
                        title="Clear conversation"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>

                    <div className="flex-1 overflow-y-auto p-3 space-y-3">
                      {messages.map((m) => {
                        const isUser = m.sender === 'user';
                        return (
                          <div
                            key={m.id}
                            className={`flex flex-col ${isUser ? 'items-end' : 'items-start'}`}
                          >
                            <div
                              className={`max-w-[85%] p-3 rounded-2xl text-xs leading-relaxed ${
                                isUser
                                  ? 'bg-cyan-500/20 border border-cyan-500/40 text-cyan-100 rounded-br-xs'
                                  : 'bg-slate-900 border border-slate-800 text-slate-200 rounded-bl-xs'
                              }`}
                            >
                              <p className="whitespace-pre-wrap">{m.text}</p>
                            </div>
                          </div>
                        );
                      })}
                      {isProcessing && (
                        <div className="flex items-center gap-2 p-2 text-xs text-cyan-400">
                          <AudioWaveform isActive={true} barCount={4} />
                          <span>লিচি লিখছে...</span>
                        </div>
                      )}
                      <div ref={chatBottomRef} />
                    </div>

                    <div className="p-2 border-t border-slate-800 bg-slate-950 flex items-center gap-2">
                      <input
                        type="text"
                        value={inputText}
                        onChange={(e) => setInputText(e.target.value)}
                        onKeyDown={(e) => e.key === 'Enter' && sendChatMessage(inputText)}
                        placeholder="এখানে বার্তা লিখুন..."
                        className="flex-1 px-3 py-2 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-100 focus:outline-none focus:border-cyan-400"
                      />
                      <button
                        onClick={() => sendChatMessage(inputText)}
                        disabled={!inputText.trim()}
                        className="p-2 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 disabled:opacity-40 transition-all font-bold"
                      >
                        <Send className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                )}

                {/* TAB 3: DEVICE ACTIONS */}
                {activePhoneTab === 'actions' && (
                  <div className="flex-1 flex flex-col bg-[#0b0d19] overflow-y-auto p-4 space-y-4">
                    <div>
                      <h2 className="text-xs font-bold text-white">ডিভাইস কন্ট্রোল (Device Actions)</h2>
                      <p className="text-[10px] text-slate-400">Permission-Aware Android Sandbox</p>
                    </div>

                    <div className="grid grid-cols-3 gap-2">
                      {[
                        { label: 'YouTube', pkg: 'com.google.android.youtube' },
                        { label: 'WhatsApp', pkg: 'com.whatsapp' },
                        { label: 'Chrome', pkg: 'com.android.chrome' },
                      ].map((app) => (
                        <button
                          key={app.label}
                          onClick={() =>
                            handleTriggerDeviceAction({
                              type: 'OPEN_APP',
                              target: app.pkg,
                              label: `${app.label} অ্যাপ ওপেন`,
                              explanation: `অফিসিয়াল ${app.label} অ্যাপ চালু করা হবে।`,
                              requiresConfirmation: true,
                            })
                          }
                          className="p-2.5 rounded-xl bg-slate-900 border border-slate-800 text-xs text-cyan-300 text-center"
                        >
                          {app.label}
                        </button>
                      ))}
                    </div>

                    <div className="p-3 rounded-2xl bg-slate-900 border border-slate-800 space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-xs text-slate-200">ফ্ল্যাশলাইট (Torch)</span>
                        <button
                          onClick={() => {
                            setIsFlashlightOn((p) => !p);
                            setActionFeedback(`ফ্ল্যাশলাইট ${!isFlashlightOn ? 'চালু' : 'বন্ধ'} হয়েছে।`);
                          }}
                          className={`px-3 py-1 rounded-lg text-xs font-bold ${
                            isFlashlightOn ? 'bg-amber-400 text-slate-950' : 'bg-slate-800 text-slate-400'
                          }`}
                        >
                          {isFlashlightOn ? 'ON' : 'OFF'}
                        </button>
                      </div>

                      <div>
                        <div className="flex items-center justify-between text-xs text-slate-300 mb-1">
                          <span>ভলিউম</span>
                          <span className="font-mono text-cyan-400">{simulatedVolume}%</span>
                        </div>
                        <input
                          type="range"
                          min="0"
                          max="100"
                          value={simulatedVolume}
                          onChange={(e) => setSimulatedVolume(Number(e.target.value))}
                          className="w-full accent-cyan-400"
                        />
                      </div>
                    </div>
                  </div>
                )}

                {/* TAB 4: MEMORY */}
                {activePhoneTab === 'memory' && (
                  <div className="flex-1 flex flex-col bg-[#0b0d19] overflow-y-auto p-4 space-y-4">
                    <div className="flex items-center justify-between">
                      <h2 className="text-xs font-bold text-white">স্মৃতি ও মেমরি (Memory)</h2>
                      <button onClick={() => setMemories([])} className="text-[10px] text-rose-400">
                        সব মুছুন
                      </button>
                    </div>
                    <div className="space-y-2">
                      {memories.map((m) => (
                        <div key={m.id} className="p-3 rounded-xl bg-slate-900 border border-slate-800 text-xs">
                          {m.fact}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* TAB 5: SETTINGS */}
                {activePhoneTab === 'settings' && (
                  <div className="flex-1 flex flex-col bg-[#0b0d19] overflow-y-auto p-4 space-y-4">
                    <h2 className="text-xs font-bold text-white">সেটিংস (Settings)</h2>
                    <div className="p-3 rounded-2xl bg-slate-900 border border-slate-800 space-y-2 text-xs">
                      <p className="font-bold text-slate-200">নিরাপদ ব্যাকএন্ড সংযোগ</p>
                      <p className="text-slate-400 text-[11px]">সার্ভার স্ট্যাটাস: রিয়েল-টাইম ভয়েস কল সক্রিয়</p>
                    </div>
                  </div>
                )}
              </div>

              {/* Bottom Phone Navigation Bar (Only visible when call is not active to maintain clean focus) */}
              {!isCallActive && (
                <div className="h-14 bg-slate-950 border-t border-slate-800/80 flex items-center justify-around px-2 text-[10px] select-none z-20">
                  {[
                    { id: 'call', label: 'Call', icon: PhoneCall },
                    { id: 'chat', label: 'Chat', icon: Send },
                    { id: 'actions', label: 'Actions', icon: Sliders },
                    { id: 'memory', label: 'Memory', icon: Sparkles },
                    { id: 'settings', label: 'Settings', icon: Settings },
                  ].map((item) => {
                    const IconComp = item.icon;
                    const isCurrent = activePhoneTab === item.id;
                    return (
                      <button
                        key={item.id}
                        onClick={() => setActivePhoneTab(item.id as any)}
                        className={`flex flex-col items-center gap-1 transition-colors ${
                          isCurrent ? 'text-pink-400 font-bold' : 'text-slate-500 hover:text-slate-300'
                        }`}
                      >
                        <IconComp className="w-4 h-4" />
                        <span>{item.label}</span>
                      </button>
                    );
                  })}
                </div>
              )}

              {/* Android Gesture Bar */}
              <div className="h-4 bg-slate-950 flex items-center justify-center">
                <div className="w-24 h-1 rounded-full bg-slate-600" />
              </div>
            </div>

            {/* Right Column: Project Highlights & Calling Features */}
            <div className="w-full lg:max-w-md flex flex-col gap-4">
              <div className="p-5 rounded-3xl bg-slate-900/60 border border-slate-800/80 space-y-3">
                <div className="flex items-center gap-2 text-pink-400">
                  <Heart className="w-5 h-5 fill-pink-400" />
                  <h3 className="font-bold text-sm text-white">Real-Time Voice Calling Features</h3>
                </div>
                <p className="text-xs text-slate-300 leading-relaxed">
                  Lichi AI features genuine continuous hands-free voice calling. Tap the call button once and speak naturally in Bengali without pressing buttons for each sentence.
                </p>
                <div className="space-y-2 text-xs pt-1">
                  <div className="p-2.5 rounded-xl bg-slate-950/80 border border-slate-800/80 flex items-center justify-between">
                    <span className="text-slate-400">Hands-Free Calling</span>
                    <span className="text-emerald-400 font-semibold">Continuous duplex mic</span>
                  </div>
                  <div className="p-2.5 rounded-xl bg-slate-950/80 border border-slate-800/80 flex items-center justify-between">
                    <span className="text-slate-400">Natural Barge-In</span>
                    <span className="text-cyan-400 font-semibold">Interrupt anytime</span>
                  </div>
                  <div className="p-2.5 rounded-xl bg-slate-950/80 border border-slate-800/80 flex items-center justify-between">
                    <span className="text-slate-400">Personality</span>
                    <span className="text-pink-400 font-semibold">Sweet, warm Bengali</span>
                  </div>
                  <div className="p-2.5 rounded-xl bg-slate-950/80 border border-slate-800/80 flex items-center justify-between">
                    <span className="text-slate-400">3D Character</span>
                    <span className="text-amber-400 font-semibold">Viseme mouth sync</span>
                  </div>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="p-5 rounded-3xl bg-slate-900/60 border border-slate-800/80 space-y-3">
                <h4 className="text-xs font-bold text-white flex items-center gap-2">
                  <FolderCode className="w-4 h-4 text-cyan-400" />
                  <span>Android Studio & Code Export</span>
                </h4>
                <div className="flex items-center gap-2.5 pt-1">
                  <button
                    onClick={() => setActiveView('project_hub')}
                    className="flex-1 py-2.5 px-3 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 text-xs font-bold transition-all shadow-lg shadow-cyan-500/20 text-center"
                  >
                    View Project Files
                  </button>
                  <a
                    href="/api/project/download"
                    download="LichiAI_RealTimeVoice_AndroidStudio.zip"
                    className="flex-1 py-2.5 px-3 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 text-xs font-bold transition-all shadow-lg shadow-emerald-500/20 text-center flex items-center justify-center gap-1.5"
                  >
                    <Download className="w-3.5 h-3.5" />
                    <span>Download ZIP</span>
                  </a>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* Sensitive Action Modal */}
      <ActionConfirmModal
        action={pendingAction}
        onConfirm={() => {
          if (pendingAction) executeActionNow(pendingAction);
        }}
        onCancel={() => {
          setPendingAction(null);
          setActionFeedback('অ্যাকশন বাতিল করা হয়েছে।');
          setTimeout(() => setActionFeedback(null), 3000);
        }}
      />

      {/* Camera Vision Modal */}
      <CameraVisionModal
        isOpen={isVisionModalOpen}
        onClose={() => setIsVisionModalOpen(false)}
        preferredLanguage="bn"
        onAnalysisComplete={(description, newEmotion) => {
          setEmotion(newEmotion);
          const visionMsg: ChatMessage = {
            id: `vision-${Date.now()}`,
            sender: 'lichi',
            text: `[ক্যামেরা ভিশন বিশ্লেষণ]:\n${description}`,
            timestamp: Date.now(),
            emotion: newEmotion,
          };
          setMessages((prev) => [...prev, visionMsg]);
        }}
      />

      {/* Gemini API Settings Modal */}
      <GeminiSettingsModal
        isOpen={isGeminiModalOpen}
        onClose={() => setIsGeminiModalOpen(false)}
        onStartVoiceCall={() => {
          setIsGeminiModalOpen(false);
          setIsCallActive(true);
        }}
      />
    </div>
  );
}
