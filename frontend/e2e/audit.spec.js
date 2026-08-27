import { test, expect } from '@playwright/test'
import { ACCOUNTS, login } from './helpers.js'

test('un auditeur consulte le journal d\'audit et applique un filtre', async ({ page }) => {
  await login(page, ACCOUNTS.auditor)
  await page.goto('/audit')

  await expect(page.getByRole('heading', { name: "Journal d'audit" })).toBeVisible()
  await expect(page.locator('table tbody tr').first()).toBeVisible()

  await page.getByPlaceholder('ex: operateur').fill('operateur')
  await page.getByRole('button', { name: 'Filtrer' }).click()

  await expect(page.locator('table tbody')).toBeVisible()
})
