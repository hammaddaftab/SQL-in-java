import { useState, useEffect } from 'react'
import { api } from '../api'
import type { SalesReport } from '../api'
import AdminLayout from '../components/AdminLayout'
import { useNavigate } from 'react-router-dom'

export default function AdminSalesPage() {
  const [report, setReport]   = useState<SalesReport | null>(null)
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    api.adminSales()
      .then(setReport)
      .catch(err => { if (err.message?.includes('401')) navigate('/admin/login') })
      .finally(() => setLoading(false))
  }, [navigate])

  if (loading) return <AdminLayout><div className="loader-wrap"><div className="loader" /></div></AdminLayout>
  if (!report)  return <AdminLayout><p>Could not load sales data.</p></AdminLayout>

  const totalOrders  = report.ordersToday
  const avgOrder     = totalOrders > 0 ? report.salesToday / totalOrders : 0
  const completedPct = totalOrders > 0 ? Math.round((report.completedOrders / totalOrders) * 100) : 0

  return (
    <AdminLayout>
      <div className="admin-page-header">
        <h1>Sales Report</h1>
        <span className="text-sm text-muted">Today's snapshot</span>
      </div>

      {/* Key metrics */}
      <div className="stat-grid" style={{ marginBottom: 32 }}>
        <div className="stat-card accent">
          <div className="stat-label">Total revenue</div>
          <div className="stat-value">${report.salesToday.toFixed(2)}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Avg order value</div>
          <div className="stat-value">${avgOrder.toFixed(2)}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Orders placed</div>
          <div className="stat-value">{totalOrders}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Completion rate</div>
          <div className="stat-value">{completedPct}%</div>
        </div>
      </div>

      {/* Order breakdown bar */}
      {totalOrders > 0 && (
        <div className="card" style={{ marginBottom: 28 }}>
          <h3 style={{ fontSize: 15, marginBottom: 16 }}>Order breakdown</h3>
          <div style={{ display: 'flex', gap: 24, marginBottom: 14 }}>
            {[
              { label: 'Completed', count: report.completedOrders, color: 'var(--success)' },
              { label: 'Pending',   count: report.pendingOrders,   color: 'var(--warning)' },
              { label: 'Other',     count: Math.max(0, totalOrders - report.completedOrders - report.pendingOrders), color: 'var(--cream-400)' },
            ].map(seg => (
              <div key={seg.label} className="cluster cluster-sm">
                <div style={{ width: 10, height: 10, borderRadius: '50%', background: seg.color, flexShrink: 0 }} />
                <span className="text-sm"><span className="fw-600">{seg.count}</span> {seg.label}</span>
              </div>
            ))}
          </div>
          {/* Progress bar */}
          <div style={{ height: 8, borderRadius: 99, background: 'var(--cream-300)', overflow: 'hidden', display: 'flex' }}>
            <div style={{ width: `${(report.completedOrders / totalOrders) * 100}%`, background: 'var(--success)' }} />
            <div style={{ width: `${(report.pendingOrders / totalOrders) * 100}%`, background: 'var(--warning)' }} />
          </div>
        </div>
      )}

      {/* Top products */}
      <div className="card card-flush">
        <div className="card-header">
          <h3>Top products by revenue</h3>
        </div>
        <table className="table">
          <thead>
            <tr>
              <th style={{ width: 40 }}>#</th>
              <th>Product</th>
              <th>Units sold</th>
              <th>Unit price</th>
              <th>Revenue</th>
            </tr>
          </thead>
          <tbody>
            {report.topProducts.length === 0 ? (
              <tr><td colSpan={5}><div className="empty-state">No sales data yet.</div></td></tr>
            ) : report.topProducts.map((p, i) => {
              const revenue = p.totalQuantitySold * p.price
              const maxRev  = report.topProducts[0] ? report.topProducts[0].totalQuantitySold * report.topProducts[0].price : 1
              const barPct  = Math.round((revenue / maxRev) * 100)
              return (
                <tr key={p.productID}>
                  <td className="text-muted fw-600 text-sm">#{i + 1}</td>
                  <td>
                    <div className="fw-500">{p.productName}</div>
                    <div style={{ height: 3, borderRadius: 99, background: 'var(--cream-300)', marginTop: 5, width: '90%' }}>
                      <div style={{ height: '100%', borderRadius: 99, background: 'var(--amber-500)', width: `${barPct}%` }} />
                    </div>
                  </td>
                  <td>{p.totalQuantitySold}</td>
                  <td className="text-muted">${p.price.toFixed(2)}</td>
                  <td className="fw-600" style={{ color: 'var(--success)' }}>${revenue.toFixed(2)}</td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </AdminLayout>
  )
}
