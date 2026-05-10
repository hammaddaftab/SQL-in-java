import { useState, useEffect } from 'react';
import { api } from '../api';
import type { DashboardStats } from '../api';
import AdminLayout from '../components/AdminLayout';
import { useNavigate } from 'react-router-dom';

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    api.adminDashboard()
      .then(setStats)
      .catch(err => {
        if (err.message.includes('401') || err.message.includes('Unauthorized')) {
          navigate('/admin/login');
        } else {
          console.error(err);
        }
      })
      .finally(() => setLoading(false));
  }, [navigate]);

  if (loading) return <AdminLayout><div className="loader"></div></AdminLayout>;
  if (!stats) return <AdminLayout><div>Error loading stats</div></AdminLayout>;

  return (
    <AdminLayout>
      <h1 style={{ marginBottom: 32 }}>Overview</h1>
      
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 24, marginBottom: 40 }}>
        <div className="card">
          <div style={{ color: 'var(--text-muted)', fontSize: 14, fontWeight: 600, textTransform: 'uppercase', marginBottom: 8 }}>Orders Today</div>
          <div style={{ fontSize: 36, fontWeight: 600, fontFamily: 'var(--font-display)' }}>{stats.ordersToday}</div>
        </div>
        <div className="card">
          <div style={{ color: 'var(--text-muted)', fontSize: 14, fontWeight: 600, textTransform: 'uppercase', marginBottom: 8 }}>Sales Today</div>
          <div style={{ fontSize: 36, fontWeight: 600, fontFamily: 'var(--font-display)', color: 'var(--success)' }}>${stats.salesToday.toFixed(2)}</div>
        </div>
        <div className="card" style={{ borderColor: stats.pendingOrders > 0 ? 'var(--amber-500)' : 'var(--border)' }}>
          <div style={{ color: 'var(--text-muted)', fontSize: 14, fontWeight: 600, textTransform: 'uppercase', marginBottom: 8 }}>Pending Orders</div>
          <div style={{ fontSize: 36, fontWeight: 600, fontFamily: 'var(--font-display)' }}>{stats.pendingOrders}</div>
        </div>
        <div className="card" style={{ borderColor: stats.lowStockItems > 0 ? 'var(--danger)' : 'var(--border)' }}>
          <div style={{ color: 'var(--text-muted)', fontSize: 14, fontWeight: 600, textTransform: 'uppercase', marginBottom: 8 }}>Low Stock Items</div>
          <div style={{ fontSize: 36, fontWeight: 600, fontFamily: 'var(--font-display)' }}>{stats.lowStockItems}</div>
        </div>
      </div>

      <div className="card">
        <h2 style={{ marginBottom: 20 }}>Top Selling Products</h2>
        <table className="table">
          <thead>
            <tr>
              <th>Product</th>
              <th>Sold</th>
              <th>Revenue</th>
            </tr>
          </thead>
          <tbody>
            {stats.topProducts.map(p => (
              <tr key={p.productID}>
                <td style={{ fontWeight: 500 }}>{p.productName}</td>
                <td>{p.totalQuantitySold}</td>
                <td>${(p.totalQuantitySold * p.price).toFixed(2)}</td>
              </tr>
            ))}
            {stats.topProducts.length === 0 && (
              <tr><td colSpan={3} style={{ textAlign: 'center', color: 'var(--text-muted)' }}>No sales yet</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </AdminLayout>
  );
}
