import { useEffect, useRef, useState } from 'react'
import type { ChangeEvent, SubmitEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  captureSnapshot,
  getApplication,
  getStageHistory,
  snapshotViewUrl,
  transitionStage,
  updateApplication,
  uploadSnapshot,
} from '../api'
import { OUTCOME_TRACKED_STAGES, STAGE_TYPES, formatStage, isStale } from '../types'
import type { Application, OutcomeStatus, StageHistoryEntry, StageType } from '../types'

const CHECKLIST_ITEMS = [
  { key: 'cvSubmitted', label: 'CV submitted' },
  { key: 'coverLetterSubmitted', label: 'Cover letter submitted' },
  { key: 'applicationAnswersSubmitted', label: 'Application answers submitted' },
] as const

const ACTION_NOTES_MAX_LENGTH = 160

interface EditableFields {
  company: string
  role: string
  jobUrl: string
  location: string
  dateApplied: string
  notes: string
}

interface ActionFields {
  actionDueDate: string
  actionNotes: string
  actionEmailLink: string
}

function toEditable(app: Application): EditableFields {
  return {
    company: app.company,
    role: app.role,
    jobUrl: app.jobUrl ?? '',
    location: app.location ?? '',
    dateApplied: app.dateApplied ?? '',
    notes: app.notes ?? '',
  }
}

function toActionEditable(app: Application): ActionFields {
  return {
    actionDueDate: app.actionDueDate ?? '',
    actionNotes: app.actionNotes ?? '',
    actionEmailLink: app.actionEmailLink ?? '',
  }
}

