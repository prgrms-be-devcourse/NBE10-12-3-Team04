'use client';

import { useSearchParams } from 'next/navigation';
import Header from '@/components/layout/Header';
import Sidebar from '@/components/layout/Sidebar';

export default function AppShell({ children }: { children: React.ReactNode }) {
  const searchParams = useSearchParams();
  const isPreview = searchParams.get('preview') === 'true';

  if (isPreview) {
    return <main className="min-h-dvh">{children}</main>;
  }

  return (
    <>
      <Sidebar />
      <Header />
      <main className="min-h-dvh pb-[calc(72px_+_env(safe-area-inset-bottom))] pt-[56px] md:ml-[72px] md:mt-[64px] md:min-h-screen md:pb-0 md:pt-0">
        {children}
      </main>
    </>
  );
}
