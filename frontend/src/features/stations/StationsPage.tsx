import { useMemo, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { RefreshCw, Search, SlidersHorizontal } from 'lucide-react'
import clsx from 'clsx'

import { StationTable } from '@/features/stations/StationTable'
import { useStations } from '@/features/stations/useStations'
import { errorMessage } from '@/shared/api/client'
import type { StationStatus } from '@/shared/api/types'
import { ErrorPanel } from '@/shared/ui/ErrorPanel'

export function StationsPage() {
  const stationsQuery = useStations()
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [factory, setFactory] = useState('ALL')
  const [direction, setDirection] = useState('ALL')
  const [status, setStatus] = useState<StationStatus | 'ALL'>('ALL')

  const stations = useMemo(() => stationsQuery.data?.items ?? [], [stationsQuery.data])
  const factories = useMemo(
    () =>
      [...new Map(stations.map((station) => [station.factoryId, station.factoryName])).entries()].map(
        ([id, name]) => ({ id, name }),
      ),
    [stations],
  )

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase()
    return stations.filter((station) => {
      const matchesTerm =
        term.length === 0 ||
        [station.stationId, station.name, station.gateId, station.kioskId, station.factoryName]
          .join(' ')
          .toLowerCase()
          .includes(term)
      const matchesFactory = factory === 'ALL' || station.factoryId === factory
      const matchesDirection = direction === 'ALL' || station.direction === direction
      const matchesStatus = status === 'ALL' || station.status === status
      return matchesTerm && matchesFactory && matchesDirection && matchesStatus
    })
  }, [stations, search, factory, direction, status])

  const hasFilters = search !== '' || factory !== 'ALL' || direction !== 'ALL' || status !== 'ALL'

  function resetFilters() {
    setSearch('')
    setFactory('ALL')
    setDirection('ALL')
    setStatus('ALL')
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.22em] text-amber-600">
            Gate Station Service
          </p>
          <h2 className="mt-1 text-2xl font-semibold tracking-tight text-slate-950">
            Cấu hình trạm cổng
          </h2>
          <p className="mt-1.5 text-sm text-slate-500">
            Mỗi trạm có một hướng cố định và ánh xạ tới một cổng quay. Thay đổi cấu hình phải tăng
            revision và được ghi audit.
          </p>
        </div>
        <button
          type="button"
          onClick={() => void queryClient.invalidateQueries({ queryKey: ['stations'] })}
          disabled={stationsQuery.isFetching}
          className="inline-flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 shadow-sm transition-colors hover:border-slate-300 hover:bg-slate-50 disabled:opacity-60"
        >
          <RefreshCw
            className={clsx('h-4 w-4', stationsQuery.isFetching && 'animate-spin')}
            aria-hidden="true"
          />
          Làm mới
        </button>
      </div>

      {stationsQuery.isError && (
        <ErrorPanel
          message={errorMessage(stationsQuery.error)}
          onRetry={() => void stationsQuery.refetch()}
        />
      )}

      <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm shadow-slate-200/50">
        <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-slate-500">
          <SlidersHorizontal className="h-3.5 w-3.5" aria-hidden="true" />
          Bộ lọc
          {hasFilters && (
            <button
              type="button"
              onClick={resetFilters}
              className="ml-auto text-[11px] font-medium normal-case tracking-normal text-slate-500 underline-offset-4 hover:text-slate-900 hover:underline"
            >
              Xóa bộ lọc
            </button>
          )}
        </div>

        <div className="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-[minmax(0,2fr)_repeat(3,minmax(0,1fr))]">
          <label className="relative block">
            <span className="sr-only">Tìm kiếm trạm</span>
            <Search
              className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"
              aria-hidden="true"
            />
            <input
              type="search"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Tìm theo mã trạm, cổng, kiosk…"
              className="w-full rounded-xl border border-slate-200 bg-slate-50 py-2.5 pl-9 pr-3 text-sm text-slate-900 outline-none transition focus:border-slate-400 focus:bg-white focus:ring-2 focus:ring-slate-200"
            />
          </label>

          <Select
            label="Nhà máy"
            value={factory}
            onChange={setFactory}
            options={[
              { value: 'ALL', label: 'Tất cả nhà máy' },
              ...factories.map((item) => ({ value: item.id, label: item.name })),
            ]}
          />
          <Select
            label="Hướng"
            value={direction}
            onChange={setDirection}
            options={[
              { value: 'ALL', label: 'Tất cả hướng' },
              { value: 'IN', label: 'Vào' },
              { value: 'OUT', label: 'Ra' },
            ]}
          />
          <Select
            label="Trạng thái"
            value={status}
            onChange={(value) => setStatus(value as StationStatus | 'ALL')}
            options={[
              { value: 'ALL', label: 'Tất cả trạng thái' },
              { value: 'ACTIVE', label: 'Hoạt động' },
              { value: 'MAINTENANCE', label: 'Bảo trì' },
              { value: 'INACTIVE', label: 'Ngưng hoạt động' },
            ]}
          />
        </div>
      </div>

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm shadow-slate-200/50">
        <div className="flex items-center justify-between gap-3 border-b border-slate-100 px-5 py-4">
          <h3 className="text-sm font-semibold text-slate-900">Danh sách trạm</h3>
          <span className="rounded-md bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600">
            {filtered.length} / {stations.length} trạm
          </span>
        </div>
        {stationsQuery.isPending ? (
          <div className="space-y-3 p-5">
            {Array.from({ length: 6 }).map((_, index) => (
              <div key={index} className="h-12 animate-pulse rounded-lg bg-slate-100" />
            ))}
          </div>
        ) : (
          <StationTable
            stations={filtered}
            emptyMessage={
              hasFilters
                ? 'Không có trạm khớp bộ lọc hiện tại.'
                : 'Chưa có trạm nào được cấu hình.'
            }
          />
        )}
      </div>

      <p className="px-1 text-xs leading-5 text-slate-500">
        Trạm ở trạng thái <span className="font-medium text-slate-700">Bảo trì</span> không được xử
        lý như trạm đang hoạt động. Cấu hình trạm là dữ liệu có revision; sự kiện quét phải lưu
        revision đã dùng để truy vết.
      </p>
    </div>
  )
}

interface SelectProps {
  label: string
  value: string
  onChange: (value: string) => void
  options: Array<{ value: string; label: string }>
}

function Select({ label, value, onChange, options }: SelectProps) {
  return (
    <label className="block">
      <span className="sr-only">{label}</span>
      <select
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-700 outline-none transition focus:border-slate-400 focus:bg-white focus:ring-2 focus:ring-slate-200"
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  )
}
