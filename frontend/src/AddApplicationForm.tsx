import { useState } from 'react'
import type { SubmitEvent } from 'react'
import { createApplication } from './api'
import type { Application } from './types'

interface Props {
  onCreated: (application: Application) => void
}

const emptyForm = {
  company: '',
  role: 'Graduate Software Engineer',
  jobUrl: '',
  location: '',
  dateApplied: '',
  notes: '',
}

export function AddApplicationForm({ onCreated }: Props) {
  const [form, setForm] = useState(emptyForm)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function updateField<K extends keyof typeof emptyForm>(field: K, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  async function handleSubmit(event: SubmitEvent) {
    event.preventDefault()
    setError(null)

    if (!form.company.trim() || !form.role.trim()) {
      setError('Company and role are required.')
      return
    }

    setSubmitting(true)
    try {
      const created = await createApplication({
        company: form.company.trim(),
        role: form.role.trim(),
        jobUrl: form.jobUrl.trim() || undefined,
        location: form.location.trim() || undefined,
        dateApplied: form.dateApplied || undefined,
        notes: form.notes.trim() || undefined,
      })
      onCreated(created)
      setForm(emptyForm)
    } catch {
      setError('Something went wrong creating the application. Is the backend running?')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="form">
      <label className="field">
        Company *
        <input
          value={form.company}
          onChange={(e) => updateField('company', e.target.value)}
          required
        />
      </label>

      <label className="field">
        Role *
        <input
          value={form.role}
          onChange={(e) => updateField('role', e.target.value)}
          required
        />
      </label>

      <label className="field">
        Job URL
        <input
          type="url"
          value={form.jobUrl}
          onChange={(e) => updateField('jobUrl', e.target.value)}
          placeholder="https://…"
        />
      </label>
      <p className="field-hint"></p>

      <div className="field-row">
        <label className="field">
          Location
          <input
            value={form.location}
            onChange={(e) => updateField('location', e.target.value)}
          />
        </label>

        <label className="field">
          Date applied
          <input
            type="date"
            value={form.dateApplied}
            onChange={(e) => updateField('dateApplied', e.target.value)}
          />
        </label>
      </div>

      <label className="field">
        Notes
        <textarea
          value={form.notes}
          onChange={(e) => updateField('notes', e.target.value)}
          rows={3}
        />
      </label>

      {error && <p className="form-error">{error}</p>}

      <button type="submit" className="btn btn-primary" disabled={submitting} style={{ alignSelf: 'flex-start' }}>
        {submitting ? 'Adding…' : 'Add application'}
      </button>
    </form>
  )
}
