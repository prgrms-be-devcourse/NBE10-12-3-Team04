# 로컬 빌드 운영 Compose

`docker-compose.prod.yml`은 GHCR에 올라간 이미지를 실행하는 운영 구성이다. GitHub Actions나 Registry push 없이 새 VM 배포를 검증할 때는 `docker-compose.prod.local.yml`을 override로 함께 사용한다.

## 준비

실제 `.env`는 Git에 넣지 않는다. 기존 `.env.example`을 기준으로 만들고, 아래 값이 있어야 한다.

- Compose runtime 값: MySQL, JWT, 이미지 경로, Caddy `SITE_ADDR` 등
- frontend build 값: `NEXT_PUBLIC_GOOGLE_MAPS_API_KEY`, `NEXT_PUBLIC_API_URL`, `NEXT_PUBLIC_APP_VERSION`
- backend build 경로: `LOG_DIR`, `IMAGE_UPLOAD`

Apple Silicon Mac에서 만들더라도 override가 `linux/amd64` 플랫폼을 지정하므로 GCP Compute Engine VM에서 실행할 수 있다. emulation 빌드는 네이티브 빌드보다 느릴 수 있다.

## 로컬 빌드·기동

```bash
docker compose \
  -f docker-compose.prod.yml \
  -f docker-compose.prod.local.yml \
  up -d --build
```

이 명령은 `triptrace-frontend:<FRONTEND_VERSION>`와 `triptrace-backend:<BACKEND_VERSION>` 로컬 이미지를 만들고, GHCR에서 해당 이미지를 pull하지 않는다. Caddy와 MySQL은 기존 운영 Compose 설정을 그대로 사용한다.

## VM으로 수동 전달할 때

VM에서는 이미지를 다시 빌드하지 않고 `docker load`한 뒤 같은 Compose 파일을 실행한다. VM의 `.env`에는 Secret Manager startup script가 만든 `/opt/triptrace/.env`를 사용한다.

MySQL 볼륨을 지우는 `docker compose down -v`는 데이터 삭제 명령이므로 운영 데이터가 있을 때 실행하지 않는다.
