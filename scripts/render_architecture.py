# -*- coding: utf-8 -*-
"""Welfry 시스템 아키텍처 다이어그램을 PNG로 렌더링한다. 실행 위치는 무관(이 파일 기준 상대 경로)."""
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch
from matplotlib.font_manager import FontProperties

plt.rcParams["font.family"] = ["Malgun Gothic", "sans-serif"]
plt.rcParams["axes.unicode_minus"] = False

OUT_PATH = Path(__file__).resolve().parent.parent / "docs" / "architecture_diagram.png"

SURFACE = "#fcfcfb"
PAGE = "#f9f9f7"
INK = "#0b0b0b"
MUTED = "#52514e"
BORDER = "#c3c2b7"
BLUE = "#2a78d6"
ORANGE = "#eb6834"
AQUA = "#1baf7a"

fig, ax = plt.subplots(figsize=(11, 7.0), dpi=200)
fig.patch.set_facecolor(PAGE)
ax.set_facecolor(PAGE)
ax.set_xlim(0, 11)
ax.set_ylim(0, 7.0)
ax.axis("off")


def box(x, y, w, h, title, lines, color, title_size=11.5, line_size=9):
    b = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.02,rounding_size=0.08",
                        linewidth=1.4, edgecolor=color, facecolor=SURFACE, zorder=2)
    ax.add_patch(b)
    ax.text(x + w / 2, y + h - 0.32, title, ha="center", va="top",
            fontsize=title_size, fontweight="bold", color=INK, zorder=3)
    for i, line in enumerate(lines):
        ax.text(x + w / 2, y + h - 0.68 - i * 0.32, line, ha="center", va="top",
                fontsize=line_size, color=MUTED, zorder=3)
    # accent top bar
    ax.add_patch(FancyBboxPatch((x, y + h - 0.06), w, 0.06, boxstyle="round,pad=0,rounding_size=0.03",
                                 linewidth=0, facecolor=color, zorder=3))
    return (x, y, w, h)


def arrow(p1, p2, color=MUTED, label=None, label_dy=0.18, label_dx=0.0, label_t=0.5, style="-", ha="center"):
    a = FancyArrowPatch(p1, p2, arrowstyle="-|>", mutation_scale=16,
                         linewidth=1.6, color=color, zorder=1, linestyle=style)
    ax.add_patch(a)
    if label:
        mx = p1[0] + (p2[0] - p1[0]) * label_t + label_dx
        my = p1[1] + (p2[1] - p1[1]) * label_t
        ax.text(mx, my + label_dy, label, ha=ha, va="bottom",
                fontsize=8.5, color=color, style="italic",
                bbox=dict(boxstyle="round,pad=0.15", facecolor=PAGE, edgecolor="none"))


# 1) DeviceSimulatorService
sim = box(0.4, 4.0, 2.7, 2.0, "DeviceSimulatorService",
          ["@Scheduled tick (1s)", "정규분포 값 생성", "이상치 확률 주입", "Sinks.Many (buffer 65536)"],
          BLUE)

# 2) AnomalyDetectorService
det = box(3.9, 4.0, 3.0, 2.0, "AnomalyDetectorService",
          ["WelfordStats (디바이스별)", "Z-score 계산 · warm-up", "Sinks.Many (buffer 20000)"],
          ORANGE)

# 3) AnomalyEventRepository
repo = box(7.7, 5.4, 3.0, 1.15, "AnomalyEventRepository",
           ["R2DBC + H2 (파일 DB)", "fire-and-forget 저장"],
           AQUA, title_size=10.5, line_size=8.5)

# 4) MonitoringStreamController -> Dashboard
ctrl = box(7.7, 3.6, 3.0, 1.55, "MonitoringStreamController",
           ["SSE (/api/stream/readings)", "→ 웹 대시보드"],
           ORANGE, title_size=10.5, line_size=8.5)

# 5) DeviceScaleController (below sim/det)
scale = box(0.4, 1.95, 3.0, 1.15, "DeviceScaleController",
            ["PUT /api/devices/scale", "(상한 20,000대)"],
            BLUE, title_size=10.5, line_size=8.5)

# 6) 부하 테스트 스크립트
loadtest = box(4.15, 0.55, 3.0, 1.15, "load_test.py",
               ["단계적 디바이스 증가", "처리량 실측 · CSV 출력"],
               "#4a3aa7", title_size=10.5, line_size=8.5)

# 7) Micrometer metrics
metrics = box(7.9, 0.55, 2.7, 1.15, "Micrometer Metrics",
              ["emitted/dropped, evaluated,", "anomalies.total 등"],
              "#4a3aa7", title_size=10.5, line_size=8.5)

# Arrows
arrow((0.4 + 2.7, 4.0 + 1.0), (3.9, 4.0 + 1.0), label="SensorReading")
arrow((3.9 + 3.0, 4.0 + 1.65), (7.7, 5.4 + 0.45), label="AnomalyResult\n(이상치)",
      label_t=0.5, label_dy=0.25, ha="center")
arrow((3.9 + 3.0, 4.0 + 0.45), (7.7, 3.6 + 0.85), label="AnomalyResult\n(전체)",
      label_t=0.5, label_dy=0.25, ha="center")
arrow((1.9, 1.95 + 1.15), (1.9, 4.0), color=BORDER, style="--", label="디바이스 수 조절",
      label_dx=1.05, label_dy=0.05, ha="left")
arrow((5.65, 0.55 + 1.15), (5.4, 4.0), color=BORDER, style="--", label="scale API 호출",
      label_dx=1.05, label_dy=0.05, ha="left")
arrow((9.25, 0.55 + 1.15), (9.25, 3.6), color=BORDER, style="--", label="계측",
      label_dx=0.45, label_dy=0.05, ha="left")

ax.text(5.5, 6.75, "Welfry 시스템 아키텍처", ha="center", va="top",
        fontsize=16, fontweight="bold", color=INK)

fig.savefig(OUT_PATH, facecolor=fig.get_facecolor(), bbox_inches="tight")
print(f"saved: {OUT_PATH}")
