import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import type { Movie } from '../types';
import MovieRow from '../components/MovieRow';
import './Home.css';

const TMDB_BACKDROP_BASE = 'https://image.tmdb.org/t/p/original';

const FEATURED_GENRES = ['Action', 'Drama', 'Science Fiction', 'Comedy', 'Thriller', 'Adventure'];

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

  const topRated = useMemo(() => {
    return [...allMovies]
      .sort((a, b) => (b.voteAverage ?? 0) - (a.voteAverage ?? 0))
      .slice(0, 12);
  }, [allMovies]);

  const genreSections = useMemo(() => {
    return FEATURED_GENRES.map((genre) => ({
      genre,
      movies: allMovies.filter((m) => m.genres?.includes(genre)),
    })).filter((section) => section.movies.length > 0);
  }, [allMovies]);

  const heroMovie = useMemo(() => {
    const candidates = allMovies.filter((m) => m.backdropPath && (m.voteAverage ?? 0) >= 7.5);
    if (candidates.length === 0) return null;
    return candidates[Math.floor(Math.random() * candidates.length)];
  }, [allMovies]);

  if (loading) {
    return (
      <div className="home-page">
        <div className="home-loading">Loading movies...</div>
      </div>
    );
  }

  return (
    <div className="home-page">
      <div
        className="home-hero"
        style={
          heroMovie?.backdropPath
            ? { backgroundImage: `url(${TMDB_BACKDROP_BASE}${heroMovie.backdropPath})` }
            : undefined
        }
      >
        <div className="home-hero-overlay" />
        <div className="home-hero-content">
          {heroMovie ? (
            <>
              <h1 className="home-hero-title">{heroMovie.title}</h1>
              <p className="home-hero-subtitle">{heroMovie.overview}</p>
              <div className="home-hero-actions">
                <button className="home-hero-btn" onClick={() => navigate(`/movie/${heroMovie.id}`)}>
                  View Details
                </button>
                {!isAuthenticated && (
                  <button className="home-hero-btn home-hero-btn-secondary" onClick={() => navigate('/register')}>
                    Get Started
                  </button>
                )}
              </div>
            </>
          ) : (
            <>
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
            </>
          )}
        </div>
      </div>

      <div className="home-sections">
        {recommendations.length > 0 && (
          <MovieRow
            title="🎯 Recommended for you"
            movies={recommendations}
            onMovieClick={handleMovieClick}
          />
        )}

        <MovieRow
          title="⭐ Top Rated"
          movies={topRated}
          onMovieClick={handleMovieClick}
        />

        {genreSections.map((section) => (
          <MovieRow
            key={section.genre}
            title={section.genre}
            movies={section.movies}
            onMovieClick={handleMovieClick}
            initialCount={6}
          />
        ))}

        <MovieRow
          title="🎬 All Movies"
          movies={allMovies}
          onMovieClick={handleMovieClick}
          initialCount={12}
        />
      </div>
    </div>
  );
}
