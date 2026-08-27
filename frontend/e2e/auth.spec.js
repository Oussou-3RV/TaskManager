import { test, expect } from '@playwright/test'
import { ACCOUNTS, login } from './helpers.js'

test.describe('Connexion', () => {
  for (const [role, account] of Object.entries(ACCOUNTS)) {
    test(`connexion reussie avec le compte ${role} (${account.username})`, async ({ page }) => {
      await login(page, account)
      await expect(page).toHaveURL('/')
      await expect(page.locator('.sidebar__username')).toHaveText(account.username)
    })
  }

  test('connexion refusee avec un mauvais mot de passe', async ({ page }) => {
    await page.goto('/login')
    await page.getByPlaceholder('operateur1').fill('operateur')
    await page.getByPlaceholder('••••••••').fill('mauvais-mot-de-passe')
    await page.getByRole('button', { name: 'Se connecter' }).click()
    await expect(page.getByText("Nom d'utilisateur ou mot de passe incorrect.")).toBeVisible()
    await expect(page).toHaveURL('/login')
  })
})
