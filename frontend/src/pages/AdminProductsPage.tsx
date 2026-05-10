import { useState, useEffect } from 'react';
import { api } from '../api';
import type { Product } from '../api';
import AdminLayout from '../components/AdminLayout';
import { useNavigate } from 'react-router-dom';

export default function AdminProductsPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [isAdding, setIsAdding] = useState(false);
  const [formData, setFormData] = useState({ name: '', price: '', category: '' });
  const navigate = useNavigate();

  const loadProducts = () => {
    setLoading(true);
    api.adminGetProducts()
      .then(res => setProducts(res.products))
      .catch(err => {
        if (err.message.includes('401')) navigate('/admin/login');
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadProducts(); }, [navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await api.adminAddProduct({ name: formData.name, price: parseFloat(formData.price), category: formData.category });
      setIsAdding(false);
      setFormData({ name: '', price: '', category: '' });
      loadProducts();
    } catch (err) {
      alert('Failed to add product');
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this product?')) return;
    try {
      await api.adminDeleteProduct(id);
      loadProducts();
    } catch (err) {
      alert('Failed to delete');
    }
  };

  return (
    <AdminLayout>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 32 }}>
        <h1>Products</h1>
        <button className="btn-primary" onClick={() => setIsAdding(!isAdding)}>
          {isAdding ? 'Cancel' : '+ Add Product'}
        </button>
      </div>

      {isAdding && (
        <div className="card" style={{ marginBottom: 32, background: 'var(--cream-200)', border: 'none' }}>
          <h3 style={{ marginBottom: 16 }}>New Product</h3>
          <form onSubmit={handleSubmit} style={{ display: 'flex', gap: 16, alignItems: 'flex-end' }}>
            <div style={{ flex: 2 }}>
              <label style={{ display: 'block', fontSize: 13, marginBottom: 4, fontWeight: 500 }}>Name</label>
              <input className="input-field" value={formData.name} onChange={e => setFormData({...formData, name: e.target.value})} required />
            </div>
            <div style={{ flex: 1 }}>
              <label style={{ display: 'block', fontSize: 13, marginBottom: 4, fontWeight: 500 }}>Price</label>
              <input type="number" step="0.01" className="input-field" value={formData.price} onChange={e => setFormData({...formData, price: e.target.value})} required />
            </div>
            <div style={{ flex: 1 }}>
              <label style={{ display: 'block', fontSize: 13, marginBottom: 4, fontWeight: 500 }}>Category</label>
              <input className="input-field" value={formData.category} onChange={e => setFormData({...formData, category: e.target.value})} required />
            </div>
            <button className="btn-primary" type="submit">Save</button>
          </form>
        </div>
      )}

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <table className="table">
          <thead style={{ background: 'var(--cream-200)' }}>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Category</th>
              <th>Price</th>
              <th style={{ textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={5} style={{ textAlign: 'center' }}><div className="loader"></div></td></tr>
            ) : products.length === 0 ? (
              <tr><td colSpan={5} style={{ textAlign: 'center', padding: 32, color: 'var(--text-muted)' }}>No products found</td></tr>
            ) : (
              products.map(p => (
                <tr key={p.productID}>
                  <td style={{ color: 'var(--text-muted)' }}>{p.productID}</td>
                  <td style={{ fontWeight: 500 }}>{p.name}</td>
                  <td><span className="badge" style={{ background: 'var(--cream-300)', color: 'var(--text)' }}>{p.category}</span></td>
                  <td>${p.price.toFixed(2)}</td>
                  <td style={{ textAlign: 'right' }}>
                    <button className="btn-danger" style={{ padding: '6px 12px', fontSize: 13 }} onClick={() => handleDelete(p.productID)}>Delete</button>
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
