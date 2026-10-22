import React, { useState, useEffect, useRef } from 'react';
import { EmotionState } from '../types';
import { LichiCharacter3D } from './LichiCharacter3D';
import { AudioWaveform } from './AudioWaveform';
import {
  PhoneCall,
  PhoneOff,
  Mic,
  MicOff,
  Volume2,
  Sliders,
  Sparkles,
  RotateCcw,
  Maximize2,
  Minimize2,
  AlertCircle,
  Clock,
  Heart,
  Key,
} from 'lucide-react';

export type CallStatus =
  | 'disconnected'
  | 'connecting'
  | 'connected'
  | 'listening'
  | 'thinking'
  | 'speaking';

interface VoiceCallModeProps {
  emotion: EmotionState;
  setEmotion: React.Dispatch<React.SetStateAction<EmotionState>>;
  isCallActive: boolean;
  setIsCallActive: React.Dispatch<React.SetStateAction<boolean>>;
  isFullscreen?: boolean;
  onToggleFullscreen?: () => void;
  onOpenGeminiSettings?: () => void;
}

export const VoiceCallMode: React.FC<VoiceCallModeProps> = ({
  emotion,
  setEmotion,
  isCallActive,
  setIsCallActive,
  isFullscreen = false,
  onToggleFullscreen,
  onOpenGeminiSettings,
}) => {
  const [callStatus, setCallStatus] = useState<CallStatus>('disconnected');
  const [callDuration, setCallDuration] = useState(0);
  const [isMuted, setIsMuted] = useState(false);
  const [voicePitch, setVoicePitch] = useState(1.15);
  const [voiceSpeed, setVoiceSpeed] = useState(1.02);
  const [showSettings, setShowSettings] = useState(false);
  const [lastLichiPhrase, setLastLichiPhrase] = useState('');
  const [lastUserTranscript, setLastUserTranscript] = useState('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // References
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const silenceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const speechRecognizerRef = useRef<any>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const mediaStreamRef = useRef<MediaStream | null>(null);
  const isSpeakingRef = useRef(false);

  // Call timer counter
  useEffect(() => {
    if (isCallActive) {
      timerRef.current = setInterval(() => {
        setCallDuration((prev) => prev + 1);
      }, 1000);
    } else {
      if (timerRef.current) clearInterval(timerRef.current);
      setCallDuration(0);
    }
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [isCallActive]);

  const formattedTime = () => {
    const mins = Math.floor(callDuration / 60);
    const secs = callDuration % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  // Play Lichi's Bengali speech with speech viseme sync
  const speakLichiResponse = async (text: string) => {
    if (!('speechSynthesis' in window)) return;

    window.speechSynthesis.cancel();
    setCallStatus('speaking');
    isSpeakingRef.current = true;
    setLastLichiPhrase(text);

    // Try high-fidelity server TTS first
    try {
      const res = await fetch('/api/lichi/call/tts', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text, pitch: voicePitch, speed: voiceSpeed }),
      });

      if (res.ok) {
        const data = await res.json();
        if (data.audioBase64) {
          const audio = new Audio(`data:audio/wav;base64,${data.audioBase64}`);
          audio.playbackRate = voiceSpeed;

          audio.onended = () => {
            isSpeakingRef.current = false;
            if (isCallActive) {
              setCallStatus('listening');
            }
          };

          audio.onerror = () => {
            fallbackSpeechSynthesis(text);
          };

          audio.play().catch(() => fallbackSpeechSynthesis(text));
          return;
        }
      }
    } catch {
      // Fallback to client Web Speech API
    }

    fallbackSpeechSynthesis(text);
  };

  const fallbackSpeechSynthesis = (text: string) => {
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'bn-BD';
    utterance.pitch = voicePitch;
    utterance.rate = voiceSpeed;

    utterance.onstart = () => {
      isSpeakingRef.current = true;
      setCallStatus('speaking');
    };

    utterance.onend = () => {
      isSpeakingRef.current = false;
      if (isCallActive) {
        setCallStatus('listening');
      }
    };

    utterance.onerror = () => {
      isSpeakingRef.current = false;
      if (isCallActive) {
        setCallStatus('listening');
      }
    };

    window.speechSynthesis.speak(utterance);
  };

  // Send turn to backend
  const handleUserTurn = async (transcript: string) => {
    if (!isCallActive || !transcript.trim()) return;

    setCallStatus('thinking');
    setLastUserTranscript(transcript);

    try {
      const res = await fetch('/api/lichi/call/respond', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          transcript,
          callDurationSeconds: callDuration,
        }),
      });

      const data = await res.json();
      if (!res.ok) {
        const errorText = data.error || 'মডেল সাময়িকভাবে ব্যস্ত আছে।';
        setErrorMessage(errorText);
        speakLichiResponse(data.fallbackReply || 'আমি একটু শুনতে পাইনি, তুমি কি আরেকবার বলবে?');
        return;
      }

      setErrorMessage(null);
      const reply = data.reply || 'হ্যাঁ, বলো, আমি শুনছি।';
      if (data.emotion) {
        setEmotion(data.emotion);
      }
      speakLichiResponse(reply);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Turn failed';
      setErrorMessage(`সংযোগ বিঘ্ন: ${msg}`);
      const fallback = 'আমার শুনতে একটু সমস্যা হয়েছে, তুমি কি আরেকবার বলবে?';
      speakLichiResponse(fallback);
    }
  };

  // Start continuous Hands-Free voice call
  const startCall = async () => {
    setErrorMessage(null);
    setCallStatus('connecting');

    try {
      // Request mic permission
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      mediaStreamRef.current = stream;

      setIsCallActive(true);
      setCallStatus('connected');

      // Setup continuous Speech Recognition with Voice Activity Detection (silence detection)
      const SpeechRecognition =
        (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

      if (SpeechRecognition) {
        const recognizer = new SpeechRecognition();
        recognizer.continuous = true;
        recognizer.interimResults = true;
        recognizer.lang = 'bn-BD';

        let accumulatedTranscript = '';

        recognizer.onresult = (event: any) => {
          if (isMuted) return;

          let interim = '';
          for (let i = event.resultIndex; i < event.results.length; i++) {
            const transcript = event.results[i][0].transcript;
            if (event.results[i].isFinal) {
              accumulatedTranscript += ' ' + transcript;
            } else {
              interim += transcript;
            }
          }

          // Natural Interruption / Barge-in: User speaks while Lichi is speaking
          if (isSpeakingRef.current && (interim.trim() || accumulatedTranscript.trim())) {
            window.speechSynthesis.cancel();
            isSpeakingRef.current = false;
            setCallStatus('listening');
          }

          // Reset silence timer whenever user speaks
          if (silenceTimerRef.current) clearTimeout(silenceTimerRef.current);

          const fullSpeech = (accumulatedTranscript + ' ' + interim).trim();
          if (fullSpeech) {
            setLastUserTranscript(fullSpeech);
            setCallStatus('listening');

            // Detect speech end (1.1s of silence indicates finished sentence)
            silenceTimerRef.current = setTimeout(() => {
              if (fullSpeech.length > 1) {
                accumulatedTranscript = '';
                handleUserTurn(fullSpeech);
              }
            }, 1100);
          }
        };

        recognizer.onerror = (e: any) => {
          if (e.error !== 'no-speech') {
            console.warn('Speech recognition warning:', e.error);
          }
        };

        recognizer.onend = () => {
          // Restart recognizer if call is still active
          if (isCallActive && speechRecognizerRef.current) {
            try {
              speechRecognizerRef.current.start();
            } catch {}
          }
        };

        recognizer.start();
        speechRecognizerRef.current = recognizer;
      }

      // Initial opening greeting from Lichi
      setTimeout(() => {
        const greetings = [
          'হ্যালো! বলো, আমি মন দিয়ে শুনছি। কেমন আছো তুমি?',
          'হ্যালো! তোমার দিনটা কেমন গেল?',
          'শুনছি বলো, তোমার সাথে কথা বলতে পেরে খুব ভালো লাগছে।',
        ];
        const greeting = greetings[Math.floor(Math.random() * greetings.length)];
        setEmotion('happy');
        speakLichiResponse(greeting);
      }, 700);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Microphone access denied';
      setErrorMessage(`মাইক্রোফোন অনুমতি ছাড়া কল চালু করা সম্ভব নয়: ${msg}`);
      endCall();
    }
  };

  // End Call & Cleanup
  const endCall = () => {
    setIsCallActive(false);
    setCallStatus('disconnected');
    isSpeakingRef.current = false;

    if (silenceTimerRef.current) clearTimeout(silenceTimerRef.current);
    if (timerRef.current) clearInterval(timerRef.current);

    if (speechRecognizerRef.current) {
      try {
        speechRecognizerRef.current.stop();
      } catch {}
      speechRecognizerRef.current = null;
    }

    if (mediaStreamRef.current) {
      mediaStreamRef.current.getTracks().forEach((track) => track.stop());
      mediaStreamRef.current = null;
    }

    if (window.speechSynthesis) {
      window.speechSynthesis.cancel();
    }
  };

  const toggleMute = () => {
    setIsMuted((prev) => !prev);
  };

  const getStatusDisplay = () => {
    switch (callStatus) {
      case 'disconnected':
        return { label: 'কল প্রস্তুত (Ready)', color: 'text-slate-400', badge: 'bg-slate-800' };
      case 'connecting':
        return { label: 'সংযুক্ত হচ্ছে... (Connecting)', color: 'text-cyan-400', badge: 'bg-cyan-500/20' };
      case 'connected':
        return { label: 'সংযুক্ত (Connected)', color: 'text-emerald-400', badge: 'bg-emerald-500/20' };
      case 'listening':
        return { label: 'শুনছি... বলো (Listening)', color: 'text-pink-400', badge: 'bg-pink-500/20 animate-pulse' };
      case 'thinking':
        return { label: 'ভাবছি... (Thinking)', color: 'text-amber-400', badge: 'bg-amber-500/20 animate-pulse' };
      case 'speaking':
        return { label: 'কথা বলছি... (Speaking)', color: 'text-cyan-300', badge: 'bg-cyan-500/30' };
    }
  };

  const statusInfo = getStatusDisplay();

  return (
    <div className="relative w-full h-full flex flex-col justify-between overflow-hidden bg-gradient-to-b from-[#0b0d19] via-[#070914] to-[#04050a] select-none">
      {/* Top Header Bar */}
      <div className="z-20 px-5 py-3 flex items-center justify-between border-b border-slate-800/40 bg-slate-950/30 backdrop-blur-md">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-sm font-bold text-white tracking-wide">লিচির সঙ্গে কথা বলো</h2>
            <span className="flex items-center gap-1 text-[10px] text-pink-400 font-semibold px-2 py-0.5 rounded-full bg-pink-500/10 border border-pink-500/20">
              <Heart className="w-2.5 h-2.5 fill-pink-400" />
              <span>Sweet Voice</span>
            </span>
          </div>
          <p className="text-[11px] text-slate-400 font-medium">
            {isCallActive ? `লাইভ কল চলছে • ${formattedTime()}` : 'One-Tap Real-time Calling'}
          </p>
        </div>

        <div className="flex items-center gap-2">
          {/* Emotion Badge */}
          <div className="px-2.5 py-1 rounded-full bg-slate-900 border border-slate-800 text-[11px] text-slate-300 flex items-center gap-1.5">
            <span
              className={`w-2 h-2 rounded-full ${
                emotion === 'happy'
                  ? 'bg-pink-500'
                  : emotion === 'curious'
                  ? 'bg-amber-400'
                  : emotion === 'surprised'
                  ? 'bg-purple-400'
                  : emotion === 'empathetic'
                  ? 'bg-emerald-400'
                  : 'bg-cyan-400'
              }`}
            />
            <span className="capitalize">{emotion}</span>
          </div>

          {/* Gemini API Settings Button */}
          {onOpenGeminiSettings && (
            <button
              onClick={onOpenGeminiSettings}
              className="p-1.5 rounded-xl bg-slate-900 hover:bg-slate-800 border border-cyan-500/30 text-cyan-400 hover:text-cyan-300 transition-colors"
              title="Gemini API Configuration"
            >
              <Key className="w-4 h-4" />
            </button>
          )}

          {/* Voice Settings Toggle */}
          <button
            onClick={() => setShowSettings((prev) => !prev)}
            className="p-1.5 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-400 hover:text-cyan-400 transition-colors"
            title="Voice Speed & Pitch"
          >
            <Sliders className="w-4 h-4" />
          </button>

          {onToggleFullscreen && (
            <button
              onClick={onToggleFullscreen}
              className="p-1.5 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-800 text-slate-400 hover:text-cyan-400 transition-colors"
              title={isFullscreen ? 'Exit Fullscreen' : 'Fullscreen Call'}
            >
              {isFullscreen ? <Minimize2 className="w-4 h-4" /> : <Maximize2 className="w-4 h-4" />}
            </button>
          )}
        </div>
      </div>

      {/* Voice Customization Settings Drawer */}
      {showSettings && (
        <div className="z-30 absolute top-14 inset-x-4 p-4 rounded-2xl bg-slate-900/95 border border-cyan-500/30 backdrop-blur-xl shadow-2xl space-y-3">
          <div className="flex items-center justify-between border-b border-slate-800 pb-2">
            <h3 className="text-xs font-bold text-cyan-300 flex items-center gap-1.5">
              <Volume2 className="w-4 h-4" />
              <span>লিচির মিষ্টি কণ্ঠস্বর সমন্বয়</span>
            </h3>
            <button
              onClick={() => setShowSettings(false)}
              className="text-xs text-slate-400 hover:text-white"
            >
              ✕
            </button>
          </div>

          <div className="space-y-2 text-xs">
            <div>
              <div className="flex items-center justify-between text-slate-300 mb-1">
                <span>কথা বলার গতি (Speed)</span>
                <span className="font-mono text-cyan-400">{voiceSpeed.toFixed(2)}x</span>
              </div>
              <input
                type="range"
                min="0.8"
                max="1.3"
                step="0.05"
                value={voiceSpeed}
                onChange={(e) => setVoiceSpeed(Number(e.target.value))}
                className="w-full accent-cyan-400"
              />
            </div>

            <div>
              <div className="flex items-center justify-between text-slate-300 mb-1">
                <span>কণ্ঠের সুর (Pitch)</span>
                <span className="font-mono text-pink-400">{voicePitch.toFixed(2)}x</span>
              </div>
              <input
                type="range"
                min="0.9"
                max="1.4"
                step="0.05"
                value={voicePitch}
                onChange={(e) => setVoicePitch(Number(e.target.value))}
                className="w-full accent-pink-400"
              />
            </div>
          </div>
        </div>
      )}

      {/* Center 3D Character & Speech Subtitle */}
      <div className="relative flex-1 flex flex-col items-center justify-center">
        {/* 3D Model View */}
        <div className="w-full h-full relative">
          <LichiCharacter3D
            emotion={emotion}
            isSpeaking={callStatus === 'speaking'}
          />
        </div>

        {/* Live Bengali Subtitle Bubble */}
        {(lastLichiPhrase || lastUserTranscript) && isCallActive && (
          <div className="absolute top-4 inset-x-6 z-10 flex flex-col items-center pointer-events-none">
            <div className="max-w-md px-4 py-2 rounded-2xl bg-black/75 border border-slate-700/60 backdrop-blur-md shadow-2xl text-center">
              <p className="text-xs text-slate-100 font-medium leading-relaxed">
                {callStatus === 'speaking' ? `“${lastLichiPhrase}”` : `তুমি: “${lastUserTranscript}”`}
              </p>
            </div>
          </div>
        )}

        {/* Status & Live Audio Waveform Indicator */}
        <div className="absolute bottom-4 inset-x-0 flex justify-center z-10">
          <div
            className={`px-4 py-1.5 rounded-full backdrop-blur-md border border-slate-700/60 flex items-center gap-2.5 shadow-xl ${statusInfo.badge}`}
          >
            <AudioWaveform
              isActive={callStatus === 'listening' || callStatus === 'speaking'}
              color={callStatus === 'listening' ? 'bg-pink-400' : 'bg-cyan-400'}
              barCount={5}
            />
            <span className={`text-xs font-semibold ${statusInfo.color}`}>{statusInfo.label}</span>
          </div>
        </div>
      </div>

      {/* Error Notice */}
      {errorMessage && (
        <div className="mx-4 mb-2 p-2.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0 text-rose-400" />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* Bottom Main Call Controls */}
      <div className="z-20 px-6 py-5 bg-slate-950/70 border-t border-slate-800/60 backdrop-blur-xl">
        {!isCallActive ? (
          /* Prominent One-Tap Start Call Button */
          <div className="flex flex-col items-center gap-2">
            <button
              onClick={startCall}
              className="w-full max-w-sm py-4 px-6 rounded-3xl bg-gradient-to-r from-cyan-400 via-teal-400 to-blue-500 hover:from-cyan-300 hover:to-blue-400 text-slate-950 font-extrabold text-sm flex items-center justify-center gap-3 shadow-xl shadow-cyan-500/25 transition-all hover:scale-[1.02] active:scale-95"
            >
              <PhoneCall className="w-5 h-5 text-slate-950" />
              <span>কল শুরু করুন (Start Call)</span>
            </button>
            <p className="text-[11px] text-slate-400 text-center font-medium">
              একবার ট্যাপ করলেই সারাক্ষণ হ্যান্ডস-ফ্রি স্বয়ংক্রিয় কথোপকথন
            </p>
          </div>
        ) : (
          /* Active Call Controls: Mute, End Call */
          <div className="flex items-center justify-around max-w-xs mx-auto">
            {/* Mute Button */}
            <button
              onClick={toggleMute}
              className={`p-3.5 rounded-full border transition-all ${
                isMuted
                  ? 'bg-rose-500/20 border-rose-500 text-rose-400'
                  : 'bg-slate-900 border-slate-700 text-slate-200 hover:border-cyan-400'
              }`}
              title={isMuted ? 'Unmute' : 'Mute'}
            >
              {isMuted ? <MicOff className="w-5 h-5" /> : <Mic className="w-5 h-5" />}
            </button>

            {/* End Call Button (Big Red Circle) */}
            <button
              onClick={endCall}
              className="w-16 h-16 rounded-full bg-rose-600 hover:bg-rose-500 text-white flex items-center justify-center shadow-2xl shadow-rose-600/40 transition-all hover:scale-105 active:scale-95"
              title="End Voice Call"
            >
              <PhoneOff className="w-7 h-7" />
            </button>

            {/* Natural Interruption Indicator */}
            <div className="text-center">
              <span className="text-[10px] text-slate-400 font-mono block">Interrupt</span>
              <span className="text-[9px] text-cyan-400 font-semibold">বলো থামবে</span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
