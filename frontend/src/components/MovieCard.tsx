import type { Movie } from '../types';
import './MovieCard.css';

const TMDB_BACKDROP_BASE = 'https://image.tmdb.org/t/p/w780';

interface MovieCardProps {
  movie: Movie;
  onClick?: (movie: Movie) => void;
}

export default function MovieCard({ movie, onClick }: MovieCardProps) {
  const year = movie.releaseDate ? new Date(movie.releaseDate).getFullYear() : '';
  const imageUrl = movie.backdropPath
    ? `${TMDB_BACKDROP_BASE}${movie.backdropPath}`
    : movie.posterPath
    ? `https://image.tmdb.org/t/p/w500${movie.posterPath}`
    : null;

  return (
    <div className="movie-card" onClick={() => onClick?.(movie)}>
      {imageUrl ? (
        <img
          className="movie-card-image"
          src={imageUrl}
          alt={movie.title}
          loading="lazy"
        />
      ) : (
        <div className="movie-card-no-image">No image</div>
      )}
      <div className="movie-card-body">
        <div className="movie-card-title">{movie.title}</div>
        <div className="movie-card-meta">
          <span className="movie-card-rating">★ {movie.voteAverage?.toFixed(1)}</span>
          {year && <span>{year}</span>}
          {movie.genres?.slice(0, 2).map((g) => (
            <span key={g}>{g}</span>
          ))}
          {movie.predictedRating && (
            <span className="movie-card-predicted">AI {movie.predictedRating}⭐</span>
          )}
        </div>
      </div>
    </div>
  );
}
