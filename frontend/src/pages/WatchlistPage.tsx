import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import type { Movie } from '../types';
import MovieCard from '../components/MovieCard';
import './Watchlist.css';

interface WatchlistItem {
  id: number;
  userId: number;
  movieId: number;
  movieTitle: string;
  status: string;
  addedAt: string;
}

const STATUS_TABS = [
  { label: 'All', value: '' },
  { label: 'Plan to Watch', value: 'PLAN_TO_WATCH' },
  { label: 'Watching', value: 'WATCHING' },
  { label: 'Watched', value: 'WATCHED' },
  { label: 'Dropped', value: 'DROPPED' },
];

const STATUS_DISPLAY: Record<string, string> = {
  PLAN_TO_WATCH: 'Plan to Watch',
  WATCHING: 'Watching',
  WATCHED: 'Watched',
  DROPPED: 'Dropped',
};

export default function WatchlistPage() {
  const { userId } = useAuth();
  const navigate = useNavigate();

  const [watchlistItems, setWatchlistItems] = useState<WatchlistItem[]>([]);
  const [movies, setMovies] = useState<Record<number, Movie>>({});
  const [activeTab, setActiveTab] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!userId) return;

    const fetchWatchlist = async () => {
      setLoading(true);
      try {
        const params: Record<string, string> = {};
        if (activeTab) params.status = activeTab;

        const res = await api.get<WatchlistItem[]>(`/users/${userId}/watchlist`, { params });
        setWatchlistItems(res.data);

        // Fetch movie details for each watchlist item
        const movieMap: Record<number, Movie> = { ...movies };
        for (const item of res.data) {
          if (!movieMap[item.movieId]) {
            try {
              const movieRes = await api.get<Movie>(`/movies/${item.movieId}`);
              movieMap[item.movieId] = movieRes.data;
            } catch {
              console.log('Movie not found:', item.movieId);
            }
          }
        }
        setMovies(movieMap);
      } catch (err) {
        console.error('Failed to fetch watchlist:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchWatchlist();
  }, [userId, activeTab]);

  const handleMovieClick = (movie: Movie) => {
    navigate(`/movie/${movie.id}`);
  };

  return (
    <div className="watchlist-page">
      <div className="watchlist-header">
        <h1>My Watchlist</h1>
        <div className="watchlist-tabs">
          {STATUS_TABS.map((tab) => (
            <button
              key={tab.value}
              className={`watchlist-tab ${activeTab === tab.value ? 'active' : ''}`}
              onClick={() => setActiveTab(tab.value)}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {loading && <p className="watchlist-empty">Loading...</p>}

      {!loading && watchlistItems.length === 0 && (
        <p className="watchlist-empty">
          {activeTab ? 'No movies with this status' : 'Your watchlist is empty. Start adding movies!'}
        </p>
      )}

      <div className="watchlist-grid">
        {watchlistItems.map((item) => {
          const movie = movies[item.movieId];
          if (!movie) return null;
          return (
            <div key={item.id} className="watchlist-card-wrapper">
              <span className="watchlist-status-badge">
                {STATUS_DISPLAY[item.status] || item.status}
              </span>
              <MovieCard movie={movie} onClick={handleMovieClick} />
            </div>
          );
        })}
      </div>
    </div>
  );
}
