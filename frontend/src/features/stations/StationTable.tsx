import { Link } from 'react-router-dom'

import type { GateStation } from '@/shared/api/types'
import { DirectionBadge, StatusBadge } from '@/shared/ui/StatusBadge'

interface StationTableProps {
  stations: GateStation[]
  emptyMessage?: string
  showFooterLink?: boolean
}

export function StationTable({
  stations,
  emptyMessage = 'Không có trạm phù hợp.',
  showFooterLink = false,
}: StationTableProps) {
  return (
    <div>
      <div className="scrollbar-thin overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200 text-sm">
          <thead>
            <tr className="text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
              <th scope="col" className="px-4 py-3">Trạm</th>
              <th scope="col" className="px-4 py-3">Nhà máy</th>
              <th scope="col" className="px-4 py-3">Hướng</th>
              <th scope="col" className="px-4 py-3">Cổng quay</th>
              <th scope="col" className="px-4 py-3">Kiosk</th>
              <th scope="col" className="px-4 py-3">Trạng thái</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {stations.map((station) => (
              <tr key={station.stationId} className="transition-colors hover:bg-slate-50/80">
                <td className="whitespace-nowrap px-4 py-3">
                  <div className="font-medium text-slate-900">{station.name}</div>
                  <div className="font-mono text-xs text-slate-500">{station.stationId}</div>
                </td>
                <td className="whitespace-nowrap px-4 py-3 text-slate-600">{station.factoryName}</td>
                <td className="whitespace-nowrap px-4 py-3">
                  <DirectionBadge direction={station.direction} />
                </td>
                <td className="whitespace-nowrap px-4 py-3 font-mono text-xs text-slate-600">
                  {station.gateId}
                </td>
                <td className="whitespace-nowrap px-4 py-3 font-mono text-xs text-slate-600">
                  {station.kioskId}
                </td>
                <td className="whitespace-nowrap px-4 py-3">
                  <StatusBadge status={station.status} />
                </td>
              </tr>
            ))}
            {stations.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-12 text-center text-sm text-slate-500">
                  {emptyMessage}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      {showFooterLink && (
        <div className="border-t border-slate-100 px-4 py-3 text-right">
          <Link
            to="/stations"
            className="text-sm font-medium text-slate-700 underline-offset-4 hover:text-slate-950 hover:underline"
          >
            Xem tất cả trạm →
          </Link>
        </div>
      )}
    </div>
  )
}
