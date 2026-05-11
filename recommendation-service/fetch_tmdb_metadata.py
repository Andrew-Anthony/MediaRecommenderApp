import os
import pickle
import sys
import time

import pandas as pd
import requests

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE_DIR, "data", "ml-latest-small")
CACHE_PATH = os.path.join(BASE_DIR, "data", "tmdb_metadata.pkl")

TMDB_BASE_URL = "https://api.themoviedb.org/3"
TOP_KEYWORDS = 10
TOP_CAST = 5
REQUEST_DELAY = 0.04
SAVE_EVERY = 100


def load_cache():
    if os.path.exists(CACHE_PATH):
        with open(CACHE_PATH, "rb") as f:
            cache = pickle.load(f)
        print(f"[Cache] Loaded {len(cache)} existing entries from {CACHE_PATH}")
        return cache
    return {}


def save_cache(cache):
    os.makedirs(os.path.dirname(CACHE_PATH), exist_ok=True)
    with open(CACHE_PATH, "wb") as f:
        pickle.dump(cache, f)


def fetch_movie(tmdb_id, session):
    url = f"{TMDB_BASE_URL}/movie/{tmdb_id}"
    params = {"append_to_response": "keywords,credits"}
    resp = session.get(url, params=params, timeout=10)

    if resp.status_code == 404:
        return None
    resp.raise_for_status()
    return resp.json()


def extract_metadata(payload):
    keywords = []
    if payload.get("keywords"):
        keywords = [kw["name"] for kw in payload["keywords"].get("keywords", [])][:TOP_KEYWORDS]

    cast = []
    director = None
    if payload.get("credits"):
        cast = [c["name"] for c in payload["credits"].get("cast", [])][:TOP_CAST]
        for crew_member in payload["credits"].get("crew", []):
            if crew_member.get("job") == "Director":
                director = crew_member["name"]
                break

    release_date = payload.get("release_date") or ""
    decade = None
    if len(release_date) >= 4 and release_date[:4].isdigit():
        year = int(release_date[:4])
        decade = f"{(year // 10) * 10}s"

    return {
        "tmdb_id": payload.get("id"),
        "overview": payload.get("overview") or "",
        "keywords": keywords,
        "cast": cast,
        "director": director,
        "decade": decade,
    }


def main():
    token = os.environ.get("TMDB_TOKEN")
    if not token:
        print("ERROR: TMDB_TOKEN environment variable not set.", file=sys.stderr)
        print('Run: export TMDB_TOKEN="your_bearer_token_here"', file=sys.stderr)
        sys.exit(1)

    links_df = pd.read_csv(os.path.join(DATA_DIR, "links.csv"))
    links_df = links_df.dropna(subset=["tmdbId"])
    links_df["tmdbId"] = links_df["tmdbId"].astype(int)

    cache = load_cache()

    session = requests.Session()
    session.headers.update({
        "Authorization": f"Bearer {token}",
        "Accept": "application/json",
    })

    total = len(links_df)
    processed = 0
    fetched = 0
    failed = 0

    for _, row in links_df.iterrows():
        processed += 1
        movielens_id = int(row["movieId"])
        tmdb_id = int(row["tmdbId"])

        if movielens_id in cache:
            continue

        try:
            payload = fetch_movie(tmdb_id, session)
            if payload is None:
                failed += 1
                cache[movielens_id] = None
            else:
                cache[movielens_id] = extract_metadata(payload)
                fetched += 1
        except Exception as e:
            failed += 1
            print(f"  [skip] movielens={movielens_id} tmdb={tmdb_id}: {e}")

        if fetched > 0 and fetched % SAVE_EVERY == 0:
            save_cache(cache)
            print(f"  [progress] {processed}/{total} processed, {fetched} new, {failed} failed -- checkpoint saved")

        time.sleep(REQUEST_DELAY)

    save_cache(cache)
    print(f"\n[Done] {processed} total, {fetched} newly fetched, {failed} failed")
    print(f"[Done] Cache saved to {CACHE_PATH} ({len(cache)} entries)")


if __name__ == "__main__":
    main()
