# 작업 인수인계 노트

다른 환경(PC)에서 이 저장소를 새로 clone해서 이어서 작업할 때, 이 파일 하나로 지금까지 진행 상황을 빠르게 파악할 수 있게 정리한 문서입니다. Claude Code에게 "HANDOFF.md 읽고 이어서 진행해줘"라고 하면 바로 브리핑됩니다.

## 프로젝트 개요

- **이름**: Welfry — Spring Boot WebFlux 기반 실시간 다중 디바이스 이상 탐지 모니터링 시스템
- **과제**: ICT특성화취업연계형사업단 개인역량 강화 프로젝트 (과제번호 2026-2101043)
- **원격 저장소**: https://github.com/Jipp0429/monitoring (기본 브랜치 `master`)

## 지금까지 한 일 (요약)

1. Spring WebFlux + Reactor Sinks 기반 실시간 모니터링 시스템 구현 (디바이스 시뮬레이터 → Welford's Algorithm 기반 이상 탐지 → H2/R2DBC 영속화 + SSE 대시보드)
2. 부하 테스트(100/1,000/5,000대)로 안정성 검증 → 두 가지 실제 버그를 발견하고 수정:
   - 버퍼 크기 부족(256)으로 처리량이 300~400/sec에서 정체 → 65,536으로 확대
   - 유휴 구독자 재연결 시 대시보드 수치 폭주(버퍼 누적) → `directBestEffort()`로 전환
3. warm-up 임계값으로 디바이스 스케일업 시 오탐률 개선 (6.5~6.6% → 1.2~1.3%)
4. 테스트 21건 + GitHub Actions CI 구축
5. 최종보고서(hwp) 제출 완료 (2026-08-21) — 내용은 `docs/paper_draft.md`에 정리됨
6. 최종보고서 내용을 바탕으로 논문 초안 작성 (`docs/paper_draft.md`, `docs/paper_draft.docx`) — 그림 1(아키텍처), 그림 2(부하테스트 차트), 그림 3(Git 커밋 이력) 포함
7. 오탈자·수치 오류 재검토 완료 (배율 계산 오류, 존재하지 않던 그림 참조 등 수정)

## 알아두어야 할 것

- **서버 실행**: `cd anomaly-monitor && mvn spring-boot:run` (기본 포트 8080). H2 DB는 `anomaly-monitor/data/`에 파일로 남음(gitignore됨, clone 후에는 빈 상태로 새로 시작).
- **부하 테스트**: `python scripts/load_test.py --stages 100,1000,5000` (표준 라이브러리만 사용, 의존성 없음).
- **논문 차트/다이어그램 재생성 스크립트**는 저장소에는 없고 세션 스크래치패드에만 있었음 — 필요하면 `docs/` 안의 PNG를 직접 재사용하거나, 다시 요청해서 만들어야 함.
- **개발/실험 환경은 Java 25 JDK**였음(빌드 타깃은 pom.xml상 Java 21). Java 25에서 Mockito/Byte Buddy가 정상 동작하지 않아 테스트는 전부 Mockito 없이 손으로 만든 fake로 작성됨 — 새 환경이 다른 JDK라면 이 이슈가 재현 안 될 수도 있음.
- 최종보고서(hwp)에 있는 수치(100.1 → 974.9 → 4,975.7 events/sec 등)와 논문 초안의 수치는 의도적으로 통일되어 있음. 이후 코드를 더 고치면(특히 `directBestEffort` 관련 sink) 이 수치들이 실제와 안 맞을 수 있으니, 재측정 없이 논문 수치만 먼저 건드리지 말 것.

## 알려진 이슈 (아직 처리 안 함)

- `directBestEffort()`로 바꾼 뒤, **활성 구독자가 있어도 5,000대 규모에서 이벤트의 약 50%가 유실**되는 회귀가 발견됨(2026-09-11 세션에서 확인). 원인은 버퍼 없는 방식이 한 tick 안의 동기적 burst를 못 받아내는 것으로 추정. 사용자가 "최종보고서와 일치시키기 위해 이번 논문에는 반영하지 않기"로 결정했음 — **코드 수정도 보류된 상태**. 다음에 이어서 할 만한 작업: 결과 sink를 `onBackpressureBuffer(maxDeviceCount 정도의 작은 크기)`로 재조정하는 방안을 검토 중이었음.
- `anomaly-monitor/src/main/java/.../AnomalyDetectorService.java`에 있던 일부 설명 주석이 로컬에서 제거된 상태로 커밋됨(이 인수인계 커밋 기준) — 필요하면 git 이력(`c9f3868` 커밋)에서 원래 주석 확인 가능.

## 주요 파일 위치

- 논문 초안: `docs/paper_draft.md`, `docs/paper_draft.docx`
- 아키텍처 다이어그램: `docs/architecture_diagram.png`
- Git 커밋 이력 그림: `docs/git_log.png`
- 부하 테스트 차트: `scripts/load_test_results.png` (gitignore 대상이라 저장소엔 없을 수 있음 — `python scripts/render_load_test_chart.py`로 재생성 가능, `matplotlib` 필요)
- 최종보고서 원본: 사용자 로컬 Downloads 폴더 (저장소에는 미포함)
