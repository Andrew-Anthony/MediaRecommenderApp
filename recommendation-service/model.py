import os
import pickle
import re

import numpy as np
import pandas as pd
from scipy.sparse import csr_matrix
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from surprise import Dataset, Reader, SVD

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE_DIR, "data", "ml-32m")
METADATA_PATH = os.path.join(BASE_DIR, "data", "tmdb_metadata.pkl")
TRAINED_CACHE_PATH = os.path.join(BASE_DIR, "data", "trained_model.pkl")

model = None
ratings_df = None
movies_df = None
links_df = None
tmdb_metadata = None
tfidf_matrix = None
movielens_id_to_row = {}
tmdb_to_movielens = {}
movie_quality = {}  # movielens_id -> (mean_rating, num_ratings)
movie_attributes = {}  # movielens_id -> {"year": int|None, "genres": set[str]}
_YEAR_RE = re.compile(r"\((\d{4})\)")

MIN_MEAN_RATING = 3.5  # ~7/10 on the app's 1-10 scale
MIN_RATING_COUNT = 50  # minimum MovieLens ratings for a reliable mean (bumped for ml-32m scale)
KEYWORD_BOOST = 0.25  # additive boost to hybrid score when a candidate matches requested TMDB keywords
RECENCY_BOOST = 0.085  # additive boost for recent films to counteract collaborative-filtering age bias
RECENT_YEAR_THRESHOLD = 2016


def _norm_token(s):
    """Lowercase and strip non-alphanumeric chars so each feature is a single TF-IDF token."""
    if not s:
        return ""
    return "".join(c.lower() for c in s if c.isalnum())


def _build_movie_soup(movielens_id, movielens_genres):
    """Combine genres + TMDB metadata into a single space-separated token string."""
    tokens = []

    if movielens_genres and movielens_genres != "(no genres listed)":
        for g in movielens_genres.split("|"):
            t = _norm_token(g)
            if t:
                tokens.append(t)

    meta = tmdb_metadata.get(movielens_id) if tmdb_metadata else None
    if meta:
        for kw in meta.get("keywords", []) or []:
            t = _norm_token(kw)
            if t:
                tokens.append(t)
        for actor in meta.get("cast", []) or []:
            t = _norm_token(actor)
            if t:
                tokens.append(t)
        director = meta.get("director")
        if director:
            t = _norm_token(director)
            if t:
                tokens.append(t)
        decade = meta.get("decade")
        if decade:
            tokens.append(decade.lower())

    return " ".join(tokens)


def _load_tmdb_metadata():
    if not os.path.exists(METADATA_PATH):
        print(f"[Content] WARNING: {METADATA_PATH} not found.")
        print("[Content] Run 'python fetch_tmdb_metadata.py' first for richer recommendations.")
        print("[Content] Falling back to MovieLens genres only.")
        return None
    with open(METADATA_PATH, "rb") as f:
        cache = pickle.load(f)
    valid = {k: v for k, v in cache.items() if v is not None}
    print(f"[Content] Loaded TMDB metadata for {len(valid)} movies (out of {len(cache)} cached entries)")
    return valid


def _save_cache():
    print(f"[Cache] Saving trained model to {TRAINED_CACHE_PATH}...")
    payload = {
        "model": model,
        "links_df": links_df,
        "tfidf_matrix": tfidf_matrix,
        "movielens_id_to_row": movielens_id_to_row,
        "tmdb_to_movielens": tmdb_to_movielens,
        "movie_quality": movie_quality,
        "movie_attributes": movie_attributes,
    }
    with open(TRAINED_CACHE_PATH, "wb") as f:
        pickle.dump(payload, f, protocol=pickle.HIGHEST_PROTOCOL)
    size_mb = os.path.getsize(TRAINED_CACHE_PATH) / (1024 * 1024)
    print(f"[Cache] Saved ({size_mb:.1f} MB).")


def _load_cache():
    global model, links_df, tfidf_matrix
    global movielens_id_to_row, tmdb_to_movielens, movie_quality, movie_attributes

    print(f"[Cache] Loading trained model from {TRAINED_CACHE_PATH}...")
    with open(TRAINED_CACHE_PATH, "rb") as f:
        payload = pickle.load(f)
    model = payload["model"]
    links_df = payload["links_df"]
    tfidf_matrix = payload["tfidf_matrix"]
    movielens_id_to_row = payload["movielens_id_to_row"]
    tmdb_to_movielens = payload["tmdb_to_movielens"]
    movie_quality = payload["movie_quality"]
    movie_attributes = payload["movie_attributes"]
    print(f"[Cache] Loaded: {tfidf_matrix.shape[0]} movies, "
          f"{len(tmdb_to_movielens)} TMDB mappings, "
          f"{sum(1 for m, c in movie_quality.values() if m >= MIN_MEAN_RATING and c >= MIN_RATING_COUNT)} quality-filtered.")
    print(f"[Cache] To force retraining, delete {TRAINED_CACHE_PATH}")


