import { useQuery } from '@tanstack/react-query'

import { apiGet } from '@/shared/api/client'
import type { SystemStatus } from '@/shared/api/types'

export const systemStatusKey = ['system-status'] as const

export function useSystemStatus() {
  return useQuery({
    queryKey: systemStatusKey,
    queryFn: ({ signal }) => apiGet<SystemStatus>('/v1/system/status', signal),
    refetchInterval: 30_000,
  })
}
