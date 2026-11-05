import { test, expect } from '@playwright/test'
test('landing and login render without JavaScript errors', async ({ page }) => {
  const errors: string[] = []
  page.on('pageerror', e => errors.push(e.message))
  await page.goto('/')
  await expect(page.getByRole('navigation')).toBeVisible()
  await page.screenshot({ path: 'test-results/screenshots/landing.png', fullPage: true })
  await page.goto('/login')
  await expect(page.getByRole('button', { name: 'Войти', exact: true })).toBeVisible()
  await expect(page.locator('input')).toHaveCount(2)
  await page.screenshot({ path: 'test-results/screenshots/brand-login.png', fullPage: true })
  await page.setViewportSize({ width: 390, height: 844 })
  await page.screenshot({ path: 'test-results/screenshots/brand-mobile.png', fullPage: true })
  expect(errors).toEqual([])
})
