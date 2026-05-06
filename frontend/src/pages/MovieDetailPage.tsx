import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';
import type { Movie } from '../types';
import './MovieDetail.css';

const TMDB_BACKDROP = 'https://image.tmdb.org/t/p/original';
const TMDB_POSTER = 'https://image.tmdb.org/t/p/w500';

export default function MovieDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [movie, setMovie] = useState<Movie | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchMovie = async () => {
      try {
        const res = await api.get<Movie>(`/movies/${id}`);
        setMovie(res.data);
      } catch (err) {
        console.error('Failed to fetch movie:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchMovie();
  }, [id]);

  if (loading) {
    return <div className="movie-detail-loading">Loading...</div>;
  }

  if (!movie) {
    return <div className="movie-detail-loading">Movie not found</div>;
  }

  const year = movie.releaseDate ? new Date(movie.releaseDate).getFullYear() : '';

  return (
    <div className="movie-detail">
      <button className="movie-detail-back" onClick={() => navigate(-1)}>
        ← Back
      </button>

      <div className="movie-detail-hero">
        {movie.backdropPath ? (
          <img
            className="movie-detail-backdrop"
            src={`${TMDB_BACKDROP}${movie.backdropPath}`}
            alt={movie.title}
          />
        ) : (
          <div className="movie-detail-backdrop-fallback" />
        )}
      </div>

      <div className="movie-detail-content">
        {movie.posterPath ? (
          <img
            className="movie-detail-poster"
            src={`${TMDB_POSTER}${movie.posterPath}`}
            alt={movie.title}
          />
        ) : (
          <div className="movie-detail-poster-placeholder">No poster</div>
        )}

        <div className="movie-detail-info">
          <h1 className="movie-detail-title">{movie.title}</h1>

          <div className="movie-detail-meta">
            <span className="movie-detail-rating">★ {movie.voteAverage?.toFixed(1)}</span>
            {year && <span>{year}</span>}
          </div>

          <div className="movie-detail-genres">
            {movie.genres?.map((genre) => (
              <span key={genre} className="movie-detail-genre-tag">{genre}</span>
            ))}
          </div>

          <p className="movie-detail-overview">{movie.overview}</p>
        </div>
      </div>
    </div>
  );
}
