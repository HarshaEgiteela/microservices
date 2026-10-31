import { writeFileSync } from 'node:fs';

const apiUrl =
  process.env.API_BASE_URL ||
  (process.env.VERCEL ? '' : 'http://localhost:8080');

if (!apiUrl) {
  throw new Error('API_BASE_URL is required for Vercel builds');
}

const url = new URL(apiUrl);

if (!['http:', 'https:'].includes(url.protocol)) {
  throw new Error('Invalid API_BASE_URL protocol');
}

writeFileSync(
  'src/app/core/config/api.config.ts',
  `export const API_BASE_URL = ${JSON.stringify(apiUrl.replace(/\/+$/, ''))};\n`
);

console.log('API configuration generated successfully');
