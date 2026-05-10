import { useState, useEffect } from 'react'
import { api } from '../api'
import type { Ingredient, Supplier, RestockRequest } from '../api'
import AdminLayout from '../components/AdminLayout'
import { useNavigate } from 'react-router-dom'

type Tab = 'stock' | 'requests'

export default function AdminInventoryPage() {
  const [ingredients, setIngredients] = useState<Ingredient[]>([])
  const [suppliers, setSuppliers]     = useState<Supplier[]>([])
  const [requests, setRequests]       = useState<RestockRequest[]>([])
  const [loading, setLoading]         = useState(true)
  const [tab, setTab]                 = useState<Tab>('stock')
  const [showForm, setShowForm]       = useState(false)
  const [formIng, setFormIng]         = useState('')
  const [formSup, setFormSup]         = useState('')
  const [formQty, setFormQty]         = useState('')
  const [formError, setFormError]     = useState('')
  const [formSuccess, setFormSuccess] = useState('')
  const navigate = useNavigate()

  const loadData = () => {
    setLoading(true)
    Promise.all([api.adminGetInventory(), api.adminGetRestockRequests()])
      .then(([inv, req]) => {
        setIngredients(inv.ingredients)
        setSuppliers(inv.suppliers)
        setRequests(req.requests)
        if (!formIng && inv.ingredients[0]) setFormIng(String(inv.ingredients[0].ingredientID))
        if (!formSup && inv.suppliers[0])   setFormSup(String(inv.suppliers[0].supplierID))
      })
      .catch(err => { if (err.message?.includes('401')) navigate('/admin/login') })
      .finally(() => setLoading(false))
  }
  useEffect(loadData, [navigate])

  const handleRestock = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError('')
    try {
      await api.adminRequestRestock(parseInt(formIng, 10), parseInt(formSup, 10), parseInt(formQty, 10))
      setFormQty('')
      setShowForm(false)
      setFormSuccess('Restock request submitted.')
      loadData()
      setTimeout(() => setFormSuccess(''), 3500)
    } catch { setFormError('Could not submit request.') }
  }

  const lowCount = ingredients.filter(i => i.stock <= i.restockThreshold).length

  if (loading) return <AdminLayout><div className="loader-wrap"><div className="loader" /></div></AdminLayout>

  return (
    <AdminLayout>
      <div className="admin-page-header">
        <h1>Inventory</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(f => !f)}>
          {showForm ? 'Cancel' : '+ Request restock'}
        </button>
      </div>

      {formSuccess && <p className="inline-success" style={{ marginBottom: 16 }}>{formSuccess}</p>}

      {/* Restock form — collapsible */}
      {showForm && (
        <div className="card" style={{ marginBottom: 20, background: 'var(--cream-200)', border: 'none' }}>
          <h3 style={{ marginBottom: 16, fontSize: 16 }}>New restock request</h3>
          <form onSubmit={handleRestock} className="cluster cluster-sm" style={{ flexWrap: 'wrap', alignItems: 'flex-end' }}>
            <div style={{ flex: '2 1 180px' }}>
              <label className="field-label">Ingredient</label>
              <select className="field" value={formIng} onChange={e => setFormIng(e.target.value)} required>
                {ingredients.map(i => <option key={i.ingredientID} value={i.ingredientID}>{i.name}</option>)}
              </select>
            </div>
            <div style={{ flex: '2 1 180px' }}>
              <label className="field-label">Supplier</label>
              <select className="field" value={formSup} onChange={e => setFormSup(e.target.value)} required>
                {suppliers.map(s => <option key={s.supplierID} value={s.supplierID}>{s.name}</option>)}
              </select>
            </div>
            <div style={{ flex: '1 1 100px' }}>
              <label className="field-label">Quantity</label>
              <input type="number" min="1" className="field" value={formQty} onChange={e => setFormQty(e.target.value)} required />
            </div>
            <button className="btn btn-primary" type="submit">Submit</button>
          </form>
          {formError && <p className="inline-error" style={{ marginTop: 10 }}>{formError}</p>}
        </div>
      )}

      {/* Tab bar */}
      <div className="tab-bar">
        <button className={`tab-pill${tab === 'stock' ? ' active' : ''}`} onClick={() => setTab('stock')}>
          Stock levels {lowCount > 0 && <span className="badge badge-warning" style={{ marginLeft: 6 }}>{lowCount} low</span>}
        </button>
        <button className={`tab-pill${tab === 'requests' ? ' active' : ''}`} onClick={() => setTab('requests')}>
          Restock requests {requests.length > 0 && <span className="badge badge-neutral" style={{ marginLeft: 6 }}>{requests.length}</span>}
        </button>
      </div>

      {tab === 'stock' && (
        <div className="card card-flush">
          <table className="table">
            <thead>
              <tr>
                <th>Ingredient</th>
                <th>Type</th>
                <th>In stock</th>
                <th>Threshold</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {ingredients.length === 0 ? (
                <tr><td colSpan={5}><div className="empty-state">No ingredients on record.</div></td></tr>
              ) : ingredients.map(ing => {
                const low = ing.stock <= ing.restockThreshold
                return (
                  <tr key={ing.ingredientID} style={{ background: low ? 'var(--danger-bg)' : undefined }}>
                    <td className="fw-500">{ing.name}</td>
                    <td className="text-sm text-muted" style={{ textTransform: 'capitalize' }}>{ing.type}</td>
                    <td className="fw-600" style={{ color: low ? 'var(--danger)' : undefined }}>{ing.stock}</td>
                    <td className="text-muted">{ing.restockThreshold}</td>
                    <td>
                      {low
                        ? <span className="badge badge-warning">Low stock</span>
                        : <span className="badge badge-completed">OK</span>}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'requests' && (
        <div className="card card-flush">
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
                <tr><td colSpan={5}><div className="empty-state">No restock requests yet.</div></td></tr>
              ) : requests.map(r => (
                <tr key={r.requestID}>
                  <td className="text-sm text-muted">{new Date(r.requestedAt * 1000).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}</td>
                  <td className="fw-500">{r.ingredientName}</td>
                  <td className="text-sm">{r.supplierName}</td>
                  <td>{r.quantityRequested}</td>
                  <td><span className={`badge ${r.status === 'pending' ? 'badge-pending' : 'badge-completed'}`}>{r.status}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </AdminLayout>
  )
}
