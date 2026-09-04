import React, { useState } from 'react'
import { FileText, Search, Shield, Cpu, PlayCircle } from 'lucide-react'

export default function AuditLogsSection({ logs, loading }) {
  const [searchTerm, setSearchTerm] = useState('')

  const filtered = (logs || []).filter(l =>
    (l.paymentId || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (l.eventType || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (l.actionSource || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (l.details || '').toLowerCase().includes(searchTerm.toLowerCase())
  )

  const getSourceBadge = (source) => {
    if (source === 'AI_LLM' || source === 'AI_SERVICE') return <span className="badge badge-purple">AI LLM</span>
    if (source === 'POLICY_ENGINE') return <span className="badge badge-info">POLICY ENGINE</span>
    if (source === 'RECOVERY_SIMULATOR') return <span className="badge badge-success">SIMULATOR</span>
    return <span className="badge badge-secondary">{source || 'SYSTEM'}</span>
  }

  return (
    <div className="audit-logs-section">
      <div className="section-header">
        <div>
          <h1 className="section-title">Compliance Audit Trail</h1>
          <p className="section-desc">Immutable timeline of system state changes, AI decisions, and policy guardrail checks</p>
        </div>
      </div>

      <div className="table-card">
        <div className="table-toolbar">
          <input
            type="text"
            className="search-box"
            placeholder="Search Payment ID, Event, Details..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
          <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            Showing <strong>{filtered.length}</strong> audit log entries
          </div>
        </div>

        <div className="data-table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Log ID</th>
                <th>Timestamp</th>
                <th>Payment ID</th>
                <th>Event Type</th>
                <th>Source Component</th>
                <th>Audit Details & Parameters</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="6" style={{ textAlign: 'center', padding: '2rem' }}>
                    <div className="spinner"></div>
                    <p style={{ color: 'var(--text-muted)' }}>Loading Audit Trail...</p>
                  </td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan="6" style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
                    No audit log entries recorded yet.
                  </td>
                </tr>
              ) : (
                filtered.map((log) => (
                  <tr key={log.logId || log.id}>
                    <td>
                      <span style={{ fontFamily: 'monospace', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                        #{log.logId || log.id}
                      </span>
                    </td>
                    <td style={{ color: 'var(--text-dim)', fontSize: '0.8rem', whiteSpace: 'nowrap' }}>
                      {log.createdAt ? new Date(log.createdAt).toLocaleString() : 'N/A'}
                    </td>
                    <td>
                      <span className="badge badge-secondary">{log.paymentId || 'GLOBAL'}</span>
                    </td>
                    <td style={{ fontWeight: '600' }}>{log.eventType}</td>
                    <td>{getSourceBadge(log.actionSource)}</td>
                    <td style={{ fontSize: '0.85rem', color: 'var(--text-main)', maxWidth: '400px' }}>
                      <div
                        style={{
                          background: 'rgba(15, 23, 42, 0.6)',
                          padding: '0.4rem 0.6rem',
                          borderRadius: '6px',
                          border: '1px solid var(--border-color)',
                          whiteSpace: 'pre-wrap',
                          wordBreak: 'break-word',
                          fontFamily: 'monospace',
                          fontSize: '0.775rem'
                        }}
                      >
                        {log.details}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
