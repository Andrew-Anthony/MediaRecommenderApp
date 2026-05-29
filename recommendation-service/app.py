from flask import Flask, request, jsonify
from model import train_model, get_hybrid_recommendations

app = Flask(__name__)


@app.route("/recommend", methods=["POST"])
def recommend():
    data = request.get_json()

    if not data or "ratings" not in data:
        return jsonify({"error": "Request body must include 'ratings' array"}), 400

    user_ratings = data["ratings"]
    n = data.get("n", 10)
    exclude_tmdb_ids = data.get("excludeTmdbIds", [])
    apply_quality_filter = data.get("applyQualityFilter", True)
    filters = data.get("filters", {}) or {}

    try:
        recommendations = get_hybrid_recommendations(
            user_ratings,
            n,
            exclude_tmdb_ids=exclude_tmdb_ids,
            apply_quality_filter=apply_quality_filter,
            filters=filters,
        )
        return jsonify(recommendations)
    except RuntimeError as e:
        return jsonify({"error": str(e)}), 500


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    train_model()
    app.run(host="0.0.0.0", port=5001, debug=False)