import type {
  Application,
  ApplicationFilters,
  NewApplicationInput,
  StageHistoryEntry,
  StageType,
  UpdateApplicationInput,
} from './types'

const BASE_URL = '/api/applications'

export async function listApplications(filters: ApplicationFilters = {}): Promise<Application[]> {
  const params = new URLSearchParams()
  if (filters.status) params.set('status', filters.status)
  if (filters.company) params.set('company', filters.company)
  if (filters.search) params.set('search', filters.search)

  const query = params.toString()
  const response = await fetch(query ? `${BASE_URL}?${query}` : BASE_URL)
  if (!response.ok) {
    throw new Error(`Failed to list applications: ${response.status}`)
  }
  return response.json()
}

export async function getApplication(id: string): Promise<Application> {
  const response = await fetch(`${BASE_URL}/${id}`)
  if (!response.ok) {
    throw new Error(`Failed to load application: ${response.status}`)
  }
  return response.json()
}

export async function createApplication(input: NewApplicationInput): Promise<Application> {
  const response = await fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

  if (!response.ok) {
    throw new Error(`Failed to create application: ${response.status}`)
  }

  return response.json()
}

export async function updateApplication(id: string, input: UpdateApplicationInput): Promise<Application> {
  const response = await fetch(`${BASE_URL}/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })

  if (!response.ok) {
    throw new Error(`Failed to update application: ${response.status}`)
  }

  return response.json()
}

export async function transitionStage(id: string, newStage: StageType): Promise<Application> {
  const response = await fetch(`${BASE_URL}/${id}/stage`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ newStage }),
  })

  if (!response.ok) {
    throw new Error(`Failed to change stage: ${response.status}`)
  }

  return response.json()
}

export async function getStageHistory(id: string): Promise<StageHistoryEntry[]> {
  const response = await fetch(`${BASE_URL}/${id}/history`)
  if (!response.ok) {
    throw new Error(`Failed to load stage history: ${response.status}`)
  }
  return response.json()
}

export async function captureSnapshot(id: string): Promise<void> {
  const response = await fetch(`${BASE_URL}/${id}/snapshot`, { method: 'POST' })
  if (!response.ok) {
    throw new Error(`Failed to request snapshot: ${response.status}`)
  }
}

export function snapshotViewUrl(id: string): string {
  return `${BASE_URL}/${id}/snapshot`
}

export async function uploadSnapshot(id: string, file: File): Promise<Application> {
  const formData = new FormData()
  formData.append('file', file)

  const response = await fetch(`${BASE_URL}/${id}/snapshot/upload`, {
    method: 'POST',
    body: formData,
  })

  if (!response.ok) {
    throw new Error(`Failed to upload snapshot: ${response.status}`)
  }

  return response.json()
}
