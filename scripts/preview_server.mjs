// Verification-only static preview and same-origin API/SockJS reverse proxy.
import { createRequire } from 'node:module';
import { dirname, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
const require = createRequire(new URL('../Vue/package.json', import.meta.url));
const { preview } = await import(pathToFileURL(resolve(dirname(require.resolve('vite/package.json')), 'dist/node/index.js')));
await preview({
  root: fileURLToPath(new URL('../Vue', import.meta.url)), configFile: false,
  preview: { host: '127.0.0.1', port: 18081, strictPort: true, proxy: {
    '/api': { target: 'http://127.0.0.1:18080' },
    '/uploads': { target: 'http://127.0.0.1:18080' },
    '/ws': { target: 'http://127.0.0.1:18080', ws: true }
  } }
});
