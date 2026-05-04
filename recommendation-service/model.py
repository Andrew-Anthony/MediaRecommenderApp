import os
import pandas as pd
from surprise import Dataset, Reader, SVD

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE_DIR, "data", "ml-latest-small")

model = None
ratings_df = None
links_df = None

def train_model():
    global model, ratings_df, links_df

    ratings_df = pd.read_csv(os.path.join(DATA_DIR, "ratings.csv"))
    links_df = pd.read_csv(os.path.join(DATA_DIR, "links.csv"))

    reader = Reader(rating_scale=(0.5, 5.0))
    data = Dataset.load_from_df(ratings_df[["userId", "movieId", "rating"]], reader)
    trainset = data.build_full_trainset()

    model = SVD()
    model.fit(trainset)

    print(f"Model trained on {trainset.n_users} users and {trainset.n_items} movies")

def get_recommendations(user_id, n=10):
    if model is None:
        raise RuntimeError("Model not trained yet")

    all_movie_ids = ratings_df["movieId"].unique()
    rated_movie_ids = ratings_df[ratings_df["userId"] == user_id]["movieId"].values
    unrated_movie_ids = [mid for mid in all_movie_ids if mid not in rated_movie_ids]

    predictions = []
    for movie_id in unrated_movie_ids:
        predicted_rating = model.predict(user_id, movie_id).est
        predictions.append({"movieId": int(movie_id), "predictedRating": round(predicted_rating, 2)})

    predictions.sort(key=lambda x: x["predictedRating"], reverse=True)
    top_predictions = predictions[:n]

    for pred in top_predictions:
        link = links_df[links_df["movieId"] == pred["movieId"]]
        if not link.empty and pd.notna(link.iloc[0]["tmdbId"]):
            pred["tmdbId"] = int(link.iloc[0]["tmdbId"])
        else:
            pred["tmdbId"] = None

    return top_predictions