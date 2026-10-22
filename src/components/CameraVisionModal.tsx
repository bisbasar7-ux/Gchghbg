import React, { useState, useRef, useEffect } from 'react';
import { Camera, RefreshCw, X, Send, Eye, ShieldCheck, AlertCircle, CheckCircle2 } from 'lucide-react';
import { EmotionState } from '../types';

interface CameraVisionModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAnalysisComplete: (description: string, emotion: EmotionState, objects: string[]) => void;
  preferredLanguage: string;
}

export const CameraVisionModal: React.FC<CameraVisionModalProps> = ({
  isOpen,
  onClose,
  onAnalysisComplete,
  preferredLanguage,
}) => {
  const [stream, setStream] = useState<MediaStream | null>(null);
  const [hasPermission, setHasPermission] = useState<boolean | null>(null);
  const [capturedImage, setCapturedImage] = useState<string | null>(null);
  const [prompt, setPrompt] = useState('এই ছবিতে কী দেখা যাচ্ছে বাংলায় বিস্তারিত বলুন।');
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const videoRef = useRef<HTMLVideoElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  // Start camera explicitly on user request
  const startCamera = async () => {
    setError(null);
    try {
      const mediaStream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment', width: { ideal: 1280 }, height: { ideal: 720 } },
        audio: false,
      });
      setStream(mediaStream);
      setHasPermission(true);
      if (videoRef.current) {
        videoRef.current.srcObject = mediaStream;
        videoRef.current.play();
      }
    } catch (err: unknown) {
      setHasPermission(false);
      const errMessage = err instanceof Error ? err.message : 'Camera access was rejected or unavailable';
      setError(`ক্যামেরা অ্যাক্সেস পাওয়া যায়নি: ${errMessage}`);
    }
  };

  // Stop camera stream explicitly
  const stopCamera = () => {
    if (stream) {
      stream.getTracks().forEach((track) => track.stop());
      setStream(null);
    }
  };

  useEffect(() => {
    if (isOpen && !stream && !capturedImage) {
      startCamera();
    }
    return () => {
      stopCamera();
    };
  }, [isOpen]);

  const handleCapture = () => {
    if (!videoRef.current || !canvasRef.current) return;
    const video = videoRef.current;
    const canvas = canvasRef.current;
    canvas.width = video.videoWidth || 640;
    canvas.height = video.videoHeight || 480;

    const ctx = canvas.getContext('2d');
    if (ctx) {
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
      const base64 = canvas.toDataURL('image/jpeg', 0.85);
      setCapturedImage(base64);
      stopCamera(); // Immediately stop camera hardware to preserve user privacy
    }
  };

  const handleRetake = () => {
    setCapturedImage(null);
    startCamera();
  };

  const handleAnalyze = async () => {
    if (!capturedImage) return;
    setIsAnalyzing(true);
    setError(null);

    try {
      const res = await fetch('/api/lichi/vision', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          imageBase64: capturedImage,
          prompt,
          language: preferredLanguage,
        }),
      });

      if (!res.ok) {
        const errData = await res.json().catch(() => ({}));
        throw new Error(errData.error || `Server responded with ${res.status}`);
      }

      const data = await res.json();
      onAnalysisComplete(data.description, data.emotion || 'curious', data.identifiedObjects || []);
      handleClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Vision analysis failed';
      setError(msg);
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleClose = () => {
    stopCamera();
    setCapturedImage(null);
    setError(null);
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fade-in">
      <div className="w-full max-w-md rounded-2xl bg-slate-900 border border-slate-700/60 p-5 shadow-2xl flex flex-col max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-800">
          <div className="flex items-center gap-2 text-cyan-400">
            <Camera className="w-5 h-5" />
            <div>
              <h2 className="text-sm font-bold text-white">CameraX Vision Sandbox</h2>
              <p className="text-[10px] text-cyan-400/80 font-mono">Explicit User-Authorized Capture</p>
            </div>
          </div>
          <button
            onClick={handleClose}
            className="p-1 rounded-full text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Security Notification */}
        <div className="mt-3 flex items-center gap-2 p-2 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-[11px] text-emerald-300">
          <ShieldCheck className="w-4 h-4 shrink-0 text-emerald-400" />
          <span>কোনো ছবি স্বয়ংক্রিয়ভাবে আপলোড হয় না। প্রিভিউ দেখে অনুমতি দিলে তবেই বিশ্লেষণ করা হবে।</span>
        </div>

        {/* Error message */}
        {error && (
          <div className="mt-3 flex items-center gap-2 p-2.5 rounded-lg bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs">
            <AlertCircle className="w-4 h-4 shrink-0 text-rose-400" />
            <span>{error}</span>
          </div>
        )}

        {/* Viewfinder or Snapshot Preview */}
        <div className="relative mt-3 w-full aspect-video rounded-xl overflow-hidden bg-black border border-slate-800 flex items-center justify-center">
          {capturedImage ? (
            <img src={capturedImage} alt="Snapshot Preview" className="w-full h-full object-cover" />
          ) : stream ? (
            <video
              ref={videoRef}
              autoPlay
              playsInline
              muted
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="text-center p-4">
              <Camera className="w-10 h-10 text-slate-600 mx-auto mb-2 animate-pulse" />
              <p className="text-xs text-slate-400">ক্যামেরা নিষ্ক্রিয় রয়েছে</p>
              <button
                onClick={startCamera}
                className="mt-3 px-3 py-1.5 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold text-xs"
              >
                ক্যামেরা চালু করুন
              </button>
            </div>
          )}

          {/* Snapshot tag */}
          {capturedImage && (
            <span className="absolute top-2 left-2 px-2 py-0.5 rounded-md bg-cyan-500/90 text-slate-950 text-[10px] font-bold">
              প্রিভিউ যাচাই (Preview)
            </span>
          )}
        </div>
        <canvas ref={canvasRef} className="hidden" />

        {/* Action Controls */}
        <div className="mt-3 flex items-center justify-center gap-3">
          {capturedImage ? (
            <button
              onClick={handleRetake}
              disabled={isAnalyzing}
              className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium transition-colors"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span>পুনরায় ছবি তুলুন (Retake)</span>
            </button>
          ) : stream ? (
            <button
              onClick={handleCapture}
              className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold text-xs shadow-lg shadow-cyan-500/20 transition-all active:scale-95"
            >
              <Camera className="w-4 h-4" />
              <span>ছবি তুলুন (Take Snapshot)</span>
            </button>
          ) : null}
        </div>

        {/* Question and Upload Button (Only active when image is captured) */}
        {capturedImage && (
          <div className="mt-4 pt-3 border-t border-slate-800 space-y-2.5">
            <label className="text-[11px] text-slate-300 font-semibold flex items-center gap-1">
              <Eye className="w-3.5 h-3.5 text-cyan-400" />
              <span>লিচিকে কী জানতে চান?</span>
            </label>
            <input
              type="text"
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              placeholder="যেমন: এই বস্তুটির নাম কী?"
              className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-700 text-slate-100 text-xs focus:outline-none focus:border-cyan-400"
            />

            <button
              onClick={handleAnalyze}
              disabled={isAnalyzing}
              className="w-full flex items-center justify-center gap-2 py-2.5 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-bold text-xs transition-all shadow-lg shadow-cyan-500/25 disabled:opacity-50"
            >
              {isAnalyzing ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>বিশ্লেষণ চলছে (Gemini Vision 3.8)...</span>
                </>
              ) : (
                <>
                  <Send className="w-4 h-4" />
                  <span>ছবি আপলোড ও বিশ্লেষণ করুন</span>
                </>
              )}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
