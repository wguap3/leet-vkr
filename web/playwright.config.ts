import { defineConfig, devices } from '@playwright/test'
export default defineConfig({
  testDir: './e2e', workers: 1, timeout: 90000,
  expect: { timeout: 15000 },
  use: { baseURL: process.env.STACKR_URL || 'http://localhost:3100', locale: 'ru-RU', timezoneId: 'Europe/Samara', trace: 'retain-on-failure', screenshot: 'only-on-failure' },
  reporter: [['list'], ['html', { open: 'never' }]],
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 960 } } }],
})
