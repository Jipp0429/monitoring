#!/usr/bin/env python3
"""
anomaly-monitor 부하 테스트 스크립트.

디바이스 수를 단계적으로 늘려가며 (기본: 100 -> 1000 -> 5000)
PUT /api/devices/scale 을 호출하고, 각 단계마다:
  - scale API 응답 시간 (ms)
  - 안정화(settle) 후 SSE(/api/stream/readings)로 측정한 실제 처리량 (events/sec)
  - 해당 구간에서 관측된 이상치(anomaly) 수
를 콘솔에 출력하고 CSV로 저장한다.

의존성 없음 (표준 라이브러리만 사용).

사용 예:
    python load_test.py
    python load_test.py --base-url http://localhost:8080 --stages 100,1000,5000
    python load_test.py --measure-seconds 15 --settle-seconds 5 --csv results.csv
"""

import argparse
import csv
import json
import socket
import sys
import time
import urllib.request
from datetime import datetime

try:
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
except AttributeError:
    pass


def http_call(method: str, url: str, timeout: float = 10.0):
    """단순 HTTP 호출. (status, body_text, elapsed_ms) 반환."""
    req = urllib.request.Request(url, method=method)
    start = time.perf_counter()
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        body = resp.read().decode("utf-8", errors="ignore")
        status = resp.status
    elapsed_ms = (time.perf_counter() - start) * 1000
    return status, body, elapsed_ms


def scale_devices(base_url: str, count: int):
    url = f"{base_url}/api/devices/scale?count={count}"
    status, body, elapsed_ms = http_call("PUT", url)
    try:
        actual = json.loads(body).get("count", count)
    except json.JSONDecodeError:
        actual = count
    return status, actual, elapsed_ms


def measure_throughput(base_url: str, duration_s: float):
    """
    지정된 시간 동안 /api/stream/readings SSE를 구독하며
    (총 이벤트 수, 이상치 수)를 센다.
    """
    url = f"{base_url}/api/stream/readings"
    req = urllib.request.Request(url, headers={"Accept": "text/event-stream"})

    event_count = 0
    anomaly_count = 0
    deadline = time.monotonic() + duration_s

    with urllib.request.urlopen(req, timeout=2) as resp:
        while time.monotonic() < deadline:
            try:
                raw_line = resp.readline()
            except (socket.timeout, TimeoutError):
                continue
            if not raw_line:
                break
            line = raw_line.decode("utf-8", errors="ignore").strip()
            if not line.startswith("data:"):
                continue
            event_count += 1
            payload = line[len("data:"):].strip()
            if '"anomaly":true' in payload:
                anomaly_count += 1

    return event_count, anomaly_count


def run_stage(base_url: str, target_count: int, settle_s: float, measure_s: float):
    print(f"\n=== 단계: 디바이스 수 -> {target_count} ===")

    status, actual_count, scale_ms = scale_devices(base_url, target_count)
    print(f"  scale 호출: status={status} actual_count={actual_count} 응답시간={scale_ms:.1f}ms")

    print(f"  안정화 대기 {settle_s:.1f}초...")
    time.sleep(settle_s)

    print(f"  처리량 측정 중 ({measure_s:.1f}초)...")
    event_count, anomaly_count = measure_throughput(base_url, measure_s)
    throughput = event_count / measure_s if measure_s > 0 else 0.0

    print(f"  결과: events={event_count} anomalies={anomaly_count} throughput={throughput:.1f} events/sec")

    return {
        "timestamp": datetime.now().isoformat(timespec="seconds"),
        "target_device_count": target_count,
        "actual_device_count": actual_count,
        "scale_response_ms": round(scale_ms, 2),
        "settle_seconds": settle_s,
        "measure_seconds": measure_s,
        "event_count": event_count,
        "anomaly_count": anomaly_count,
        "throughput_events_per_sec": round(throughput, 2),
    }


def print_summary_table(rows):
    headers = ["target_count", "actual_count", "scale_ms", "events", "anomalies", "events/sec"]
    widths = [12, 12, 10, 10, 10, 12]

    def fmt_row(values):
        return "  ".join(str(v).rjust(w) for v, w in zip(values, widths))

    print("\n" + "=" * 80)
    print("부하 테스트 요약")
    print("=" * 80)
    print(fmt_row(headers))
    print(fmt_row(["-" * w for w in widths]))
    for r in rows:
        print(fmt_row([
            r["target_device_count"],
            r["actual_device_count"],
            r["scale_response_ms"],
            r["event_count"],
            r["anomaly_count"],
            r["throughput_events_per_sec"],
        ]))


def write_csv(path: str, rows: list):
    if not rows:
        return
    fieldnames = list(rows[0].keys())
    with open(path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)
    print(f"\nCSV 저장 완료: {path}")


def main():
    parser = argparse.ArgumentParser(description="anomaly-monitor 부하 테스트")
    parser.add_argument("--base-url", default="http://localhost:8080",
                         help="서버 base URL (기본값: http://localhost:8080)")
    parser.add_argument("--stages", default="100,1000,5000",
                         help="쉼표로 구분된 디바이스 수 단계 (기본값: 100,1000,5000)")
    parser.add_argument("--settle-seconds", type=float, default=3.0,
                         help="scale 호출 후 처리량 측정 전 대기 시간 (기본값: 3초)")
    parser.add_argument("--measure-seconds", type=float, default=10.0,
                         help="각 단계에서 처리량을 측정할 시간 (기본값: 10초)")
    parser.add_argument("--csv", default="load_test_results.csv",
                         help="결과를 저장할 CSV 파일 경로")
    args = parser.parse_args()

    stages = [int(s.strip()) for s in args.stages.split(",") if s.strip()]

    print(f"대상 서버: {args.base_url}")
    print(f"단계: {stages}")

    rows = []
    try:
        for stage in stages:
            rows.append(run_stage(args.base_url, stage, args.settle_seconds, args.measure_seconds))
    except (urllib.error.URLError, ConnectionError) as e:
        print(f"\n서버에 연결할 수 없습니다: {e}", file=sys.stderr)
        print("애플리케이션이 실행 중인지 확인하세요 (mvn spring-boot:run).", file=sys.stderr)
        sys.exit(1)
    except KeyboardInterrupt:
        print("\n중단됨.")

    if rows:
        print_summary_table(rows)
        write_csv(args.csv, rows)


if __name__ == "__main__":
    main()
