import { useState, useEffect } from 'react';
import { api } from '../api';
import type { Product, CafeTable } from '../api';
import CustomerLayout from '../components/CustomerLayout';
import { useNavigate } from 'react-router-dom';

export default function MenuPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [tables, setTables] = useState<CafeTable[]>([]);
  const [loading, setLoading] = useState(true);
  const [cart, setCart] = useState<{product: Product, quantity: number}[]>([]);
  
  // Registration state
  const [customerID, setCustomerID] = useState<string | null>(localStorage.getItem('customerID'));
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  
  // Checkout state
  const [paymentMethod, setPaymentMethod] = useState('card');
  const [tableID, setTableID] = useState('');
  const [isCheckingOut, setIsCheckingOut] = useState(false);
  const [checkoutSuccess, setCheckoutSuccess] = useState<{orderID: number, total: number} | null>(null);

  const navigate = useNavigate();

  useEffect(() => {
    async function load() {
      try {
        const [prodRes, tabRes] = await Promise.all([
          api.getProducts(),
          api.getTables()
        ]);
        setProducts(prodRes.products);
        setTables(tabRes.tables);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    }
    load();
    
    const savedCart = localStorage.getItem('cart');
    if (savedCart) setCart(JSON.parse(savedCart));
  }, []);

  const saveCart = (newCart: any) => {
    setCart(newCart);
    localStorage.setItem('cart', JSON.stringify(newCart));
  };

  const addToCart = (product: Product) => {
    const existing = cart.find(item => item.product.productID === product.productID);
    if (existing) {
      saveCart(cart.map(item => item.product.productID === product.productID ? { ...item, quantity: item.quantity + 1 } : item));
    } else {
      saveCart([...cart, { product, quantity: 1 }]);
    }
  };

  const removeFromCart = (productID: number) => {
    saveCart(cart.filter(item => item.product.productID !== productID));
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const customer = await api.registerCustomer(firstName, lastName);
      localStorage.setItem('customerID', String(customer.customerID));
      localStorage.setItem('firstName', customer.firstName);
      localStorage.setItem('lastName', customer.lastName);
      setCustomerID(String(customer.customerID));
    } catch (err) {
      console.error(err);
      alert('Registration failed');
    }
  };

  const handleCheckout = async () => {
    if (!customerID) return;
    try {
      setIsCheckingOut(true);
      const res = await api.placeOrder({
        customerID: parseInt(customerID, 10),
        tableID: tableID ? parseInt(tableID, 10) : undefined,
        paymentMethod,
        items: cart.map(item => ({ productID: item.product.productID, quantity: item.quantity }))
      });
      setCheckoutSuccess(res);
      saveCart([]);
    } catch (err) {
      console.error(err);
      alert('Checkout failed');
    } finally {
      setIsCheckingOut(false);
    }
  };

  const cartTotal = cart.reduce((sum, item) => sum + item.product.price * item.quantity, 0);

  if (loading) return <CustomerLayout><div style={{ textAlign: 'center' }}><div className="loader"></div></div></CustomerLayout>;

  if (!customerID) {
    return (
      <CustomerLayout>
        <div style={{ maxWidth: 400, margin: '60px auto' }} className="card">
          <h2>Welcome to The Daily Grind</h2>
          <p style={{ marginBottom: 24, color: 'var(--text-muted)' }}>Please enter your name to start ordering.</p>
          <form onSubmit={handleRegister} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <input className="input-field" placeholder="First Name" value={firstName} onChange={e => setFirstName(e.target.value)} required />
            <input className="input-field" placeholder="Last Name" value={lastName} onChange={e => setLastName(e.target.value)} required />
            <button className="btn-primary" type="submit">Start Ordering</button>
          </form>
        </div>
      </CustomerLayout>
    );
  }

  if (checkoutSuccess) {
    return (
      <CustomerLayout>
        <div style={{ maxWidth: 500, margin: '60px auto', textAlign: 'center' }} className="card">
          <div style={{ fontSize: 48, marginBottom: 16 }}>☕</div>
          <h2>Order Confirmed!</h2>
          <p style={{ marginBottom: 24, fontSize: 18 }}>Your order #{checkoutSuccess.orderID} has been placed.</p>
          <p style={{ marginBottom: 32, fontWeight: 'bold' }}>Total: ${checkoutSuccess.total.toFixed(2)}</p>
          <button className="btn-primary" onClick={() => navigate(`/orders/${checkoutSuccess.orderID}`)}>
            View Order Status
          </button>
        </div>
      </CustomerLayout>
    );
  }

  // Group products by category
  const categories = Array.from(new Set(products.map(p => p.category)));

  return (
    <CustomerLayout>
      <div style={{ display: 'flex', gap: 40, alignItems: 'flex-start' }}>
        <div style={{ flex: 1 }}>
          <h1 style={{ marginBottom: 32 }}>Our Menu</h1>
          {categories.map(cat => (
            <div key={cat} style={{ marginBottom: 40 }}>
              <h2 style={{ borderBottom: '2px solid var(--border)', paddingBottom: 8, marginBottom: 20 }}>{cat}</h2>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 20 }}>
                {products.filter(p => p.category === cat).map(p => (
                  <div key={p.productID} className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '20px' }}>
                    <div>
                      <div style={{ fontWeight: 500, fontSize: 18, marginBottom: 4 }}>{p.name}</div>
                      <div style={{ color: 'var(--text-muted)' }}>${p.price.toFixed(2)}</div>
                    </div>
                    <button className="btn-secondary" onClick={() => addToCart(p)}>Add</button>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>

        <div style={{ width: 350, position: 'sticky', top: 40 }} className="card">
          <h2 style={{ marginBottom: 24 }}>Your Order</h2>
          {cart.length === 0 ? (
            <p style={{ color: 'var(--text-muted)' }}>Cart is empty</p>
          ) : (
            <>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16, marginBottom: 24 }}>
                {cart.map(item => (
                  <div key={item.product.productID} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                      <span style={{ fontWeight: 500 }}>{item.quantity}x</span> {item.product.name}
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                      <span>${(item.product.price * item.quantity).toFixed(2)}</span>
                      <button style={{ background: 'none', border: 'none', color: 'var(--danger)', fontSize: 20, padding: 0 }} onClick={() => removeFromCart(item.product.productID)}>&times;</button>
                    </div>
                  </div>
                ))}
              </div>
              <div style={{ borderTop: '1px solid var(--border)', paddingTop: 16, marginBottom: 24, display: 'flex', justifyContent: 'space-between', fontSize: 18, fontWeight: 600 }}>
                <span>Total</span>
                <span>${cartTotal.toFixed(2)}</span>
              </div>
              
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                <div>
                  <label style={{ display: 'block', marginBottom: 8, fontSize: 14, fontWeight: 500 }}>Payment Method</label>
                  <select className="input-field" value={paymentMethod} onChange={e => setPaymentMethod(e.target.value)}>
                    <option value="card">Credit Card</option>
                    <option value="cash">Cash</option>
                    <option value="online">Online Payment</option>
                  </select>
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: 8, fontSize: 14, fontWeight: 500 }}>Table (Optional)</label>
                  <select className="input-field" value={tableID} onChange={e => setTableID(e.target.value)}>
                    <option value="">Takeaway / Counter</option>
                    {tables.map(t => (
                      <option key={t.tableID} value={t.tableID}>{t.location} (Capacity: {t.capacity})</option>
                    ))}
                  </select>
                </div>
                <button 
                  className="btn-primary" 
                  style={{ width: '100%', marginTop: 8 }} 
                  onClick={handleCheckout}
                  disabled={isCheckingOut}
                >
                  {isCheckingOut ? 'Placing Order...' : `Pay $${cartTotal.toFixed(2)}`}
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </CustomerLayout>
  );
}
