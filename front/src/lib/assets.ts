import type { SyntheticEvent } from 'react';

export const DEFAULT_TRIP_THUMBNAIL = '/images/defaults/trip-thumbnail.webp';
export const DEFAULT_PROFILE_AVATAR = '/images/defaults/profile-avatar.webp';

export function applyImageFallback(
  event: SyntheticEvent<HTMLImageElement>,
  fallbackSrc: string,
) {
  const image = event.currentTarget;
  if (image.getAttribute('src') === fallbackSrc) return;
  image.src = fallbackSrc;
}
