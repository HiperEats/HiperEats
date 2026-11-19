import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Vitest globals are off, so Testing Library does not clean up on its own:
// unmount what each test rendered, so tests never see each other's DOM.
afterEach(() => {
  cleanup()
})
