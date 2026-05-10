import { useState, useEffect } from 'react'
import { api } from '../api'
import type { Order } from '../api'
import CustomerLayout from '../components/CustomerLayout'
import { useNavigate } from 'react-router-dom'

function statusBadge(s: string) {
  const cls: Record<string, string> = {
    pending: 'badge-pending', confirmed: 'badge-confirmed', completed: 'badge-completed',
  }
  return <span className={`badge ${cls[s] ?? 'badge-neutral'}`}>{s}</span>
}

export default function OrderHistoryPage() {
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)
  const navigate    = useNavigate()
  const customerID  = localStorage.getItem('customerID')
  const firstName   = localStorage.getItem('firstName')

  useEffect(() => {
    if (!customerID) { setLoading(false); return }
    api.getOrders(parseInt(customerID, 10))
      .then(r => setOrders(r.orders))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [customerID])

  if (!customerID) return (
    <CustomerLayout>
      <div className="empty-state" style={{ marginTop: 60 }}>
        <p style={{ marginBottom: 16 }}>Register on the menu page to start tracking your orders.</p>
        <button className="btn btn-primary" onClick={() => navigate('/')}>Go to Menu</button>
      </div>
    </CustomerLayout>
  )

  if (loading) return (
    <CustomerLayout><div className="loader-wrap"><div className="loader" /></div></CustomerLayout>
  )

  return (
    <CustomerLayout>
      <h1 style={{ marginBottom: 6, fontSize: 28 }}>
        {firstName ? `${firstName}'s orders` : 'Your orders'}
      </h1>
      <p className="text-muted text-sm" style={{ marginBottom: 28 }}>
        Tap any order to see what's in it.
      </p>

      {orders.length === 0 ? (
        <div className="empty-state">
          <p style={{ marginBottom: 16 }}>You haven't placed any orders yet.</p>
          <button className="btn btn-primary" onClick={() => navigate('/')}>Browse the menu</button>
        </div>
      ) : (
        <div className="stack stack-sm">
          {orders.map(order => {
            const date = new Date(order.time * 1000)
            return (
              <div
                key={order.orderID}
                className="card"
                style={{ cursor: 'pointer', padding: '18px 24px' }}
                onClick={() => navigate(`/orders/${order.orderID}`)}
              >
                <div className="cluster cluster-md space-between">
                  <div>
                    <div className="cluster cluster-sm" style={{ marginBottom: 6 }}>
                      <span className="fw-600" style={{ fontSize: 16 }}>Order #{order.orderID}</span>
                      {statusBadge(order.status)}
                      {order.isOnline && <span className="badge badge-confirmed">Online</span>}
                    </div>
                    <div className="text-sm text-muted">
                      {date.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })}
                      {' · '}
                      {date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      {' · '}
                      {order.paymentMethod}
                      {order.tableID ? ` · Table ${order.tableID}` : ' · Takeaway'}
                    </div>
                  </div>
                  <span className="text-muted" style={{ fontSize: 20 }}>›</span>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </CustomerLayout>
  )
}
