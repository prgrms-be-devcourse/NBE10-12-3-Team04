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
