'use client';

import { FormEvent, useEffect, useState } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { LogOut, Search } from 'lucide-react';
import { isAuthenticated, userApi } from '@/lib/api';
import { applyImageFallback, DEFAULT_PROFILE_AVATAR } from '@/lib/assets';
import { appVersion } from '@/lib/version';

interface HeaderProps {
  rightSlot?: React.ReactNode;
}

export default function Header({ rightSlot }: HeaderProps) {
  const pathname = usePathname();
  const router = useRouter();
  const [loggedIn, setLoggedIn] = useState(false);
  const [profileImageUrl, setProfileImageUrl] = useState('');
  const [keyword, setKeyword] = useState('');

  useEffect(() => {
    console.info(`[TripTrace] version: ${appVersion}`);
  }, []);

  useEffect(() => {
    const syncAuthState = () => {
      const authed = isAuthenticated();
      setLoggedIn(authed);
      if (!authed) {
        setProfileImageUrl('');
        return;
      }

      userApi
        .getMe()
        .then((user) => {
          setLoggedIn(true);
          setProfileImageUrl(typeof user.profileImageUrl === 'string' ? user.profileImageUrl : '');
        })
        .catch(() => {
          setLoggedIn(false);
          setProfileImageUrl('');
        });
    };

    syncAuthState();
    window.addEventListener('auth-change', syncAuthState);

    return () => window.removeEventListener('auth-change', syncAuthState);
  }, [pathname]);

  const submitSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const query = new URLSearchParams();
    if (keyword.trim()) query.set('keyword', keyword.trim());
    router.push(`/search${query.size ? `?${query}` : ''}`);
  };

  return (
    <header className="fixed left-0 right-0 top-0 z-30 flex h-[56px] items-center border-b border-gray-200 bg-white/95 px-4 backdrop-blur md:left-[72px] md:h-[64px] md:px-8">
      <Link href="/" className="mr-3 shrink-0 text-base font-bold text-gray-900 md:mr-6 md:text-lg">
        TripTrace
      </Link>

      <div className="relative mr-auto w-full max-w-xl">
        <form onSubmit={submitSearch} role="search">
          <Search
            size={17}
            className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
          />
          <input
            type="text"
            role="searchbox"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="여행 제목이나 기록 검색"
            aria-label="여행 검색"
            className="h-9 w-full rounded-xl border border-gray-200 bg-gray-50 pl-9 pr-11 text-sm text-gray-900 outline-none transition focus:border-emerald-500 focus:bg-white focus:ring-2 focus:ring-emerald-100"
          />
          <button
            type="submit"
            aria-label="트립 검색"
            className="absolute right-1 top-1/2 flex h-7 w-8 -translate-y-1/2 items-center justify-center rounded-lg text-emerald-700 hover:bg-emerald-50"
          >
            <Search size={16} />
          </button>
        </form>
      </div>

      <div className="flex min-w-0 items-center gap-2">
        {rightSlot}
        {loggedIn ? (
          <>
            <Link
              href="/auth/logout"
              className="hidden h-9 items-center gap-1.5 rounded-lg border border-gray-200 px-3 text-sm font-semibold text-gray-600 hover:bg-gray-50 sm:flex"
            >
              <LogOut size={15} />
              로그아웃
            </Link>
            <Link
              href="/users/me"
              className="flex h-9 items-center gap-1.5 rounded-lg bg-emerald-600 px-3 text-sm font-semibold text-white hover:bg-emerald-700"
            >
              <img
                src={profileImageUrl || DEFAULT_PROFILE_AVATAR}
                alt=""
                onError={(event) => applyImageFallback(event, DEFAULT_PROFILE_AVATAR)}
                className="h-5 w-5 rounded-full object-cover bg-white/20"
              />
              <span className="hidden sm:inline">내정보</span>
            </Link>
          </>
        ) : (
          <Link
            href="/auth/login"
            className="flex h-9 items-center rounded-lg bg-emerald-600 px-3 text-sm font-semibold text-white hover:bg-emerald-700 sm:px-4"
          >
            로그인
          </Link>
        )}
      </div>
    </header>
  );
}
