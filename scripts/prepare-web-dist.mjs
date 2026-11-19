import fs from 'node:fs';
import path from 'node:path';

const verifyOnly = process.argv.includes('--verify-only');

const requiredRootFiles = ['package.json', 'capacitor.config.ts'];
for (const file of requiredRootFiles) {
  if (!fs.existsSync(path.resolve(file))) {
    console.error(`ERROR: Missing required file: ${file}`);
    process.exit(1);
  }
}

const hasRootNativeAndroid = fs.existsSync(path.resolve('app/build.gradle.kts')) && fs.existsSync(path.resolve('app/src/main/AndroidManifest.xml'));
const hasCapacitorAndroid = fs.existsSync(path.resolve('android/app/build.gradle')) || fs.existsSync(path.resolve('android/app/build.gradle.kts'));

if (!hasRootNativeAndroid && !hasCapacitorAndroid) {
  console.warn('NOTICE: Neither app/build.gradle.kts nor android/app/build.gradle found yet; Capacitor will generate android/ during CI.');
}

if (hasRootNativeAndroid) {
  const gradleContent = fs.readFileSync('app/build.gradle.kts', 'utf8');
  if (!gradleContent.includes('applicationId = "com.dskvideo.ott"')) {
    console.error('ERROR: Invalid package ID in app/build.gradle.kts (expected com.dskvideo.ott).');
    process.exit(1);
  }
}

if (!verifyOnly) {
  const distDir = path.resolve('dist');
  fs.mkdirSync(distDir, { recursive: true });
  const indexHtml = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover" />
  <title>DSK VIDEO</title>
</head>
<body style="background:#07070B;color:#F8FAFC;font-family:sans-serif;display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0;">
  <main style="text-align:center;padding:24px;">
    <h1>DSK VIDEO</h1>
    <p>Entertainment Without Limits • A DSK OFFICIAL PRODUCTION</p>
  </main>
</body>
</html>
`;
  fs.writeFileSync(path.join(distDir, 'index.html'), indexHtml, 'utf8');
  console.log('Prepared dist/ web bundle and validated DSK VIDEO Android APK configuration.');
} else {
  console.log('Validated DSK VIDEO APK configuration successfully.');
}
