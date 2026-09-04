import React from 'react'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend
} from 'chart.js'
import { Doughnut, Bar, Line } from 'react-chartjs-2'
import { DollarSign, CheckCircle2, AlertTriangle, RefreshCw, TrendingUp, CreditCard, Activity } from 'lucide-react'

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend
)

export default function DashboardSection({ data, loading, onRefresh }) {
  if (loading) {
    return (
      <div className="loading-state">
        <div className="spinner"></div>
        <p>Loading Dashboard Analytics...</p>
      </div>
    )
  }

  if (!data || !data.metrics) {
    return (
      <div className="empty-state">
        <p>No dashboard data available from backend API.</p>
        <button className="btn btn-primary" onClick={onRefresh} style={{ marginTop: '1rem' }}>
          Retry Fetching Data
        </button>
      </div>
    )
  }

  const { metrics, failureBreakdown, methodBreakdown, dailyTrend } = data

  // KPI cards definition
  const kpis = [
    {
      title: 'Revenue Recovered',
      value: `₹${Number(metrics.revenueRecovered || 0).toLocaleString('en-IN')}`,
      sub: 'Total value of recovered transactions',
      icon: DollarSign,
      color: '#10b981',
      bg: 'rgba(16, 185, 129, 0.15)'
    },
    {
      title: 'Recovery Rate',
      value: `${(metrics.recoveryRate || 0).toFixed(1)}%`,
      sub: `${metrics.recoveredPayments} recovered of ${metrics.failedPayments} failed`,
      icon: TrendingUp,
      color: '#6366f1',
      bg: 'rgba(99, 102, 241, 0.15)'
    },
    {
      title: 'Recovered Payments',
      value: metrics.recoveredPayments,
      sub: `Across ${metrics.recoveryAttempts} total attempts`,
      icon: CheckCircle2,
      color: '#34d399',
      bg: 'rgba(52, 211, 153, 0.15)'
    },
    {
      title: 'Failed Payments',
      value: metrics.failedPayments,
      sub: `${((metrics.failedPayments / (metrics.totalPayments || 1)) * 100).toFixed(1)}% of total traffic`,
      icon: AlertTriangle,
      color: '#ef4444',
      bg: 'rgba(239, 68, 68, 0.15)'
    },
    {
      title: 'Total Transactions',
      value: metrics.totalPayments,
      sub: `${metrics.successfulPayments} successful initial attempts`,
      icon: CreditCard,
      color: '#3b82f6',
      bg: 'rgba(59, 130, 246, 0.15)'
    }
  ]

  // Chart 1: Failure Code Doughnut Chart
  const failureChartData = {
    labels: (failureBreakdown || []).map(item => item.label),
    datasets: [
      {
        data: (failureBreakdown || []).map(item => item.count),
        backgroundColor: ['#ef4444', '#f59e0b', '#8b5cf6', '#3b82f6', '#ec4899', '#64748b'],
        borderWidth: 1,
        borderColor: '#1e293b'
      }
    ]
  }

  // Chart 2: Method Failure Rate Bar Chart
  const methodChartData = {
    labels: (methodBreakdown || []).map(item => item.method),
    datasets: [
      {
        label: 'Failed Count',
        data: (methodBreakdown || []).map(item => item.failed),
        backgroundColor: 'rgba(239, 68, 68, 0.85)',
        borderRadius: 6
      },
      {
        label: 'Recovered Count',
        data: (methodBreakdown || []).map(item => item.recovered),
        backgroundColor: 'rgba(16, 185, 129, 0.85)',
        borderRadius: 6
      }
    ]
  }

  // Chart 3: Daily Trend Line Chart
  const trendChartData = {
    labels: (dailyTrend || []).map(item => item.date),
    datasets: [
      {
        label: 'Failed Payments',
        data: (dailyTrend || []).map(item => item.failed),
        borderColor: '#ef4444',
        backgroundColor: 'rgba(239, 68, 68, 0.1)',
        tension: 0.3,
        fill: true
      },
      {
        label: 'Recovered Payments',
        data: (dailyTrend || []).map(item => item.recovered),
        borderColor: '#10b981',
        backgroundColor: 'rgba(16, 185, 129, 0.1)',
        tension: 0.3,
        fill: true
      }
    ]
  }

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { labels: { color: '#94a3b8', font: { family: 'Inter', size: 11 } } }
    },
    scales: {
      x: { ticks: { color: '#64748b' }, grid: { color: 'rgba(255,255,255,0.05)' } },
      y: { ticks: { color: '#64748b' }, grid: { color: 'rgba(255,255,255,0.05)' } }
    }
  }

  return (
    <div className="dashboard-section">
      <div className="section-header">
        <div>
          <h1 className="section-title">Executive Recovery Dashboard</h1>
          <p className="section-desc">Real-time payment failure analysis & automated AI revenue recovery metrics</p>
        </div>
        <button className="btn btn-secondary" onClick={onRefresh}>
          <RefreshCw size={14} /> Refresh Data
        </button>
      </div>

      {/* KPI Cards Grid */}
      <div className="kpi-grid">
        {kpis.map((kpi, idx) => {
          const Icon = kpi.icon
          return (
            <div className="kpi-card" key={idx}>
              <div className="kpi-top">
                <span className="kpi-label">{kpi.title}</span>
                <div className="kpi-icon-box" style={{ background: kpi.bg, color: kpi.color }}>
                  <Icon size={18} />
                </div>
              </div>
              <div className="kpi-value">{kpi.value}</div>
              <div className="kpi-subtext">{kpi.sub}</div>
            </div>
          )
        })}
      </div>

      {/* Charts Grid */}
      <div className="charts-grid">
        <div className="chart-card">
          <div className="chart-title">
            <Activity size={16} color="#ef4444" />
            Failure Reason Distribution
          </div>
          <div className="chart-container">
            <Doughnut data={failureChartData} options={{ ...chartOptions, scales: undefined }} />
          </div>
        </div>

        <div className="chart-card">
          <div className="chart-title">
            <CreditCard size={16} color="#3b82f6" />
            Method Failure vs. Recovery
          </div>
          <div className="chart-container">
            <Bar data={methodChartData} options={chartOptions} />
          </div>
        </div>

        <div className="chart-card" style={{ gridColumn: '1 / -1' }}>
          <div className="chart-title">
            <TrendingUp size={16} color="#10b981" />
            7-Day Failure & Recovery Trend
          </div>
          <div className="chart-container">
            <Line data={trendChartData} options={chartOptions} />
          </div>
        </div>
      </div>
    </div>
  )
}
