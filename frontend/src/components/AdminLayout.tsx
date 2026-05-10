import { Link, useNavigate, useLocation } from 'react-router-dom'
import { api } from '../api'

const NAV = [
  { path: '/admin',           label: 'Dashboard' },
  { path: '/admin/orders',    label: 'Orders' },
  { path: '/admin/products',  label: 'Products' },
  { path: '/admin/inventory', label: 'Inventory' },
  { path: '/admin/sales',     label: 'Sales Report' },
]

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const navigate  = useNavigate()
  const location  = useLocation()

  const handleLogout = async () => {
    await api.adminLogout().catch(() => {})
    navigate('/admin/login')
  }

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <div className="admin-logo">Grind Admin</div>
        <nav className="admin-nav">
          {NAV.map(n => (
            <Link
              key={n.path}
              to={n.path}
              className={location.pathname === n.path ? 'active' : ''}
            >
              {n.label}
            </Link>
          ))}
          <div className="admin-nav-divider" />
          <div className="admin-nav-logout">
            <a href="#" onClick={e => { e.preventDefault(); handleLogout() }}>Log out</a>
          </div>
        </nav>
      </aside>
      <div className="admin-body">
        <div className="admin-page">{children}</div>
      </div>
    </div>
  )
}
