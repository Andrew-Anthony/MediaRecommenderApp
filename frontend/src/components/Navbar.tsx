import { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Navbar.css';

export default function Navbar() {
  const { isAuthenticated, username, logout } = useAuth();
  const [scrolled, setScrolled] = useState(false);
  const location = useLocation();

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <nav className={`navbar ${scrolled ? 'scrolled' : ''}`}>
      <div className="navbar-left">
        <Link to="/" className="navbar-logo">MediaRecommender</Link>
        <div className="navbar-links">
          <Link to="/search" className={location.pathname === '/search' ? 'active' : ''}>Search</Link>
          {isAuthenticated && (
            <Link to="/watchlist" className={location.pathname === '/watchlist' ? 'active' : ''}>Watchlist</Link>
          )}
        </div>
      </div>

      <div className="navbar-user">
        {isAuthenticated ? (
          <>
            <Link to="/profile" className="navbar-username">{username}</Link>
            <button className="navbar-btn btn-logout" onClick={logout}>Logout</button>
          </>
        ) : (
          <Link to="/login" className="navbar-btn btn-login">Sign In</Link>
        )}
      </div>
    </nav>
  );
}
