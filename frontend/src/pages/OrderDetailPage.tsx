import { useState, useEffect } from 'react'
import { api } from '../api'
import type { Order, OrderItem } from '../api'
import CustomerLayout from '../components/CustomerLayout'
import { useParams, useNavigate } from 'react-router-dom'

const STATUS_CLASS: Record<string, string> = {
  pending: 'badge-pending', confirmed: 'badge-confirmed', completed: 'badge-completed',
}

export default function OrderDetailPage() {
  const { id }   = useParams()
  const navigate = useNavigate()
  const [order, setOrder]   = useState<Order | null>(null)
  const [items, setItems]   = useState<OrderItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError]   = useState('')
  const customerID = localStorage.getItem('customerID')

  useEffect(() => {
    if (!customerID || !id) { setLoading(false); return }
    api.getOrder(parseInt(id, 10), parseInt(customerID, 10))
      .then(r => { setOrder(r.order); setItems(r.items) })
      .catch(() => setError('Order not found or access denied.'))
      .finally(() => setLoading(false))
  }, [id, customerID])

  if (loading) return (
    <CustomerLayout><div className="loader-wrap"><div className="loader" /></div></CustomerLayout>
  )

  if (error || !order) return (
    <CustomerLayout>
      <div className="empty-state" style={{ marginTop: 60 }}>
        <p style={{ marginBottom: 16 }}>{error || 'Order not found.'}</p>
        <button className="btn btn-primary" onClick={() => navigate('/orders')}>Back to orders</button>
      </div>
    </CustomerLayout>
  )

  const total = items.reduce((s, i) => s + i.priceAtOrder * i.quantity, 0)
  const date  = new Date(order.time * 1000)

  return (
    <CustomerLayout>
      <button
        className="btn btn-ghost btn-sm"
        style={{ marginBottom: 20 }}
        onClick={() => navigate('/orders')}
      >
        ← Back
      </button>

      <div className="card" style={{ maxWidth: 560 }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24, paddingBottom: 20, borderBottom: '1px solid var(--border)' }}>
          <div>
            <h1 style={{ fontSize: 28, marginBottom: 4 }}>Order #{order.orderID}</h1>
            <p className="text-sm text-muted">
              {date.toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' })}
              {' at '}
              {date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
            </p>
          </div>
          <span className={`badge ${STATUS_CLASS[order.status] ?? 'badge-neutral'}`} style={{ fontSize: 12 }}>
            {order.status}
          </span>
        </div>

        {/* Meta */}
        <div className="cluster cluster-md" style={{ marginBottom: 24, flexWrap: 'wrap' }}>
          <div>
            <div className="text-xs text-muted fw-600" style={{ textTransform: 'uppercase', letterSpacing: '.5px', marginBottom: 3 }}>Type</div>
            <div className="text-sm">
              {order.isOnline ? 'Online' : 'In-store'}
              {order.tableID ? ` · Table ${order.tableID}` : ' · Takeaway'}
            </div>
          </div>
          <div style={{ width: 1, background: 'var(--border)', alignSelf: 'stretch' }} />
          <div>
            <div className="text-xs text-muted fw-600" style={{ textTransform: 'uppercase', letterSpacing: '.5px', marginBottom: 3 }}>Payment</div>
            <div className="text-sm" style={{ textTransform: 'capitalize' }}>{order.paymentMethod}</div>
          </div>
        </div>

        {/* Items */}
        <h3 style={{ fontSize: 15, marginBottom: 12 }}>Items</h3>
        <div style={{ marginBottom: 20 }}>
          {items.map(item => (
            <div key={item.orderItemID} style={{ display: 'flex', justifyContent: 'space-between', padding: '9px 0', borderBottom: '1px solid var(--border)' }}>
              <span>
                <span className="fw-500">{item.quantity}×</span> {item.productName}
              </span>
              <span className="text-muted">${(item.priceAtOrder * item.quantity).toFixed(2)}</span>
            </div>
          ))}
        </div>

        {/* Total */}
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 18, fontWeight: 600, paddingTop: 4 }}>
          <span>Total</span>
          <span>${total.toFixed(2)}</span>
        </div>
      </div>
    </CustomerLayout>
  )
}
