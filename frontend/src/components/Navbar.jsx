import React from 'react'
import { LayoutDashboard, AlertCircle, RotateCcw, FileText, Zap } from 'lucide-react'

export default function Navbar({ activeTab, setActiveTab, backendStatus }) {
  const tabs = [
    { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { id: 'failed', label: 'Failed Payments', icon: AlertCircle },
    { id: 'recoveries', label: 'Recovery History', icon: RotateCcw },
    { id: 'audit', label: 'Audit Logs', icon: FileText }
  ]

  return (
    <header className="navbar">
      <div className="nav-brand">
        <div className="brand-icon">
          <Zap size={22} />
        </div>
        <div>
          <div className="brand-title">PayRecover AI</div>
          <div className="brand-subtitle">Revenue Recovery Platform</div>
        </div>
      </div>

      <nav className="nav-tabs">
        {tabs.map((tab) => {
          const Icon = tab.icon
          return (
            <button
              key={tab.id}
              className={`nav-tab ${activeTab === tab.id ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.id)}
            >
              <Icon size={16} />
              {tab.label}
            </button>
          )
        })}
      </nav>

      <div className="header-status">
        <span className="status-dot"></span>
        <span>{backendStatus || 'Spring Boot API Connected'}</span>
      </div>
    </header>
  )
}
