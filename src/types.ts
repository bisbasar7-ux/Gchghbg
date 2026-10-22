export type EmotionState = 'happy' | 'calm' | 'curious' | 'surprised' | 'empathetic';

export type DeviceActionType = 
  | 'OPEN_APP'
  | 'OPEN_SETTINGS'
  | 'TOGGLE_FLASHLIGHT'
  | 'SET_VOLUME'
  | 'SET_ALARM'
  | 'SEARCH_WEB'
  | 'UNKNOWN';

export interface DeviceActionRequest {
  type: DeviceActionType;
  target?: string;
  label: string;
  explanation: string;
  requiresConfirmation: boolean;
  parameters?: Record<string, string>;
}

export interface ChatMessage {
  id: string;
  sender: 'user' | 'lichi' | 'system';
  text: string;
  timestamp: number;
  emotion?: EmotionState;
  actionRequest?: DeviceActionRequest;
  imageBase64?: string;
}

export interface MemoryItem {
  id: string;
  fact: string;
  category: string;
  timestamp: number;
}

export interface AndroidProjectFile {
  path: string;
  name: string;
  content: string;
}
