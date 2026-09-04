import React, { useState } from 'react'
import { Sparkles, ShieldCheck, Play, Search, AlertCircle, ArrowUpRight } from 'lucide-react'

export default function FailedPaymentsSection({ payments, onAction, loading }) {
  const [searchTerm, setSearchTerm] = useState('')

  const filtered = (payments || []).filter(p =>
    (p.paymentId || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (p.customerId || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (p.failureCode || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (p.paymentMethod || '').toLowerCase().includes(searchTerm.toLowerCase())
  )

  const getPolicyBadge = (decision) => {
    if (decision === 'ALLOWED') return <span className="badge badge-success">ALLOWED</span>
    if (decision === 'BLOCKED') return <span className="badge badge-danger">BLOCKED</span>
    if (decision === 'ESCALATED') return <span className="badge badge-warning">ESCALATED</span>
    return <span className="badge badge-secondary">UNCHECKED</span>
  }

  const getOutcomeBadge = (outcome, status) => {
    if (outcome === 'RECOVERED' || status === 'RECOVERED') return <span className="badge badge-success">RECOVERED</span>
    if (outcome === 'FAILED_AGAIN' || status === 'FAILED') return <span className="badge badge-danger">FAILED</span>
    if (outcome === 'NOT_ATTEMPTED') return <span className="badge badge-secondary">NOT ATTEMPTED</span>
    if (outcome === 'ESCALATED') return <span className="badge badge-warning">ESCALATED</span>
    return <span className="badge badge-secondary">{status || 'FAILED'}</span>
  }

  return (
    <div className="failed-payments-section">
      <div className="section-header">
        <div>
          <h1 className="section-title">Failed Payments Queue</h1>
          <p className="section-desc">Analyze failures, evaluate safety policies, and execute smart automated recoveries</p>
        </div>
      </div>

      <div className="table-card">
        <div className="table-toolbar">
          <input
            type="text"
            className="search-box"
            placeholder="Search Payment ID, Code, Method..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
          <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            Showing <strong>{filtered.length}</strong> failed transactions
          </div>
        </div>

        <div className="data-table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Payment ID</th>
                <th>Amount</th>
                <th>Method</th>
                <th>Failure Code</th>
                <th>Attempts</th>
                <th>AI Category</th>
                <th>Policy Verdict</th>
                <th>Status</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan="9" style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
                    No failed payments found matching your filter.
                  </td>
                </tr>
              ) : (
                filtered.map((item) => (
                  <tr key={item.paymentId}>
                    <td>
                      <strong style={{ color: '#f8fafc' }}>{item.paymentId}</strong>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>{item.customerId}</div>
                    </td>
                    <td style={{ fontWeight: '600' }}>
                      ₹{Number(item.amount || 0).toLocaleString('en-IN')}
                    </td>
                    <td>{item.paymentMethod}</td>
                    <td>
                      <span className="badge badge-purple">{item.failureCode}</span>
                    </td>
                    <td style={{ textAlign: 'center', fontWeight: 'bold' }}>{item.attempts}</td>
                    <td>
                      {item.analyzed ? (
                        <div>
                          <span className="badge badge-info">{item.failureCategoryLabel || item.failureCategory}</span>
                          <div style={{ fontSize: '0.725rem', color: 'var(--text-dim)', marginTop: '2px' }}>
                            Conf: {item.confidencePercent}% ({item.aiSource})
                          </div>
                        </div>
                      ) : (
                        <span className="badge badge-secondary">Not Analyzed</span>
                      )}
                    </td>
                    <td>{getPolicyBadge(item.policyDecision)}</td>
                    <td>{getOutcomeBadge(item.recoveryOutcome, item.status)}</td>
                    <td style={{ textAlign: 'right' }}>
                      <div className="action-btn-group" style={{ justifyContent: 'flex-end' }}>
                        {/* 1. Analyze Button */}
                        <button
                          className="btn btn-purple"
                          title="Run AI Failure Diagnosis"
                          disabled={loading}
                          onClick={() => onAction(item.paymentId, 'analyze')}
                        >
                          <Sparkles size={13} /> Analyze
                        </button>

                        {/* 2. Policy Button */}
                        <button
                          className="btn btn-primary"
                          title="Evaluate Deterministic Policy Rules"
                          disabled={loading || !item.analyzed}
                          onClick={() => onAction(item.paymentId, 'policy')}
                        >
                          <ShieldCheck size={13} /> Policy
                        </button>

                        {/* 3. Recover Button */}
                        <button
                          className="btn btn-success"
                          title="Simulate Recovery Execution"
                          disabled={loading || !item.policyDecision}
                          onClick={() => onAction(item.paymentId, 'recover')}
                        >
                          <Play size={13} /> Recover
                        </button>
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
