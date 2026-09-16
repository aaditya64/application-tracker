import { Link, Route, Routes } from 'react-router-dom'
import { AddApplicationPage } from './pages/AddApplicationPage'
import { ApplicationDetailPage } from './pages/ApplicationDetailPage'
import { ApplicationListPage } from './pages/ApplicationListPage'
import './App.css'

function App() {
  return (
    <main className="app-shell">
      <nav className="nav-bar">
        <Link to="/" className="nav-bar__brand">Application Tracker</Link>
        <div className="nav-bar__actions">
          <Link to="/add" className="btn btn-primary btn-sm">+ Add application</Link>
        </div>
      </nav>
      <Routes>
        <Route path="/" element={<ApplicationListPage />} />
        <Route path="/add" element={<AddApplicationPage />} />
        <Route path="/applications/:id" element={<ApplicationDetailPage />} />
      </Routes>
    </main>
  )
}

export default App
