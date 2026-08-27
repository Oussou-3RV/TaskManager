import { test, expect } from '@playwright/test'
import { ACCOUNTS, login } from './helpers.js'

test('soumission d\'une tache et affichage du resultat de placement', async ({ page }) => {
  await login(page, ACCOUNTS.operator)
  await page.goto('/tasks')

  await page.getByPlaceholder('1.5').fill('0.5')
  await page.getByPlaceholder('512').fill('128')
  await page.getByRole('button', { name: 'Soumettre' }).click()

  // Le backend repond 202 avec la decision de placement (acceptee ou rejetee) ;
  // le toast affiche ce resultat immediatement, sans rechargement de page.
  const toast = page.locator('.toast__message')
  await expect(toast).toContainText(/Tâche (placée sur|rejetée)/)
})

test('la page de suivi des placements affiche l\'historique des decisions', async ({ page }) => {
  await login(page, ACCOUNTS.operator)
  await page.goto('/placements')

  await expect(page.locator('.card__title')).toHaveText('Suivi des placements')
  await expect(page.locator('table thead')).toContainText('Politique')
  await expect(page.locator('table tbody tr').first()).toBeVisible()
})
