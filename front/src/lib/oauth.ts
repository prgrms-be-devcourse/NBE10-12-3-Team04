const GOOGLE_AUTH_URL = 'https://accounts.google.com/o/oauth2/v2/auth';
const GOOGLE_SCOPE = 'openid email profile';

export const OAUTH_STATE_KEY = 'oauth_state';

export const GOOGLE_REDIRECT_URI = process.env.NEXT_PUBLIC_GOOGLE_REDIRECT_URI ?? '';

// CSRF 방지용 state. 콜백에서 돌려받은 값과 대조해 우리가 시작한 요청인지 확인한다.
function createState() {
  const bytes = new Uint8Array(16);
  crypto.getRandomValues(bytes);

  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
}

export function saveOAuthState() {
  const state = createState();
  sessionStorage.setItem(OAUTH_STATE_KEY, state);

  return state;
}

export function takeOAuthState() {
  const state = sessionStorage.getItem(OAUTH_STATE_KEY);
  sessionStorage.removeItem(OAUTH_STATE_KEY);

  return state;
}

export function buildGoogleAuthUrl(state: string) {
  const params = new URLSearchParams({
    client_id: process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID ?? '',
    redirect_uri: GOOGLE_REDIRECT_URI,
    response_type: 'code',
    scope: GOOGLE_SCOPE,
    state,
  });

  return `${GOOGLE_AUTH_URL}?${params.toString()}`;
}
