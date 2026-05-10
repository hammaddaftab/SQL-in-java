import { useState, useEffect, useCallback } from 'react'
import { api } from '../api'
import type { AdminOrder } from '../api'
import AdminLayout from '../components/AdminLayout'
import { useNavigate } from 'react-router-dom'

const STATUS_BADGE: Record<string, string> = {
  pending: 'badge-pending', confirmed: 'badge-confirmed', completed: 'badge-completed',
}

const FILTERS = ['', 'pending', 'confirmed', 'completed'] as const

export default function AdminOrdersPage() {
  const [orders, setOrders]   = useState<AdminOrder[]>([])
  const [filter, setFilter]   = useState<string>('')
  const [loading, setLoading] = useState(true)
  const [actionErr, setActionErr] = useState<Record<number, string>>({})
  const navigate = useNavigate()

  const loadOrders = useCallback(() => {
    setLoading(true)
    api.adminGetOrders(filter || undefined)
      .then(r => setOrders(r.orders))
      .catch(err => { if (err.message?.includes('401')) navigate('/admin/login') })
      .finally(() => setLoading(false))
  }, [filter, navigate])

  useEffect(() => { loadOrders() }, [loadOrders])

  const act = async (id: number, fn: () => Promise<unknown>) => {
    setActionErr(prev => ({ ...prev, [id]: '' }))
    try { await fn(); loadOrders() }
    catch { setActionErr(prev => ({ ...prev, [id]: 'Action failed' })) }
  }

  return (
    <AdminLayout>
      <div className="admin-page-header">
        <h1>Orders</h1>
        <div className="cluster cluster-sm">
          {FILTERS.map(f => (
            <button
              key={f} onClick={() => setFilter(f)}
              className={`btn btn-sm ${filter === f ? 'btn-primary' : 'btn-ghost'}`}
            >
              {f === '' ? 'All' : f.charAt(0).toUpperCase() + f.slice(1)}
            </button>
          ))}
        </div>
      </div>

      <div className="card card-flush">
        <table className="table">
          <thead>
            <tr>
              <th>#</th>
              <th>Time</th>
              <th>Customer</th>
              <th>Type</th>
              <th>Payment</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7}><div className="loader-wrap"><div className="loader" /></div></td></tr>
            ) : orders.length === 0 ? (
              <tr><td colSpan={7}><div className="empty-state">No orders found.</div></td></tr>
            ) : orders.map(o => {
              const t = new Date(o.time * 1000)
              return (
                <tr key={o.orderID}>
                  <td className="fw-600 text-sm">#{o.orderID}</td>
                  <td className="text-sm text-muted">
                    {t.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    <br />
                    <span style={{ fontSize: 11 }}>{t.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}</span>
                  </td>
                  <td className="fw-500">{o.customerName}</td>
                  <td className="text-sm">
                    {o.isOnline ? <span className="badge badge-confirmed">Online</span>
                      : o.tableID ? `Table ${o.tableID}` : 'Takeaway'}
                  </td>
                  <td className="text-sm" style={{ textTransform: 'capitalize' }}>{o.paymentMethod}</td>
                  <td><span className={`badge ${STATUS_BADGE[o.status] ?? 'badge-neutral'}`}>{o.status}</span></td>
                  <td>
                    <div className="cluster cluster-sm">
                      {actionErr[o.orderID] && (
                        <span className="text-sm" style={{ color: 'var(--danger)' }}>{actionErr[o.orderID]}</span>
                      )}
                      {o.status === 'pending' && o.isOnline && (
                        <button className="btn btn-secondary btn-sm" onClick={() => act(o.orderID, () => api.adminConfirmOrder(o.orderID))}>
                          Confirm
                        </button>
                      )}
                      {(o.status === 'pending' && !o.isOnline) || o.status === 'confirmed' ? (
                        <button className="btn btn-primary btn-sm" onClick={() => act(o.orderID, () => api.adminUpdateOrderStatus(o.orderID, 'completed'))}>
                          Complete
                        </button>
                      ) : null}
                    </div>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </AdminLayout>
  )
}
