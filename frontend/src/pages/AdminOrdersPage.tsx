import { useState, useEffect } from 'react';
import { api } from '../api';
import type { AdminOrder } from '../api';
import AdminLayout from '../components/AdminLayout';
import { useNavigate } from 'react-router-dom';

export default function AdminOrdersPage() {
  const [orders, setOrders] = useState<AdminOrder[]>([]);
  const [filter, setFilter] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const loadOrders = () => {
    setLoading(true);
    api.adminGetOrders(filter === '' ? undefined : filter)
      .then(res => setOrders(res.orders))
      .catch(err => {
        if (err.message.includes('401')) navigate('/admin/login');
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadOrders();
  }, [filter, navigate]);

  const handleUpdateStatus = async (id: number, status: string) => {
    try {
      await api.adminUpdateOrderStatus(id, status);
      loadOrders();
    } catch (err) {
      alert('Failed to update status');
    }
  };

  const handleConfirm = async (id: number) => {
    try {
      await api.adminConfirmOrder(id);
      loadOrders();
    } catch (err) {
      alert('Failed to confirm order');
    }
  };

  return (
    <AdminLayout>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 32 }}>
        <h1>Orders</h1>
        <select className="input-field" style={{ width: 200 }} value={filter} onChange={e => setFilter(e.target.value)}>
          <option value="">All Statuses</option>
          <option value="pending">Pending</option>
          <option value="confirmed">Confirmed</option>
          <option value="completed">Completed</option>
        </select>
      </div>

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <table className="table">
          <thead style={{ background: 'var(--cream-200)' }}>
            <tr>
              <th>Order ID</th>
              <th>Time</th>
              <th>Customer</th>
              <th>Type/Table</th>
              <th>Payment</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} style={{ textAlign: 'center' }}><div className="loader"></div></td></tr>
            ) : orders.length === 0 ? (
              <tr><td colSpan={7} style={{ textAlign: 'center', padding: 32, color: 'var(--text-muted)' }}>No orders found</td></tr>
            ) : (
              orders.map(o => (
                <tr key={o.orderID}>
                  <td style={{ fontWeight: 600 }}>#{o.orderID}</td>
                  <td style={{ fontSize: 14 }}>{new Date(o.time).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</td>
                  <td>{o.customerName}</td>
                  <td>{o.isOnline ? 'Online' : (o.tableID ? `Table ${o.tableID}` : 'Takeaway')}</td>
                  <td style={{ textTransform: 'capitalize' }}>{o.paymentMethod}</td>
                  <td><span className={`badge ${o.status}`}>{o.status}</span></td>
                  <td>
                    <div style={{ display: 'flex', gap: 8 }}>
                      {o.status === 'pending' && o.isOnline && (
                        <button className="btn-secondary" style={{ padding: '6px 12px', fontSize: 13 }} onClick={() => handleConfirm(o.orderID)}>Confirm</button>
                      )}
                      {o.status === 'pending' && !o.isOnline && (
                        <button className="btn-primary" style={{ padding: '6px 12px', fontSize: 13 }} onClick={() => handleUpdateStatus(o.orderID, 'completed')}>Complete</button>
                      )}
                      {o.status === 'confirmed' && (
                        <button className="btn-primary" style={{ padding: '6px 12px', fontSize: 13 }} onClick={() => handleUpdateStatus(o.orderID, 'completed')}>Complete</button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </AdminLayout>
  );
}
