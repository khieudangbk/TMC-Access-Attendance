export type Direction = 'IN' | 'OUT'

export type StationStatus = 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE'

export interface GateStation {
  stationId: string
  factoryId: string
  factoryName: string
  name: string
  direction: Direction
  gateId: string
  kioskId: string
  status: StationStatus
  configRevision: number
}

export interface StationListResponse {
  items: GateStation[]
  total: number
  generatedAt: string
}

export interface SystemStatus {
  application: string
  version: string
  environment: string
  serverTime: string
  database: 'UP' | 'DOWN'
}
