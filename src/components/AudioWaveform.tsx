import React from 'react';

interface AudioWaveformProps {
  isActive: boolean;
  color?: string;
  barCount?: number;
}

export const AudioWaveform: React.FC<AudioWaveformProps> = ({
  isActive,
  color = 'bg-cyan-400',
  barCount = 6,
}) => {
  return (
    <div className="flex items-center gap-1 h-5">
      {Array.from({ length: barCount }).map((_, i) => (
        <span
          key={i}
          className={`w-1 rounded-full transition-all duration-300 ${color} ${
            isActive ? 'animate-pulse' : 'h-1 opacity-40'
          }`}
          style={{
            height: isActive ? `${Math.max(6, (Math.sin(i * 1.2 + Date.now() / 200) * 0.5 + 0.5) * 18 + 4)}px` : '4px',
            animationDelay: `${i * 120}ms`,
          }}
        />
      ))}
    </div>
  );
};
