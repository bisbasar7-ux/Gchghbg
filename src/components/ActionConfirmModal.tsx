import React from 'react';
import { DeviceActionRequest } from '../types';
import { AlertTriangle, Check, X, ShieldAlert } from 'lucide-react';

interface ActionConfirmModalProps {
  action: DeviceActionRequest | null;
  onConfirm: () => void;
  onCancel: () => void;
}

export const ActionConfirmModal: React.FC<ActionConfirmModalProps> = ({
  action,
  onConfirm,
  onCancel,
}) => {
  if (!action) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-fade-in">
      <div className="w-full max-w-sm rounded-2xl bg-slate-900 border border-amber-500/50 p-5 shadow-2xl shadow-amber-500/10">
        <div className="flex items-center gap-3 text-amber-400 mb-3">
          <div className="p-2 rounded-xl bg-amber-500/10 border border-amber-500/30">
            <AlertTriangle className="w-5 h-5 text-amber-400" />
          </div>
          <div>
            <h3 className="font-bold text-base text-white">ডিভাইস অ্যাকশন নিশ্চিতকরণ</h3>
            <p className="text-[11px] text-amber-400/80 font-mono">Sensitive Action Authorization</p>
          </div>
        </div>

        <div className="my-3 p-3 rounded-xl bg-slate-950/80 border border-slate-800 text-sm">
          <p className="font-medium text-slate-100">{action.label}</p>
          {action.explanation && (
            <p className="text-xs text-slate-400 mt-1 leading-relaxed">{action.explanation}</p>
          )}

          <div className="mt-2.5 pt-2 border-t border-slate-800 flex items-center justify-between text-[11px] text-slate-400 font-mono">
            <span>Action Type:</span>
            <span className="text-cyan-400 font-semibold">{action.type}</span>
          </div>
          {action.target && (
            <div className="mt-1 flex items-center justify-between text-[11px] text-slate-400 font-mono truncate">
              <span>Target:</span>
              <span className="text-slate-300 truncate max-w-[180px]">{action.target}</span>
            </div>
          )}
        </div>

        <div className="flex items-center gap-2 p-2 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-[11px] text-emerald-400 mb-4">
          <ShieldAlert className="w-4 h-4 shrink-0" />
          <span>অ্যান্ড্রয়েড নিরাপত্তা নীতি অনুসারে আপনার অনুমতি আবশ্যক।</span>
        </div>

        <div className="grid grid-cols-2 gap-2.5">
          <button
            onClick={onCancel}
            className="flex items-center justify-center gap-1.5 py-2.5 px-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium transition-colors"
          >
            <X className="w-4 h-4" />
            <span>বাতিল (Cancel)</span>
          </button>
          <button
            onClick={onConfirm}
            className="flex items-center justify-center gap-1.5 py-2.5 px-3 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 text-xs font-bold transition-all shadow-lg shadow-cyan-500/20"
          >
            <Check className="w-4 h-4" />
            <span>অনুমতি দিন (Allow)</span>
          </button>
        </div>
      </div>
    </div>
  );
};
