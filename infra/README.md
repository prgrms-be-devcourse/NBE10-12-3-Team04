# Compose 실행 구성

이 폴더는 현재 애플리케이션 레포의 실행 구성만 관리한다. Terraform·GCP·모니터링 인프라는 별도 `triptrace-infra` 폴더에서 관리한다.

## 환경 파일

- `.env.example`: 변수 이름과 안전한 예시값. Git에 포함한다.
- `.env.local`: 로컬 개발 실행값. Git에 포함하지 않는다.
- `.env.prod`: 운영 실행값. Git에 포함하지 않는다.
- `.env`: 필요 시 Compose 기본 실행에 쓰는 로컬 값. Git에 포함하지 않는다.

실제 환경 파일을 새로 만들 때는 다음을 사용한다.

```bash
cp infra/.env.example infra/.env.local
```

## 실행 명령

```bash
# 로컬: 애플리케이션 이미지를 현재 소스에서 build한다.
docker compose --env-file infra/.env.local -f infra/docker-compose.local.yml up --build

# 운영: 레지스트리에 배포된 고정 버전 이미지를 사용한다.
docker compose --env-file infra/.env.prod -f infra/docker-compose.prod.yml up -d
```

Compose 파일이 `infra/`에 있으므로, build context는 `../back`, `../front`을 사용한다. Caddy 설정 파일도 같은 폴더의 `Caddyfile`을 마운트한다.

로컬 Compose에는 Prometheus가 포함된다. `backend:8080/actuator/prometheus`를 Compose 내부 네트워크로 수집하며, UI는 `http://localhost:9090`에서만 확인할 수 있다.

## 구성 검증 기록

2026-08-03에 아래 명령으로 경로와 환경변수 해석을 검증했다.

```bash
docker compose --env-file infra/.env.local -f infra/docker-compose.local.yml config --quiet
docker compose --env-file infra/.env.prod -f infra/docker-compose.prod.yml config --quiet
```

- local 구성: 성공
- prod 구성: 성공. 다만 `.env.prod`에 `NEXT_PUBLIC_APP_VERSION`이 없어 Compose가 빈 값 경고를 출력했다. 운영 배포 전 해당 값을 고정 이미지 태그와 같은 버전으로 입력한다.

2026-08-03에 Caddyfile의 환경변수 치환도 다음 명령으로 검증했다.

```bash
docker run --rm \
  -e SITE_ADDR=localhost \
  -e BACKEND_UPSTREAM=backend:8080 \
  -e FRONTEND_UPSTREAM=frontend:3000 \
  -v "$PWD/infra/Caddyfile:/etc/caddy/Caddyfile:ro" \
  caddy:2 caddy adapt --config /etc/caddy/Caddyfile --adapter caddyfile --validate
```

`BACKEND_UPSTREAM`과 `FRONTEND_UPSTREAM`은 각각 Compose 내부 주소로 정상 해석됐다.

2026-08-03에 로컬 Prometheus 구성을 다음 명령으로 검증했다.

```bash
docker compose --env-file infra/.env.local -f infra/docker-compose.local.yml config --quiet
docker run --rm --entrypoint promtool \
  -v "$PWD/infra/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro" \
  prom/prometheus:v3.5.0 check config /etc/prometheus/prometheus.yml
```

Compose 구성과 Prometheus scrape 설정 문법은 모두 통과했다. 실제 수집은 로컬 Compose를 기동한 뒤 Prometheus Targets 화면에서 `triptrace-backend` 상태가 `UP`인지 확인한다.

## 현재 수집되는 메트릭

아래는 2026-08-03에 로컬 백엔드의 `/actuator/prometheus` 응답에서 확인한 메트릭이다. 라이브러리 추가, 애플리케이션 요청량, 실행 환경에 따라 항목은 달라질 수 있다.

| 분류 | 의미 | 메트릭 |
| --- | --- | --- |
| JVM 클래스 | JVM에 적재·해제된 클래스 수 | `jvm_classes_loaded_classes`, `jvm_classes_loaded_count_classes_total`, `jvm_classes_unloaded_classes_total` |
| JVM 가비지 컬렉션 | GC 이후 살아 있는 데이터 크기, 메모리 할당·승격량, GC 점유율 | `jvm_gc_live_data_size_bytes`, `jvm_gc_max_data_size_bytes`, `jvm_gc_memory_allocated_bytes_total`, `jvm_gc_memory_promoted_bytes_total`, `jvm_gc_overhead` |
| JVM 스레드 | 현재·최대·데몬 스레드 수와 누적 생성 수 | `jvm_threads_daemon_threads`, `jvm_threads_live_threads`, `jvm_threads_peak_threads`, `jvm_threads_started_threads_total` |
| 프로세스 | 애플리케이션 프로세스의 CPU 사용량, 열린 파일 수, 시작 시각, 가동 시간 | `process_cpu_time_ns_total`, `process_cpu_usage`, `process_files_max_files`, `process_files_open_files`, `process_start_time_seconds`, `process_uptime_seconds` |
| 시스템 | 컨테이너가 보는 CPU 개수·사용률·1분 평균 부하 | `system_cpu_count`, `system_cpu_usage`, `system_load_average_1m` |
| Spring Security | 보안 필터를 거친 활성 HTTP 요청의 개수·최대 시간·누적 시간 | `spring_security_http_secured_requests_active_seconds_count`, `spring_security_http_secured_requests_active_seconds_max`, `spring_security_http_secured_requests_active_seconds_sum` |
| Tomcat 세션 | 현재·최대 활성 세션, 세션 최장 유지 시간, 생성·만료·거절 세션 수 | `tomcat_sessions_active_current_sessions`, `tomcat_sessions_active_max_sessions`, `tomcat_sessions_alive_max_seconds`, `tomcat_sessions_created_sessions_total`, `tomcat_sessions_expired_sessions_total`, `tomcat_sessions_rejected_sessions_total` |

현재 응답에는 일반 HTTP 요청 건수·응답 시간이나 DB 커넥션 풀 메트릭이 없었다. 필요한 메트릭이 있으면 별도로 계측을 추가한 뒤 이 목록을 갱신한다.
