import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useEffect, useCallback } from 'react';
import { viewport } from '@telegram-apps/sdk-react';
import { useBackButton } from '@/hooks/telegram/useBackButton';
import { useAuthMode } from '@/hooks/auth/useAuthMode';

export function AppLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { isMiniApp } = useAuthMode();

  const isHomePage = location.pathname === '/';

  useEffect(() => {
    if (viewport.mount.isAvailable()) {
      viewport.mount();
      if (viewport.expand.isAvailable()) {
        viewport.expand();
      }
    }

    return () => {
      if (viewport.unmount) {
        viewport.unmount();
      }
    };
  }, []);

  // Wrap navigate callback to prevent memory leak
  const handleBack = useCallback(() => navigate(-1), [navigate]);

  // Show back button only in Mini App mode and when not on home page
  useBackButton({
    onClick: handleBack,
    visible: isMiniApp && !isHomePage,
  });

  return (
    <div style={{
      minHeight: '100vh',
      backgroundColor: 'var(--tgui--bg_color)',
      color: 'var(--tgui--text_color)'
    }}>
      <Outlet />
    </div>
  );
}
