import { useState, useEffect, useCallback } from 'react';
import { getAdminLogs, exportAdminLogs } from '@/api';
import type { AdminLogsResponse, AdminLogsFilter } from '@/types';

interface UseAdminLogsResult {
  data: AdminLogsResponse | null;
  isLoading: boolean;
  isExporting: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
  exportLogs: () => Promise<void>;
  setFilter: (filter: AdminLogsFilter) => void;
}

export function useAdminLogs(chatId: number, initialFilter?: AdminLogsFilter): UseAdminLogsResult {
  const [data, setData] = useState<AdminLogsResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isExporting, setIsExporting] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [filter, setFilter] = useState<AdminLogsFilter | undefined>(initialFilter);

  const fetchLogs = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const logs = await getAdminLogs(chatId, filter);
      setData(logs);
    } catch (err) {
      setError(err as Error);
    } finally {
      setIsLoading(false);
    }
  }, [chatId, filter]);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  const handleExport = useCallback(async () => {
    try {
      setIsExporting(true);
      setError(null);
      const blob = await exportAdminLogs(chatId, filter);

      // Download file
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `admin-logs-${chatId}-${new Date().toISOString()}.csv`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err as Error);
      throw err;
    } finally {
      setIsExporting(false);
    }
  }, [chatId, filter]);

  return {
    data,
    isLoading,
    isExporting,
    error,
    refetch: fetchLogs,
    exportLogs: handleExport,
    setFilter,
  };
}
