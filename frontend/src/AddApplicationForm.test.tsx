import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { AddApplicationForm } from './AddApplicationForm'
import * as api from './api'
import type { Application } from './types'

vi.mock('./api')

const mockApplication: Application = {
  id: 'app-1',
  company: 'Acme Corp',
  role: 'Graduate Software Engineer',
  jobUrl: null,
  location: null,
  dateApplied: null,
  currentStage: 'NOT_YET_APPLIED',
  currentStageEnteredAt: new Date().toISOString(),
  outcomeStatus: null,
  actionDueDate: null,
  actionNotes: null,
  actionEmailLink: null,
  cvSubmitted: false,
  coverLetterSubmitted: false,
  applicationAnswersSubmitted: false,
  notes: null,
  snapshotAvailable: false,
  snapshotCapturedAt: null,
  snapshotManuallyUploaded: false,
  snapshotLikelyFaulty: false,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
}

describe('AddApplicationForm', () => {
  it('defaults the role field to Graduate Software Engineer', () => {
    render(<AddApplicationForm onCreated={vi.fn()} />)
    expect(screen.getByLabelText('Role *')).toHaveValue('Graduate Software Engineer')
  })

  it('shows a validation error and does not call the API when company/role are blank', () => {
    const onCreated = vi.fn()
    const { container } = render(<AddApplicationForm onCreated={onCreated} />)

    fireEvent.change(screen.getByLabelText('Role *'), { target: { value: '' } })
    fireEvent.submit(container.querySelector('form')!)

    expect(screen.getByText('Company and role are required.')).toBeInTheDocument()
    expect(api.createApplication).not.toHaveBeenCalled()
    expect(onCreated).not.toHaveBeenCalled()
  })

  it('submits trimmed field values (without a job description field) and calls onCreated on success', async () => {
    vi.mocked(api.createApplication).mockResolvedValue(mockApplication)
    const onCreated = vi.fn()
    const user = userEvent.setup()

    render(<AddApplicationForm onCreated={onCreated} />)

    await user.type(screen.getByLabelText('Company *'), '  Acme Corp  ')
    await user.click(screen.getByRole('button', { name: 'Add application' }))

    await waitFor(() => expect(onCreated).toHaveBeenCalledWith(mockApplication))

    expect(api.createApplication).toHaveBeenCalledWith({
      company: 'Acme Corp',
      role: 'Graduate Software Engineer',
      jobUrl: undefined,
      location: undefined,
      dateApplied: undefined,
      notes: undefined,
    })
    expect(screen.queryByLabelText('Job description')).not.toBeInTheDocument()
  })
})
