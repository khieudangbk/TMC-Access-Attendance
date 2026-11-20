import { useQuery } from '@tanstack/react-query'

import { apiGet } from '@/shared/api/client'
import type { StationListResponse } from '@/shared/api/types'

export const stationKeys = {
  all: ['stations'] as const,
}

export function useStations() {
  return useQuery({
    queryKey: stationKeys.all,
    queryFn: ({ signal }) => apiGet<StationListResponse>('/v1/stations', signal),
  })
}
