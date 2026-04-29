import express from 'express';
import path from 'path';
import { fileURLToPath } from 'url';
import puppeteer from 'puppeteer-core';

const app = express();
app.use(express.json());
app.use(express.urlencoded({ extended: false }));

const PORT = Number(process.env.PORT || 5000);
const TARGET = process.env.BOT_TARGET || 'https://nginx';
const ADMIN_USERNAME = process.env.BOT_USERNAME || 'admin';
const ADMIN_PASSWORD = process.env.BOT_PASSWORD || 'password';
const CHROME_PATH = '/usr/bin/google-chrome';
const BOT_WAIT_MS = 20000;
const BOT_NAV_TIMEOUT_MS = 5000;

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

app.use(express.static(path.join(__dirname, '..', 'public')));

function isValidHttpUrl(url) {
  return typeof url === 'string' && (url.startsWith('http://') || url.startsWith('https://'));
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function visitUrl(url) {
  const browser = await puppeteer.launch({
    executablePath: CHROME_PATH,
    args: ['--no-sandbox', '--ignore-certificate-errors'],
  });

  try {
    const page = await browser.newPage();
    page.on('dialog', async (dialog) => {
      await dialog.accept();
    });

    await page.goto(`${TARGET}/login`, { waitUntil: 'domcontentloaded', timeout: BOT_NAV_TIMEOUT_MS });
    await page.type('input[name="username"]', ADMIN_USERNAME);
    await page.type('input[name="password"]', ADMIN_PASSWORD);
    await page.click('button[type="submit"]');

    await delay(1000);

    await page.goto(url, { waitUntil: 'domcontentloaded', timeout: BOT_NAV_TIMEOUT_MS });
    await delay(BOT_WAIT_MS);
  } finally {
    await browser.close();
  }
}

function enqueueVisit(url) {
  visitUrl(url).catch((error) => {
    console.error('[bot] visit failed:', error);
  });
}

app.post('/report', async (req, res) => {
  const { url } = req.body || {};
  if (!isValidHttpUrl(url)) {
    return res.status(400).json({ message: 'Invalid url' });
  }

  enqueueVisit(url);

  return res.json({ message: 'Reported.' });
});

process.on('unhandledRejection', (reason) => {
  console.error('[bot] unhandled rejection:', reason);
});

app.listen(PORT, () => {
  console.log(`[bot] listening on :${PORT}`);
});