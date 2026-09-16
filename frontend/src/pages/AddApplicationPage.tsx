import { Link, useNavigate } from 'react-router-dom'
import { AddApplicationForm } from '../AddApplicationForm'

export function AddApplicationPage() {
  const navigate = useNavigate()

  return (
    <div className="app-narrow">
      <Link to="/" className="back-link">&larr; Back to applications</Link>
      <div className="card">
        <h2>Add application</h2>
        <AddApplicationForm onCreated={(app) => navigate(`/applications/${app.id}`)} />
      </div>
    </div>
  )
}
