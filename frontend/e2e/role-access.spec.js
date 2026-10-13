import { test, expect } from '@playwright/test'
import { ACCOUNTS, login } from './helpers.js'

test('un operateur n\'a pas acces a l\'audit (lien masque et route protegee)', async ({ page }) => {
  await login(page, ACCOUNTS.operator)

  // L'interface masque le lien pour un role non autorise.
  await expect(page.locator('.sidebar')).not.toContainText('Audit')

  // Meme en forcant l'URL, le garde de route (router/index.js) redirige vers '/'
  // plutot que d'afficher la page — la verification cote frontend ne suffit pas,
  // le backend refuse aussi la meme requete avec 403 (voir TaskApiIntegrationTest).
  await page.goto('/audit')
  await expect(page).toHaveURL('/')
  await expect(page.getByRole('heading', { name: "Journal d'audit" })).toHaveCount(0)
})

test('un auditeur n\'a pas acces a la soumission de taches', async ({ page }) => {
  await login(page, ACCOUNTS.auditor)

  await expect(page.locator('.sidebar')).not.toContainText('Soumettre')

  await page.goto('/tasks')
  await expect(page).toHaveURL('/')
})
