'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { isAuthenticated, userApi } from '@/lib/api';

export default function CompleteProfilePage() {
  const router = useRouter();
  const [username, setUsername] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // 로그인 상태에서만 호출할 수 있는 API라, 토큰이 없으면 403 대신 로그인 화면으로 보낸다.
  useEffect(() => {
    if (!isAuthenticated()) {
      router.replace('/auth/login');
    }
  }, [router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await userApi.completeProfile({ username });
      // 뒤로가기로 온보딩 화면에 다시 들어오면 이미 ACTIVE라 409가 나므로 히스토리를 남기지 않는다.
      router.replace('/');
    } catch (err) {
      setError(err instanceof Error ? err.message : '닉네임 설정에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-[calc(100dvh_-_56px_-_72px_-_env(safe-area-inset-bottom))] items-center justify-center px-4 py-6 md:min-h-screen">
      <div className="w-full max-w-[420px] rounded-2xl border border-gray-100 bg-white p-5 shadow-sm sm:p-8">
        <p className="mb-1 text-xs text-gray-400">TripTrace</p>
        <h1 className="mb-1 text-2xl font-bold text-gray-900">닉네임 설정</h1>
        <p className="mb-6 text-sm text-gray-500">
          가입이 거의 끝났어요. 사용하실 닉네임만 정해주세요.
        </p>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">닉네임</label>
            <input
              type="text"
              placeholder="2~50자로 입력해주세요"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              minLength={2}
              maxLength={50}
              autoFocus
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none focus:border-green-500 focus:ring-1 focus:ring-green-500"
            />
          </div>

          {error && <p className="text-sm text-red-500">{error}</p>}

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-lg bg-green-600 py-2.5 font-semibold text-white transition-colors hover:bg-green-700 disabled:opacity-60"
          >
            {loading ? '처리 중...' : '시작하기'}
          </button>
        </form>
      </div>
    </div>
  );
}
