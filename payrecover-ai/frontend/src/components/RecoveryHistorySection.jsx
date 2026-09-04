import React, { useState } from 'react'
import { RotateCcw, Search, CheckCircle, XCircle } from 'lucide-react'

export default function RecoveryHistorySection({ recoveries, loading }) {
  const [searchTerm, setSearchTerm] = useState('')

  const filtered = (recoveries || []).filter(r =>
    (String(r.id || '')).toLowerCase().includes(searchTerm.toLowerCase()) ||
    (r.paymentId || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (r.finalAction || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (r.outcome || '').toLowerCase().includes(searchTerm.toLowerCase())
  )

  return (
    <div className="recovery-history-section">
      <div className="section-header">
        <div>
          <h1 className="section-title">Recovery Execution History</h1>
          <p className="section-desc">Audit trail of all simulated recovery actions attempted across merchant transactions</p>
        </div>
      </div>

      <div className="table-card">
        <div className="table-toolbar">
          <input
            type="text"
            className="search-box"
            placeholder="Search Action ID, Payment ID, Type..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
          <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            Showing <strong>{filtered.length}</strong> recovery records
          </div>
        </div>

        <div className="data-table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Action ID</th>
                <th>Payment ID</th>
                <th>Action Type</th>
                <th>Execution Status</th>
                <th>Policy Decision</th>
                <th>Amount Recovered</th>
                <th>Timestamp</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="7" style={{ textAlign: 'center', padding: '2rem' }}>
                    <div className="spinner"></div>
                    <p style={{ color: 'var(--text-muted)' }}>Loading Recovery History...</p>
                  </td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan="7" style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
                    No recovery actions recorded yet.
                  </td>
                </tr>
              ) : (
                filtered.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <strong style={{ color: '#f8fafc' }}>{item.id}</strong>
                    </td>
                    <td>
                      <span className="badge badge-purple">{item.paymentId}</span>
                    </td>
                    <td>{item.finalActionLabel || item.finalAction || 'N/A'}</td>
                    <td>
                      <span
                        className={`badge ${
                          item.outcome === 'RECOVERED' ? 'badge-success' : 'badge-danger'
                        }`}
                      >
                        {item.outcome || 'N/A'}
                      </span>
                    </td>
                    <td>
                      <span className="badge badge-secondary">{item.policyDecisionLabel || item.policyDecision || 'N/A'}</span>
                    </td>
                    <td style={{ fontWeight: '600', color: item.outcome === 'RECOVERED' ? '#10b981' : '#64748b' }}>
                      ₹{Number(item.amountRecovered || 0).toLocaleString('en-IN')}
                    </td>
                    <td style={{ color: 'var(--text-dim)', fontSize: '0.8rem' }}>
                      {item.createdAt ? new Date(item.createdAt).toLocaleString() : 'N/A'}
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
