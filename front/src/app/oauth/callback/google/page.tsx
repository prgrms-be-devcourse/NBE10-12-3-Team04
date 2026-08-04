'use client';

import { Suspense, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { authApi } from '@/lib/api';
import { GOOGLE_REDIRECT_URI, takeOAuthState } from '@/lib/oauth';

type CallbackParams = {
  code: string | null;
  state: string | null;
  deniedReason: string | null;
  savedState: string | null;
};

// 검증 실패도 예외로 흘려보내서 화면 표시를 catch 한 곳에서만 처리한다.
async function exchangeGoogleCode({ code, state, deniedReason, savedState }: CallbackParams) {
  if (deniedReason) {
    throw new Error('구글 로그인이 취소되었습니다.');
  }

  if (!code || !state) {
    throw new Error('잘못된 접근입니다. 로그인을 다시 시도해주세요.');
  }

  // state 불일치는 우리가 시작하지 않은 요청이라는 뜻이라 여기서 끊는다.
  if (!savedState || savedState !== state) {
    throw new Error('로그인 요청이 유효하지 않습니다. 처음부터 다시 시도해주세요.');
  }

  return authApi.loginWithGoogle({ code, redirectUri: GOOGLE_REDIRECT_URI });
}

function GoogleCallback() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [error, setError] = useState('');
  // 인가 코드는 1회용이라 두 번 교환하면 실패한다. StrictMode의 effect 재실행을 막는다.
  const startedRef = useRef(false);

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    exchangeGoogleCode({
      code: searchParams.get('code'),
      state: searchParams.get('state'),
      deniedReason: searchParams.get('error'),
      savedState: takeOAuthState(),
    })
      .then((data) => {
        if (data.status === 'PENDING_PROFILE') {
          // TODO: 온보딩 페이지 구현 후 해당 경로로 이동시킨다.
          console.log('추가 정보 입력이 필요합니다 (온보딩 페이지는 추후 구현)');
        }

        router.replace('/');
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : '구글 로그인에 실패했습니다.');
      });
  }, [router, searchParams]);

  if (error) {
    return (
      <Shell>
        <h1 className="mb-1 text-xl font-bold text-gray-900">로그인 실패</h1>
        <p className="mb-6 text-sm text-red-500">{error}</p>
        <Link
          href="/auth/login"
          className="block w-full rounded-lg bg-green-600 py-2.5 text-center text-sm font-semibold text-white transition-colors hover:bg-green-700"
        >
          로그인 페이지로 돌아가기
        </Link>
      </Shell>
    );
  }

  return (
    <Shell>
      <h1 className="mb-1 text-xl font-bold text-gray-900">로그인 중</h1>
      <p className="text-sm text-gray-500">구글 계정으로 로그인하고 있어요. 잠시만 기다려주세요.</p>
    </Shell>
  );
}

function Shell({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-[calc(100dvh_-_56px_-_72px_-_env(safe-area-inset-bottom))] items-center justify-center px-4 py-6 md:min-h-screen">
      <div className="w-full max-w-[420px] rounded-2xl border border-gray-100 bg-white p-5 shadow-sm sm:p-8">
        <p className="mb-1 text-xs text-gray-400">TripTrace</p>
        {children}
      </div>
    </div>
  );
}

export default function GoogleCallbackPage() {
  return (
    <Suspense
      fallback={
        <Shell>
          <h1 className="mb-1 text-xl font-bold text-gray-900">로그인 중</h1>
          <p className="text-sm text-gray-500">잠시만 기다려주세요.</p>
        </Shell>
      }
    >
      <GoogleCallback />
    </Suspense>
  );
}
