export type StageType =
  | 'NOT_YET_APPLIED'
  | 'APPLIED'
  | 'OA_TEST'
  | 'HIREVUE'
  | 'INTERVIEW'
  | 'FINAL_STAGE'
  | 'OFFER'
  | 'REJECTED'
  | 'WITHDRAWN'

export const STAGE_TYPES: StageType[] = [
  'NOT_YET_APPLIED',
  'APPLIED',
  'OA_TEST',
  'HIREVUE',
  'INTERVIEW',
  'FINAL_STAGE',
  'OFFER',
  'REJECTED',
  'WITHDRAWN',
]

export type OutcomeStatus = 'ACTION_NEEDED' | 'AWAITING_OUTCOME'

export const OUTCOME_TRACKED_STAGES: StageType[] = ['OA_TEST', 'HIREVUE', 'INTERVIEW', 'FINAL_STAGE']

export const STAGE_ACCENT_CLASS: Record<StageType, string> = {
  NOT_YET_APPLIED: 'stage-accent-neutral',
  APPLIED: 'stage-accent-blue',
  OA_TEST: 'stage-accent-violet',
  HIREVUE: 'stage-accent-violet',
  INTERVIEW: 'stage-accent-indigo',
  FINAL_STAGE: 'stage-accent-pink',
  OFFER: 'stage-accent-green',
  REJECTED: 'stage-accent-red',
  WITHDRAWN: 'stage-accent-neutral',
}

const STAGE_LABEL_OVERRIDES: Partial<Record<StageType, string>> = {
  OA_TEST: 'Online Assessment',
  HIREVUE: 'HireVue',
}

export function formatStage(stage: StageType): string {
  if (STAGE_LABEL_OVERRIDES[stage]) return STAGE_LABEL_OVERRIDES[stage]!
  return stage
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

export interface Application {
  id: string
  company: string
  role: string
  jobUrl: string | null
  location: string | null
  dateApplied: string | null
  currentStage: StageType
  currentStageEnteredAt: string
  outcomeStatus: OutcomeStatus | null
  actionDueDate: string | null
  actionNotes: string | null
  actionEmailLink: string | null
  cvSubmitted: boolean
  coverLetterSubmitted: boolean
  applicationAnswersSubmitted: boolean
  notes: string | null
  snapshotAvailable: boolean
  snapshotCapturedAt: string | null
  snapshotManuallyUploaded: boolean
  snapshotLikelyFaulty: boolean
  createdAt: string
  updatedAt: string
}

export const TERMINAL_STAGES: StageType[] = ['REJECTED', 'WITHDRAWN']
export const STALE_THRESHOLD_DAYS = 14

export function isStale(app: Application): boolean {
  if (TERMINAL_STAGES.includes(app.currentStage)) return false
  const enteredAt = new Date(app.currentStageEnteredAt).getTime()
  const ageInDays = (Date.now() - enteredAt) / (1000 * 60 * 60 * 24)
  return ageInDays >= STALE_THRESHOLD_DAYS
}

export function isActionOverdue(app: Application): boolean {
  if (app.outcomeStatus !== 'ACTION_NEEDED' || !app.actionDueDate) return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return new Date(app.actionDueDate).getTime() < today.getTime()
}

export interface NewApplicationInput {
  company: string
  role: string
  jobUrl?: string
  location?: string
  dateApplied?: string
  notes?: string
}

export interface UpdateApplicationInput {
  company?: string
  role?: string
  jobUrl?: string
  location?: string
  dateApplied?: string
  notes?: string
  cvSubmitted?: boolean
  coverLetterSubmitted?: boolean
  applicationAnswersSubmitted?: boolean
  outcomeStatus?: OutcomeStatus
  actionDueDate?: string
  actionNotes?: string
  actionEmailLink?: string
}

export interface StageHistoryEntry {
  id: string
  stage: StageType
  enteredAt: string
}

export interface ApplicationFilters {
  status?: StageType
  company?: string
  search?: string
}
