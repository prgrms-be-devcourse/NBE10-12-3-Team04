// 서버 @Email 검증에 걸릴 주소를 왕복 없이 미리 거르는 용도.
// 엄밀한 RFC 판별이 아니라 오타 방지가 목적이고, 최종 방어선은 서버다.
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const INVALID_EMAIL_MESSAGE = '이메일 형식이 올바르지 않습니다.';

export function isValidEmail(email: string) {
  return EMAIL_PATTERN.test(email);
}
