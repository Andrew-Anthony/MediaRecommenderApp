import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import type { Movie } from '../types';
import './MovieDetail.css';

const TMDB_BACKDROP = 'https://image.tmdb.org/t/p/original';
const TMDB_POSTER = 'https://image.tmdb.org/t/p/w500';

interface RatingData {
  id: number;
  userId: number;
  username: string;
  movieId: number;
  movieTitle: string;
  score: number;
  review: string | null;
  createdAt: string;
}

interface WatchlistData {
  id: number;
  status: string;
}

const WATCHLIST_STATUSES = [
  { value: 'PLAN_TO_WATCH', label: 'Plan to Watch' },
  { value: 'WATCHING', label: 'Watching' },
  { value: 'WATCHED', label: 'Watched' },
  { value: 'DROPPED', label: 'Dropped' },
];

export default function MovieDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isAuthenticated, userId } = useAuth();

  const [movie, setMovie] = useState<Movie | null>(null);
  const [loading, setLoading] = useState(true);

  // Watchlist state
  const [watchlistStatus, setWatchlistStatus] = useState<string | null>(null);
  const [watchlistMenuOpen, setWatchlistMenuOpen] = useState(false);

  // Rating state
  const [userRating, setUserRating] = useState<RatingData | null>(null);
  const [allRatings, setAllRatings] = useState<RatingData[]>([]);
  const [isEditing, setIsEditing] = useState(false);
  const [ratingScore, setRatingScore] = useState(0);
  const [ratingHover, setRatingHover] = useState(0);
  const [reviewText, setReviewText] = useState('');

  useEffect(() => {
    const fetchMovie = async () => {
      try {
        const res = await api.get<Movie>(`/movies/${id}`);
        setMovie(res.data);

        // Fetch movie ratings
        const ratingsRes = await api.get<RatingData[]>(`/movies/${id}/ratings`);
        setAllRatings(ratingsRes.data);

        // Fetch user's rating if authenticated
        if (isAuthenticated && userId) {
          try {
            const userRatingRes = await api.get<RatingData>(`/users/${userId}/movies/${id}/ratings`);
            setUserRating(userRatingRes.data);
          } catch {
            // No rating yet
          }

          // Fetch watchlist status
          try {
            const watchlistRes = await api.get<WatchlistData[]>(`/users/${userId}/watchlist`);
            const found = watchlistRes.data.find((w) => w.id === Number(id) || (w as any).movieId === Number(id));
            if (found) setWatchlistStatus(found.status);
          } catch {
            // Not in watchlist
          }
        }
      } catch (err) {
        console.error('Failed to fetch movie:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchMovie();
  }, [id, isAuthenticated, userId]);

  const handleWatchlistAdd = async (status: string) => {
    if (!userId || !id) return;
    try {
      if (watchlistStatus) {
        await api.put(`/users/${userId}/watchlist/${id}`, { status });
      } else {
        await api.post(`/users/${userId}/watchlist`, { movieId: Number(id), status });
      }
      setWatchlistStatus(status);
    } catch (err) {
      console.error('Watchlist error:', err);
    }
    setWatchlistMenuOpen(false);
  };

  const handleWatchlistRemove = async () => {
    if (!userId || !id) return;
    try {
      await api.delete(`/users/${userId}/watchlist/${id}`);
      setWatchlistStatus(null);
    } catch (err) {
      console.error('Remove watchlist error:', err);
    }
    setWatchlistMenuOpen(false);
  };

  const handleSubmitRating = async () => {
    if (!userId || !id || ratingScore === 0) return;
    try {
      const body = { score: ratingScore, review: reviewText || null };
      let res;
      if (userRating) {
        res = await api.put<RatingData>(`/users/${userId}/movies/${id}/ratings`, body);
      } else {
        res = await api.post<RatingData>(`/users/${userId}/movies/${id}/ratings`, body);
      }
      setUserRating(res.data);
      setIsEditing(false);

      // Refresh all ratings
      const ratingsRes = await api.get<RatingData[]>(`/movies/${id}/ratings`);
      setAllRatings(ratingsRes.data);
    } catch (err) {
      console.error('Rating error:', err);
    }
  };

  const handleDeleteRating = async () => {
    if (!userId || !id) return;
    try {
      await api.delete(`/users/${userId}/movies/${id}/ratings`);
      setUserRating(null);
      setRatingScore(0);
      setReviewText('');

      const ratingsRes = await api.get<RatingData[]>(`/movies/${id}/ratings`);
      setAllRatings(ratingsRes.data);
    } catch (err) {
      console.error('Delete rating error:', err);
    }
  };

  const startEditing = () => {
    setRatingScore(userRating?.score || 0);
    setReviewText(userRating?.review || '');
    setIsEditing(true);
  };

  if (loading) {
    return <div className="movie-detail-loading">Loading...</div>;
  }

  if (!movie) {
    return <div className="movie-detail-loading">Movie not found</div>;
  }

  const year = movie.releaseDate ? new Date(movie.releaseDate).getFullYear() : '';
  const otherRatings = allRatings.filter((r) => r.userId !== userId);

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

          {/* Action buttons */}
          {isAuthenticated && (
            <div className="movie-detail-actions">
              <div className="watchlist-dropdown">
                <button
                  className={`action-btn ${watchlistStatus ? 'action-btn-primary' : 'action-btn-outline'}`}
                  onClick={() => setWatchlistMenuOpen(!watchlistMenuOpen)}
                >
                  {watchlistStatus
                    ? WATCHLIST_STATUSES.find((s) => s.value === watchlistStatus)?.label
                    : '+ Add to Watchlist'}
                </button>
                {watchlistMenuOpen && (
                  <div className="watchlist-menu">
                    {WATCHLIST_STATUSES.map((s) => (
                      <button
                        key={s.value}
                        className={`watchlist-menu-item ${watchlistStatus === s.value ? 'active' : ''}`}
                        onClick={() => handleWatchlistAdd(s.value)}
                      >
                        {s.label}
                      </button>
                    ))}
                    {watchlistStatus && (
                      <button className="watchlist-menu-item" style={{ color: 'var(--accent-error)' }} onClick={handleWatchlistRemove}>
                        Remove
                      </button>
                    )}
                  </div>
                )}
              </div>

              {!userRating && !isEditing && (
                <button className="action-btn action-btn-outline" onClick={startEditing}>
                  ★ Rate this movie
                </button>
              )}
            </div>
          )}

          {/* Rating section */}
          {isAuthenticated && (
            <div className="movie-detail-rating-section">
              <h2 className="rating-section-title">
                {userRating ? 'Your Rating' : isEditing ? 'Rate this movie' : 'Ratings & Reviews'}
              </h2>

              {/* User's existing review */}
              {userRating && !isEditing && (
                <div className="user-review">
                  <div className="user-review-header">
                    <span className="user-review-score">
                      {'★'.repeat(Math.round(userRating.score))} {userRating.score}/10
                    </span>
                    <div className="user-review-actions">
                      <button className="user-review-btn" onClick={startEditing}>Edit</button>
                      <button className="user-review-btn" onClick={handleDeleteRating}>Delete</button>
                    </div>
                  </div>
                  {userRating.review && (
                    <p className="user-review-text">{userRating.review}</p>
                  )}
                </div>
              )}

              {/* Rating form */}
              {isEditing && (
                <div>
                  <div className="star-rating">
                    {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((star) => (
                      <button
                        key={star}
                        className={`star-btn ${star <= (ratingHover || ratingScore) ? 'filled' : ''}`}
                        onMouseEnter={() => setRatingHover(star)}
                        onMouseLeave={() => setRatingHover(0)}
                        onClick={() => setRatingScore(star)}
                      >
                        ★
                      </button>
                    ))}
                    {ratingScore > 0 && <span style={{ color: 'var(--text-secondary)', marginLeft: 8 }}>{ratingScore}/10</span>}
                  </div>
                  <textarea
                    className="review-textarea"
                    placeholder="Write a review (optional)..."
                    value={reviewText}
                    onChange={(e) => setReviewText(e.target.value)}
                  />
                  <div className="rating-form-actions">
                    <button className="action-btn action-btn-primary" onClick={handleSubmitRating}>
                      {userRating ? 'Update' : 'Submit'}
                    </button>
                    <button className="action-btn action-btn-outline" onClick={() => setIsEditing(false)}>
                      Cancel
                    </button>
                  </div>
                </div>
              )}

              {/* Other reviews */}
              {otherRatings.length > 0 && (
                <div className="reviews-list">
                  <h3 className="rating-section-title" style={{ fontSize: '1rem', marginTop: 24 }}>
                    Other Reviews ({otherRatings.length})
                  </h3>
                  {otherRatings.map((r) => (
                    <div key={r.id} className="review-item">
                      <div className="review-item-header">
                        <span className="review-item-user">{r.username}</span>
                        <span className="review-item-score">★ {r.score}/10</span>
                        <span className="review-item-date">
                          {new Date(r.createdAt).toLocaleDateString()}
                        </span>
                      </div>
                      {r.review && <p className="review-item-text">{r.review}</p>}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
