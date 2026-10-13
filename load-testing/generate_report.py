#!/usr/bin/env python3
"""
Genere le livrable demande par l'enonce (section 4) a partir des resultats de
load-testing/results/ :
  - latence-par-couche.svg : "un graphique en aires montrant la decomposition
    de la latence par couche"
  - le tableau de synthese est affiche dans le terminal (l'enonce le presente
    comme un tableau texte, pas une image) — a copier dans le rapport/ANALYSE.md.

Usage : python3 generate_report.py   (apres avoir lance ./run-all.sh)
"""
import json
from pathlib import Path

RESULTS_DIR = Path(__file__).parent / "results"

LAYERS = [
    ("db", "Base de donnees", "#2a78d6"),
    ("business", "Logique de placement", "#eb6834"),
    ("network", "Reseau + frontend", "#1baf7a"),
    ("api", "API + serialisation", "#eda100"),
]
LEVELS = [10, 50, 100]


def load_layer_p99(level):
    with open(RESULTS_DIR / f"summary-{level}.json") as f:
        metrics = json.load(f)["metrics"]
    return {key: metrics[f"layer_{key}"]["p(99)"] for key, _, _ in LAYERS}


def generate_area_chart(data, out_path):
    W, H = 760, 420
    PAD_L, PAD_B, PAD_T, PAD_R = 70, 60, 30, 30
    plot_w, plot_h = W - PAD_L - PAD_R, H - PAD_T - PAD_B

    y_max = 2800
    x_pos = [PAD_L + f * plot_w for f in (0.05, 0.5, 0.95)]

    def y_for(v):
        return PAD_T + plot_h - (v / y_max) * plot_h

    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" '
             f'font-family="system-ui, -apple-system, sans-serif">']
    parts.append(f'<rect width="{W}" height="{H}" fill="#ffffff"/>')
    parts.append(f'<text x="{PAD_L}" y="22" font-size="16" font-weight="700" fill="#0b0b0b">'
                 f'Decomposition de la latence par couche</text>')
    parts.append(f'<text x="{PAD_L}" y="40" font-size="11" fill="#6b6a66">'
                 f'p99 empile, en millisecondes — 10 / 50 / 100 clients concurrents</text>')

    # gridlines + y ticks
    for v in (0, 700, 1400, 2100, 2800):
        y = y_for(v)
        parts.append(f'<line x1="{PAD_L}" x2="{W - PAD_R}" y1="{y:.1f}" y2="{y:.1f}" '
                     f'stroke="#e1e0d9" stroke-width="1"/>')
        label = f"{v}ms" if v == 2800 else str(v)
        parts.append(f'<text x="{PAD_L - 10}" y="{y + 4:.1f}" font-size="10" fill="#898781" '
                     f'text-anchor="end">{label}</text>')

    # stacked bands
    cumulative = [0.0, 0.0, 0.0]
    for key, label, color in LAYERS:
        bottom = list(cumulative)
        top = [c + d[key] for c, d in zip(cumulative, data)]
        cumulative = top
        top_pts = [f"{x:.1f},{y_for(v):.1f}" for x, v in zip(x_pos, top)]
        bottom_pts = [f"{x:.1f},{y_for(v):.1f}" for x, v in zip(x_pos, bottom)][::-1]
        points = " ".join(top_pts + bottom_pts)
        parts.append(f'<polygon points="{points}" fill="{color}" opacity="0.88"/>')

    # axis lines
    parts.append(f'<line x1="{PAD_L}" x2="{PAD_L}" y1="{PAD_T}" y2="{PAD_T + plot_h}" stroke="#c3c2b7"/>')
    parts.append(f'<line x1="{PAD_L}" x2="{W - PAD_R}" y1="{PAD_T + plot_h}" y2="{PAD_T + plot_h}" stroke="#c3c2b7"/>')

    # x labels
    for x, lvl in zip(x_pos, LEVELS):
        parts.append(f'<text x="{x:.1f}" y="{H - PAD_B + 20}" font-size="11" font-weight="600" '
                     f'fill="#52514e" text-anchor="middle">{lvl} clients</text>')

    # legend
    lx, ly = PAD_L, H - 18
    for i, (key, label, color) in enumerate(LAYERS):
        gx = lx + i * 170
        parts.append(f'<rect x="{gx}" y="{ly - 9}" width="10" height="10" rx="2" fill="{color}"/>')
        parts.append(f'<text x="{gx + 15}" y="{ly}" font-size="10.5" fill="#52514e">{label}</text>')

    parts.append('</svg>')
    out_path.write_text("\n".join(parts))


def print_synthesis_table(data):
    totals = [sum(d.values()) for d in data]
    print()
    print("Tableau de synthese — part relative de chaque couche (p99) :")
    print(f"{'Couche':<22} {'10 clients':>12} {'50 clients':>12} {'100 clients':>12}")
    for key, label, _ in LAYERS:
        cells = [f"{100 * d[key] / t:.1f}%" for d, t in zip(data, totals)]
        print(f"{label:<22} {cells[0]:>12} {cells[1]:>12} {cells[2]:>12}")


def main():
    data = [load_layer_p99(lvl) for lvl in LEVELS]
    generate_area_chart(data, RESULTS_DIR / "latence-par-couche.svg")
    print(f"Graphique genere : {RESULTS_DIR}/latence-par-couche.svg")
    print_synthesis_table(data)


if __name__ == "__main__":
    main()
