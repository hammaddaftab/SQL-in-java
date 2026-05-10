import { Link } from 'react-router-dom'

export default function CustomerLayout({ children }: { children: React.ReactNode }) {
  const firstName = localStorage.getItem('firstName')

  return (
    <div className="app-shell">
      <header className="topbar">
        <Link to="/" className="topbar-logo">The Daily Grind</Link>
        <nav className="topbar-links">
          <Link to="/">Menu</Link>
          <Link to="/orders">My Orders</Link>
          {firstName && <span className="topbar-name">Hi, {firstName}</span>}
        </nav>
      </header>
      <main className="page-body">{children}</main>
    </div>
  )
}
