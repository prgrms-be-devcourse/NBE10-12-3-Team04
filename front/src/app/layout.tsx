import type { Metadata } from 'next';
import { Suspense } from 'react';
import './globals.css';
import AppShell from '@/components/layout/AppShell';
import { appVersion } from '@/lib/version';

export const metadata: Metadata = {
  title: 'TripTrace',
  description: '나만의 여행 기록을 시작해보세요',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body className="bg-gray-50 antialiased" data-app-version={appVersion}>
        <Suspense fallback={<main className="min-h-dvh">{children}</main>}>
          <AppShell>{children}</AppShell>
        </Suspense>
      </body>
    </html>
  );
}
