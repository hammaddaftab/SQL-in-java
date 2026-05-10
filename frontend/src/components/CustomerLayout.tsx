import React from 'react';
import { Link } from 'react-router-dom';

export default function CustomerLayout({ children }: { children: React.ReactNode }) {
  const firstName = localStorage.getItem('firstName');

  return (
    <div className="app-container">
      <header className="nav-header">
        <Link to="/" className="logo">The Daily Grind</Link>
        <div className="nav-links">
          <Link to="/">Menu</Link>
          <Link to="/orders">Orders</Link>
          {firstName && <span style={{ opacity: 0.7 }}>Hi, {firstName}</span>}
        </div>
      </header>
      <main className="main-content">
        {children}
      </main>
    </div>
  );
}
