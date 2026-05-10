import { useState, useEffect } from 'react'
import { api } from '../api'
import type { Product, CafeTable } from '../api'
import CustomerLayout from '../components/CustomerLayout'
import { useNavigate } from 'react-router-dom'

type CartItem = { product: Product; quantity: number }

function getStoredCart(): CartItem[] {
  try { return JSON.parse(localStorage.getItem('cart') || '[]') } catch { return [] }
}

export default function MenuPage() {
  const [products, setProducts]   = useState<Product[]>([])
  const [tables, setTables]       = useState<CafeTable[]>([])
  const [loading, setLoading]     = useState(true)
  const [cart, setCart]           = useState<CartItem[]>(getStoredCart)
  const [activeCategory, setActiveCategory] = useState<string>('all')

  const [customerID, setCustomerID] = useState<string | null>(localStorage.getItem('customerID'))
  const [firstName, setFirstName]   = useState('')
  const [lastName, setLastName]     = useState('')
  const [regError, setRegError]     = useState('')
  const [isRegistering, setIsRegistering] = useState(false)

  const [paymentMethod, setPaymentMethod] = useState('card')
  const [tableID, setTableID]             = useState('')
  const [isOrdering, setIsOrdering]       = useState(false)
  const [orderError, setOrderError]       = useState('')
  const [orderSuccess, setOrderSuccess]   = useState<{ orderID: number; total: number } | null>(null)

  const navigate = useNavigate()

  useEffect(() => {
    Promise.all([api.getProducts(), api.getTables()])
      .then(([p, t]) => { setProducts(p.products); setTables(t.tables) })
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  const saveCart = (next: CartItem[]) => {
    setCart(next)
    localStorage.setItem('cart', JSON.stringify(next))
  }

  const changeQty = (product: Product, delta: number) => {
    const existing = cart.find(i => i.product.productID === product.productID)
    if (!existing) {
      if (delta > 0) saveCart([...cart, { product, quantity: 1 }])
      return
    }
    const next = existing.quantity + delta
    if (next <= 0) saveCart(cart.filter(i => i.product.productID !== product.productID))
    else saveCart(cart.map(i => i.product.productID === product.productID ? { ...i, quantity: next } : i))
  }

  const cartQty = (pid: number) => cart.find(i => i.product.productID === pid)?.quantity ?? 0
  const cartTotal = cart.reduce((s, i) => s + i.product.price * i.quantity, 0)
  const cartCount = cart.reduce((s, i) => s + i.quantity, 0)

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault()
    setRegError('')
    setIsRegistering(true)
    try {
      const c = await api.registerCustomer(firstName.trim(), lastName.trim())
      localStorage.setItem('customerID', String(c.customerID))
      localStorage.setItem('firstName', c.firstName)
      localStorage.setItem('lastName', c.lastName)
      setCustomerID(String(c.customerID))
    } catch (err: any) {
      setRegError(err.message || 'Registration failed')
    } finally {
      setIsRegistering(false)
    }
  }

  const handleOrder = async () => {
    if (!customerID || cart.length === 0) return
    setOrderError('')
    setIsOrdering(true)
    try {
      const res = await api.placeOrder({
        customerID: parseInt(customerID, 10),
        tableID: tableID ? parseInt(tableID, 10) : undefined,
        paymentMethod,
        items: cart.map(i => ({ productID: i.product.productID, quantity: i.quantity })),
      })
      setOrderSuccess(res)
      saveCart([])
    } catch (err: any) {
      setOrderError(err.message || 'Could not place order')
    } finally {
      setIsOrdering(false)
    }
  }

  if (loading) return (
    <CustomerLayout>
      <div className="loader-wrap"><div className="loader" /></div>
    </CustomerLayout>
  )

  if (orderSuccess) return (
    <CustomerLayout>
      <div className="order-confirm">
        <div className="confirm-id">#{orderSuccess.orderID}</div>
        <p className="confirm-sub">Your order is confirmed and being prepared.</p>
        <p className="confirm-total">${orderSuccess.total.toFixed(2)}</p>
        <div style={{ display: 'flex', gap: 12, justifyContent: 'center' }}>
          <button className="btn btn-primary btn-lg" onClick={() => navigate(`/orders/${orderSuccess.orderID}`)}>
            Track Order
          </button>
          <button className="btn btn-ghost btn-lg" onClick={() => setOrderSuccess(null)}>
            Order More
          </button>
        </div>
      </div>
    </CustomerLayout>
  )

  const categories = ['all', ...Array.from(new Set(products.map(p => p.category)))]
  const visible    = activeCategory === 'all' ? products : products.filter(p => p.category === activeCategory)

  return (
    <CustomerLayout>
      {/* Registration prompt — only shown if not yet registered */}
      {!customerID && (
        <div className="register-prompt" style={{ marginBottom: 28 }}>
          <h2>Welcome</h2>
          <p>Tell us your name so we can save your order history.</p>
          <form onSubmit={handleRegister}>
            <div className="register-form-row" style={{ marginBottom: regError ? 10 : 0 }}>
              <input
                className="field" placeholder="First name"
                value={firstName} onChange={e => setFirstName(e.target.value)} required
                style={{ flex: 1 }}
              />
              <input
                className="field" placeholder="Last name"
                value={lastName} onChange={e => setLastName(e.target.value)} required
                style={{ flex: 1 }}
              />
              <button className="btn btn-amber" type="submit" disabled={isRegistering}>
                {isRegistering ? 'Saving…' : 'Start ordering'}
              </button>
            </div>
            {regError && <p style={{ color: '#fca5a5', fontSize: 13, marginTop: 6 }}>{regError}</p>}
          </form>
        </div>
      )}

      <div className="menu-layout">
        {/* Left: menu */}
        <div>
          <div className="tab-bar">
            {categories.map(cat => (
              <button
                key={cat}
                className={`tab-pill${activeCategory === cat ? ' active' : ''}`}
                onClick={() => setActiveCategory(cat)}
              >
                {cat.charAt(0).toUpperCase() + cat.slice(1)}
              </button>
            ))}
          </div>

          <div className="menu-grid">
            {visible.map(p => {
              const qty = cartQty(p.productID)
              return (
                <div key={p.productID} className={`product-card${qty > 0 ? ' in-cart' : ''}`}>
                  <div>
                    <div className="product-name">{p.name}</div>
                    <div className="product-price">${p.price.toFixed(2)}</div>
                  </div>
                  {qty === 0 ? (
                    <button className="btn btn-secondary btn-sm" onClick={() => changeQty(p, 1)}>Add</button>
                  ) : (
                    <div className="qty-ctrl">
                      <button className="qty-btn" onClick={() => changeQty(p, -1)}>−</button>
                      <span className="qty-num">{qty}</span>
                      <button className="qty-btn" onClick={() => changeQty(p, 1)}>+</button>
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        </div>

        {/* Right: cart */}
        <div className="cart-panel">
          <div className="card">
            <h3 style={{ marginBottom: 16, fontSize: 18 }}>
              Your order{cartCount > 0 ? ` · ${cartCount} item${cartCount !== 1 ? 's' : ''}` : ''}
            </h3>

            {cart.length === 0 ? (
              <p className="text-muted text-sm" style={{ paddingBottom: 8 }}>Nothing added yet.</p>
            ) : (
              <>
                <div style={{ marginBottom: 16 }}>
                  {cart.map(item => (
                    <div key={item.product.productID} className="cart-item">
                      <div className="qty-ctrl" style={{ flexShrink: 0 }}>
                        <button className="qty-btn" onClick={() => changeQty(item.product, -1)}>−</button>
                        <span className="qty-num">{item.quantity}</span>
                        <button className="qty-btn" onClick={() => changeQty(item.product, 1)}>+</button>
                      </div>
                      <span className="cart-item-name">{item.product.name}</span>
                      <span className="cart-item-price">${(item.product.price * item.quantity).toFixed(2)}</span>
                    </div>
                  ))}
                </div>

                <div className="cart-total" style={{ marginBottom: 20 }}>
                  <span>Total</span>
                  <span>${cartTotal.toFixed(2)}</span>
                </div>

                {/* Show checkout controls only if registered */}
                {customerID ? (
                  <div className="stack stack-sm">
                    <div>
                      <label className="field-label">Payment</label>
                      <select className="field" value={paymentMethod} onChange={e => setPaymentMethod(e.target.value)}>
                        <option value="card">Credit card</option>
                        <option value="cash">Cash</option>
                        <option value="online">Online payment</option>
                      </select>
                    </div>
                    <div>
                      <label className="field-label">Table (optional)</label>
                      <select className="field" value={tableID} onChange={e => setTableID(e.target.value)}>
                        <option value="">Takeaway / counter</option>
                        {tables.map(t => (
                          <option key={t.tableID} value={t.tableID}>
                            {t.location} — seats {t.capacity}
                          </option>
                        ))}
                      </select>
                    </div>
                    {orderError && <p className="inline-error">{orderError}</p>}
                    <button
                      className="btn btn-primary btn-full"
                      style={{ marginTop: 4 }}
                      onClick={handleOrder}
                      disabled={isOrdering}
                    >
                      {isOrdering ? 'Placing order…' : `Place order · $${cartTotal.toFixed(2)}`}
                    </button>
                  </div>
                ) : (
                  <p className="text-sm text-muted" style={{ textAlign: 'center', paddingTop: 4 }}>
                    Enter your name above to place this order.
                  </p>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    </CustomerLayout>
  )
}
