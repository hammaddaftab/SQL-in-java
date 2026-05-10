import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { api } from '../api';

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = async () => {
    try {
      await api.adminLogout();
      navigate('/admin/login');
    } catch (err) {
      console.error(err);
    }
  };

  const links = [
    { path: '/admin', label: 'Dashboard' },
    { path: '/admin/orders', label: 'Orders' },
    { path: '/admin/products', label: 'Products' },
    { path: '/admin/inventory', label: 'Inventory' },
    { path: '/admin/sales', label: 'Sales Report' },
  ];

  return (
    <div className="admin-layout">
      <aside className="admin-sidebar">
        <div className="logo">Grind Admin</div>
        <nav className="admin-nav">
          {links.map((link) => (
            <Link
              key={link.path}
              to={link.path}
              className={location.pathname === link.path ? 'active' : ''}
            >
              {link.label}
            </Link>
          ))}
          <a href="#" onClick={(e) => { e.preventDefault(); handleLogout(); }}>Logout</a>
        </nav>
      </aside>
      <main className="admin-content">
        {children}
      </main>
    </div>
  );
}
