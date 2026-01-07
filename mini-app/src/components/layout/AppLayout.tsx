import { Outlet } from 'react-router-dom';
import { useEffect } from 'react';
import { viewport } from '@telegram-apps/sdk-react';

export function AppLayout() {
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
