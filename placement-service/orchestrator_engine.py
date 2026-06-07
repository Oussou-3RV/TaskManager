"""
Fonction principale :
    engine = SimplePlacementEngine(policy="balanced")
    decision = engine.place_task(task, nodes)
"""

from __future__ import annotations

import json
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from typing import Iterable, Literal, Optional

PlacementPolicy = Literal["least_loaded", "lowest_latency", "balanced"]


@dataclass(frozen=True)
class TaskRequest:
    """Tâche à placer sur une machine candidate."""

    id: str
    cpu_required: float
    memory_required_mb: int
    priority: Literal["low", "normal", "high"] = "normal"


@dataclass(frozen=True)
class NodeState:
    """État minimal d'une machine candidate."""

    id: str
    level: Literal["edge", "fog", "cloud"]
    cpu_available: float
    memory_available_mb: int
    latency_ms: float
    healthy: bool = True


@dataclass(frozen=True)
class PlacementDecision:
    """Décision retournée par le moteur de placement."""

    task_id: str
    accepted: bool
    selected_node_id: Optional[str]
    policy: PlacementPolicy
    score: Optional[float]
    reason: str
    decided_at: str

    def to_dict(self) -> dict:
        return asdict(self)


class SimplePlacementEngine:
    """
    Politiques :
    - least_loaded : privilégie le plus grand CPU disponible.
    - lowest_latency : privilégie la latence réseau la plus faible.
    - balanced : combine CPU disponible, mémoire disponible et latence.

    Le moteur filtre d'abord les machines incapables d'accueillir la tâche.
    Une machine est admissible si elle est saine et si ses ressources disponibles
    satisfont les besoins CPU et mémoire de la tâche.
    """

    def __init__(self, policy: PlacementPolicy = "balanced") -> None:
        if policy not in {"least_loaded", "lowest_latency", "balanced"}:
            raise ValueError(f"Politique inconnue : {policy}")
        self.policy = policy

    def place_task(
        self,
        task: TaskRequest,
        nodes: Iterable[NodeState],
        *,
        policy: Optional[PlacementPolicy] = None,
    ) -> PlacementDecision:
        active_policy = policy or self.policy
        if active_policy not in {"least_loaded", "lowest_latency", "balanced"}:
            raise ValueError(f"Politique inconnue : {active_policy}")

        candidates = [
            node
            for node in nodes
            if node.healthy
            and node.cpu_available >= task.cpu_required
            and node.memory_available_mb >= task.memory_required_mb
        ]

        if not candidates:
            return PlacementDecision(
                task_id=task.id,
                accepted=False,
                selected_node_id=None,
                policy=active_policy,
                score=None,
                reason="Aucune machine admissible ne possède les ressources requises.",
                decided_at=self._now(),
            )

        ranked = sorted(
            ((self._score(task, node, active_policy), node) for node in candidates),
            key=lambda item: (-item[0], item[1].id),
        )
        score, selected = ranked[0]

        return PlacementDecision(
            task_id=task.id,
            accepted=True,
            selected_node_id=selected.id,
            policy=active_policy,
            score=round(score, 6),
            reason=f"Machine {selected.id} retenue selon la politique {active_policy}.",
            decided_at=self._now(),
        )

    def _score(self, task: TaskRequest, node: NodeState, policy: PlacementPolicy) -> float:
        if policy == "least_loaded":
            return node.cpu_available

        if policy == "lowest_latency":
            return 1.0 / max(node.latency_ms, 1.0)

        # Politique balanced.
        # Les composantes sont volontairement simples et explicables pour les étudiants.
        cpu_score = node.cpu_available / 100.0
        memory_score = min(node.memory_available_mb / max(task.memory_required_mb * 4, 1), 1.0)
        latency_score = 1.0 / max(node.latency_ms, 1.0)
        priority_bonus = {"low": 0.0, "normal": 0.05, "high": 0.10}[task.priority]

        return (0.45 * cpu_score) + (0.35 * memory_score) + (0.20 * latency_score) + priority_bonus

    @staticmethod
    def _now() -> str:
        return datetime.now(timezone.utc).isoformat()


def demo() -> None:
    task = TaskRequest(
        id="task-demo-001",
        cpu_required=2.0,
        memory_required_mb=512,
        priority="normal",
    )
    nodes = [
        NodeState(id="edge-01", level="edge", cpu_available=1.0, memory_available_mb=512, latency_ms=8),
        NodeState(id="fog-01", level="fog", cpu_available=4.0, memory_available_mb=2048, latency_ms=20),
        NodeState(id="cloud-01", level="cloud", cpu_available=12.0, memory_available_mb=8192, latency_ms=70),
    ]

    for policy in ("least_loaded", "lowest_latency", "balanced"):
        engine = SimplePlacementEngine(policy=policy)  # type: ignore[arg-type]
        decision = engine.place_task(task, nodes)
        print(json.dumps(decision.to_dict(), ensure_ascii=False))


if __name__ == "__main__":
    demo()
