// Comptes de demonstration crees par la migration V2__seed.sql (voir README).
export const ACCOUNTS = {
  operator: { username: 'operateur', password: 'password123' },
  admin: { username: 'admin', password: 'password123' },
  auditor: { username: 'auditeur', password: 'password123' },
}

export async function login(page, { username, password }) {
  await page.goto('/login')
  await page.getByPlaceholder('operateur1').fill(username)
  await page.getByPlaceholder('••••••••').fill(password)
  await page.getByRole('button', { name: 'Se connecter' }).click()
  await page.waitForURL('/')
}
