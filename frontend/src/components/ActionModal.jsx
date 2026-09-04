import React from 'react'
import { X, Sparkles, ShieldCheck, Play, CheckCircle2, AlertTriangle, XCircle, ArrowRight } from 'lucide-react'

export default function ActionModal({ isOpen, onClose, data, actionType, loading }) {
  if (!isOpen || !data) return null

  // Calculate timeline state
  const isDiagnosed = !!data.analyzed || actionType === 'analyze' || actionType === 'policy' || actionType === 'recover'
  const isPolicyChecked = !!data.policyDecision || actionType === 'policy' || actionType === 'recover'
  const isExecuted = !!data.simulatedOutcome || actionType === 'recover'
 const isSuccess = data.simulatedOutcome === 'RECOVERED' || data.finalStatus === 'RECOVERED'
  const isBlocked = data.policyDecision === 'BLOCKED'

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title">
            <Sparkles size={18} color="#8b5cf6" />
            Payment Diagnostics & Recovery Console
          </div>
          <button className="close-btn" onClick={onClose}>
            <X size={20} />
          </button>
        </div>

        <div className="modal-body">
          {loading ? (
            <div className="loading-state">
              <div className="spinner"></div>
              <p>Executing {actionType?.toUpperCase()} operation on Spring Boot backend...</p>
            </div>
          ) : (
            <>
              {/* 5-Step Visual Recovery Timeline */}
              <div className="timeline-container">
                {/* Step 1: Failed */}
                <div className="timeline-step completed">
                  <div className="step-node">1</div>
                  <div className="step-label">Failed</div>
                </div>

                {/* Step 2: Diagnosed */}
                <div className={`timeline-step ${isDiagnosed ? 'completed' : ''}`}>
                  <div className="step-node">2</div>
                  <div className="step-label">AI Diagnosis</div>
                </div>

                {/* Step 3: Policy Check */}
                <div className={`timeline-step ${isPolicyChecked ? (isBlocked ? 'blocked' : 'completed') : ''}`}>
                  <div className="step-node">3</div>
                  <div className="step-label">Policy Engine</div>
                </div>

                {/* Step 4: Execution */}
                <div className={`timeline-step ${isExecuted ? 'completed' : ''}`}>
                  <div className="step-node">4</div>
                  <div className="step-label">Simulation</div>
                </div>

                {/* Step 5: Final Result */}
                <div className={`timeline-step ${isExecuted ? (isSuccess ? 'success' : 'blocked') : ''}`}>
                  <div className="step-node">5</div>
                  <div className="step-label">{isSuccess ? 'Recovered' : 'Final Status'}</div>
                </div>
              </div>

              {/* Transaction Basic Details */}
              <div className="info-block">
                <div className="info-block-title">Payment Overview</div>
                <div className="info-row">
                  <span className="info-label">Payment ID:</span>
                  <span className="info-val">{data.paymentId}</span>
                </div>
                <div className="info-row">
                  <span className="info-label">Amount:</span>
                  <span className="info-val">₹{Number(data.amount || 0).toLocaleString('en-IN')}</span>
                </div>
                <div className="info-row">
                  <span className="info-label">Provider & Method:</span>
                  <span className="info-val">{data.provider} ({data.paymentMethod})</span>
                </div>
                <div className="info-row">
                  <span className="info-label">Attempts Made:</span>
                  <span className="info-val">{data.attempts}</span>
                </div>
              </div>

              {/* AI Diagnosis Block */}
              {isDiagnosed && (
                <div className="info-block" style={{ borderLeft: '4px solid var(--purple)' }}>
                  <div className="info-block-title" style={{ color: 'var(--purple)' }}>
                    <Sparkles size={14} /> AI Failure Diagnosis
                  </div>
                  <div className="info-row">
                    <span className="info-label">Category:</span>
                    <span className="info-val">{data.failureCategoryLabel || data.failureCategory || 'Analysed'}</span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">AI Recommended Action:</span>
                    <span className="info-val" style={{ color: '#a855f7' }}>
                      {data.aiRecommendationLabel || data.aiRecommendation}
                    </span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">Confidence & Engine:</span>
                    <span className="info-val">{data.confidencePercent}% confidence via {data.aiSource}</span>
                  </div>
                </div>
              )}

              {/* Policy Decision Guardrails Block */}
              {isPolicyChecked && (
                <div
                  className="info-block"
                  style={{
                    borderLeft: `4px solid ${
                      data.policyDecision === 'ALLOWED' ? '#10b981' : data.policyDecision === 'BLOCKED' ? '#ef4444' : '#f59e0b'
                    }`
                  }}
                >
                  <div
                    className="info-block-title"
                    style={{
                      color:
                        data.policyDecision === 'ALLOWED' ? '#10b981' : data.policyDecision === 'BLOCKED' ? '#ef4444' : '#f59e0b'
                    }}
                  >
                    <ShieldCheck size={14} /> Policy Engine Guardrail Check
                  </div>
                  <div className="info-row">
                    <span className="info-label">Verdict:</span>
                    <span
                      className={`badge ${
                        data.policyDecision === 'ALLOWED'
                          ? 'badge-success'
                          : data.policyDecision === 'BLOCKED'
                          ? 'badge-danger'
                          : 'badge-warning'
                      }`}
                    >
                      {data.policyDecisionLabel || data.policyDecision}
                    </span>
                  </div>
                  <div className="info-row">
                    <span className="info-label">Approved Action:</span>
                    <span className="info-val">{data.finalActionLabel || data.finalAction}</span>
                  </div>
                  <div className="info-row" style={{ flexDirection: 'column', gap: '0.2rem', marginTop: '0.4rem' }}>
                    <span className="info-label">Policy Evaluation Reason:</span>
                    <div
                      style={{
                        fontSize: '0.85rem',
                        color: 'var(--text-main)',
                        background: 'rgba(0,0,0,0.3)',
                        padding: '0.5rem',
                        borderRadius: '6px'
                      }}
                    >
                      {data.policyReason || 'Evaluated against safety limits (Attempts <= 2, Amount <= ₹10k, Confidence >= 60%)'}
                    </div>
                  </div>
                </div>
              )}

              {/* Recovery Simulation Result Block */}
              {isExecuted && (
                <div
                  className="info-block"
                  style={{
                    borderLeft: `4px solid ${isSuccess ? '#10b981' : '#ef4444'}`
                  }}
                >
                  <div className="info-block-title" style={{ color: isSuccess ? '#10b981' : '#ef4444' }}>
                    <Play size={14} /> Recovery Simulator Outcome
                  </div>
                  <div className="info-row">
                    <span className="info-label">Execution Result:</span>
                    <span className={`badge ${isSuccess ? 'badge-success' : 'badge-danger'}`}>
                     {data.simulatedOutcome || data.finalStatus || 'N/A'}
                    </span>
                  </div>
                  {isSuccess && (
                    <div className="info-row">
                      <span className="info-label">Recovered Amount:</span>
                      <span className="info-val" style={{ color: '#10b981', fontSize: '1.1rem' }}>
                        ₹{Number(data.amountRecovered || data.amount || 0).toLocaleString('en-IN')}
                      </span>
                    </div>
                  )}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  )
}