def train_model():
    global model, ratings_df, movies_df, links_df, tmdb_metadata
    global tfidf_matrix, movielens_id_to_row, tmdb_to_movielens, movie_quality, movie_attributes

    if os.path.exists(TRAINED_CACHE_PATH):
        try:
            _load_cache()
            return
        except Exception as e:
            print(f"[Cache] Failed to load ({e}). Falling back to full training...")

    print(f"[Train] Starting full training (data: {DATA_DIR})...")
    ratings_df = pd.read_csv(os.path.join(DATA_DIR, "ratings.csv"))
    movies_df = pd.read_csv(os.path.join(DATA_DIR, "movies.csv"))
    links_df = pd.read_csv(os.path.join(DATA_DIR, "links.csv"))

    quality_stats = ratings_df.groupby("movieId")["rating"].agg(["mean", "count"])
    movie_quality = {
        int(mid): (float(row["mean"]), int(row["count"]))
        for mid, row in quality_stats.iterrows()
    }
    eligible = sum(1 for m, c in movie_quality.values() if m >= MIN_MEAN_RATING and c >= MIN_RATING_COUNT)
    print(f"[Quality] {eligible} movies pass filter (mean >= {MIN_MEAN_RATING}, count >= {MIN_RATING_COUNT})")

    reader = Reader(rating_scale=(0.5, 5.0))
    data = Dataset.load_from_df(ratings_df[["userId", "movieId", "rating"]], reader)
    trainset = data.build_full_trainset()

    model = SVD(n_factors=50)
    model.fit(trainset)
    print(f"[SVD] Trained on {trainset.n_users} users and {trainset.n_items} movies")

    for _, row in links_df.iterrows():
        if pd.notna(row["tmdbId"]):
            tmdb_to_movielens[int(row["tmdbId"])] = int(row["movieId"])
    print(f"[Links] Mapped {len(tmdb_to_movielens)} TMDB IDs to MovieLens IDs")

    tmdb_metadata = _load_tmdb_metadata()

    soups = []
    movielens_ids = []
    for _, row in movies_df.iterrows():
        movielens_id = int(row["movieId"])
        raw_genres = row.get("genres", "") or ""
        title = row.get("title", "") or ""

        soup = _build_movie_soup(movielens_id, raw_genres)
        soups.append(soup)
        movielens_ids.append(movielens_id)

        year = None
        m = _YEAR_RE.search(title)
        if m:
            year = int(m.group(1))
        genres_set = set()
        if raw_genres and raw_genres != "(no genres listed)":
            genres_set = {g for g in raw_genres.split("|") if g}

        cast_set = set()
        director_str = None
        keywords_set = set()
        meta = tmdb_metadata.get(movielens_id) if tmdb_metadata else None
        if meta:
            for actor in meta.get("cast", []) or []:
                if actor:
                    cast_set.add(actor.lower())
            director_raw = meta.get("director")
            if director_raw:
                director_str = director_raw.lower()
            for kw in meta.get("keywords", []) or []:
                if kw:
                    keywords_set.add(kw.lower())

        movie_attributes[movielens_id] = {
            "year": year,
            "genres": genres_set,
            "cast": cast_set,
            "director": director_str,
            "keywords": keywords_set,
        }

    movielens_id_to_row = {mid: idx for idx, mid in enumerate(movielens_ids)}

    vectorizer = TfidfVectorizer(token_pattern=r"\S+", min_df=2, max_features=50000)
    tfidf_matrix = vectorizer.fit_transform(soups)
    print(f"[Content] TF-IDF matrix: {tfidf_matrix.shape[0]} movies x {tfidf_matrix.shape[1]} features")

    _save_cache()