export function ApplicationDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [application, setApplication] = useState<Application | null>(null)
  const [form, setForm] = useState<EditableFields | null>(null)
  const [actionForm, setActionForm] = useState<ActionFields | null>(null)
  const [history, setHistory] = useState<StageHistoryEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [savingAction, setSavingAction] = useState(false)
  const [changingStage, setChangingStage] = useState(false)
  const [pendingStage, setPendingStage] = useState<StageType | ''>('')
  const [error, setError] = useState<string | null>(null)
  const [savedAt, setSavedAt] = useState<number | null>(null)
  const [actionSavedAt, setActionSavedAt] = useState<number | null>(null)
  const [togglingKey, setTogglingKey] = useState<string | null>(null)
  const [updatingOutcome, setUpdatingOutcome] = useState(false)
  const [capturingSnapshot, setCapturingSnapshot] = useState(false)
  const [uploadingSnapshot, setUploadingSnapshot] = useState(false)
  const [snapshotMessage, setSnapshotMessage] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  function loadHistory(appId: string) {
    getStageHistory(appId)
      .then(setHistory)
      .catch(() => setHistory([]))
  }

  useEffect(() => {
    if (!id) return
    setLoading(true)
    setError(null)
    getApplication(id)
      .then((app) => {
        setApplication(app)
        setForm(toEditable(app))
        setActionForm(toActionEditable(app))
        setPendingStage(app.currentStage)
        loadHistory(id)
      })
      .catch(() => setError('Could not load this application. Is the backend running?'))
      .finally(() => setLoading(false))
  }, [id])

  function updateField<K extends keyof EditableFields>(field: K, value: EditableFields[K]) {
    setForm((prev) => (prev ? { ...prev, [field]: value } : prev))
  }

  function updateActionField<K extends keyof ActionFields>(field: K, value: ActionFields[K]) {
    setActionForm((prev) => (prev ? { ...prev, [field]: value } : prev))
  }

  async function handleSubmit(event: SubmitEvent) {
    event.preventDefault()
    if (!id || !form) return
    setError(null)

    if (!form.company.trim() || !form.role.trim()) {
      setError('Company and role are required.')
      return
    }

    setSaving(true)
    try {
      const updated = await updateApplication(id, {
        company: form.company.trim(),
        role: form.role.trim(),
        jobUrl: form.jobUrl.trim() || undefined,
        location: form.location.trim() || undefined,
        dateApplied: form.dateApplied || undefined,
        notes: form.notes.trim() || undefined,
      })
      setApplication(updated)
      setForm(toEditable(updated))
      setSavedAt(Date.now())
    } catch {
      setError('Something went wrong saving this application.')
    } finally {
      setSaving(false)
    }
  }

  async function handleActionSubmit(event: SubmitEvent) {
    event.preventDefault()
    if (!id || !actionForm) return
    setError(null)

    setSavingAction(true)
    try {
      const updated = await updateApplication(id, {
        actionDueDate: actionForm.actionDueDate || undefined,
        actionNotes: actionForm.actionNotes.trim() || undefined,
        actionEmailLink: actionForm.actionEmailLink.trim() || undefined,
      })
      setApplication(updated)
      setActionForm(toActionEditable(updated))
      setActionSavedAt(Date.now())
    } catch {
      setError('Something went wrong saving the action details.')
    } finally {
      setSavingAction(false)
    }
  }

  async function handleChecklistToggle(key: (typeof CHECKLIST_ITEMS)[number]['key'], value: boolean) {
    if (!id || !application) return
    setError(null)
    setTogglingKey(key)
    try {
      const updated = await updateApplication(id, { [key]: value })
      setApplication(updated)
    } catch {
      setError('Something went wrong updating the checklist.')
    } finally {
      setTogglingKey(null)
    }
  }

  async function handleOutcomeToggle(value: OutcomeStatus) {
    if (!id || !application) return
    setError(null)
    setUpdatingOutcome(true)
    try {
      const updated = await updateApplication(id, { outcomeStatus: value })
      setApplication(updated)
    } catch {
      setError('Something went wrong updating the outcome status.')
    } finally {
      setUpdatingOutcome(false)
    }
  }

  async function handleCaptureSnapshot() {
    if (!id) return
    setError(null)
    setSnapshotMessage(null)
    setCapturingSnapshot(true)
    const startedAt = application?.snapshotCapturedAt ?? null
    try {
      await captureSnapshot(id)
      // Capture runs in the background (it renders the page, which takes a few seconds), so poll
      // briefly for it to finish rather than making the user manually reload the page.
      for (let attempt = 0; attempt < 20; attempt++) {
        await new Promise((resolve) => setTimeout(resolve, 3000))
        const refreshed = await getApplication(id)
        if (refreshed.snapshotCapturedAt && refreshed.snapshotCapturedAt !== startedAt) {
          setApplication(refreshed)
          setSnapshotMessage(
            refreshed.snapshotLikelyFaulty
              ? 'Snapshot saved, but it might be incomplete or blocked — see the warning below.'
              : 'Snapshot saved.',
          )
          return
        }
      }
      setSnapshotMessage('Still capturing — this can take a while for slow pages. Check back shortly.')
    } catch {
      setError('Something went wrong requesting the snapshot.')
    } finally {
      setCapturingSnapshot(false)
    }
  }

  async function handleUploadSnapshot(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!id || !file) return
    setError(null)
    setSnapshotMessage(null)
    setUploadingSnapshot(true)
    try {
      const updated = await uploadSnapshot(id, file)
      setApplication(updated)
      setSnapshotMessage('Uploaded snapshot saved.')
    } catch {
      setError('Something went wrong uploading that file. Make sure it is a PDF.')
    } finally {
      setUploadingSnapshot(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  async function handleStageChange() {
    if (!id || !pendingStage || !application) return
    setError(null)
    setChangingStage(true)
    try {
      const updated = await transitionStage(id, pendingStage)
      setApplication(updated)
      loadHistory(id)
    } catch {
      setError('Something went wrong changing the stage.')
    } finally {
      setChangingStage(false)
    }
  }

  if (loading) return <p>Loading…</p>
  if (error && !form) return <p className="form-error">{error}</p>
  if (!application || !form || !actionForm) return null

  const showActionDetails = OUTCOME_TRACKED_STAGES.includes(application.currentStage)
    && application.outcomeStatus === 'ACTION_NEEDED'

  return (
    <>
      <Link to="/" className="back-link">&larr; Back to applications</Link>

      <div className="card stage-control">
        <h2>{application.company} — {application.role}</h2>
        <div className="stage-control__current">
          Current stage: <strong>{formatStage(application.currentStage)}</strong>
          {isStale(application) && <span className="badge badge-stale">Stale</span>}
        </div>

        <div className="stage-control__row">
          <label className="field">
            Move to stage
            <select value={pendingStage} onChange={(e) => setPendingStage(e.target.value as StageType)}>
              {STAGE_TYPES.map((stage) => (
                <option key={stage} value={stage}>
                  {formatStage(stage)}
                </option>
              ))}
            </select>
          </label>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={handleStageChange}
            disabled={changingStage || pendingStage === application.currentStage}
          >
            {changingStage ? 'Updating…' : 'Update stage'}
          </button>
        </div>

        {OUTCOME_TRACKED_STAGES.includes(application.currentStage) && (
          <div className="outcome-toggle" role="group" aria-label="Outcome status">
            <button
              type="button"
              className={application.outcomeStatus === 'ACTION_NEEDED' ? 'active' : ''}
              disabled={updatingOutcome}
              onClick={() => handleOutcomeToggle('ACTION_NEEDED')}
            >
              Action needed
            </button>
            <button
              type="button"
              className={application.outcomeStatus === 'AWAITING_OUTCOME' ? 'active' : ''}
              disabled={updatingOutcome}
              onClick={() => handleOutcomeToggle('AWAITING_OUTCOME')}
            >
              Awaiting outcome
            </button>
          </div>
        )}

        {showActionDetails && (
          <form onSubmit={handleActionSubmit} className="action-details">
            <h4>Action needed — details</h4>
            <div className="field-row">
              <label className="field">
                Due date
                <input
                  type="date"
                  value={actionForm.actionDueDate}
                  onChange={(e) => updateActionField('actionDueDate', e.target.value)}
                />
              </label>
              <label className="field">
                Relevant email
                <input
                  type="url"
                  placeholder="Link to the email…"
                  value={actionForm.actionEmailLink}
                  onChange={(e) => updateActionField('actionEmailLink', e.target.value)}
                />
              </label>
            </div>
            <label className="field">
              Notes 
              <input
                value={actionForm.actionNotes}
                onChange={(e) => updateActionField('actionNotes', e.target.value)}
                maxLength={ACTION_NOTES_MAX_LENGTH}
              />
            </label>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <button type="submit" className="btn btn-secondary btn-sm" disabled={savingAction}>
                {savingAction ? 'Saving…' : 'Save action details'}
              </button>
              {actionForm.actionEmailLink && (
                <a href={actionForm.actionEmailLink} target="_blank" rel="noopener noreferrer" className="btn-sm">
                  Open email ↗
                </a>
              )}
              {actionSavedAt && <span className="form-saved">Saved.</span>}
            </div>
          </form>
        )}
      </div>

      <div className="card checklist">
        <h3>Submitted materials</h3>
        {CHECKLIST_ITEMS.map(({ key, label }) => (
          <label key={key} className="checklist-item">
            <input
              type="checkbox"
              checked={application[key]}
              disabled={togglingKey === key}
              onChange={(e) => handleChecklistToggle(key, e.target.checked)}
            />
            {label}
          </label>
        ))}
      </div>

      <div className="card">
        <h3>Page snapshot</h3>
        <p className="field-hint" style={{ marginTop: 0, marginBottom: '0.9rem' }}>
          A saved PDF copy of the job URL's page, so you can still read it for interview prep if the
          listing closes.
        </p>

        <div className="snapshot-status">
          {application.snapshotAvailable ? (
            <>
              <span>
                Saved {application.snapshotCapturedAt && new Date(application.snapshotCapturedAt).toLocaleString()}
                {application.snapshotManuallyUploaded ? ' (uploaded by you)' : ' (auto-captured)'}
              </span>
              <a href={snapshotViewUrl(application.id)} target="_blank" rel="noopener noreferrer" className="btn btn-secondary btn-sm">
                View PDF ↗
              </a>
            </>
          ) : (
            <span>No snapshot saved yet.</span>
          )}
        </div>

        {application.snapshotLikelyFaulty && (
          <p className="form-error">
            ⚠️ This auto-captured snapshot looks incomplete or blocked (it may be a robot-check page,
            or very little content came through) — open the job URL yourself and upload a PDF of it
            below instead.
          </p>
        )}

        <div className="snapshot-actions">
          <button type="button" className="btn btn-secondary btn-sm" onClick={handleCaptureSnapshot} disabled={capturingSnapshot || !application.jobUrl}>
            {capturingSnapshot ? 'Capturing…' : application.snapshotAvailable ? 'Re-capture snapshot' : 'Capture snapshot'}
          </button>
          <label className="upload-button btn-sm">
            {uploadingSnapshot ? 'Uploading…' : 'Upload PDF'}
            <input
              ref={fileInputRef}
              type="file"
              accept="application/pdf"
              onChange={handleUploadSnapshot}
              disabled={uploadingSnapshot}
              hidden
            />
          </label>
        </div>
        {!application.jobUrl && <p className="field-hint">Add a job URL below to enable auto-capture.</p>}
        {snapshotMessage && <p className="form-saved">{snapshotMessage}</p>}
      </div>

      <div className="card stage-history">
        <h3>Stage history</h3>
        {history.length === 0 && <p>No history yet.</p>}
        {history.length > 0 && (
          <ul>
            {history.map((entry) => (
              <li key={entry.id}>
                <strong>{formatStage(entry.stage)}</strong> — {new Date(entry.enteredAt).toLocaleString()}
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="card">
        <h3>Details</h3>
        <form onSubmit={handleSubmit} className="form">
          <div className="field-row">
            <label className="field">
              Company *
              <input value={form.company} onChange={(e) => updateField('company', e.target.value)} required />
            </label>
            <label className="field">
              Role *
              <input value={form.role} onChange={(e) => updateField('role', e.target.value)} required />
            </label>
          </div>

          <label className="field">
            Job URL
            <input type="url" value={form.jobUrl} onChange={(e) => updateField('jobUrl', e.target.value)} />
          </label>

          <div className="field-row">
            <label className="field">
              Location
              <input value={form.location} onChange={(e) => updateField('location', e.target.value)} />
            </label>
            <label className="field">
              Date applied
              <input type="date" value={form.dateApplied} onChange={(e) => updateField('dateApplied', e.target.value)} />
            </label>
          </div>

          <label className="field">
            Notes
            <textarea value={form.notes} onChange={(e) => updateField('notes', e.target.value)} rows={3} />
          </label>

          {error && <p className="form-error">{error}</p>}
          {savedAt && !error && <p className="form-saved">Saved.</p>}

          <button type="submit" className="btn btn-primary" disabled={saving} style={{ alignSelf: 'flex-start' }}>
            {saving ? 'Saving…' : 'Save changes'}
          </button>
        </form>
      </div>
    </>
  )
}
