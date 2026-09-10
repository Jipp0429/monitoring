# 작업 인수인계 노트

다른 환경(PC)에서 이 저장소를 새로 clone해서 이어서 작업할 때, 이 파일 하나로 지금까지 진행 상황을 빠르게 파악할 수 있게 정리한 문서입니다. Claude Code에게 "HANDOFF.md 읽고 이어서 진행해줘"라고 하면 바로 브리핑됩니다.

## 프로젝트 개요

- **이름**: Welfry — Spring Boot WebFlux 기반 실시간 다중 디바이스 이상 탐지 모니터링 시스템
- **과제**: ICT특성화취업연계형사업단 개인역량 강화 프로젝트 (과제번호 2026-2101043)
- **원격 저장소**: https://github.com/Jipp0429/monitoring (기본 브랜치 `master`)

## 지금까지 한 일 (요약)

1. Spring WebFlux + Reactor Sinks 기반 실시간 모니터링 시스템 구현 (디바이스 시뮬레이터 → Welford's Algorithm 기반 이상 탐지 → H2/R2DBC 영속화 + SSE 대시보드)
2. 부하 테스트(100/1,000/5,000대)로 안정성 검증 → 실제 버그 여러 건을 발견하고 수정:
   - 버퍼 크기 부족(256)으로 처리량이 300~400/sec에서 정체 → 65,536으로 확대
   - 유휴 구독자 재연결 시 대시보드 수치 폭주(버퍼 누적) → `directBestEffort()`로 전환했다가,
   - `directBestEffort()`가 활성 구독자의 이벤트까지 유실시키는 걸 발견 → 결과 sink를 `onBackpressureBuffer(20000)`로 재조정(현재 상태). 자세한 내용은 아래 "알려진 이슈" 참고.
3. warm-up 임계값으로 디바이스 스케일업 시 오탐률 개선 (6.5~6.6% → 1.2~1.3%)
4. 테스트 21건 + GitHub Actions CI 구축
5. 최종보고서(hwp) 제출 완료 (2026-08-21) — 내용은 `docs/paper_draft.md`에 정리됨
6. 최종보고서 내용을 바탕으로 논문 초안 작성 (`docs/paper_draft.md`, `docs/paper_draft.docx`) — 그림 1(아키텍처), 그림 2(부하테스트 차트), 그림 3(Git 커밋 이력) 포함
7. 오탈자·수치 오류 재검토 완료 (배율 계산 오류, 존재하지 않던 그림 참조 등 수정)

## 알아두어야 할 것

- **서버 실행**: `cd anomaly-monitor && mvn spring-boot:run` (기본 포트 8080). H2 DB는 `anomaly-monitor/data/`에 파일로 남음(gitignore됨, clone 후에는 빈 상태로 새로 시작).
- **부하 테스트**: `python scripts/load_test.py --stages 100,1000,5000` (표준 라이브러리만 사용, 의존성 없음).
- **논문 차트/다이어그램 재생성 스크립트**는 `scripts/`에 있음 (`render_architecture.py`, `render_git_log.py`, `render_load_test_chart.py`, `build_paper_docx.py`). 전부 실행 위치 무관하게 동작하고(스크립트 파일 기준 상대 경로), `docs/`에 결과물을 씀. `matplotlib`·`python-docx` 설치 필요(`pip install matplotlib python-docx`).
- **개발/실험 환경은 Java 25 JDK**였음(빌드 타깃은 pom.xml상 Java 21). Java 25에서 Mockito/Byte Buddy가 정상 동작하지 않아 테스트는 전부 Mockito 없이 손으로 만든 fake로 작성됨 — 새 환경이 다른 JDK라면 이 이슈가 재현 안 될 수도 있음.
- 최종보고서(hwp)에 있는 수치(100.1 → 974.9 → 4,975.7 events/sec 등)와 논문 초안의 수치는 의도적으로 통일되어 있음. 이후 코드를 더 고치면(특히 `directBestEffort` 관련 sink) 이 수치들이 실제와 안 맞을 수 있으니, 재측정 없이 논문 수치만 먼저 건드리지 말 것.

## 알려진 이슈

- ~~`directBestEffort()`로 바꾼 뒤 활성 구독자가 있어도 5,000대 규모에서 이벤트의 약 50%가 유실되는 회귀~~ →
  **해결됨(2026-09-11).** 결과 sink를 `onBackpressureBuffer(20000, false)`로 재조정
  (`DetectionProperties.resultsBufferSize`, 기본값 `maxDeviceCount`와 동일한 20,000). 활성 구독자 드롭 0건 확인.
  다만 이 방식은 구독자가 몇 초간 끊겼다가 재연결하면 그 사이 쌓인 backlog(최대 20,000개)가 한꺼번에 들어오는
  현상은 정도는 줄었어도 완전히는 없어지지 않음(과거 65,536/무제한 누적보다는 훨씬 나음) — 대시보드를 오래 안 보다가
  다시 열면 초당 이벤트 수치가 잠깐 튈 수 있다는 뜻. 완벽한 해결은 아니고 트레이드오프를 택한 것.
  같은 세션에서 `scripts/load_test.py`의 측정 방식 버그도 발견해 같이 고침 — settle 구간 동안 SSE 연결을
  안 하고 있다가 나중에 새로 열면 그 사이 쌓인 backlog가 측정 구간 앞부분을 오염시켰음(첫 스테이지가
  실제보다 수십 배 부풀려짐). 연결을 끊지 않고 settle 구간에도 계속 읽어서 버리는 방식으로 수정.
  재측정 결과: 100 → 104.2, 1,000 → 1,026.3, 5,000 → 5,001.2 events/sec (거의 정확히 1:1 비례).
- `anomaly-monitor/src/main/java/.../AnomalyDetectorService.java`에 있던 일부 설명 주석이 로컬에서 제거된 상태로 커밋됨(`c9f3868` 커밋 기준) — 필요하면 그 커밋에서 원래 주석 확인 가능.
- **`docs/paper_draft.md`/`.docx`의 수치(100.1 → 974.9 → 4,975.7 등)는 위 재측정 결과와 이제 다르다.** 이건 의도적임 —
  논문은 제출된 최종보고서와 수치를 맞추기로 결정했고, 이번 회귀/재측정 건은 논문에 반영하지 않기로 함. 논문 수치를
  이번 재측정값으로 바꾸지 말 것(따로 요청받기 전까지).

## 주요 파일 위치

- 논문 초안: `docs/paper_draft.md`, `docs/paper_draft.docx`
- 아키텍처 다이어그램: `docs/architecture_diagram.png`
- Git 커밋 이력 그림: `docs/git_log.png`
- 부하 테스트 차트: `docs/load_test_results.png` (`python scripts/render_load_test_chart.py`로 재생성 가능 — 단, 재생성하려면 `scripts/load_test_results.csv`가 먼저 있어야 하므로 `python scripts/load_test.py --stages 100,1000,5000`로 새로 측정부터 해야 함)
- 최종보고서 원본: 사용자 로컬 Downloads 폴더 (저장소에는 미포함)
