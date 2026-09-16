import { describe, expect, it } from 'vitest'
import { isActionOverdue, isStale, STALE_THRESHOLD_DAYS } from './types'
import type { Application } from './types'

function daysAgo(days: number): string {
  return new Date(Date.now() - days * 24 * 60 * 60 * 1000).toISOString()
}

function baseApplication(overrides: Partial<Application> = {}): Application {
  return {
    id: 'app-1',
    company: 'Acme Corp',
    role: 'Backend Engineer',
    jobUrl: null,
    location: null,
    dateApplied: null,
    currentStage: 'APPLIED',
    currentStageEnteredAt: daysAgo(0),
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
    createdAt: daysAgo(0),
    updatedAt: daysAgo(0),
    ...overrides,
  }
}

describe('isStale', () => {
  it('is not stale when the current stage was entered recently', () => {
    const app = baseApplication({ currentStageEnteredAt: daysAgo(1) })
    expect(isStale(app)).toBe(false)
  })

  it('is stale once the threshold has passed for a non-terminal stage', () => {
    const app = baseApplication({ currentStageEnteredAt: daysAgo(STALE_THRESHOLD_DAYS + 1) })
    expect(isStale(app)).toBe(true)
  })

  it('is never stale for terminal stages like REJECTED', () => {
    const app = baseApplication({
      currentStage: 'REJECTED',
      currentStageEnteredAt: daysAgo(STALE_THRESHOLD_DAYS + 30),
    })
    expect(isStale(app)).toBe(false)
  })

  it('is never stale for terminal stages like WITHDRAWN', () => {
    const app = baseApplication({
      currentStage: 'WITHDRAWN',
      currentStageEnteredAt: daysAgo(STALE_THRESHOLD_DAYS + 30),
    })
    expect(isStale(app)).toBe(false)
  })
})

describe('isActionOverdue', () => {
  it('is false when there is no action due date', () => {
    const app = baseApplication({ outcomeStatus: 'ACTION_NEEDED', actionDueDate: null })
    expect(isActionOverdue(app)).toBe(false)
  })

  it('is false when outcome status is not ACTION_NEEDED, even with a past due date', () => {
    const app = baseApplication({ outcomeStatus: 'AWAITING_OUTCOME', actionDueDate: daysAgo(3).slice(0, 10) })
    expect(isActionOverdue(app)).toBe(false)
  })

  it('is true when the due date is in the past and outcome is ACTION_NEEDED', () => {
    const app = baseApplication({ outcomeStatus: 'ACTION_NEEDED', actionDueDate: daysAgo(3).slice(0, 10) })
    expect(isActionOverdue(app)).toBe(true)
  })

  it('is false when the due date is today or in the future', () => {
    const futureDate = new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
    const app = baseApplication({ outcomeStatus: 'ACTION_NEEDED', actionDueDate: futureDate })
    expect(isActionOverdue(app)).toBe(false)
  })
})
