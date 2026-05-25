import { useState, useEffect, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import './Profile.css';

interface UserData {
  id: number;
  username: string;
  email: string;
  createdAt: string;
}

interface Stats {
  watchlistCount: number;
  ratingsCount: number;
}

const parseError = (err: any): string => {
  const data = err.response?.data;
  if (typeof data === 'string') return data;
  if (data?.message) return data.message;
  return 'Something went wrong';
};

export default function ProfilePage() {
  const { username, userId, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const [userData, setUserData] = useState<UserData | null>(null);
  const [stats, setStats] = useState<Stats>({ watchlistCount: 0, ratingsCount: 0 });

  const [newUsername, setNewUsername] = useState('');
  const [newEmail, setNewEmail] = useState('');
  const [profileMsg, setProfileMsg] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordMsg, setPasswordMsg] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  useEffect(() => {
    if (!userId) return;

    const fetchData = async () => {
      try {
        const userRes = await api.get<UserData>(`/users/${userId}`);
        setUserData(userRes.data);
        setNewUsername(userRes.data.username);
        setNewEmail(userRes.data.email);

        const [watchlistRes, ratingsRes] = await Promise.all([
          api.get<any[]>(`/users/${userId}/watchlist`),
          api.get<any[]>(`/users/${userId}/ratings`),
        ]);
        setStats({
          watchlistCount: watchlistRes.data.length,
          ratingsCount: ratingsRes.data.length,
        });
      } catch (err) {
        console.error('Failed to fetch profile:', err);
      }
    };

    fetchData();
  }, [userId]);

  const handleUpdateProfile = async (e: FormEvent) => {
    e.preventDefault();
    if (!userId) return;
    setProfileMsg(null);

    try {
      const res = await api.put<UserData>(`/users/${userId}`, {
        username: newUsername,
        email: newEmail,
      });
      setUserData(res.data);

      if (res.data.username !== username) {
        logout();
        // Hard redirect so the new login gets a fresh JWT with the new username
        window.location.href = '/login?reason=username_changed';
      } else {
        setProfileMsg({ text: 'Profile updated successfully!', type: 'success' });
      }
    } catch (err: any) {
      setProfileMsg({ text: parseError(err), type: 'error' });
    }
  };

  const handleChangePassword = async (e: FormEvent) => {
    e.preventDefault();
    if (!userId) return;
    setPasswordMsg(null);

    if (newPassword !== confirmPassword) {
      setPasswordMsg({ text: 'New passwords do not match', type: 'error' });
      return;
    }

    if (newPassword.length < 6) {
      setPasswordMsg({ text: 'Password must be at least 6 characters', type: 'error' });
      return;
    }

    try {
      await api.patch(`/users/${userId}/password`, {
        currentPassword,
        newPassword,
      });
      setPasswordMsg({ text: 'Password changed successfully!', type: 'success' });
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err: any) {
      setPasswordMsg({ text: parseError(err), type: 'error' });
    }
  };

  const memberSince = userData?.createdAt
    ? new Date(userData.createdAt).toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
    : '';

  return (
    <div className="profile-page">
      <h1>My Profile</h1>

      {/* Stats — full width */}
      <div className="profile-section" style={{ marginBottom: 20 }}>
        <div className="profile-section-title">Activity</div>
        <div className="profile-stats">
          <div className="profile-stat">
            <span className="profile-stat-value">{stats.ratingsCount}</span>
            <span className="profile-stat-label">Movies Rated</span>
          </div>
          <div className="profile-stat">
            <span className="profile-stat-value">{stats.watchlistCount}</span>
            <span className="profile-stat-label">In Watchlist</span>
          </div>
          {memberSince && (
            <div className="profile-stat">
              <span className="profile-stat-value">{memberSince}</span>
              <span className="profile-stat-label">Member Since</span>
            </div>
          )}
        </div>
      </div>

      {/* Two-column grid */}
      <div className="profile-grid">
        {/* Left — Account Info */}
        <div className="profile-section">
          <div className="profile-section-title">Account Info</div>
          <form className="profile-form" onSubmit={handleUpdateProfile}>
            <div className="form-group">
              <label>Username</label>
              <input
                type="text"
                value={newUsername}
                onChange={(e) => setNewUsername(e.target.value)}
                required
              />
            </div>
            <div className="form-group">
              <label>Email <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>(cannot be changed)</span></label>
              <input
                type="email"
                value={newEmail}
                disabled
                style={{ opacity: 0.4, cursor: 'not-allowed' }}
              />
            </div>
            {profileMsg && (
              <div className={`profile-message ${profileMsg.type}`}>{profileMsg.text}</div>
            )}
            <button type="submit" className="profile-btn profile-btn-primary">
              Save Changes
            </button>
          </form>
        </div>

        {/* Right — Change Password + Sign Out */}
        <div className="profile-section">
          <div className="profile-section-title">Change Password</div>
          <form className="profile-form" onSubmit={handleChangePassword}>
            <div className="form-group">
              <label>Current Password</label>
              <input
                type="password"
                placeholder="Enter current password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                required
              />
            </div>
            <div className="form-group">
              <label>New Password</label>
              <input
                type="password"
                placeholder="Enter new password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
              />
            </div>
            <div className="form-group">
              <label>Confirm New Password</label>
              <input
                type="password"
                placeholder="Confirm new password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
              />
            </div>
            {passwordMsg && (
              <div className={`profile-message ${passwordMsg.type}`}>{passwordMsg.text}</div>
            )}
            <button type="submit" className="profile-btn profile-btn-primary">
              Change Password
            </button>
          </form>
        </div>
      </div>

      <div className="profile-section" style={{ marginTop: 20 }}>
        <div className="profile-section-title">Sign Out</div>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginBottom: 16 }}>
          Sign out of your account on this device.
        </p>
        <button type="button" className="profile-btn profile-btn-danger" onClick={handleLogout}>
          Sign Out
        </button>
      </div>

      <div className="profile-section profile-credits" style={{ marginTop: 20 }}>
        <div className="profile-section-title">Credits</div>
        <div className="profile-credits-content">
          <a
            href="https://www.themoviedb.org/"
            target="_blank"
            rel="noopener noreferrer"
            className="profile-credits-logo"
          >
            <img src="/tmdb-logo.svg" alt="TMDB" />
          </a>
          <p>
            This product uses the TMDB API but is not endorsed or certified by TMDB.
          </p>
        </div>
      </div>
    </div>
  );
}
