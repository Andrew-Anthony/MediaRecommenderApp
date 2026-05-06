import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Profile.css';

export default function ProfilePage() {
  const { username, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div className="profile-page">
      <div className="profile-card">
        <h1>Profile</h1>

        <div className="profile-field">
          <div className="profile-field-label">Username</div>
          <div className="profile-field-value">{username}</div>
        </div>

        <button className="profile-logout-btn" onClick={handleLogout}>
          Logout
        </button>
      </div>
    </div>
  );
}
