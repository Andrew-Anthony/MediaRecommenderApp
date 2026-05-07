export interface Movie {
  id: number;
  tmdbId: number;
  title: string;
  overview: string;
  posterPath: string;
  backdropPath: string;
  releaseDate: string;
  voteAverage: number;
  genres: string[];
  predictedRating?: number;
}

export interface User {
  id: number;
  username: string;
  email: string;
  createdAt: string;
}

export interface AuthResponse {
  userId: number;
  token: string;
  username: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}
