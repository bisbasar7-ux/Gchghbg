import React, { useState, useEffect } from 'react';
import { AndroidProjectFile } from '../types';
import {
  FolderTree,
  FileCode,
  Download,
  Copy,
  Check,
  Search,
  ExternalLink,
  Terminal,
  BookOpen,
  Layers,
  Cpu,
  RefreshCw,
} from 'lucide-react';

export const ProjectCodeExplorer: React.FC = () => {
  const [files, setFiles] = useState<AndroidProjectFile[]>([]);
  const [selectedFile, setSelectedFile] = useState<AndroidProjectFile | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [copied, setCopied] = useState(false);
  const [activeTab, setActiveTab] = useState<'code' | 'instructions' | 'apk'>('code');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetch('/api/project/files')
      .then((res) => res.json())
      .then((data) => {
        if (data.files && data.files.length > 0) {
          setFiles(data.files);
          // Default to MainActivity.kt or build.gradle.kts
          const main = data.files.find((f: AndroidProjectFile) => f.name === 'MainActivity.kt') || data.files[0];
          setSelectedFile(main);
        }
      })
      .catch((err) => console.error('Failed to load project files:', err))
      .finally(() => setIsLoading(false));
  }, []);

  const handleCopyCode = () => {
    if (!selectedFile) return;
    navigator.clipboard.writeText(selectedFile.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const filteredFiles = files.filter(
    (f) =>
      f.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      f.path.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="flex flex-col h-full bg-slate-950 text-slate-100 rounded-2xl border border-slate-800 overflow-hidden shadow-2xl">
      {/* Top Header Bar */}
      <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-3.5 bg-slate-900 border-b border-slate-800">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-cyan-500/10 border border-cyan-500/30">
            <Cpu className="w-5 h-5 text-cyan-400" />
          </div>
          <div>
            <h2 className="text-sm font-bold text-white tracking-wide">Android Studio Project Hub</h2>
            <p className="text-[11px] text-cyan-400/80 font-mono">
              Complete Kotlin, Compose, CameraX & 3D Repository
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {/* Tabs */}
          <div className="flex items-center rounded-xl bg-slate-950 p-1 border border-slate-800 text-xs">
            <button
              onClick={() => setActiveTab('code')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg transition-all ${
                activeTab === 'code' ? 'bg-cyan-500 text-slate-950 font-bold' : 'text-slate-400 hover:text-white'
              }`}
            >
              <FileCode className="w-3.5 h-3.5" />
              <span>Source Code</span>
            </button>
            <button
              onClick={() => setActiveTab('instructions')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg transition-all ${
                activeTab === 'instructions'
                  ? 'bg-cyan-500 text-slate-950 font-bold'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <BookOpen className="w-3.5 h-3.5" />
              <span>Setup Guide</span>
            </button>
            <button
              onClick={() => setActiveTab('apk')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg transition-all ${
                activeTab === 'apk' ? 'bg-cyan-500 text-slate-950 font-bold' : 'text-slate-400 hover:text-white'
              }`}
            >
              <Terminal className="w-3.5 h-3.5" />
              <span>Build APK</span>
            </button>
          </div>

          {/* Download ZIP Button */}
          <a
            href="/api/project/download"
            download="LichiAI_AndroidStudio_Project.zip"
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 text-slate-950 text-xs font-bold shadow-lg shadow-emerald-500/20 transition-all active:scale-95"
            title="Download full project ZIP to open in Android Studio"
          >
            <Download className="w-4 h-4" />
            <span>Download Project (.ZIP)</span>
          </a>
        </div>
      </div>

      {/* Main Content Area */}
      {activeTab === 'code' && (
        <div className="grid grid-cols-1 md:grid-cols-12 flex-1 overflow-hidden">
          {/* File Tree Sidebar */}
          <div className="md:col-span-4 border-r border-slate-800 bg-slate-900/60 flex flex-col h-[650px] overflow-hidden">
            <div className="p-3 border-b border-slate-800">
              <div className="relative">
                <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search files..."
                  className="w-full pl-8 pr-3 py-1.5 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-200 placeholder:text-slate-500 focus:outline-none focus:border-cyan-400"
                />
              </div>
            </div>

            <div className="flex-1 overflow-y-auto p-2 space-y-1">
              {isLoading ? (
                <div className="flex items-center justify-center p-8 text-xs text-slate-400 gap-2">
                  <RefreshCw className="w-4 h-4 animate-spin text-cyan-400" />
                  <span>Loading project repository...</span>
                </div>
              ) : (
                filteredFiles.map((file) => {
                  const isSelected = selectedFile?.path === file.path;
                  const isKotlin = file.name.endsWith('.kt');
                  const isGradle = file.name.endsWith('.kts') || file.name.endsWith('.toml');
                  const isXml = file.name.endsWith('.xml');

                  return (
                    <button
                      key={file.path}
                      onClick={() => setSelectedFile(file)}
                      className={`w-full text-left px-3 py-2 rounded-lg text-xs font-mono transition-colors flex items-center gap-2 ${
                        isSelected
                          ? 'bg-cyan-500/20 text-cyan-300 font-semibold border border-cyan-500/30'
                          : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                      }`}
                    >
                      <span className="text-[10px] opacity-75 shrink-0">
                        {isKotlin ? '🟣' : isGradle ? '🐘' : isXml ? '📄' : '📁'}
                      </span>
                      <div className="truncate">
                        <div className="truncate text-slate-200">{file.name}</div>
                        <div className="text-[10px] text-slate-500 truncate">{file.path}</div>
                      </div>
                    </button>
                  );
                })
              )}
            </div>
          </div>

          {/* Code Viewer */}
          <div className="md:col-span-8 flex flex-col h-[650px] bg-slate-950 overflow-hidden">
            {selectedFile ? (
              <>
                <div className="flex items-center justify-between px-4 py-2.5 bg-slate-900 border-b border-slate-800 text-xs">
                  <div className="flex items-center gap-2 font-mono text-cyan-400">
                    <FileCode className="w-4 h-4" />
                    <span>{selectedFile.path}</span>
                  </div>
                  <button
                    onClick={handleCopyCode}
                    className="flex items-center gap-1.5 px-3 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
                  >
                    {copied ? (
                      <>
                        <Check className="w-3.5 h-3.5 text-emerald-400" />
                        <span className="text-emerald-400 font-medium">Copied!</span>
                      </>
                    ) : (
                      <>
                        <Copy className="w-3.5 h-3.5" />
                        <span>Copy Code</span>
                      </>
                    )}
                  </button>
                </div>

                <div className="flex-1 overflow-auto p-4 font-mono text-xs text-slate-300 leading-relaxed bg-[#0b0d17]">
                  <pre className="whitespace-pre">{selectedFile.content}</pre>
                </div>
              </>
            ) : (
              <div className="flex items-center justify-center h-full text-slate-500 text-sm">
                Select a file from the explorer to view its contents.
              </div>
            )}
          </div>
        </div>
      )}

      {/* Setup Instructions Tab */}
      {activeTab === 'instructions' && (
        <div className="p-6 overflow-y-auto max-h-[650px] space-y-6">
          <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-3">
            <h3 className="text-base font-bold text-cyan-400 flex items-center gap-2">
              <FolderTree className="w-5 h-5" />
              <span>Android Studio Open & Gradle Sync</span>
            </h3>
            <ol className="list-decimal list-inside space-y-2 text-xs text-slate-300 leading-relaxed">
              <li>
                Click the <strong>Download Project (.ZIP)</strong> button above to download the full repository.
              </li>
              <li>Extract the ZIP folder onto your workstation.</li>
              <li>Launch <strong>Android Studio</strong> (Koala, Ladybug, or newer recommended).</li>
              <li>Click <strong>File &gt; Open...</strong> and select the unzipped root project folder.</li>
              <li>Android Studio will read <code className="text-cyan-300">settings.gradle.kts</code> and run the initial Gradle Sync.</li>
              <li>JDK requirement: Java 17 or Java 21 (configured by default in Android Studio Gradle settings).</li>
            </ol>
          </div>

          <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-3">
            <h3 className="text-base font-bold text-emerald-400 flex items-center gap-2">
              <Layers className="w-5 h-5" />
              <span>Connecting Phone to the Backend Proxy</span>
            </h3>
            <div className="space-y-2 text-xs text-slate-300">
              <p>
                <strong>Emulator testing:</strong> The app is pre-configured to reach <code className="text-cyan-300">http://10.0.2.2:3000/</code> which connects directly to your computer's local server.
              </p>
              <p>
                <strong>Physical phone testing (via USB):</strong> Run ADB port-forwarding:
              </p>
              <div className="p-2.5 rounded-lg bg-black font-mono text-cyan-400 text-xs">
                adb reverse tcp:3000 tcp:3000
              </div>
              <p>
                Alternatively, enter your computer's local Wi-Fi IP (e.g. <code className="text-cyan-300">http://192.168.1.50:3000/</code>) in the in-app <strong>Settings</strong> screen.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* APK Generation Guide Tab */}
      {activeTab === 'apk' && (
        <div className="p-6 overflow-y-auto max-h-[650px] space-y-5">
          <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-3">
            <h3 className="text-base font-bold text-cyan-400 flex items-center gap-2">
              <Terminal className="w-5 h-5" />
              <span>Generate Standalone Debug APK</span>
            </h3>
            <p className="text-xs text-slate-300">
              Run this command in the project root directory inside Android Studio's terminal:
            </p>
            <div className="p-3 rounded-xl bg-black border border-slate-800 font-mono text-cyan-400 text-xs flex items-center justify-between">
              <span>./gradlew assembleDebug</span>
              <span className="text-[10px] text-slate-500">(Windows: gradlew.bat assembleDebug)</span>
            </div>
            <p className="text-xs text-slate-400">
              Upon build completion, your installable APK will be output at:
            </p>
            <div className="p-2.5 rounded-lg bg-slate-950 font-mono text-emerald-400 text-xs">
              app/build/outputs/apk/debug/app-debug.apk
            </div>
          </div>

          <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-3">
            <h3 className="text-base font-bold text-amber-400 flex items-center gap-2">
              <ExternalLink className="w-5 h-5" />
              <span>Install to Android Phone via ADB</span>
            </h3>
            <div className="p-3 rounded-xl bg-black border border-slate-800 font-mono text-amber-400 text-xs">
              adb install -r app/build/outputs/apk/debug/app-debug.apk
            </div>
            <p className="text-xs text-slate-400">
              Or copy <code className="text-slate-300">app-debug.apk</code> directly to your phone's Downloads folder and open it via the Files app to install with package installer.
            </p>
          </div>
        </div>
      )}
    </div>
  );
};
