import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { listApplications } from '../api'
import { STAGE_ACCENT_CLASS, STAGE_TYPES, formatStage, isActionOverdue, isStale } from '../types'
import type { Application, StageType } from '../types'

export function ApplicationListPage() {
  const navigate = useNavigate()
  const [applications, setApplications] = useState<Application[]>([])
  const [loading, setLoading] = useState(true)
  const [hasLoadedOnce, setHasLoadedOnce] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<StageType | ''>('')
  const [actionOnly, setActionOnly] = useState(false)

  useEffect(() => {
    const timeout = setTimeout(() => setSearch(searchInput), 300)
    return () => clearTimeout(timeout)
  }, [searchInput])

  function refresh() {
    setLoading(true)
    setError(null)
    listApplications({ search: search || undefined, status: status || undefined })
      .then((data) => {
        setApplications(data)
        setHasLoadedOnce(true)
      })
      .catch(() => setError('Could not load applications. Is the backend running?'))
      .finally(() => setLoading(false))
  }

  useEffect(refresh, [search, status])

  const filtersActive = search.trim() !== '' || status !== '' || actionOnly

  const visibleApplications = actionOnly
    ? applications.filter((app) => app.outcomeStatus === 'ACTION_NEEDED')
    : applications

  const grouped = STAGE_TYPES.map((stage) => ({
    stage,
    items: visibleApplications.filter((app) => app.currentStage === stage),
  })).filter((group) => group.items.length > 0)

  return (
    <>
      <div className="page-header">
        <h1>Applications</h1>
        {hasLoadedOnce && !error && <span className="stage-group__count">{visibleApplications.length} total</span>}
      </div>

      <div className="filters">
        <input
          type="search"
          placeholder="Search company, role, location…"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
        />
        <select value={status} onChange={(e) => setStatus(e.target.value as StageType | '')}>
          <option value="">All stages</option>
          {STAGE_TYPES.map((stage) => (
            <option key={stage} value={stage}>
              {formatStage(stage)}
            </option>
          ))}
        </select>
        <button
          type="button"
          className={`filter-toggle ${actionOnly ? 'active' : ''}`}
          onClick={() => setActionOnly((prev) => !prev)}
        >
          Action needed only
        </button>
      </div>

      {!hasLoadedOnce && loading && <p>Loading…</p>}
      {error && <p className="form-error">{error}</p>}

      {hasLoadedOnce && !error && visibleApplications.length === 0 && !filtersActive && (
        <div className="empty-state">
          No applications yet — <Link to="/add">add the first one</Link> you're applying to.
        </div>
      )}

      {hasLoadedOnce && !error && visibleApplications.length === 0 && filtersActive && (
        <div className="empty-state">No applications match your search or filter.</div>
      )}

      {hasLoadedOnce && !error && visibleApplications.length > 0 && (
        <div className="application-list">
          {grouped.map((group) => (
            <div key={group.stage} className="stage-group">
              <div className={`stage-group__header ${STAGE_ACCENT_CLASS[group.stage]}`}>
                <h3>{formatStage(group.stage)}</h3>
                <span className="stage-group__count">{group.items.length}</span>
              </div>
              <table>
                <thead>
                  <tr>
                    <th>Company</th>
                    <th>Role</th>
                    <th>Location</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {group.items.map((app) => (
                    <tr
                      key={app.id}
                      className="app-row"
                      onClick={() => navigate(`/applications/${app.id}`)}
                    >
                      <td>
                        <Link
                          to={`/applications/${app.id}`}
                          className="row-link"
                          onClick={(e) => e.stopPropagation()}
                        >
                          {app.company}
                        </Link>
                      </td>
                      <td>{app.role}</td>
                      <td>{app.location ?? '—'}</td>
                      <td>
                        <div className="row-meta">
                          {app.outcomeStatus === 'ACTION_NEEDED' && (
                            <span
                              className={`badge badge-action-needed ${isActionOverdue(app) ? 'badge-action-needed--overdue' : ''}`}
                            >
                              {app.actionDueDate ? (
                                <>
                                  Due
                                  <span className="badge-action-needed__date">
                                    {new Date(app.actionDueDate).toLocaleDateString(undefined, { day: 'numeric', month: 'short' })}
                                  </span>
                                </>
                              ) : (
                                'Action needed'
                              )}
                            </span>
                          )}
                          {app.outcomeStatus === 'AWAITING_OUTCOME' && (
                            <span className="badge badge-awaiting">Awaiting outcome</span>
                          )}
                          {app.outcomeStatus === 'ACTION_NEEDED' && app.actionEmailLink && (
                            <a
                              href={app.actionEmailLink}
                              target="_blank"
                              rel="noreferrer"
                              className="row-action-detail__email"
                              onClick={(e) => e.stopPropagation()}
                            >
                              Open email
                            </a>
                          )}
                          {isStale(app) && <span className="badge badge-stale">Stale</span>}
                        </div>
                        {app.outcomeStatus === 'ACTION_NEEDED' && app.actionNotes && (
                          <div className="row-action-detail">
                            <span className="row-action-detail__notes">{app.actionNotes}</span>
                          </div>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ))}
        </div>
      )}
    </>
  )
}
