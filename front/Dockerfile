# syntax=docker/dockerfile:1.7

# ---- deps ----
FROM node:22-alpine AS deps
WORKDIR /app
RUN corepack enable
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile

# ---- builder ----
FROM node:22-alpine AS builder
WORKDIR /app
ARG APP_VERSION=0.0.1-SNAPSHOT
# NEXT_PUBLIC_* 값은 Next.js 빌드 때 번들에 포함된다.
ENV NEXT_PUBLIC_APP_VERSION=${APP_VERSION}
RUN corepack enable
COPY --from=deps /app/node_modules ./node_modules
COPY . .
# BuildKit secret mount를 사용해 빌드 레이어나 최종 이미지에는 원본 파일을 남기지 않는다.
RUN --mount=type=secret,id=frontend_build_env \
    set -a \
    && . /run/secrets/frontend_build_env \
    && set +a \
    && NEXT_PUBLIC_APP_VERSION="${APP_VERSION}" pnpm build

# ---- runner ----
FROM node:22-alpine AS runner
WORKDIR /app
ARG APP_VERSION=0.0.1-SNAPSHOT
# docker image inspect로도 실제 빌드 버전을 확인할 수 있게 남긴다.
LABEL org.opencontainers.image.version=${APP_VERSION}
ENV NODE_ENV=production
ENV PORT=3000
ENV APP_VERSION=${APP_VERSION}
ENV NEXT_PUBLIC_APP_VERSION=${APP_VERSION}

RUN addgroup --system --gid 1001 nodejs \
  && adduser --system --uid 1001 nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000
CMD ["node", "server.js"]
