'use client';

import { FormEvent, Suspense, useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { LoaderCircle, Search } from 'lucide-react';
import { feedApi } from '@/lib/api';
import type { TripSearchLocation, TripSearchPage } from '@/types';

type SearchScope = 'ALL' | 'TRIP_TITLE' | 'POST_TITLE' | 'POST_CONTENT';
type SearchSort = 'LATEST' | 'OLDEST' | 'MOST_LIKED' | 'LEAST_LIKED';

const MAX_SEARCH_RESULTS = 300;

const scopeOptions: Array<{ value: SearchScope; label: string }> = [
  { value: 'ALL', label: '전체' },
  { value: 'TRIP_TITLE', label: '트립 제목' },
  { value: 'POST_TITLE', label: '포스트 제목' },
  { value: 'POST_CONTENT', label: '포스트 내용' },
];

const sortOptions: Array<{ value: SearchSort; label: string }> = [
  { value: 'LATEST', label: '최신순' },
  { value: 'OLDEST', label: '오래된순' },
  { value: 'MOST_LIKED', label: '좋아요 많은순' },
  { value: 'LEAST_LIKED', label: '좋아요 적은순' },
];

function SearchContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const queryKeyword = searchParams.get('keyword') ?? '';
  const queryScope = (searchParams.get('scope') as SearchScope | null) ?? 'ALL';
  const querySort = (searchParams.get('sort') as SearchSort | null) ?? 'LATEST';
  const queryCountry = searchParams.get('country') ?? '';
  const queryCity = searchParams.get('city') ?? '';

  const [keyword, setKeyword] = useState(queryKeyword);
  const [scope, setScope] = useState<SearchScope>(queryScope);
  const [sort, setSort] = useState<SearchSort>(querySort);
  const [country, setCountry] = useState(queryCountry);
  const [city, setCity] = useState(queryCity);
  const [result, setResult] = useState<TripSearchPage | null>(null);
  const [locations, setLocations] = useState<TripSearchLocation[]>([]);
  const [locationsLoading, setLocationsLoading] = useState(true);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState('');
  const loadMoreRef = useRef<HTMLDivElement>(null);
  const cityOptions = locations.find((location) => location.country === country)?.cities ?? [];

  useEffect(() => {
    let active = true;
    feedApi
      .getSearchLocations()
      .then((data) => {
        if (active) setLocations(data);
      })
      .catch(() => {
        if (active) setLocations([]);
      })
      .finally(() => {
        if (active) setLocationsLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;

    feedApi
      .search({
        keyword: queryKeyword,
        scope: queryScope,
        sort: querySort,
        country: queryCountry,
        city: queryCity,
        page: 0,
        size: 12,
      })
      .then((data) => {
        if (active) setResult(data);
      })
      .catch((reason: unknown) => {
        if (!active) return;
        setResult(null);
        setError(reason instanceof Error ? reason.message : '검색 중 오류가 발생했습니다.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [queryCity, queryCountry, queryKeyword, queryScope, querySort]);

  const loadNextPage = useCallback(async () => {
    if (
      !result
      || result.last
      || result.content.length >= result.totalElements
      || result.content.length >= MAX_SEARCH_RESULTS
      || loading
      || loadingMore
    ) {
      return;
    }

    setLoadingMore(true);
    try {
      const nextPage = await feedApi.search({
        keyword: queryKeyword,
        scope: queryScope,
        sort: querySort,
        country: queryCountry,
        city: queryCity,
        page: result.page + 1,
        size: 12,
      });
      setResult((current) => {
        if (!current) return nextPage;
        const content = Array.from(
          new Map(
            [...current.content, ...nextPage.content].map((trip) => [trip.tripId, trip]),
          ).values(),
        ).slice(0, MAX_SEARCH_RESULTS);
        return { ...nextPage, content };
      });
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : '다음 검색 결과를 불러오지 못했습니다.');
    } finally {
      setLoadingMore(false);
    }
  }, [
    loading,
    loadingMore,
    queryCity,
    queryCountry,
    queryKeyword,
    queryScope,
    querySort,
    result,
  ]);

  useEffect(() => {
    const target = loadMoreRef.current;
    if (
      !target
      || !result
      || result.last
      || result.content.length >= result.totalElements
      || result.content.length >= MAX_SEARCH_RESULTS
      || loading
      || loadingMore
    ) {
      return;
    }

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) void loadNextPage();
      },
      { rootMargin: '240px 0px' },
    );
    observer.observe(target);
    return () => observer.disconnect();
  }, [loadNextPage, loading, loadingMore, result]);

  const navigateToSearch = (nextSort = sort) => {
    setLoading(true);
    setError('');
    const query = new URLSearchParams();
    if (keyword.trim()) query.set('keyword', keyword.trim());
    if (scope !== 'ALL') query.set('scope', scope);
    if (nextSort !== 'LATEST') query.set('sort', nextSort);
    if (country.trim()) query.set('country', country.trim());
    if (country.trim() && city.trim()) query.set('city', city.trim());
    router.push(`/search${query.size ? `?${query}` : ''}`);
  };

  const submitSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    navigateToSearch();
  };

  return (
    <div className="mx-auto w-full max-w-6xl px-4 py-8 md:px-8 md:py-10">
      <div className="mb-7">
        <h1 className="text-2xl font-bold text-gray-900">트립 검색</h1>
        <p className="mt-1 text-sm text-gray-500">여행 제목과 포스트 기록에서 원하는 트립을 찾아보세요.</p>
      </div>

      <form
        onSubmit={submitSearch}
        className="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm md:p-5"
      >
        <div className="grid gap-3 md:grid-cols-[150px_160px_minmax(240px,1fr)_160px_auto]">
          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold text-gray-600">국가</span>
            <select
              value={country}
              disabled={locationsLoading}
              onChange={(event) => {
                setCountry(event.target.value);
                setCity('');
              }}
              className="h-10 w-full rounded-lg border border-gray-200 bg-white px-3 text-sm outline-none focus:border-emerald-500 disabled:cursor-wait disabled:bg-gray-100"
            >
              <option value="">전체</option>
              {locations.map((location) => (
                <option key={location.country} value={location.country}>{location.country}</option>
              ))}
            </select>
          </label>

          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold text-gray-600">도시</span>
            <select
              value={city}
              onChange={(event) => setCity(event.target.value)}
              disabled={!country || locationsLoading}
              className="h-10 w-full rounded-lg border border-gray-200 bg-white px-3 text-sm outline-none focus:border-emerald-500 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-500"
            >
              <option value="">전체</option>
              {cityOptions.map((option) => (
                <option key={option} value={option}>{option}</option>
              ))}
            </select>
          </label>

          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold text-gray-600">검색어</span>
            <div className="relative">
              <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="검색어를 입력하세요"
                className="h-10 w-full rounded-lg border border-gray-200 pl-9 pr-3 text-sm outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100"
              />
            </div>
          </label>

          <label className="block">
            <span className="mb-1.5 block text-xs font-semibold text-gray-600">카테고리</span>
            <select
              value={scope}
              onChange={(event) => setScope(event.target.value as SearchScope)}
              className="h-10 w-full rounded-lg border border-gray-200 bg-white px-3 text-sm outline-none focus:border-emerald-500"
            >
              {scopeOptions.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </label>

          <button
            type="submit"
            className="mt-auto h-10 rounded-lg bg-emerald-600 px-5 text-sm font-bold text-white transition hover:bg-emerald-700"
          >
            검색
          </button>
        </div>
      </form>

      <div className="mb-4 mt-8 flex items-end justify-between gap-4">
        <div>
          <h2 className="text-lg font-bold text-gray-900">검색 결과</h2>
          {!loading && result && (
            <p className="mt-0.5 text-sm text-gray-500">총 {result.totalElements.toLocaleString()}개의 트립</p>
          )}
        </div>
        <label className="flex items-center gap-2">
          <span className="text-xs font-semibold text-gray-500">정렬</span>
          <select
            value={sort}
            disabled={loading}
            onChange={(event) => {
              const nextSort = event.target.value as SearchSort;
              setSort(nextSort);
              navigateToSearch(nextSort);
            }}
            className="h-9 rounded-lg border border-gray-200 bg-white px-3 text-sm font-semibold text-gray-700 outline-none focus:border-emerald-500 disabled:opacity-50"
          >
            {sortOptions.map((option) => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
        </label>
      </div>

      {loading ? (
        <div className="flex min-h-64 items-center justify-center gap-2 rounded-2xl border border-gray-200 bg-white text-sm text-gray-500">
          <LoaderCircle size={19} className="animate-spin" />
          검색 중
        </div>
      ) : error ? (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-5 py-12 text-center text-sm text-red-700">
          {error}
        </div>
      ) : !result?.content.length ? (
        <div className="rounded-2xl border border-gray-200 bg-white px-5 py-16 text-center">
          <p className="font-semibold text-gray-700">검색 결과가 없습니다.</p>
          <p className="mt-1 text-sm text-gray-500">검색어나 지역 조건을 변경해보세요.</p>
        </div>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {result.content.map((trip) => (
              <Link
                key={trip.tripId}
                href={`/trips/${trip.tripId}`}
                className="overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm transition hover:-translate-y-0.5 hover:border-emerald-300 hover:shadow-md"
              >
                <div className="h-44 bg-gradient-to-br from-emerald-100 to-sky-100">
                  {trip.thumbnailUrl && (
                    <img
                      src={trip.thumbnailUrl}
                      alt=""
                      onError={(event) => {
                        event.currentTarget.style.display = 'none';
                      }}
                      className="h-full w-full object-cover"
                    />
                  )}
                </div>
                <div className="p-4">
                  <h3 className="truncate font-bold text-gray-900">{trip.title}</h3>
                  <p className="mt-1 truncate text-xs text-gray-500">
                    {[trip.country, trip.city].filter(Boolean).join(' · ') || '지역 미지정'}
                  </p>
                  <p className="mt-1 text-xs text-gray-400">
                    {trip.startDate || '날짜 미지정'}
                    {trip.endDate && trip.endDate !== trip.startDate ? ` ~ ${trip.endDate}` : ''}
                  </p>
                  <p className="mt-3 line-clamp-2 min-h-10 text-sm leading-5 text-gray-600">
                    {trip.previewText || '작성된 미리보기가 없습니다.'}
                  </p>
                </div>
              </Link>
            ))}
          </div>

          <div ref={loadMoreRef} className="flex min-h-20 items-center justify-center">
            {loadingMore ? (
              <div className="flex items-center gap-2 text-sm text-gray-500">
                <LoaderCircle size={18} className="animate-spin" />
                다음 결과를 불러오는 중
              </div>
            ) : result.content.length >= MAX_SEARCH_RESULTS
              && result.content.length < result.totalElements ? (
              <p className="text-center text-sm text-gray-500">
                최대 300개까지 표시했습니다. 검색 조건을 추가하면 더 정확한 결과를 확인할 수 있습니다.
              </p>
            ) : result.last || result.content.length >= result.totalElements ? (
              <p className="text-sm text-gray-400">모든 검색 결과를 확인했습니다.</p>
            ) : null}
          </div>
        </>
      )}
    </div>
  );
}

function SearchPageWithParams() {
  const searchParams = useSearchParams();
  return <SearchContent key={searchParams.toString()} />;
}

export default function SearchPage() {
  return (
    <Suspense fallback={null}>
      <SearchPageWithParams />
    </Suspense>
  );
}
