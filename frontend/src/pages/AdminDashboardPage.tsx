import { useState, useEffect } from 'react'
import { api } from '../api'
import type { DashboardStats } from '../api'
import AdminLayout from '../components/AdminLayout'
import { useNavigate, Link } from 'react-router-dom'

export default function AdminDashboardPage() {
  const [stats, setStats]   = useState<DashboardStats | null>(null)
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    api.adminDashboard()
      .then(setStats)
      .catch(err => {
        if (err.message?.includes('401') || err.message?.includes('Unauthorized')) navigate('/admin/login')
      })
      .finally(() => setLoading(false))
  }, [navigate])

  if (loading) return <AdminLayout><div className="loader-wrap"><div className="loader" /></div></AdminLayout>
  if (!stats)  return <AdminLayout><p>Could not load dashboard.</p></AdminLayout>

  const avgOrder = stats.ordersToday > 0 ? stats.salesToday / stats.ordersToday : 0

  return (
    <AdminLayout>
      <div className="admin-page-header">
        <h1>Dashboard</h1>
        <span className="text-sm text-muted">Live overview</span>
      </div>

      {/* Stat cards */}
      <div className="stat-grid" style={{ marginBottom: 28 }}>
        <div className="stat-card">
          <div className="stat-label">Orders today</div>
          <div className="stat-value">{stats.ordersToday}</div>
        </div>
        <div className={`stat-card${stats.pendingOrders > 0 ? ' alert-med' : ''}`}>
          <div className="stat-label">Pending</div>
          <div className="stat-value">{stats.pendingOrders}</div>
        </div>
        <div className="stat-card accent">
          <div className="stat-label">Revenue today</div>
          <div className="stat-value">${stats.salesToday.toFixed(2)}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Avg order</div>
          <div className="stat-value">${avgOrder.toFixed(2)}</div>
        </div>
      </div>

      {/* Alert row — things that need attention */}
      {(stats.pendingOrders > 0 || stats.lowStockItems > 0 || stats.pendingRestocks > 0) && (
        <div className="card" style={{ marginBottom: 28, background: '#fffbeb', borderColor: '#fde68a' }}>
          <h3 style={{ fontSize: 14, textTransform: 'uppercase', letterSpacing: '.5px', color: '#92400e', marginBottom: 14 }}>
            Needs attention
          </h3>
          <div className="stack stack-sm">
            {stats.pendingOrders > 0 && (
              <div className="cluster cluster-md space-between">
                <span className="text-sm">{stats.pendingOrders} order{stats.pendingOrders !== 1 ? 's' : ''} waiting to be processed</span>
                <Link to="/admin/orders" className="btn btn-amber btn-sm">View orders</Link>
              </div>
            )}
            {stats.lowStockItems > 0 && (
              <div className="cluster cluster-md space-between">
                <span className="text-sm">{stats.lowStockItems} ingredient{stats.lowStockItems !== 1 ? 's' : ''} below restock threshold</span>
                <Link to="/admin/inventory" className="btn btn-secondary btn-sm">View inventory</Link>
              </div>
            )}
            {stats.pendingRestocks > 0 && (
              <div className="cluster cluster-md space-between">
                <span className="text-sm">{stats.pendingRestocks} restock request{stats.pendingRestocks !== 1 ? 's' : ''} pending</span>
                <Link to="/admin/inventory" className="btn btn-secondary btn-sm">View requests</Link>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Quick nav */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 14 }}>
        {[
          { to: '/admin/orders',    title: 'Orders',       desc: 'Manage and process incoming orders' },
          { to: '/admin/products',  title: 'Products',     desc: 'Add, edit, or remove menu items' },
          { to: '/admin/inventory', title: 'Inventory',    desc: 'Track stock levels and suppliers' },
          { to: '/admin/sales',     title: 'Sales Report', desc: 'Revenue breakdown and top performers' },
        ].map(item => (
          <Link
            key={item.to} to={item.to}
            style={{ display: 'block', padding: '18px 20px', background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 'var(--radius-lg)', transition: 'all 0.15s', textDecoration: 'none' }}
            onMouseOver={e => { (e.currentTarget as HTMLElement).style.borderColor = 'var(--coffee-700)' }}
            onMouseOut={e  => { (e.currentTarget as HTMLElement).style.borderColor = 'var(--border)' }}
          >
            <div className="fw-600" style={{ marginBottom: 4 }}>{item.title}</div>
            <div className="text-sm text-muted">{item.desc}</div>
          </Link>
        ))}
      </div>
    </AdminLayout>
  )
}
