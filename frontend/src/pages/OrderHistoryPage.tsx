import { useState, useEffect } from 'react';
import { api } from '../api';
import type { Order } from '../api';
import CustomerLayout from '../components/CustomerLayout';
import { useNavigate } from 'react-router-dom';

export default function OrderHistoryPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const customerID = localStorage.getItem('customerID');

  useEffect(() => {
    if (!customerID) {
      setLoading(false);
      return;
    }
    api.getOrders(parseInt(customerID, 10))
      .then(res => setOrders(res.orders))
      .catch(err => console.error(err))
      .finally(() => setLoading(false));
  }, [customerID]);

  if (loading) return <CustomerLayout><div className="loader" style={{margin: '40px auto', display: 'block'}}></div></CustomerLayout>;

  if (!customerID) {
    return (
      <CustomerLayout>
        <div style={{ textAlign: 'center', marginTop: 80 }}>
          <h2>Please register to view orders</h2>
          <button className="btn-primary" style={{ marginTop: 16 }} onClick={() => navigate('/')}>Go to Menu</button>
        </div>
      </CustomerLayout>
    );
  }

  return (
    <CustomerLayout>
      <h1 style={{ marginBottom: 32 }}>Your Orders</h1>
      {orders.length === 0 ? (
        <p>You haven't placed any orders yet.</p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {orders.map(order => (
            <div 
              key={order.orderID} 
              className="card" 
              style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', cursor: 'pointer', transition: 'border 0.2s' }}
              onClick={() => navigate(`/orders/${order.orderID}`)}
              onMouseOver={e => e.currentTarget.style.borderColor = 'var(--amber-500)'}
              onMouseOut={e => e.currentTarget.style.borderColor = 'var(--border)'}
            >
              <div>
                <div style={{ fontWeight: 600, fontSize: 18, marginBottom: 8 }}>Order #{order.orderID}</div>
                <div style={{ color: 'var(--text-muted)', fontSize: 14 }}>
                  {new Date(order.time).toLocaleString()} • {order.paymentMethod} {order.tableID ? `• Table ${order.tableID}` : '• Takeaway'}
                </div>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 24 }}>
                <span className={`badge ${order.status}`}>{order.status}</span>
                <span style={{ fontSize: 24, color: 'var(--text-muted)' }}>&rarr;</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </CustomerLayout>
  );
}
