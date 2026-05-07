import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import type { Movie } from '../types';
import MovieRow from '../components/MovieRow';
import './Home.css';

export default function HomePage() {
  const { isAuthenticated, userId } = useAuth();
  const navigate = useNavigate();

  const [allMovies, setAllMovies] = useState<Movie[]>([]);
  const [recommendations, setRecommendations] = useState<Movie[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const moviesRes = await api.get<Movie[]>('/movies');
        setAllMovies(moviesRes.data);

        if (isAuthenticated) {
          try {
            const recRes = await api.get<Movie[]>('/recommendations', {
              params: { user_id: userId, n: 15 },
            });
            setRecommendations(recRes.data);
          } catch {
            console.log('Recommendations unavailable');
          }
        }
      } catch (err) {
        console.error('Failed to fetch movies:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [isAuthenticated, userId]);

  const handleMovieClick = (movie: Movie) => {
    navigate(`/movie/${movie.id}`);
  };

  if (loading) {
    return (
      <div className="home-page">
        <div className="home-loading">Loading movies...</div>
      </div>
    );
  }

  return (
    <div className="home-page">
      <div className="home-hero">
        <div className="home-hero-content">
          <h1 className="home-hero-title">Discover movies tailored for you</h1>
          <p className="home-hero-subtitle">
            Get AI-powered recommendations based on your taste. Rate movies, build your watchlist,
            and find your next favorite film.
          </p>
          {!isAuthenticated && (
            <button className="home-hero-btn" onClick={() => navigate('/register')}>
              Get Started
            </button>
          )}
        </div>
      </div>

      <div className="home-sections">
        {recommendations.length > 0 && (
          <MovieRow
            title="Recommended for you"
            movies={recommendations}
            onMovieClick={handleMovieClick}
          />
        )}

        <MovieRow
          title="All Movies"
          movies={allMovies}
          onMovieClick={handleMovieClick}
        />
      </div>
    </div>
  );
}
