import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  // Un seul worker : les tests partagent la meme base de donnees (pas de
  // stack isolee par test), donc en executer plusieurs en parallele cree des
  // collisions aleatoires (ex: la liste de taches change pendant qu'un autre
  // test la lit). "fullyParallel: false" ne suffit pas seul : ca empeche la
  // parallelisation DANS un fichier, mais plusieurs fichiers tournent quand
  // meme en parallele par defaut.
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'retain-on-failure',
  },
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
  },
})
