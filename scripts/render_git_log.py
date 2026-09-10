# -*- coding: utf-8 -*-
"""Git 커밋 이력(그림 3)을 PNG로 렌더링한다. 최종보고서 제출 시점 기준 6개 커밋만 담는다.

실행 위치는 무관(이 파일 기준 상대 경로). 커밋 목록은 아래 `commits`에 수동으로 박아뒀으니,
이후 커밋을 더 추가하려면 `git log --format="%h %ad %s" --date=format:"%Y-%m-%d %H:%M"`로
직접 뽑아서 갱신할 것.
"""
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

plt.rcParams["font.family"] = ["Malgun Gothic", "sans-serif"]
plt.rcParams["axes.unicode_minus"] = False

OUT_PATH = Path(__file__).resolve().parent.parent / "docs" / "git_log.png"

SURFACE = "#fcfcfb"
PAGE = "#f9f9f7"
INK = "#0b0b0b"
SECONDARY = "#52514e"
MUTED = "#898781"
BLUE = "#2a78d6"
GRIDLINE = "#e1e0d9"

commits = [
    ("df2f214", "2026-08-14 21:09", "Add GitHub Actions CI to run mvn test on push/PR"),
    ("b962b0d", "2026-08-14 21:08", "Cap device scale API and add controller/repository test coverage"),
    ("af76a92", "2026-08-14 21:01", "Document configuration properties and metrics in README"),
    ("c039650", "2026-08-14 21:01", "Expose Micrometer metrics for throughput, drops, and anomaly rate"),
    ("2adddda", "2026-08-14 20:57", "Externalize simulator/detection tuning values into application.yml"),
    ("67dbe07", "2026-08-14 20:52", "Add real-time multi-device monitoring system with Welford/Z-score anomaly detection"),
]

row_h = 0.9
fig_h = 0.9 + row_h * len(commits) + 0.4
fig, ax = plt.subplots(figsize=(11, fig_h), dpi=200)
fig.patch.set_facecolor(PAGE)
ax.set_facecolor(PAGE)
ax.set_xlim(0, 11)
ax.set_ylim(0, fig_h)
ax.axis("off")

ax.text(0.3, fig_h - 0.35, "Git 커밋 이력 (master, 6개 커밋)", fontsize=14.5, fontweight="bold", color=INK)

top = fig_h - 0.9
node_x = 0.55
for i, (h, date, msg) in enumerate(commits):
    y = top - i * row_h
    # gridline separator
    if i > 0:
        ax.plot([0.3, 10.7], [y + row_h - 0.05, y + row_h - 0.05], color=GRIDLINE, linewidth=1, zorder=1)
    # commit graph node + line
    ax.scatter([node_x], [y - row_h / 2 + 0.15], s=70, color=BLUE, zorder=3, edgecolors=SURFACE, linewidths=1.5)
    if i < len(commits) - 1:
        ax.plot([node_x, node_x], [y - row_h / 2 + 0.15, y - row_h - row_h / 2 + 0.15],
                color=BLUE, linewidth=2, zorder=2, alpha=0.5)
    ax.text(node_x + 0.35, y - 0.28, h, fontsize=10.5, fontweight="bold", color=BLUE,
            fontfamily="Consolas", va="center")
    ax.text(2.15, y - 0.28, date, fontsize=9.5, color=MUTED, fontfamily="Consolas", va="center")
    ax.text(4.15, y - 0.28, msg, fontsize=10, color=INK, va="center")

fig.savefig(OUT_PATH, facecolor=fig.get_facecolor(), bbox_inches="tight")
print(f"saved: {OUT_PATH}")
