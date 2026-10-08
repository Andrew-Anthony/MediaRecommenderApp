# MediaRecommenderApp

Web application for movie recommendations. Users can create an account, search for movies, rate them and get personalized recommendations based on their preferences.

## Project Structure

The project is split into 3 parts:

- **Backend (Spring Boot + Java 21)** - main API, JWT authentication, PostgreSQL database
- **Frontend (React + TypeScript + Vite)** - user interface
- **Recommendation Service (Python + Flask)** - separate microservice that generates recommendations using a hybrid model (SVD + content-based filtering with TF-IDF)

## Main Features

- Register / Login (JWT)
- Movie search (via TMDB API)
- Movie ratings (1-10 scale)
- Personalized recommendations (based on user ratings)
- Watchlist (plan to watch / watching / watched / dropped)
- LLM chat (Gemini) for conversational recommendations with filtering (genre, year, actor, director, keywords, mood)

## Technologies

| Component | Stack |
|---|---|
| Backend | Java 21, Spring Boot, Spring Security, JPA, PostgreSQL |
| Frontend | React 19, TypeScript, Vite, React Router, Axios |
| ML Service | Python, Flask, scikit-surprise (SVD), scikit-learn (TF-IDF), pandas, numpy |
| Data | MovieLens 32M dataset, TMDB API |

## Prerequisites

- **Java 21**
- **Node.js** (v18 or newer)
- **Python** (3.9 or newer)
- **PostgreSQL** (and create an empty database before starting)

## How to Run

### 1. Configuration
Create a `application-local.properties` file in `src/main/resources/` with your credentials:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/your_db
spring.datasource.username=your_user
spring.datasource.password=your_password
tmdb.api.read-access-token=your_tmdb_token
gemini.api.key=your_gemini_key
jwt.secret=your_jwt_base64_secret_key
jwt.expiration=86400000
```

### 2. Recommendation Service (Start this first)
You need to download the [MovieLens 32M](https://grouplens.org/datasets/movielens/32m/) dataset and place it in `recommendation-service/data/ml-32m/`. *(Note: You must request permission from GroupLens to download and use this dataset).*

To enrich the recommendation model with TMDB metadata (cast, keywords, director), run this **before** starting the app:
```bash
# On Linux/Mac: export TMDB_TOKEN="your_token"
# On Windows (PowerShell): $env:TMDB_TOKEN="your_token"
python fetch_tmdb_metadata.py
```

Then start the service:
```bash
cd recommendation-service
pip install -r requirements.txt
python app.py
```
*Note: On first run, it trains the SVD model on the MovieLens dataset (takes a while) and saves it to cache.*

### 3. Backend
```bash
./gradlew bootRun
```
Requires PostgreSQL running.

### 4. Frontend
```bash
cd frontend
npm install
npm run dev
```

## Acknowledgments

This project uses the MovieLens 32M dataset.

> F. Maxwell Harper and Joseph A. Konstan. 2015. The MovieLens Datasets: History and Context. *ACM Transactions on Interactive Intelligent Systems (TiiS)* 5, 4, Article 19.
