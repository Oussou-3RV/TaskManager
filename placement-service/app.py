from flask import Flask, request, jsonify
from orchestrator_engine import SimplePlacementEngine, TaskRequest, NodeState

app = Flask(__name__)

@app.get("/health")
def health():
    return {"status": "up"}

@app.post("/place")
def place():
    data = request.get_json()

    task = TaskRequest(
        id=data["task"]["id"],
        cpu_required=data["task"]["cpu_required"],
        memory_required_mb=data["task"]["memory_required_mb"],
        priority=data["task"].get("priority", "normal"),
    )

    nodes = [
        NodeState(
            id=n["id"],
            level=n["level"],
            cpu_available=n["cpu_available"],
            memory_available_mb=n["memory_available_mb"],
            latency_ms=n["latency_ms"],
            healthy=n.get("healthy", True),
        )
        for n in data["nodes"]
    ]

    policy = data.get("policy", "balanced")
    engine = SimplePlacementEngine(policy=policy)
    decision = engine.place_task(task, nodes)
    return jsonify(decision.to_dict())

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8000)