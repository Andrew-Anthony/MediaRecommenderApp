import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import type { Movie } from '../types';
import MovieCard from '../components/MovieCard';
import './Search.css';

interface TmdbResult {
  id: number;
  title: string;
  overview: string;
  poster_path: string | null;
  backdrop_path: string | null;
  release_date: string;
  vote_average: number;
  genre_ids: number[];
}

interface TmdbSearchResponse {
  results: TmdbResult[];
}

export default function SearchPage() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<Movie[]>([]);
  const [searched, setSearched] = useState(false);
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();

  const handleSearch = async (searchQuery: string) => {
    setQuery(searchQuery);

    if (searchQuery.trim().length < 2) {
      setResults([]);
      setSearched(false);
      return;
    }

    setLoading(true);
    setSearched(true);

    try {
      // Search local DB first
      const localRes = await api.get<Movie[]>('/movies/search', {
        params: { keyword: searchQuery },
      });

      if (localRes.data.length > 0) {
        setResults(localRes.data);
      } else {
        // Fallback to TMDB search
        const tmdbRes = await api.get<TmdbSearchResponse>('/tmdb/search', {
          params: { query: searchQuery },
        });

        const mapped: Movie[] = tmdbRes.data.results
          .filter((r) => r.poster_path || r.backdrop_path)
          .slice(0, 20)
          .map((r) => ({
            id: 0,
            tmdbId: r.id,
            title: r.title,
            overview: r.overview,
            posterPath: r.poster_path || '',
            backdropPath: r.backdrop_path || '',
            releaseDate: r.release_date,
            voteAverage: r.vote_average,
            genres: [],
          }));

        setResults(mapped);
      }
    } catch (err) {
      console.error('Search failed:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleMovieClick = async (movie: Movie) => {
    if (movie.id > 0) {
      navigate(`/movie/${movie.id}`);
    } else {
      // Import from TMDB first, then navigate
      try {
        const res = await api.post<Movie>(`/tmdb/import/${movie.tmdbId}`);
        navigate(`/movie/${res.data.id}`);
      } catch (err) {
        console.error('Failed to import movie:', err);
      }
    }
  };

  return (
    <div className="search-page">
      <div className="search-header">
        <h1>Search Movies</h1>
        <div className="search-input-wrapper">
          <span className="search-icon">🔍</span>
          <input
            className="search-input"
            type="text"
            placeholder="Search for a movie..."
            value={query}
            onChange={(e) => handleSearch(e.target.value)}
            autoFocus
          />
        </div>
        {!searched && <p className="search-hint">Search movies from our database and TMDB</p>}
      </div>

      {loading && <p className="search-empty">Searching...</p>}

      {!loading && searched && results.length === 0 && (
        <p className="search-empty">No movies found for "{query}"</p>
      )}

      <div className="search-results">
        {results.map((movie, index) => (
          <MovieCard key={movie.id || `tmdb-${movie.tmdbId}-${index}`} movie={movie} onClick={handleMovieClick} />
        ))}
      </div>
    </div>
  );
}
