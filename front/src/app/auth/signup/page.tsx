'use client';

import { useEffect, useState, useRef } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Plus, CheckCircle2 } from 'lucide-react';
import { authApi, ApiError } from '@/lib/api';
import { applyImageFallback, DEFAULT_PROFILE_AVATAR } from '@/lib/assets';
import { isValidEmail, INVALID_EMAIL_MESSAGE } from '@/lib/validation';

// 서버 재전송 쿨다운과 같은 값. 서버가 최종 방어선이고 이건 UI 힌트일 뿐이다.
const RESEND_COOLDOWN_SECONDS = 60;

type Notice = { type: 'success' | 'error'; text: string };

export default function SignupPage() {
  const router = useRouter();
  const fileRef = useRef<HTMLInputElement>(null);

  const [form, setForm] = useState({
    email: '',
    username: '',
    password: '',
    passwordConfirm: '',
  });
  const [profilePreview, setProfilePreview] = useState<string | null>(null);
  const [profileFile, setProfileFile] = useState<File | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // 이메일 인증 상태
  const [code, setCode] = useState('');
  const [codeSent, setCodeSent] = useState(false);
  const [emailVerified, setEmailVerified] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const [sendingCode, setSendingCode] = useState(false);
  const [verifyingCode, setVerifyingCode] = useState(false);
  const [verificationNotice, setVerificationNotice] = useState<Notice | null>(null);

  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setTimeout(() => setCooldown(cooldown - 1), 1000);
    return () => clearTimeout(timer);
  }, [cooldown]);

  const resetVerification = () => {
    setEmailVerified(false);
    setCodeSent(false);
    setCode('');
    setCooldown(0);
    setVerificationNotice(null);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const url = URL.createObjectURL(file);
    setProfilePreview(url);
    setProfileFile(file);
  };

  // 발송된 코드는 그 시점의 주소에 묶여 있다. 주소가 바뀌면 인증 절차를 처음부터 다시 밟게 한다.
  // 발송·인증·가입이 모두 같은 문자열을 보내도록 입력 시점에 공백을 한 번만 정리한다.
  const handleEmailChange = (email: string) => {
    setForm({ ...form, email: email.trim() });
    if (codeSent) resetVerification();
  };

  const handleSendCode = async () => {
    if (!form.email) {
      setVerificationNotice({ type: 'error', text: '이메일을 먼저 입력해주세요.' });
      return;
    }

    // 발송 버튼은 type="button"이라 브라우저 기본 검증을 타지 않는다. 형식 검사를 직접 해준다.
    if (!isValidEmail(form.email)) {
      setVerificationNotice({ type: 'error', text: INVALID_EMAIL_MESSAGE });
      return;
    }

    setSendingCode(true);
    setVerificationNotice(null);
    try {
      await authApi.sendEmailVerificationCode({ email: form.email });
      setCodeSent(true);
      setCooldown(RESEND_COOLDOWN_SECONDS);
      setVerificationNotice({ type: 'success', text: '인증번호를 보냈습니다. 메일함을 확인해주세요.' });
    } catch (err) {
      // 새로고침 등으로 프론트 카운트다운이 초기화된 뒤 서버 쿨다운(429)에 걸리는 경우.
      // 이미 발급된 코드가 있다는 뜻이므로 입력칸을 열어두고 카운트다운도 다시 건다.
      if (err instanceof ApiError && err.status === 429) {
        setCodeSent(true);
        setCooldown(RESEND_COOLDOWN_SECONDS);
      }
      setVerificationNotice({
        type: 'error',
        text: err instanceof Error ? err.message : '인증번호 발송에 실패했습니다.',
      });
    } finally {
      setSendingCode(false);
    }
  };

  const handleVerifyCode = async () => {
    if (!code.trim()) {
      setVerificationNotice({ type: 'error', text: '인증번호를 입력해주세요.' });
      return;
    }

    setVerifyingCode(true);
    setVerificationNotice(null);
    try {
      await authApi.verifyEmailCode({ email: form.email, code: code.trim() });
      setEmailVerified(true);
      setCooldown(0);
      setVerificationNotice({ type: 'success', text: '이메일 인증이 완료되었습니다.' });
    } catch (err) {
      // 코드 불일치·시도 초과·만료 등은 서버 메시지를 그대로 보여준다.
      setVerificationNotice({
        type: 'error',
        text: err instanceof Error ? err.message : '인증에 실패했습니다.',
      });
    } finally {
      setVerifyingCode(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    // 인증을 마치면 입력칸이 잠기므로 정상 경로에서는 이미 걸러진 값이다.
    // 다만 인증 여부와 무관하게 형식은 형식대로 막아야 하므로 제출 시점에도 확인한다.
    if (!isValidEmail(form.email)) {
      setError(INVALID_EMAIL_MESSAGE);
      return;
    }

    if (!emailVerified) {
      setError('이메일 인증을 완료해주세요.');
      return;
    }

    if (form.password !== form.passwordConfirm) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }

    setLoading(true);
    try {
      let profileImageUrl: string | undefined;
      if (profileFile) {
        const formData = new FormData();
        formData.append('image', profileFile);
        const uploaded = await authApi.uploadProfileImage(formData);
        profileImageUrl = uploaded.profileImageUrl;
      }

      await authApi.signup({
        email: form.email,
        username: form.username,
        password: form.password,
        profileImageUrl,
      });
      router.push('/auth/login');
    } catch (err) {
      // 인증 후 30분이 지나면 서버가 403으로 막는다. 인증 상태를 풀어 다시 받을 수 있게 한다.
      if (err instanceof ApiError && err.status === 403) {
        resetVerification();
        setError('이메일 인증이 만료되었습니다. 다시 인증해주세요.');
        return;
      }
      setError(err instanceof Error ? err.message : '회원가입에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const sendButtonLabel = () => {
    if (sendingCode) return '전송 중...';
    if (cooldown > 0) return `${cooldown}초 후 재전송`;
    if (codeSent) return '재전송';
    return '인증번호 받기';
  };

  return (
    <div className="flex min-h-[calc(100dvh_-_56px_-_72px_-_env(safe-area-inset-bottom))] items-center justify-center px-4 py-6 md:min-h-screen md:py-12">
      <div className="w-full max-w-[420px] rounded-2xl border border-gray-100 bg-white p-5 shadow-sm sm:p-8">
        <p className="text-xs text-gray-400 mb-1">TripTrace</p>
        <h1 className="text-2xl font-bold text-gray-900 mb-1">회원가입</h1>
        <p className="text-sm text-gray-500 mb-6">나만의 여행 기록을 시작해보세요.</p>

        {/* 프로필 이미지 */}
        <div className="flex flex-col items-center mb-6">
          <button
            type="button"
            onClick={() => fileRef.current?.click()}
            className="relative w-20 h-20 rounded-full bg-gray-100 flex items-center justify-center hover:bg-gray-200 transition-colors"
          >
            <img
              src={profilePreview || DEFAULT_PROFILE_AVATAR}
              alt="프로필"
              onError={(event) => applyImageFallback(event, DEFAULT_PROFILE_AVATAR)}
              className="w-full h-full rounded-full object-cover"
            />
            <span className="absolute bottom-0 right-0 w-6 h-6 bg-green-600 rounded-full flex items-center justify-center">
              <Plus size={14} className="text-white" />
            </span>
          </button>
          <p className="text-xs text-gray-400 mt-2">프로필 이미지 등록</p>
          <input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={handleFileChange} />
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="block text-sm font-medium text-gray-700">이메일</label>
              {emailVerified && (
                <span className="inline-flex items-center gap-1 text-xs font-medium text-green-600">
                  <CheckCircle2 size={14} />
                  인증완료
                </span>
              )}
            </div>
            <div className="flex gap-2">
              <input
                type="email"
                placeholder="name@example.com"
                value={form.email}
                onChange={(e) => handleEmailChange(e.target.value)}
                required
                disabled={emailVerified}
                className="min-w-0 flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500 focus:ring-1 focus:ring-green-500 disabled:bg-gray-50 disabled:text-gray-500"
              />
              {!emailVerified && (
                <button
                  type="button"
                  onClick={handleSendCode}
                  disabled={sendingCode || cooldown > 0 || !form.email.trim()}
                  className="shrink-0 whitespace-nowrap rounded-lg border border-green-600 px-3 py-2 text-sm font-medium text-green-600 transition-colors hover:bg-green-50 disabled:border-gray-300 disabled:text-gray-400 disabled:hover:bg-transparent"
                >
                  {sendButtonLabel()}
                </button>
              )}
            </div>
          </div>

          {codeSent && !emailVerified && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">인증번호</label>
              <div className="flex gap-2">
                <input
                  type="text"
                  inputMode="numeric"
                  maxLength={6}
                  placeholder="6자리 숫자"
                  value={code}
                  onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
                  className="min-w-0 flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm tracking-widest outline-none focus:border-green-500 focus:ring-1 focus:ring-green-500"
                />
                <button
                  type="button"
                  onClick={handleVerifyCode}
                  disabled={verifyingCode || !code.trim()}
                  className="shrink-0 whitespace-nowrap rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-green-700 disabled:opacity-60"
                >
                  {verifyingCode ? '확인 중...' : '확인'}
                </button>
              </div>
            </div>
          )}

          {verificationNotice && (
            <p className={`text-sm ${verificationNotice.type === 'success' ? 'text-green-600' : 'text-red-500'}`}>
              {verificationNotice.text}
            </p>
          )}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">사용자 이름</label>
            <input
              type="text"
              placeholder="Traveler_shb"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              required
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500 focus:ring-1 focus:ring-green-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">비밀번호</label>
            <input
              type="password"
              placeholder="••••••••"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              required
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500 focus:ring-1 focus:ring-green-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">비밀번호 확인</label>
            <input
              type="password"
              placeholder="••••••••"
              value={form.passwordConfirm}
              onChange={(e) => setForm({ ...form, passwordConfirm: e.target.value })}
              required
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm outline-none focus:border-green-500 focus:ring-1 focus:ring-green-500"
            />
          </div>

          {error && <p className="text-sm text-red-500">{error}</p>}

          {!emailVerified && (
            <p className="text-xs text-gray-400">가입하려면 이메일 인증을 먼저 완료해주세요.</p>
          )}

          <button
            type="submit"
            disabled={loading || !emailVerified}
            className="w-full bg-green-600 hover:bg-green-700 disabled:opacity-60 disabled:hover:bg-green-600 text-white font-semibold py-2.5 rounded-lg transition-colors"
          >
            {loading ? '처리 중...' : '가입하기'}
          </button>
        </form>

        <p className="text-center text-sm text-gray-500 mt-4">
          이미 계정이 있으신가요?{' '}
          <Link href="/auth/login" className="text-green-600 font-medium hover:underline">
            로그인
          </Link>
        </p>
      </div>
    </div>
  );
}
