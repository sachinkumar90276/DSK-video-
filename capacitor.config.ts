import type { CapacitorConfig } from '@capacitor/cli';
import fs from 'node:fs';

function detectWebDir(): string {
  for (const candidate of ['dist', 'build', 'out', 'www']) {
    if (fs.existsSync(candidate)) {
      return candidate;
    }
  }
  return 'dist';
}

const config: CapacitorConfig = {
  appId: 'com.dskvideo.ott',
  appName: 'DSK VIDEO',
  webDir: detectWebDir(),
  server: {
    androidScheme: 'https',
    allowNavigation: [
      'ottlzprymsdfgbgsrmxn.supabase.co',
      '*.supabase.co',
      '*.youtube.com',
      'youtu.be'
    ]
  },
  android: {
    allowMixedContent: false,
    captureInput: true,
    webContentsDebuggingEnabled: false,
    appendUserAgent: 'DSKVideoAndroid/1.0'
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 1500,
      backgroundColor: '#07070B',
      showSpinner: true,
      spinnerColor: '#E50914'
    },
    StatusBar: {
      style: 'DARK',
      backgroundColor: '#07070B',
      overlaysWebView: true
    }
  }
};

export default config;
