import React, { useState, useEffect } from 'react'
import Navbar from './components/Navbar'
import DashboardSection from './components/DashboardSection'
import FailedPaymentsSection from './components/FailedPaymentsSection'
import RecoveryHistorySection from './components/RecoveryHistorySection'
import AuditLogsSection from './components/AuditLogsSection'
import ActionModal from './components/ActionModal'

export default function App() {
  const [activeTab, setActiveTab] = useState('dashboard')
  const [backendStatus, setBackendStatus] = useState('Connecting...')
  
  // Data states
  const [dashboardData, setDashboardData] = useState(null)
  const [failedPayments, setFailedPayments] = useState([])
  const [recoveries, setRecoveries] = useState([])
  const [auditLogs, setAuditLogs] = useState([])

  // Loading states
  const [loadingDashboard, setLoadingDashboard] = useState(false)
  const [loadingPayments, setLoadingPayments] = useState(false)
  const [loadingRecoveries, setLoadingRecoveries] = useState(false)
  const [loadingAudit, setLoadingAudit] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [modalData, setModalData] = useState(null)
  const [activeAction, setActiveAction] = useState(null)

  // API Base URL (uses relative /api which Vite proxies to http://localhost:8080)
  const API_BASE = ''

  // Fetch Dashboard Analytics
  const fetchDashboard = async () => {
    setLoadingDashboard(true)
    try {
      const res = await fetch(`${API_BASE}/api/dashboard/summary`)
      if (!res.ok) throw new Error(`HTTP error ${res.status}`)
      const json = await res.json()
      setDashboardData(json)
      setBackendStatus('Spring Boot API Connected (v1.0.0)')
    } catch (err) {
      console.error('Failed to fetch dashboard summary:', err)
      setBackendStatus('Backend Disconnected')
    } finally {
      setLoadingDashboard(false)
    }
  }

  // Fetch Failed Payments
  const fetchFailedPayments = async () => {
    setLoadingPayments(true)
    try {
      const res = await fetch(`${API_BASE}/api/payments/failed`)
      if (!res.ok) throw new Error(`HTTP error ${res.status}`)
      const json = await res.json()
      setFailedPayments(json)
    } catch (err) {
      console.error('Failed to fetch failed payments:', err)
    } finally {
      setLoadingPayments(false)
    }
  }

  // Fetch Recovery History
  const fetchRecoveries = async () => {
    setLoadingRecoveries(true)
    try {
      const res = await fetch(`${API_BASE}/api/recoveries`)
      if (!res.ok) throw new Error(`HTTP error ${res.status}`)
      const json = await res.json()
      setRecoveries(json)
    } catch (err) {
      console.error('Failed to fetch recoveries:', err)
    } finally {
      setLoadingRecoveries(false)
    }
  }

  // Fetch Audit Logs
  const fetchAuditLogs = async () => {
    setLoadingAudit(true)
    try {
      const res = await fetch(`${API_BASE}/api/audit-logs`)
      if (!res.ok) throw new Error(`HTTP error ${res.status}`)
      const json = await res.json()
      setAuditLogs(json)
    } catch (err) {
      console.error('Failed to fetch audit logs:', err)
    } finally {
      setLoadingAudit(false)
    }
  }

  // Initial Load & Tab Switching refresh
  useEffect(() => {
    fetchDashboard()
    fetchFailedPayments()
    fetchRecoveries()
    fetchAuditLogs()
  }, [])

  useEffect(() => {
    if (activeTab === 'dashboard') fetchDashboard()
    if (activeTab === 'failed') fetchFailedPayments()
    if (activeTab === 'recoveries') fetchRecoveries()
    if (activeTab === 'audit') fetchAuditLogs()
  }, [activeTab])

  // Handle Actions: Analyze, Policy, Recover
  const handleAction = async (paymentId, actionType) => {
    setActionLoading(true)
    setActiveAction(actionType)
    
    // Find current payment details from table
    const currentPayment = failedPayments.find(p => p.paymentId === paymentId) || { paymentId }
    setModalData(currentPayment)
    setIsModalOpen(true)

    try {
      let endpoint = ''
      if (actionType === 'analyze') endpoint = `/api/payments/${paymentId}/analyze`
      if (actionType === 'policy') endpoint = `/api/payments/${paymentId}/policy`
      if (actionType === 'recover') endpoint = `/api/payments/${paymentId}/recover`

      const res = await fetch(`${API_BASE}${endpoint}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      })

      if (!res.ok) throw new Error(`API error ${res.status}`)
      const result = await res.json()

      // Merge response with modal display data
      setModalData(prev => ({
        ...prev,
        ...result,
        paymentId: result.paymentId || paymentId
      }))

      // Refresh all lists to keep metrics updated
      fetchFailedPayments()
      fetchDashboard()
      fetchRecoveries()
      fetchAuditLogs()

    } catch (err) {
      console.error(`Error performing ${actionType} on ${paymentId}:`, err)
      alert(`Action failed: ${err.message}`)
    } finally {
      setActionLoading(false)
    }
  }

  return (
    <div className="app-container">
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        backendStatus={backendStatus}
      />

      <main className="main-content">
        {activeTab === 'dashboard' && (
          <DashboardSection
            data={dashboardData}
            loading={loadingDashboard}
            onRefresh={fetchDashboard}
          />
        )}

        {activeTab === 'failed' && (
          <FailedPaymentsSection
            payments={failedPayments}
            onAction={handleAction}
            loading={actionLoading}
          />
        )}

        {activeTab === 'recoveries' && (
          <RecoveryHistorySection
            recoveries={recoveries}
            loading={loadingRecoveries}
          />
        )}

        {activeTab === 'audit' && (
          <AuditLogsSection
            logs={auditLogs}
            loading={loadingAudit}
          />
        )}
      </main>

      <ActionModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        data={modalData}
        actionType={activeAction}
        loading={actionLoading}
      />

      <footer className="footer">
        PayRecover AI — Built for Razorpay AI Buildathon • Java Spring Boot + React & Vite Architecture
      </footer>
    </div>
  )
}
