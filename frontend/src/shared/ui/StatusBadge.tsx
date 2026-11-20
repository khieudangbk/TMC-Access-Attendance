import clsx from 'clsx'

import type { Direction, StationStatus } from '@/shared/api/types'

const statusStyles: Record<StationStatus, string> = {
  ACTIVE: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  INACTIVE: 'bg-slate-100 text-slate-600 ring-slate-500/20',
  MAINTENANCE: 'bg-amber-50 text-amber-700 ring-amber-600/20',
}

const statusLabels: Record<StationStatus, string> = {
  ACTIVE: 'Hoạt động',
  INACTIVE: 'Ngưng hoạt động',
  MAINTENANCE: 'Bảo trì',
}

export function StatusBadge({ status }: { status: StationStatus }) {
  return (
    <span
      className={clsx(
        'inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ring-1 ring-inset',
        statusStyles[status],
      )}
    >
      <span
        className={clsx(
          'h-1.5 w-1.5 rounded-full',
          status === 'ACTIVE' && 'bg-emerald-500',
          status === 'INACTIVE' && 'bg-slate-400',
          status === 'MAINTENANCE' && 'bg-amber-500',
        )}
      />
      {statusLabels[status]}
    </span>
  )
}

const directionStyles: Record<Direction, string> = {
  IN: 'bg-sky-50 text-sky-700 ring-sky-600/20',
  OUT: 'bg-violet-50 text-violet-700 ring-violet-600/20',
}

const directionLabels: Record<Direction, string> = {
  IN: 'Vào',
  OUT: 'Ra',
}

export function DirectionBadge({ direction }: { direction: Direction }) {
  return (
    <span
      className={clsx(
        'inline-flex items-center rounded-md px-2 py-1 text-xs font-semibold ring-1 ring-inset',
        directionStyles[direction],
      )}
    >
      {directionLabels[direction]}
    </span>
  )
}
