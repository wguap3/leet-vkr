import { test, expect } from '@playwright/test'
import { readFileSync } from 'node:fs'
const fixture = JSON.parse(readFileSync(new URL('./.state.json', import.meta.url), 'utf8'))
const org = fixture.users.org

test('guest registers from invitation, joins, and organizer replaces the link', async ({ page, request }) => {
  const created = await request.post('/api/contests', { headers: { Authorization: `Bearer ${org.token}` }, data: {
    title: 'Java • закрытая тренировка', description: 'Решаем задачи вместе. Вступите по приглашению, чтобы получить доступ к соревнованию.',
    startTime: new Date(Date.now() + 3600000).toISOString(), endTime: new Date(Date.now() + 7200000).toISOString(), problemIds: [fixture.problem.id], isPublic: false,
  } })
  expect(created.status()).toBe(201)
  const contest = await created.json()
  const invite = `/contests/invite/${contest.inviteToken}`
  await page.goto(invite)
  await expect(page.getByRole('heading', { name: 'Приглашение в контест' })).toBeVisible()
  await page.screenshot({ path: 'test-results/screenshots/invitation-guest.png', fullPage: true })
  await page.getByRole('link', { name: 'Создать аккаунт', exact: true }).click()
  const name = 'invite_' + Date.now().toString(36)
  await page.getByLabel('Имя пользователя').fill(name)
  await page.getByLabel('Email').fill(name + '@example.test')
  await page.getByLabel('Пароль', { exact: true }).fill('InvitePassword123!')
  await page.getByRole('button', { name: 'Создать аккаунт', exact: true }).click()
  await expect(page).toHaveURL(url => url.pathname === invite)
  await page.getByRole('button', { name: 'Зарегистрироваться и войти' }).click()
  await expect(page).toHaveURL(new RegExp(`/contests/${contest.id}$`))
  await expect(page.getByText('✓ Зарегистрирован', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Выйти' }).click()
  await page.goto(invite)
  await page.getByRole('link', { name: 'Войти', exact: true }).last().click()
  await page.getByLabel('Логин', { exact: true }).fill(org.name)
  await page.getByLabel('Пароль', { exact: true }).fill(org.password)
  await page.getByRole('button', { name: 'Войти', exact: true }).click()
  await expect(page).toHaveURL(url => url.pathname === invite)
  await page.goto(`/contests/${contest.id}`)
  await expect(page.getByText('Приватная ссылка-приглашение')).toBeVisible()
  await page.getByRole('button', { name: 'Заменить ссылку' }).click()
  await page.getByRole('button', { name: 'Подтвердить замену' }).click()
  await expect(page.getByText('Новая ссылка готова', { exact: true })).toBeVisible()
  expect((await request.get('/api/contests/invite/' + contest.inviteToken)).status()).toBe(404)
  await page.screenshot({ path: 'test-results/screenshots/invitation-organizer.png', fullPage: true })
  await page.setViewportSize({ width: 390, height: 844 })
  await page.screenshot({ path: 'test-results/screenshots/invitation-mobile.png', fullPage: true })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy()
})
