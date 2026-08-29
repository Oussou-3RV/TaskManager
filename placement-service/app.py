import logging
import time

from flask import Flask, request, jsonify
from orchestrator_engine import SimplePlacementEngine, TaskRequest, NodeState

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("placement")

# Seuil au-dela duquel une tache est consideree "lourde" en CPU. Correspond a
# la capacite d'une machine "fog" typique : en dessous, une machine edge peut
# suffire ; au-dessus, il faut chercher une machine avec de la place.
HEAVY_CPU_THRESHOLD = 4.0


def select_policy(task: TaskRequest) -> str:
    """
    Choisit automatiquement la politique de placement selon la charge de la
    tache, quand aucune regle admin n'impose explicitement une politique.
    """
    if task.cpu_required >= HEAVY_CPU_THRESHOLD:
        return "least_loaded"  # tache lourde -> eviter une machine deja chargee
    if task.priority == "high":
        return "lowest_latency"  # tache urgente -> machine la plus rapide
    return "balanced"  # cas general


@app.get("/health")
def health():
    return {"status": "up"}

@app.post("/place")
def place():
    trace_id = request.headers.get("X-Trace-Id", "")
    start = time.perf_counter()

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

    # "policy" n'est envoye par le backend que si un admin a active une regle
    # (bascule manuelle) ; sinon, le moteur choisit lui-meme selon la charge.
    forced_policy = data.get("policy")
    policy = forced_policy or select_policy(task)
    auto_selected = forced_policy is None

    engine = SimplePlacementEngine(policy=policy)
    decision = engine.place_task(task, nodes)

    duration_ms = (time.perf_counter() - start) * 1000
    logger.info("trace_id=%s task_id=%s policy=%s auto=%s duration_ms=%.2f accepted=%s",
                trace_id, task.id, policy, auto_selected, duration_ms, decision.accepted)

    response = jsonify(decision.to_dict())
    response.headers["X-Trace-Id"] = trace_id
    response.headers["Server-Timing"] = f"engine;dur={duration_ms:.2f}"
    return response

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8000)