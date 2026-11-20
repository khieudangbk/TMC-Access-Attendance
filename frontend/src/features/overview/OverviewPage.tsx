import { useMemo } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import {
  BadgeCheck,
  Boxes,
  Clock3,
  Database,
  Factory,
  RefreshCw,
  ServerCog,
  ShieldCheck,
  Wrench,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import clsx from 'clsx'

import { StationTable } from '@/features/stations/StationTable'
import { useStations } from '@/features/stations/useStations'
import { useSystemStatus } from '@/features/system/useSystemStatus'
import { errorMessage } from '@/shared/api/client'
import type { GateStation } from '@/shared/api/types'
import { formatDateTime } from '@/shared/format/date'
import { ErrorPanel } from '@/shared/ui/ErrorPanel'
import { KpiCard } from '@/shared/ui/KpiCard'

export function OverviewPage() {
  const stationsQuery = useStations()
  const systemQuery = useSystemStatus()
  const queryClient = useQueryClient()

  const stations = useMemo(() => stationsQuery.data?.items ?? [], [stationsQuery.data])
  const total = stationsQuery.data?.total ?? stations.length
  const active = stations.filter((station) => station.status === 'ACTIVE').length
  const maintenance = stations.filter((station) => station.status === 'MAINTENANCE').length
  const factories = useMemo(() => summarizeFactories(stations), [stations])
  const refreshing = stationsQuery.isFetching || systemQuery.isFetching

  async function refreshAll() {
    await queryClient.invalidateQueries()
  }

  return (
    <div className="space-y-6">
      <section className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm shadow-slate-200/60">
        <div className="relative px-6 py-7 sm:px-8">
          <div
            className="pointer-events-none absolute inset-y-0 right-0 w-1/2 bg-[radial-gradient(circle_at_top_right,rgba(245,158,11,0.16),transparent_62%)]"
            aria-hidden="true"
          />
          <div className="relative flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
            <div className="max-w-2xl">
              <p className="text-[11px] font-semibold uppercase tracking-[0.22em] text-amber-600">
                Trung tâm điều hành
              </p>
              <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950 sm:text-3xl">
                Một lượt quét, một chứng cứ.
              </h2>
              <p className="mt-3 text-sm leading-6 text-slate-600">
                Theo dõi trạng thái trạm cổng tại Bình Dương và Long An. Đường quét chạy tại nhà
                máy; báo cáo và đồng bộ HR xử lý nền, không nằm trên đường găng.
              </p>
            </div>
            <button
              type="button"
              onClick={refreshAll}
              disabled={refreshing}
              className="inline-flex shrink-0 items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 shadow-sm transition-colors hover:border-slate-300 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
            >
              <RefreshCw className={clsx('h-4 w-4', refreshing && 'animate-spin')} aria-hidden="true" />
              Làm mới dữ liệu
            </button>
          </div>
        </div>
      </section>

      {stationsQuery.isError && (
        <ErrorPanel
          message={errorMessage(stationsQuery.error)}
          onRetry={() => void stationsQuery.refetch()}
        />
      )}

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {stationsQuery.isPending ? (
          Array.from({ length: 4 }).map((_, index) => (
            <div
              key={index}
              className="h-[148px] animate-pulse rounded-2xl border border-slate-200 bg-white"
            />
          ))
        ) : (
          <>
            <KpiCard
              label="Tổng số trạm"
              value={total}
              hint="Bình Dương 6 · Long An 4"
              icon={ServerCog}
              tone="slate"
            />
            <KpiCard
              label="Đang hoạt động"
              value={active}
              hint={total > 0 ? `${Math.round((active / total) * 100)}% tổng cấu hình trạm` : 'Chưa có dữ liệu'}
              icon={BadgeCheck}
              tone="emerald"
            />
            <KpiCard
              label="Cần bảo trì"
              value={maintenance}
              hint={maintenance > 0 ? 'Kiểm tra kiosk và cổng quay' : 'Không có trạm bảo trì'}
              icon={Wrench}
              tone={maintenance > 0 ? 'amber' : 'slate'}
            />
            <KpiCard
              label="Nhà máy"
              value={factories.length}
              hint="Central tại Bình Dương · DR tại Long An"
              icon={Factory}
              tone="sky"
            />
          </>
        )}
      </section>

      <section className="grid gap-4 xl:grid-cols-[minmax(0,1.55fr)_minmax(320px,1fr)]">
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm shadow-slate-200/50 sm:p-6">
          <div className="flex items-center justify-between gap-3">
            <div>
              <h3 className="text-base font-semibold text-slate-950">Tình hình theo nhà máy</h3>
              <p className="mt-1 text-xs text-slate-500">
                Tỷ lệ trạm đang hoạt động trên tổng cấu hình
              </p>
            </div>
            <ShieldCheck className="h-5 w-5 text-slate-300" aria-hidden="true" />
          </div>

          <div className="mt-5 grid gap-3 md:grid-cols-2">
            {factories.map((factory) => {
              const ratio = factory.total === 0 ? 0 : (factory.active / factory.total) * 100
              return (
                <article
                  key={factory.id}
                  className="rounded-xl border border-slate-200 bg-slate-50/60 p-4"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <p className="font-medium text-slate-900">{factory.name}</p>
                      <p className="mt-0.5 font-mono text-[11px] text-slate-500">{factory.id}</p>
                    </div>
                    <span className="rounded-md bg-white px-2 py-1 text-xs font-semibold text-slate-700 ring-1 ring-inset ring-slate-200">
                      {factory.active}/{factory.total}
                    </span>
                  </div>

                  <div className="mt-4 h-2 overflow-hidden rounded-full bg-slate-200">
                    <div
                      className={clsx(
                        'h-full rounded-full',
                        ratio === 100 ? 'bg-emerald-500' : ratio >= 50 ? 'bg-amber-500' : 'bg-rose-500',
                      )}
                      style={{ width: `${ratio}%` }}
                    />
                  </div>

                  <div className="mt-3 flex flex-wrap gap-3 text-[11px] font-medium text-slate-500">
                    <span className="text-sky-700">Vào: {factory.inCount}</span>
                    <span className="text-violet-700">Ra: {factory.outCount}</span>
                    {factory.maintenance > 0 && (
                      <span className="text-amber-700">Bảo trì: {factory.maintenance}</span>
                    )}
                  </div>
                </article>
              )
            })}
            {factories.length === 0 && !stationsQuery.isPending && (
              <p className="rounded-xl border border-dashed border-slate-300 p-6 text-sm text-slate-500">
                Chưa có nhà máy hoặc trạm nào trong cơ sở dữ liệu.
              </p>
            )}
          </div>
        </div>

        <div className="rounded-2xl bg-slate-950 p-5 text-slate-200 shadow-lg shadow-slate-900/10 sm:p-6">
          <div className="flex items-center justify-between gap-3">
            <div>
              <h3 className="text-base font-semibold text-white">Sức khỏe hệ thống</h3>
              <p className="mt-1 text-xs text-slate-400">Từ API trung tâm · làm mới mỗi 30 giây</p>
            </div>
            <span
              className={clsx(
                'inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] font-medium',
                systemQuery.data?.database === 'UP'
                  ? 'border-emerald-400/30 bg-emerald-400/10 text-emerald-300'
                  : 'border-rose-400/30 bg-rose-400/10 text-rose-300',
              )}
            >
              <span
                className={clsx(
                  'h-1.5 w-1.5 rounded-full',
                  systemQuery.data?.database === 'UP' ? 'bg-emerald-400' : 'bg-rose-400',
                )}
              />
              {systemQuery.isPending ? 'Đang kiểm tra' : systemQuery.data?.database === 'UP' ? 'Ổn định' : 'Sự cố'}
            </span>
          </div>

          <dl className="mt-6 space-y-4 text-sm">
            <StatusRow icon={Database} label="PostgreSQL" value={systemQuery.data?.database ?? '—'} />
            <StatusRow icon={Boxes} label="Dịch vụ" value={systemQuery.data?.application ?? '—'} mono />
            <StatusRow icon={Clock3} label="Giờ máy chủ" value={formatDateTime(systemQuery.data?.serverTime)} />
            <StatusRow icon={BadgeCheck} label="Phiên bản" value={systemQuery.data?.version ?? '—'} mono />
          </dl>

          <p className="mt-6 border-t border-slate-800 pt-4 text-[11px] leading-5 text-slate-500">
            Trạng thái này chỉ phản ánh API trung tâm. Đường quét tại nhà máy được giám sát riêng
            qua heartbeat trạm và backlog outbox.
          </p>
        </div>
      </section>

      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm shadow-slate-200/50">
        <div className="flex items-center justify-between gap-3 border-b border-slate-100 px-5 py-4 sm:px-6">
          <div>
            <h3 className="text-base font-semibold text-slate-950">Danh mục trạm</h3>
            <p className="mt-1 text-xs text-slate-500">Cấu hình hiện hành, không bao gồm lịch sử</p>
          </div>
          <span className="rounded-md bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600">
            {stations.length} trạm
          </span>
        </div>
        <StationTable
          stations={stations.slice(0, 6)}
          showFooterLink
          emptyMessage="Chưa có trạm nào được cấu hình."
        />
      </section>
    </div>
  )
}

interface StatusRowProps {
  icon: LucideIcon
  label: string
  value: string
  mono?: boolean
}

function StatusRow({ icon: Icon, label, value, mono = false }: StatusRowProps) {
  return (
    <div className="flex items-center justify-between gap-3">
      <dt className="flex items-center gap-2 text-slate-400">
        <Icon className="h-4 w-4" aria-hidden="true" />
        {label}
      </dt>
      <dd className={clsx('text-right text-slate-100', mono && 'font-mono text-xs')}>{value}</dd>
    </div>
  )
}

interface FactorySummary {
  id: string
  name: string
  total: number
  active: number
  maintenance: number
  inCount: number
  outCount: number
}

function summarizeFactories(stations: GateStation[]): FactorySummary[] {
  const byFactory = new Map<string, FactorySummary>()
  for (const station of stations) {
    const summary = byFactory.get(station.factoryId) ?? {
      id: station.factoryId,
      name: station.factoryName,
      total: 0,
      active: 0,
      maintenance: 0,
      inCount: 0,
      outCount: 0,
    }
    summary.total += 1
    if (station.status === 'ACTIVE') summary.active += 1
    if (station.status === 'MAINTENANCE') summary.maintenance += 1
    if (station.direction === 'IN') summary.inCount += 1
    if (station.direction === 'OUT') summary.outCount += 1
    byFactory.set(station.factoryId, summary)
  }
  return [...byFactory.values()]
}
