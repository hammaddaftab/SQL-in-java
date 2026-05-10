import { useState, useEffect } from 'react';
import { api } from '../api';
import type { Ingredient, Supplier, RestockRequest } from '../api';
import AdminLayout from '../components/AdminLayout';
import { useNavigate } from 'react-router-dom';

export default function AdminInventoryPage() {
  const [ingredients, setIngredients] = useState<Ingredient[]>([]);
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [requests, setRequests] = useState<RestockRequest[]>([]);
  const [loading, setLoading] = useState(true);
  
  const [restockIngredient, setRestockIngredient] = useState('');
  const [restockSupplier, setRestockSupplier] = useState('');
  const [restockQuantity, setRestockQuantity] = useState('');
  
  const navigate = useNavigate();

  const loadData = () => {
    setLoading(true);
    Promise.all([
      api.adminGetInventory(),
      api.adminGetRestockRequests()
    ]).then(([invRes, reqRes]) => {
      setIngredients(invRes.ingredients);
      setSuppliers(invRes.suppliers);
      setRequests(reqRes.requests);
      if (invRes.ingredients.length > 0) setRestockIngredient(String(invRes.ingredients[0].ingredientID));
      if (invRes.suppliers.length > 0) setRestockSupplier(String(invRes.suppliers[0].supplierID));
    }).catch(err => {
      if (err.message.includes('401')) navigate('/admin/login');
    }).finally(() => setLoading(false));
  };

  useEffect(() => { loadData(); }, [navigate]);

  const handleRestock = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!restockIngredient || !restockSupplier || !restockQuantity) return;
    try {
      await api.adminRequestRestock(parseInt(restockIngredient, 10), parseInt(restockSupplier, 10), parseInt(restockQuantity, 10));
      setRestockQuantity('');
      loadData();
      alert('Restock requested successfully');
    } catch (err) {
      alert('Failed to request restock');
    }
  };

  if (loading) return <AdminLayout><div className="loader"></div></AdminLayout>;

  return (
    <AdminLayout>
      <h1 style={{ marginBottom: 32 }}>Inventory Management</h1>
      
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 32 }}>
        
        <div style={{ display: 'flex', flexDirection: 'column', gap: 32 }}>
          <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
            <div style={{ padding: '20px 24px', borderBottom: '1px solid var(--border)', background: 'var(--cream-200)' }}>
              <h2 style={{ margin: 0, fontSize: 20 }}>Current Stock</h2>
            </div>
            <table className="table">
              <thead>
                <tr>
                  <th>Ingredient</th>
                  <th>Type</th>
                  <th>Stock</th>
                  <th>Threshold</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {ingredients.map(ing => {
                  const isLow = ing.stock <= ing.restockThreshold;
                  return (
                    <tr key={ing.ingredientID} style={{ background: isLow ? 'rgba(220, 38, 38, 0.05)' : 'transparent' }}>
                      <td style={{ fontWeight: 500 }}>{ing.name}</td>
                      <td>{ing.type}</td>
                      <td style={{ fontWeight: 600, color: isLow ? 'var(--danger)' : 'inherit' }}>{ing.stock}</td>
                      <td style={{ color: 'var(--text-muted)' }}>{ing.restockThreshold}</td>
                      <td>
                        {isLow ? <span className="badge pending" style={{ background: 'var(--danger)' }}>Low Stock</span> : <span className="badge completed">OK</span>}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
            <div style={{ padding: '20px 24px', borderBottom: '1px solid var(--border)', background: 'var(--cream-200)' }}>
              <h2 style={{ margin: 0, fontSize: 20 }}>Pending Restock Requests</h2>
            </div>
            <table className="table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Ingredient</th>
                  <th>Supplier</th>
                  <th>Qty</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {requests.length === 0 ? (
                  <tr><td colSpan={5} style={{ textAlign: 'center', padding: 24, color: 'var(--text-muted)' }}>No pending requests</td></tr>
                ) : (
                  requests.map(req => (
                    <tr key={req.requestID}>
                      <td>{new Date(req.requestedAt).toLocaleDateString()}</td>
                      <td style={{ fontWeight: 500 }}>{req.ingredientName}</td>
                      <td>{req.supplierName}</td>
                      <td>{req.quantityRequested}</td>
                      <td><span className={`badge ${req.status}`}>{req.status}</span></td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        <div>
          <div className="card" style={{ position: 'sticky', top: 40 }}>
            <h2 style={{ marginBottom: 24 }}>Request Restock</h2>
            <form onSubmit={handleRestock} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div>
                <label style={{ display: 'block', fontSize: 13, marginBottom: 4, fontWeight: 500 }}>Ingredient</label>
                <select className="input-field" value={restockIngredient} onChange={e => setRestockIngredient(e.target.value)} required>
                  {ingredients.map(ing => (
                    <option key={ing.ingredientID} value={ing.ingredientID}>{ing.name}</option>
                  ))}
                </select>
              </div>
              <div>
                <label style={{ display: 'block', fontSize: 13, marginBottom: 4, fontWeight: 500 }}>Supplier</label>
                <select className="input-field" value={restockSupplier} onChange={e => setRestockSupplier(e.target.value)} required>
                  {suppliers.map(sup => (
                    <option key={sup.supplierID} value={sup.supplierID}>{sup.name}</option>
                  ))}
                </select>
              </div>
              <div>
                <label style={{ display: 'block', fontSize: 13, marginBottom: 4, fontWeight: 500 }}>Quantity</label>
                <input type="number" className="input-field" min="1" value={restockQuantity} onChange={e => setRestockQuantity(e.target.value)} required />
              </div>
              <button className="btn-primary" type="submit" style={{ marginTop: 8 }}>Submit Request</button>
            </form>
          </div>
        </div>

      </div>
    </AdminLayout>
  );
}
