'use client';

import type { PointerEventHandler, TouchEventHandler } from 'react';
import { createPortal } from 'react-dom';
import Link from 'next/link';
import { CalendarDays, Clock3, MapPin, X } from 'lucide-react';
import MarkdownContent from '@/components/MarkdownContent';
import { applyImageFallback, DEFAULT_TRIP_THUMBNAIL } from '@/lib/assets';
import type { Marker } from '@/types';

type PreviewImage = {
  id: string;
  url: string;
  thumbnailUrl?: string;
};

type PreviewPost = {
  id: string;
  tripId: string;
  title: string;
  content: string;
  date?: string;
  time?: string;
  marker?: Marker;
  images: PreviewImage[];
};

type PostPreviewModalProps = {
  post: PreviewPost;
  selectedImageId: string | null;
  onSelectImage: (imageId: string) => void;
  onClose: () => void;
  showTripLink?: boolean;
  hasMultiplePosts?: boolean;
  onPreviousPost?: () => void;
  onNextPost?: () => void;
  onTouchStart?: TouchEventHandler<HTMLDivElement>;
  onTouchEnd?: TouchEventHandler<HTMLDivElement>;
  onPointerDown?: PointerEventHandler<HTMLDivElement>;
  onPointerUp?: PointerEventHandler<HTMLDivElement>;
};

function formatDate(value?: string) {
  return value ? value.slice(0, 10).replaceAll('-', '.') : '';
}

export default function PostPreviewModal({
  post,
  selectedImageId,
  onSelectImage,
  onClose,
  showTripLink = true,
  hasMultiplePosts = false,
  onPreviousPost,
  onNextPost,
  onTouchStart,
  onTouchEnd,
  onPointerDown,
  onPointerUp,
}: PostPreviewModalProps) {
  if (typeof document === 'undefined') return null;

  const selectedImage = post.images.find((image) => image.id === selectedImageId) ?? post.images[0] ?? {
    id: `default-${post.id}`,
    url: DEFAULT_TRIP_THUMBNAIL,
    thumbnailUrl: DEFAULT_TRIP_THUMBNAIL,
  };
  const selectedImageIndex = post.images.findIndex((image) => image.id === selectedImage.id);

  return createPortal(
    <div className="fixed inset-0 z-[90] flex items-center justify-center bg-black/70 p-2 sm:p-4">
      {hasMultiplePosts && onPreviousPost && onNextPost && (
        <>
          <button type="button" onClick={onPreviousPost} className="absolute left-2 top-1/2 z-20 hidden h-11 w-11 -translate-y-1/2 items-center justify-center rounded-full bg-white/90 text-2xl text-gray-800 shadow-lg hover:bg-white sm:flex" aria-label="이전 Post">‹</button>
          <button type="button" onClick={onNextPost} className="absolute right-2 top-1/2 z-20 hidden h-11 w-11 -translate-y-1/2 items-center justify-center rounded-full bg-white/90 text-2xl text-gray-800 shadow-lg hover:bg-white sm:flex" aria-label="다음 Post">›</button>
        </>
      )}

      <div className="relative flex max-h-[calc(100dvh_-_24px)] w-full max-w-6xl flex-col overflow-hidden rounded-xl bg-white shadow-2xl lg:max-h-[88vh] lg:flex-row">
        <button type="button" onClick={onClose} title="닫기" className="absolute right-3 top-3 z-30 flex h-9 w-9 items-center justify-center rounded-full bg-black/50 text-white hover:bg-black/70">
          <X size={18} />
        </button>

        <div className="relative flex min-h-[280px] flex-1 touch-pan-y items-center justify-center bg-black sm:min-h-[360px] lg:min-h-[640px]" onTouchStart={onTouchStart} onTouchEnd={onTouchEnd} onPointerDown={onPointerDown} onPointerUp={onPointerUp}>
          {post.images.length > 1 && (
            <span className="absolute bottom-3 left-1/2 z-10 -translate-x-1/2 rounded-full bg-black/55 px-2.5 py-1 text-xs font-semibold text-white">
              {(selectedImageIndex >= 0 ? selectedImageIndex : 0) + 1} / {post.images.length}
            </span>
          )}
          <img src={selectedImage.url} alt="" onError={(event) => applyImageFallback(event, DEFAULT_TRIP_THUMBNAIL)} className="max-h-[62dvh] w-full object-contain lg:max-h-[88vh]" />
        </div>

        <aside className="max-h-[38dvh] w-full flex-shrink-0 overflow-y-auto border-t border-gray-100 p-4 sm:p-5 lg:max-h-none lg:w-96 lg:border-l lg:border-t-0">
          <p className="line-clamp-2 text-base font-bold text-gray-900">{post.title}</p>
          <div className="mt-4 space-y-3 text-sm text-gray-500">
            {post.marker?.placeName && <p className="flex items-center gap-2"><MapPin size={15} /><span>{post.marker.placeName}</span></p>}
            {post.date && <p className="flex items-center gap-2"><CalendarDays size={15} /><span>{formatDate(post.date)}</span></p>}
            {post.time && <p className="flex items-center gap-2"><Clock3 size={15} /><span>{post.time}</span></p>}
          </div>
          {post.content.trim() ? <MarkdownContent markdown={post.content} variant="detail" className="mt-5" /> : <p className="mt-5 text-sm leading-6 text-gray-600">메모가 없습니다.</p>}

          {post.images.length > 0 && (
            <div className="mt-5 grid grid-cols-4 gap-1.5">
              {post.images.map((image) => (
                <button key={image.id} type="button" onClick={() => onSelectImage(image.id)} className={`aspect-square overflow-hidden rounded-md border ${selectedImage.id === image.id ? 'border-emerald-500 ring-2 ring-emerald-500/20' : 'border-transparent'}`}>
                  <img src={image.thumbnailUrl || image.url} alt="" onError={(event) => applyImageFallback(event, DEFAULT_TRIP_THUMBNAIL)} className="h-full w-full object-cover" />
                </button>
              ))}
            </div>
          )}

          {showTripLink && <Link href={`/trips/${post.tripId}`} className="mt-6 inline-flex rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-700">Trip 보기</Link>}
        </aside>
      </div>
    </div>,
    document.body,
  );
}
