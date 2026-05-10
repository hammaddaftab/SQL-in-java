import { useState, useEffect } from 'react';
import { api } from '../api';
import type { SalesReport } from '../api';
import AdminLayout from '../components/AdminLayout';
import { useNavigate } from 'react-router-dom';

export default function AdminSalesPage() {
  const [report, setReport] = useState<SalesReport | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    api.adminSales()
      .then(setReport)
      .catch(err => {
        if (err.message.includes('401')) navigate('/admin/login');
      })
      .finally(() => setLoading(false));
  }, [navigate]);

  if (loading) return <AdminLayout><div className="loader"></div></AdminLayout>;
  if (!report) return <AdminLayout><div>Error loading report</div></AdminLayout>;

  return (
    <AdminLayout>
      <h1 style={{ marginBottom: 32 }}>Sales Report</h1>
      
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 24, marginBottom: 40 }}>
        <div className="card" style={{ background: 'var(--coffee-900)', color: 'var(--cream-100)', borderColor: 'var(--coffee-900)' }}>
          <div style={{ color: 'var(--cream-300)', fontSize: 14, fontWeight: 600, textTransform: 'uppercase', marginBottom: 8 }}>Total Revenue</div>
          <div style={{ fontSize: 36, fontWeight: 600, fontFamily: 'var(--font-display)', color: 'var(--amber-500)' }}>${report.salesToday.toFixed(2)}</div>
        </div>
        <div className="card">
          <div style={{ color: 'var(--text-muted)', fontSize: 14, fontWeight: 600, textTransform: 'uppercase', marginBottom: 8 }}>Total Orders</div>
          <div style={{ fontSize: 36, fontWeight: 600, fontFamily: 'var(--font-display)' }}>{report.ordersToday}</div>
        </div>
        <div className="card">
          <div style={{ color: 'var(--text-muted)', fontSize: 14, fontWeight: 600, textTransform: 'uppercase', marginBottom: 8 }}>Completed</div>
          <div style={{ fontSize: 36, fontWeight: 600, fontFamily: 'var(--font-display)', color: 'var(--success)' }}>{report.completedOrders}</div>
        </div>
        <div className="card">
          <div style={{ color: 'var(--text-muted)', fontSize: 14, fontWeight: 600, textTransform: 'uppercase', marginBottom: 8 }}>Pending</div>
          <div style={{ fontSize: 36, fontWeight: 600, fontFamily: 'var(--font-display)' }}>{report.pendingOrders}</div>
        </div>
      </div>

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <div style={{ padding: '20px 24px', borderBottom: '1px solid var(--border)', background: 'var(--cream-200)' }}>
          <h2 style={{ margin: 0, fontSize: 20 }}>Top Products by Volume</h2>
        </div>
        <table className="table">
          <thead>
            <tr>
              <th>Rank</th>
              <th>Product Name</th>
              <th>Quantity Sold</th>
              <th>Unit Price</th>
              <th>Total Revenue</th>
            </tr>
          </thead>
          <tbody>
            {report.topProducts.map((p, index) => (
              <tr key={p.productID}>
                <td style={{ color: 'var(--text-muted)', fontWeight: 600 }}>#{index + 1}</td>
                <td style={{ fontWeight: 500 }}>{p.productName}</td>
                <td>{p.totalQuantitySold}</td>
                <td>${p.price.toFixed(2)}</td>
                <td style={{ fontWeight: 600, color: 'var(--success)' }}>${(p.totalQuantitySold * p.price).toFixed(2)}</td>
              </tr>
            ))}
            {report.topProducts.length === 0 && (
              <tr><td colSpan={5} style={{ textAlign: 'center', padding: 24, color: 'var(--text-muted)' }}>No sales data available</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </AdminLayout>
  );
}
