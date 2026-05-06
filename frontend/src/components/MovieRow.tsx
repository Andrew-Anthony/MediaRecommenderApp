import type { Movie } from '../types';
import MovieCard from './MovieCard';
import './MovieRow.css';

interface MovieRowProps {
  title: string;
  movies: Movie[];
  onMovieClick?: (movie: Movie) => void;
}

export default function MovieRow({ title, movies, onMovieClick }: MovieRowProps) {
  if (movies.length === 0) {
    return null;
  }

  return (
    <div className="movie-row">
      <div className="movie-row-header">
        <h2 className="movie-row-title">{title}</h2>
      </div>
      <div className="movie-row-list">
        {movies.map((movie) => (
          <MovieCard key={movie.id} movie={movie} onClick={onMovieClick} />
        ))}
      </div>
    </div>
  );
}
