import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname } from 'node:path';

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

const outputPath = 'src/app/core/config/api.config.ts';

// Create the directory if it doesn't exist
mkdirSync(dirname(outputPath), { recursive: true });

// Generate the API configuration
writeFileSync(
  outputPath,
  `export const API_BASE_URL = ${JSON.stringify(
    apiUrl.replace(/\/+$/, '')
  )};\n`
);

console.log('API configuration generated successfully');
