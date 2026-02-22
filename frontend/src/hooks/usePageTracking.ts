import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { eventApi } from '@/api/events';

export function usePageTracking() {
  const location = useLocation();

  useEffect(() => {
    eventApi
      .track({
        eventType: 'PAGE_VIEW',
        page: location.pathname + location.search,
        referrer: document.referrer || undefined,
      })
      .catch(() => {});
  }, [location.pathname, location.search]);
}
