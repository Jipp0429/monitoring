"""부하 테스트 결과(load_test_results.csv)를 보고서용 PNG 이미지로 렌더링한다."""

import csv

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

# --- 팔레트 (dataviz 스킬 reference/palette.md, light mode) ---
SURFACE = "#fcfcfb"
PAGE = "#f9f9f7"
INK_PRIMARY = "#0b0b0b"
INK_SECONDARY = "#52514e"
INK_MUTED = "#898781"
GRIDLINE = "#e1e0d9"
BASELINE = "#c3c2b7"
SERIES_1 = "#2a78d6"  # categorical slot 1 (blue)

plt.rcParams["font.family"] = ["Malgun Gothic", "sans-serif"]
plt.rcParams["axes.unicode_minus"] = False

with open("scripts/load_test_results.csv", encoding="utf-8") as f:
    rows = list(csv.DictReader(f))

labels = [f"{int(r['target_device_count']):,}대" for r in rows]
throughput = [float(r["throughput_events_per_sec"]) for r in rows]

fig = plt.figure(figsize=(8.5, 8.5), dpi=200, facecolor=PAGE)
gs = fig.add_gridspec(2, 1, height_ratios=[3, 2], hspace=0.32, top=0.90, bottom=0.06, left=0.10, right=0.95)

# ---------------- 상단: 막대그래프 ----------------
ax = fig.add_subplot(gs[0])
ax.set_facecolor(SURFACE)

fig.text(0.10, 0.965, "부하 테스트 결과", fontsize=17, fontweight="bold", color=INK_PRIMARY)
fig.text(0.10, 0.935, "디바이스 수에 따른 SSE 스트림 처리량 (events/sec)", fontsize=10.5, color=INK_SECONDARY)

y_max = 5000
for gy in range(0, y_max + 1, 1000):
    ax.axhline(gy, color=GRIDLINE, linewidth=1, zorder=0)

bar_width = 0.28
x_positions = range(len(labels))

ax.bar(list(x_positions), throughput, width=bar_width, color=SERIES_1, zorder=2)
for x, value in zip(x_positions, throughput):
    ax.text(x, value + y_max * 0.02, f"{value:,.1f}", ha="center", va="bottom",
             fontsize=11, color=INK_PRIMARY, fontweight="600", zorder=3)

ax.set_xlim(-0.6, len(labels) - 0.4)
ax.set_ylim(0, y_max * 1.08)
ax.set_xticks(list(x_positions))
ax.set_xticklabels(labels, fontsize=11, color=INK_SECONDARY)
ax.set_yticks(range(0, y_max + 1, 1000))
ax.set_yticklabels([f"{v:,}" for v in range(0, y_max + 1, 1000)], fontsize=9.5, color=INK_MUTED)
ax.set_ylabel("처리량 (events/sec)", fontsize=9.5, color=INK_MUTED)

for spine in ax.spines.values():
    spine.set_visible(False)
ax.spines["bottom"].set_visible(True)
ax.spines["bottom"].set_color(BASELINE)
ax.spines["bottom"].set_linewidth(1)
ax.tick_params(length=0)

# ---------------- 하단: 데이터 테이블 ----------------
ax2 = fig.add_subplot(gs[1])
ax2.axis("off")

col_labels = ["디바이스 수", "scale 응답시간", "이벤트 수", "이상치 수", "처리량(events/sec)"]
table_rows = []
for r in rows:
    table_rows.append([
        f"{int(r['target_device_count']):,}",
        f"{float(r['scale_response_ms']):.1f} ms",
        f"{int(r['event_count']):,}",
        f"{int(r['anomaly_count']):,}",
        f"{float(r['throughput_events_per_sec']):,.1f}",
    ])

table = ax2.table(cellText=table_rows, colLabels=col_labels, loc="center", cellLoc="center")
table.auto_set_font_size(False)
table.set_fontsize(10.5)
table.scale(1, 2.1)

for (row, col), cell in table.get_celld().items():
    cell.set_edgecolor(GRIDLINE)
    if row == 0:
        cell.set_facecolor(SURFACE)
        cell.set_text_props(color=INK_SECONDARY, fontweight="bold")
    else:
        cell.set_facecolor(SURFACE if row % 2 == 1 else PAGE)
        cell.set_text_props(color=INK_PRIMARY)

fig.text(0.10, 0.335, "측정값 상세", fontsize=12, fontweight="bold", color=INK_PRIMARY)

out_path = "scripts/load_test_results.png"
fig.savefig(out_path, facecolor=fig.get_facecolor())
print(f"saved: {out_path}")
