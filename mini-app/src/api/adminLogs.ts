import client from './client';
import type { AdminLogsResponse, AdminLogsFilter } from '@/types';

export async function getAdminLogs(chatId: number, filter?: AdminLogsFilter): Promise<AdminLogsResponse> {
  const searchParams = new URLSearchParams();

  if (filter?.action) searchParams.set('action', filter.action);
  if (filter?.performedBy) searchParams.set('performedBy', filter.performedBy.toString());
  if (filter?.targetUserId) searchParams.set('targetUserId', filter.targetUserId.toString());
  if (filter?.startDate) searchParams.set('startDate', filter.startDate);
  if (filter?.endDate) searchParams.set('endDate', filter.endDate);
  if (filter?.page !== undefined) searchParams.set('page', filter.page.toString());
  if (filter?.pageSize !== undefined) searchParams.set('pageSize', filter.pageSize.toString());

  const url = `chats/${chatId}/logs${searchParams.toString() ? `?${searchParams.toString()}` : ''}`;
  return client.get(url).json<AdminLogsResponse>();
}

export async function exportAdminLogs(chatId: number, filter?: AdminLogsFilter): Promise<Blob> {
  const searchParams = new URLSearchParams();

  if (filter?.action) searchParams.set('action', filter.action);
  if (filter?.performedBy) searchParams.set('performedBy', filter.performedBy.toString());
  if (filter?.targetUserId) searchParams.set('targetUserId', filter.targetUserId.toString());
  if (filter?.startDate) searchParams.set('startDate', filter.startDate);
  if (filter?.endDate) searchParams.set('endDate', filter.endDate);

  const url = `chats/${chatId}/logs${searchParams.toString() ? `?${searchParams.toString()}` : ''}`;
  return client.get(url).blob();
}
