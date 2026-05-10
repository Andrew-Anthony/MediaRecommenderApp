import { useState } from 'react';
import type { Movie } from '../types';
import MovieCard from './MovieCard';
import './MovieRow.css';

interface MovieRowProps {
  title: string;
  movies: Movie[];
  onMovieClick?: (movie: Movie) => void;
  initialCount?: number;
}

export default function MovieRow({ title, movies, onMovieClick, initialCount }: MovieRowProps) {
  const [visibleCount, setVisibleCount] = useState(initialCount ?? movies.length);

  if (movies.length === 0) {
    return null;
  }

  const displayedMovies = movies.slice(0, visibleCount);
  const hasMore = visibleCount < movies.length;

  const handleShowMore = () => {
    setVisibleCount((prev) => Math.min(prev + (initialCount ?? 12), movies.length));
  };

  return (
    <div className="movie-row">
      <div className="movie-row-header">
        <h2 className="movie-row-title">{title}</h2>
      </div>
      <div className="movie-row-list">
        {displayedMovies.map((movie) => (
          <MovieCard key={movie.id} movie={movie} onClick={onMovieClick} />
        ))}
      </div>
      {hasMore && (
        <div className="movie-row-show-more">
          <button className="movie-row-show-more-btn" onClick={handleShowMore}>
            Show More ({movies.length - visibleCount} remaining)
          </button>
        </div>
      )}
    </div>
  );
}
