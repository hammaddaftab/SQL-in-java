import { useState, useEffect } from 'react'
import { api } from '../api'
import type { Product } from '../api'
import AdminLayout from '../components/AdminLayout'
import { useNavigate } from 'react-router-dom'

type EditState = { name: string; price: string; category: string }
type NewState  = EditState & { open: boolean }

const CATEGORIES = ['coffee', 'food', 'other']

export default function AdminProductsPage() {
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading]   = useState(true)
  const [editID, setEditID]     = useState<number | null>(null)
  const [editVals, setEditVals] = useState<EditState>({ name: '', price: '', category: '' })
  const [newForm, setNewForm]   = useState<NewState>({ open: false, name: '', price: '', category: 'coffee' })
  const [error, setError]       = useState('')
  const [success, setSuccess]   = useState('')
  const navigate = useNavigate()

  const load = () => {
    setLoading(true)
    api.adminGetProducts()
      .then(r => setProducts(r.products))
      .catch(err => { if (err.message?.includes('401')) navigate('/admin/login') })
      .finally(() => setLoading(false))
  }
  useEffect(load, [navigate])

  const flash = (msg: string, isErr = false) => {
    isErr ? setError(msg) : setSuccess(msg)
    setTimeout(() => isErr ? setError('') : setSuccess(''), 3000)
  }

  const startEdit = (p: Product) => {
    setEditID(p.productID)
    setEditVals({ name: p.name, price: String(p.price), category: p.category })
  }
  const cancelEdit = () => setEditID(null)

  const saveEdit = async (id: number) => {
    try {
      await api.adminUpdateProduct(id, { name: editVals.name, price: parseFloat(editVals.price), category: editVals.category })
      setEditID(null)
      load()
      flash('Product updated.')
    } catch { flash('Could not save changes.', true) }
  }

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      await api.adminAddProduct({ name: newForm.name, price: parseFloat(newForm.price), category: newForm.category })
      setNewForm({ open: false, name: '', price: '', category: 'coffee' })
      load()
      flash('Product added.')
    } catch { flash('Could not add product.', true) }
  }

  const handleDelete = async (id: number, name: string) => {
    if (!window.confirm(`Remove "${name}" from the menu?`)) return
    try { await api.adminDeleteProduct(id); load(); flash('Product removed.') }
    catch { flash('Could not delete product.', true) }
  }

  return (
    <AdminLayout>
      <div className="admin-page-header">
        <h1>Products</h1>
        <button className="btn btn-primary" onClick={() => setNewForm(f => ({ ...f, open: !f.open }))}>
          {newForm.open ? 'Cancel' : '+ Add product'}
        </button>
      </div>

      {error   && <p className="inline-error"   style={{ marginBottom: 16 }}>{error}</p>}
      {success && <p className="inline-success" style={{ marginBottom: 16 }}>{success}</p>}

      {newForm.open && (
        <div className="card" style={{ marginBottom: 20, background: 'var(--cream-200)', border: 'none' }}>
          <h3 style={{ marginBottom: 16, fontSize: 16 }}>New product</h3>
          <form onSubmit={handleAdd} className="cluster cluster-sm" style={{ flexWrap: 'wrap', alignItems: 'flex-end' }}>
            <div style={{ flex: '2 1 180px' }}>
              <label className="field-label">Name</label>
              <input className="field" value={newForm.name} onChange={e => setNewForm(f => ({ ...f, name: e.target.value }))} required />
            </div>
            <div style={{ flex: '1 1 100px' }}>
              <label className="field-label">Price ($)</label>
              <input type="number" step="0.01" min="0" className="field" value={newForm.price} onChange={e => setNewForm(f => ({ ...f, price: e.target.value }))} required />
            </div>
            <div style={{ flex: '1 1 120px' }}>
              <label className="field-label">Category</label>
              <select className="field" value={newForm.category} onChange={e => setNewForm(f => ({ ...f, category: e.target.value }))}>
                {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
              </select>
            </div>
            <button className="btn btn-primary" type="submit">Save</button>
          </form>
        </div>
      )}

      <div className="card card-flush">
        <table className="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Category</th>
              <th>Price</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={4}><div className="loader-wrap"><div className="loader" /></div></td></tr>
            ) : products.length === 0 ? (
              <tr><td colSpan={4}><div className="empty-state">No products yet.</div></td></tr>
            ) : products.map(p => {
              const isEditing = editID === p.productID
              if (isEditing) return (
                <tr key={p.productID} className="edit-row">
                  <td>
                    <input className="field" value={editVals.name} onChange={e => setEditVals(v => ({ ...v, name: e.target.value }))} />
                  </td>
                  <td>
                    <select className="field" value={editVals.category} onChange={e => setEditVals(v => ({ ...v, category: e.target.value }))}>
                      {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                    </select>
                  </td>
                  <td>
                    <input type="number" step="0.01" className="field" value={editVals.price} onChange={e => setEditVals(v => ({ ...v, price: e.target.value }))} style={{ maxWidth: 90 }} />
                  </td>
                  <td>
                    <div className="cluster cluster-sm">
                      <button className="btn btn-primary btn-sm" onClick={() => saveEdit(p.productID)}>Save</button>
                      <button className="btn btn-ghost btn-sm" onClick={cancelEdit}>Cancel</button>
                    </div>
                  </td>
                </tr>
              )
              return (
                <tr key={p.productID}>
                  <td className="fw-500">{p.name}</td>
                  <td><span className="badge badge-neutral">{p.category}</span></td>
                  <td>${p.price.toFixed(2)}</td>
                  <td>
                    <div className="cluster cluster-sm">
                      <button className="btn btn-ghost btn-sm" onClick={() => startEdit(p)}>Edit</button>
                      <button className="btn btn-danger btn-sm" onClick={() => handleDelete(p.productID, p.name)}>Remove</button>
                    </div>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </AdminLayout>
  )
}
