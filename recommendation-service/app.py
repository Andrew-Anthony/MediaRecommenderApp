from flask import Flask, request, jsonify
from model import train_model, get_recommendations

app = Flask(__name__)

@app.route("/recommend", methods=["GET"])
def recommend():
    user_id = request.args.get("user_id", type=int)
    n = request.args.get("n", default=10, type=int)

    if user_id is None:
        return jsonify({"error": "user_id parameter is required"}), 400

    try:
        recommendations = get_recommendations(user_id, n)
        return jsonify(recommendations)
    except RuntimeError as e:
        return jsonify({"error": str(e)}), 500

@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})

if __name__ == "__main__":
    train_model()
    app.run(host="0.0.0.0", port=5001, debug=True)