def _build_user_feature_profile(user_ratings):
    """Weighted sum of TF-IDF vectors for the user's rated movies, L2-normalized."""
    profile = None

    for r in user_ratings:
        tmdb_id = r.get("tmdbId")
        score = r.get("score", 5)

        movie_id = tmdb_to_movielens.get(tmdb_id)
        if movie_id is None:
            continue
        row_idx = movielens_id_to_row.get(movie_id)
        if row_idx is None:
            continue

        weighted = tfidf_matrix[row_idx] * (score / 10.0)
        profile = weighted if profile is None else profile + weighted

    if profile is None:
        return None

    norm = float(np.sqrt(profile.multiply(profile).sum()))
    if norm > 0:
        profile = profile / norm

    return csr_matrix(profile)


def get_hybrid_recommendations(user_ratings, n=10, svd_weight=0.6, content_weight=0.4, exclude_tmdb_ids=None, apply_quality_filter=True, filters=None):
    if model is None:
        raise RuntimeError("Model not trained yet")

    filters = filters or {}
    filter_genres = set(filters.get("genres") or [])
    filter_min_year = filters.get("minYear")
    filter_max_year = filters.get("maxYear")
    filter_cast = [c.lower() for c in (filters.get("cast") or []) if c]
    filter_director = [d.lower() for d in (filters.get("director") or []) if d]
    filter_keywords = [k.lower() for k in (filters.get("keywords") or []) if k]

    excluded_movielens_ids = set()
    for r in user_ratings:
        ml_id = tmdb_to_movielens.get(r.get("tmdbId"))
        if ml_id is not None:
            excluded_movielens_ids.add(ml_id)

    if exclude_tmdb_ids:
        for tmdb_id in exclude_tmdb_ids:
            ml_id = tmdb_to_movielens.get(tmdb_id)
            if ml_id is not None:
                excluded_movielens_ids.add(ml_id)

    user_profile = _build_user_feature_profile(user_ratings)

    if user_profile is not None:
        cb_raw = cosine_similarity(user_profile, tfidf_matrix).flatten()
    else:
        cb_raw = np.zeros(tfidf_matrix.shape[0])

    svd_user_id = 1

    candidate_data = []
    for movie_id, row_idx in movielens_id_to_row.items():
        if movie_id in excluded_movielens_ids:
            continue
        if apply_quality_filter:
            stats = movie_quality.get(movie_id)
            if stats is None or stats[0] < MIN_MEAN_RATING or stats[1] < MIN_RATING_COUNT:
                continue

        attrs = movie_attributes.get(movie_id)
        if filter_genres:
            if not attrs or not (attrs["genres"] & filter_genres):
                continue
        if filter_min_year is not None:
            if not attrs or attrs["year"] is None or attrs["year"] < filter_min_year:
                continue
        if filter_max_year is not None:
            if not attrs or attrs["year"] is None or attrs["year"] > filter_max_year:
                continue
        if filter_cast:
            movie_cast = attrs.get("cast", set()) if attrs else set()
            if not movie_cast:
                continue
            if not any(fc in actor for fc in filter_cast for actor in movie_cast):
                continue
        if filter_director:
            movie_director = attrs.get("director") if attrs else None
            if not movie_director:
                continue
            if not any(fd in movie_director for fd in filter_director):
                continue

        keyword_match = False
        if filter_keywords:
            movie_keywords = attrs.get("keywords", set()) if attrs else set()
            keyword_match = any(fk in mk for fk in filter_keywords for mk in movie_keywords)

        recent_match = bool(attrs and attrs["year"] is not None and attrs["year"] >= RECENT_YEAR_THRESHOLD)

        svd_raw = model.predict(svd_user_id, movie_id).est
        svd_score = (svd_raw - 0.5) / 4.5
        cb_score = float(cb_raw[row_idx])
        candidate_data.append((int(movie_id), svd_score, cb_score, keyword_match, recent_match))

    if candidate_data:
        cb_vals = np.array([c[2] for c in candidate_data])
        cb_min, cb_max = float(cb_vals.min()), float(cb_vals.max())
        if cb_max > cb_min:
            cb_norm = (cb_vals - cb_min) / (cb_max - cb_min)
        else:
            cb_norm = np.zeros_like(cb_vals)
    else:
        cb_norm = np.array([])

    predictions = []
    for (movie_id, svd_score, _, keyword_match, recent_match), cb in zip(candidate_data, cb_norm):
        hybrid = svd_weight * svd_score + content_weight * float(cb)
        if keyword_match:
            hybrid = min(1.0, hybrid + KEYWORD_BOOST)
        if recent_match:
            hybrid = min(1.0, hybrid + RECENCY_BOOST)
        predicted_rating = round(5 + 5 * hybrid)

        predictions.append({
            "movieId": movie_id,
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
