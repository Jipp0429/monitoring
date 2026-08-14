# anomaly-monitor

Spring Boot WebFlux 기반 실시간 멀티 디바이스 모니터링 시스템. 가상 디바이스들의 센서 값을 스트리밍으로 발행하고, Welford's Online Algorithm으로 디바이스별 평균/분산을 유지하며 Z-score 기반으로 이상치를 탐지한다.

## 구조

```
anomaly-monitor/          Spring Boot 애플리케이션
  src/main/java/com/example/monitor/
    simulator/             가상 디바이스 센서 값 발행 (DeviceSimulatorService, SensorReading)
    detection/             Welford 통계 + Z-score 이상 탐지 (WelfordStats, AnomalyDetectorService, AnomalyResult)
    persistence/            이상치 이력 영속화 (R2DBC + H2, AnomalyEventEntity/Repository)
    web/                    REST/SSE 컨트롤러 (스트리밍, 스케일 API, 이상치 조회)
  src/main/resources/
    application.yml         H2 파일 DB 설정
    schema.sql               anomaly_event 테이블
    static/index.html        실시간 대시보드
  src/test/java/...          단위 테스트

scripts/
  load_test.py               디바이스 수를 단계적으로 늘려가며 처리량/응답시간을 측정하는 부하 테스트 스크립트
```

## 핵심 동작

- `DeviceSimulatorService`가 매 1초(`@Scheduled`)마다 활성 디바이스 수만큼 센서 값을 발행한다 (평균 50, 표준편차 5, 3% 확률로 이상치 주입). 디바이스 수는 런타임에 API로 조절 가능하다.
- `AnomalyDetectorService`가 디바이스별 `WelfordStats`를 유지하며, 판독값을 반영하기 **직전** mean/stdDev 기준으로 Z-score를 계산한다 (그래야 극단값 자신이 자기 기준선을 왜곡하지 않는다). 표본이 10개 미만인 동안(warm-up)은 값과 무관하게 이상치 판정을 보류한다.
- `|z| > 3`이면 이상치로 판단하고, 구조화된 로그(`ANOMALY detected deviceId=...`)를 남기고 H2 DB에 비동기로 저장한다.
- 두 곳의 내부 `Sinks.Many`는 버퍼 크기를 65536으로 넉넉히 잡아둔다 — 기본값(256)으로는 디바이스 수가 많을 때(예: 5000대) 매 tick마다 몰리는 이벤트 버스트를 버퍼가 못 받아내고 조용히 드롭하는 문제가 있었다.

## 실행

```bash
cd anomaly-monitor
mvn spring-boot:run
```

기본적으로 `http://localhost:8080` 에서 대시보드가 뜬다. H2 DB 파일은 `anomaly-monitor/data/` 밑에 생성되며 재시작해도 유지된다.

## API

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/stream/readings` | SSE. 판독값 + 이상치 판정 결과 실시간 스트림 |
| GET | `/api/devices/scale` | 현재 디바이스 수 조회 |
| PUT | `/api/devices/scale?count=N` | 디바이스 수 변경 |
| GET | `/api/anomalies/recent?limit=N` | 최근 이상치 이력 조회 (기본 50건) |
| GET | `/api/anomalies/device/{deviceId}` | 특정 디바이스의 이상치 이력 조회 |
| GET | `/actuator/metrics/{name}` | Micrometer 메트릭 조회 (아래 커스텀 메트릭 + JVM 기본 메트릭) |

## 설정

`application.yml`의 `monitor.simulator.*`, `monitor.detection.*`에서 재빌드 없이 튜닝 가능 (Z-score 임계값, warm-up 최소 표본 수, 시뮬레이터 베이스라인/이상치 확률, 싱크 버퍼 크기 등).

## 메트릭

`/actuator/metrics`로 노출되는 커스텀 메트릭:

| 이름 | 설명 |
|---|---|
| `monitor.simulator.readings.emitted` | 시뮬레이터가 발행한 판독값 누적 수 |
| `monitor.simulator.readings.dropped` | 싱크 버퍼 오버플로우로 드롭된 판독값 수 |
| `monitor.simulator.device.count` | 현재 디바이스 수 (게이지) |
| `monitor.detection.readings.evaluated` | AnomalyDetectorService가 평가한 판독값 누적 수 |
| `monitor.detection.anomalies.total` | 탐지된 이상치 누적 수 |
| `monitor.detection.results.dropped` | 결과 싱크 버퍼 오버플로우로 드롭된 수 |

## 테스트

```bash
cd anomaly-monitor
mvn test
```

`WelfordStatsTest`, `DeviceSimulatorServiceTest`, `AnomalyDetectorServiceTest` — 총 11개. Mockito 대신 손으로 만든 fake를 사용한다 (이 환경의 Java 25 JVM에서 Mockito의 바이트코드 생성 자체가 계속 걸려서).

## 부하 테스트

```bash
python scripts/load_test.py --stages 100,1000,5000
```

디바이스 수를 단계적으로 늘려가며 스케일 API 응답시간과 SSE 스트림 처리량(events/sec)을 측정해 콘솔에 출력하고 CSV로 저장한다.
