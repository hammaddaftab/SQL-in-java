import { useState, useEffect } from 'react';
import { api } from '../api';
import type { Order, OrderItem } from '../api';
import CustomerLayout from '../components/CustomerLayout';
import { useParams, useNavigate } from 'react-router-dom';

export default function OrderDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [order, setOrder] = useState<Order | null>(null);
  const [items, setItems] = useState<OrderItem[]>([]);
  const [loading, setLoading] = useState(true);
  const customerID = localStorage.getItem('customerID');

  useEffect(() => {
    if (!customerID || !id) return;
    api.getOrder(parseInt(id, 10), parseInt(customerID, 10))
      .then(res => {
        setOrder(res.order);
        setItems(res.items);
      })
      .catch(err => {
        console.error(err);
        alert('Order not found');
        navigate('/orders');
      })
      .finally(() => setLoading(false));
  }, [id, customerID, navigate]);

  if (loading) return <CustomerLayout><div className="loader" style={{margin: '40px auto', display: 'block'}}></div></CustomerLayout>;
  if (!order) return <CustomerLayout><div>Order not found.</div></CustomerLayout>;

  const total = items.reduce((sum, item) => sum + item.priceAtOrder * item.quantity, 0);

  return (
    <CustomerLayout>
      <button 
        style={{ background: 'none', border: 'none', color: 'var(--text-muted)', marginBottom: 24, padding: 0, fontSize: 16 }} 
        onClick={() => navigate('/orders')}
      >
        &larr; Back to Orders
      </button>

      <div className="card" style={{ maxWidth: 600, margin: '0 auto' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24, borderBottom: '1px solid var(--border)', paddingBottom: 24 }}>
          <div>
            <h1 style={{ margin: '0 0 8px 0', fontSize: 32 }}>Order #{order.orderID}</h1>
            <div style={{ color: 'var(--text-muted)' }}>
              {new Date(order.time).toLocaleString()}
            </div>
          </div>
          <span className={`badge ${order.status}`} style={{ fontSize: 14, padding: '6px 12px' }}>{order.status}</span>
        </div>

        <div style={{ display: 'flex', gap: 32, marginBottom: 32, color: 'var(--text-muted)', fontSize: 14 }}>
          <div>
            <div style={{ fontWeight: 600, color: 'var(--text)', marginBottom: 4 }}>Type</div>
            <div>{order.isOnline ? 'Online Order' : 'In-Store'} {order.tableID ? `(Table ${order.tableID})` : '(Takeaway)'}</div>
          </div>
          <div>
            <div style={{ fontWeight: 600, color: 'var(--text)', marginBottom: 4 }}>Payment</div>
            <div style={{ textTransform: 'capitalize' }}>{order.paymentMethod}</div>
          </div>
        </div>

        <h3 style={{ marginBottom: 16 }}>Items</h3>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginBottom: 24 }}>
          {items.map(item => (
            <div key={item.orderItemID} style={{ display: 'flex', justifyContent: 'space-between' }}>
              <div>
                <span style={{ fontWeight: 500 }}>{item.quantity}x</span> {item.productName}
              </div>
              <div>${(item.priceAtOrder * item.quantity).toFixed(2)}</div>
            </div>
          ))}
        </div>

        <div style={{ borderTop: '2px solid var(--border)', paddingTop: 16, display: 'flex', justifyContent: 'space-between', fontSize: 20, fontWeight: 600 }}>
          <span>Total</span>
          <span>${total.toFixed(2)}</span>
        </div>
      </div>
    </CustomerLayout>
  );
}
