import { useEffect, useState } from 'react'
import './App.css'

// Walking skeleton (HE-16): shows whether the web UI can reach the backend.
function App() {
  const [status, setStatus] = useState('checking')

  useEffect(() => {
    fetch('/api/status')
      .then((response) => (response.ok ? response.json() : Promise.reject(response.status)))
      .then((body) => setStatus(body.status === 'UP' ? 'up' : 'down'))
      .catch(() => setStatus('down'))
  }, [])

  const messages = {
    checking: 'Checking the backend…',
    up: 'Backend is up',
    down: 'Backend is unreachable',
  }

  return (
    <main>
      <h1>HiperEats</h1>
      <p>Managed e-scooter fleet for food delivery from hypermarket food courts.</p>
      <p role="status" data-testid="backend-status" className={`status status-${status}`}>
        {messages[status]}
      </p>
    </main>
  )
}

export default App
