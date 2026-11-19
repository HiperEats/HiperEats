import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

describe('App', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('shows that the backend is up when the status endpoint answers UP', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ service: 'hipereats-backend', status: 'UP' }),
    }))

    render(<App />)

    expect(await screen.findByText('Backend is up')).toBeInTheDocument()
    expect(fetch).toHaveBeenCalledWith('/api/status')
  })

  it('shows that the backend is unreachable when it answers with an HTTP error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 500 }))

    render(<App />)

    expect(await screen.findByText('Backend is unreachable')).toBeInTheDocument()
  })

  it('shows that the backend is unreachable when it answers a status other than UP', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ service: 'hipereats-backend', status: 'DOWN' }),
    }))

    render(<App />)

    expect(await screen.findByText('Backend is unreachable')).toBeInTheDocument()
  })

  it('shows that the backend is unreachable when the request fails', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('network down')))

    render(<App />)

    expect(await screen.findByText('Backend is unreachable')).toBeInTheDocument()
  })
})
