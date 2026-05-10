import os
import numpy as np
import pandas as pd
from surprise import Dataset, Reader, SVD

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE_DIR, "data", "ml-latest-small")

model = None
ratings_df = None
movies_df = None
links_df = None
genre_matrix = None
all_genres = []
tmdb_to_movielens = {}


def train_model():
    global model, ratings_df, movies_df, links_df, genre_matrix, all_genres, tmdb_to_movielens

    ratings_df = pd.read_csv(os.path.join(DATA_DIR, "ratings.csv"))
    movies_df = pd.read_csv(os.path.join(DATA_DIR, "movies.csv"))
    links_df = pd.read_csv(os.path.join(DATA_DIR, "links.csv"))

    reader = Reader(rating_scale=(0.5, 5.0))
    data = Dataset.load_from_df(ratings_df[["userId", "movieId", "rating"]], reader)
    trainset = data.build_full_trainset()

    model = SVD()
    model.fit(trainset)
    print(f"[SVD] Trained on {trainset.n_users} users and {trainset.n_items} movies")

    for _, row in links_df.iterrows():
        if pd.notna(row["tmdbId"]):
            tmdb_to_movielens[int(row["tmdbId"])] = int(row["movieId"])
    print(f"[Links] Mapped {len(tmdb_to_movielens)} TMDB IDs to MovieLens IDs")

    all_genres_set = set()
    for genres_str in movies_df["genres"].dropna():
        if genres_str != "(no genres listed)":
            all_genres_set.update(genres_str.split("|"))

    all_genres = sorted(all_genres_set)

    genre_data = []
    for _, row in movies_df.iterrows():
        movie_genres = set(row["genres"].split("|")) if row["genres"] != "(no genres listed)" else set()
        genre_data.append({genre: 1 if genre in movie_genres else 0 for genre in all_genres})

    genre_matrix = pd.DataFrame(genre_data, index=movies_df["movieId"])
    print(f"[Content] Genre matrix: {len(genre_matrix)} movies x {len(all_genres)} genres")


def _build_user_genre_profile(user_ratings):
    profile = np.zeros(len(all_genres))
    count = 0

    for r in user_ratings:
        tmdb_id = r.get("tmdbId")
        score = r.get("score", 5)

        movie_id = tmdb_to_movielens.get(tmdb_id)
        if movie_id is None or movie_id not in genre_matrix.index:
            continue

        movie_genres = genre_matrix.loc[movie_id].values
        profile += movie_genres * (score / 10.0)
        count += 1

    if count == 0:
        return None

    norm = np.linalg.norm(profile)
    if norm > 0:
        profile = profile / norm

    return profile


def _content_score(user_profile, movie_id):
    if movie_id not in genre_matrix.index:
        return 0.0

    movie_vector = genre_matrix.loc[movie_id].values.astype(float)

    norm = np.linalg.norm(movie_vector)
    if norm == 0:
        return 0.0

    movie_normalized = movie_vector / norm
    return float(np.dot(user_profile, movie_normalized))


def get_hybrid_recommendations(user_ratings, n=10, svd_weight=0.4, content_weight=0.6, exclude_tmdb_ids=None):
    if model is None:
        raise RuntimeError("Model not trained yet")

    # Exclude rated movies
    excluded_movielens_ids = set()
    for r in user_ratings:
        ml_id = tmdb_to_movielens.get(r.get("tmdbId"))
        if ml_id is not None:
            excluded_movielens_ids.add(ml_id)

    # Exclude watchlist movies
    if exclude_tmdb_ids:
        for tmdb_id in exclude_tmdb_ids:
            ml_id = tmdb_to_movielens.get(tmdb_id)
            if ml_id is not None:
                excluded_movielens_ids.add(ml_id)

    user_profile = _build_user_genre_profile(user_ratings)

    svd_user_id = 1

    all_movie_ids = movies_df["movieId"].unique()
    candidate_ids = [mid for mid in all_movie_ids if mid not in excluded_movielens_ids]

    predictions = []
    for movie_id in candidate_ids:
        svd_raw = model.predict(svd_user_id, movie_id).est
        svd_score = (svd_raw - 0.5) / 4.5
        cb_score = _content_score(user_profile, movie_id) if user_profile is not None else 0.0
        hybrid = svd_weight * svd_score + content_weight * cb_score
        predicted_rating = round(hybrid * 9 + 1, 1)

        predictions.append({
            "movieId": int(movie_id),
            "predictedRating": predicted_rating,
        })

    predictions.sort(key=lambda x: x["predictedRating"], reverse=True)
    top_predictions = predictions[:n]

    for pred in top_predictions:
        link = links_df[links_df["movieId"] == pred["movieId"]]
        if not link.empty and pd.notna(link.iloc[0]["tmdbId"]):
            pred["tmdbId"] = int(link.iloc[0]["tmdbId"])
        else:
            pred["tmdbId"] = None

    return top_predictions