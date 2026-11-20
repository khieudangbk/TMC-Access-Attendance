import { useState } from 'react'
import {
  Activity,
  CalendarDays,
  ChevronRight,
  ClipboardList,
  LayoutDashboard,
  LockKeyhole,
  ServerCog,
  Settings2,
  ShieldCheck,
  Users,
} from 'lucide-react'
import clsx from 'clsx'
import { NavLink, Outlet, useLocation } from 'react-router-dom'

import { useSystemStatus } from '@/features/system/useSystemStatus'
import { formatDateLong } from '@/shared/format/date'

const primaryNav = [
  { to: '/', label: 'Tổng quan', icon: LayoutDashboard, end: true },
  { to: '/stations', label: 'Trạm cổng', icon: ServerCog, end: false },
]

const plannedNav = [
  { label: 'Nhân viên & QR', icon: Users },
  { label: 'Chấm công', icon: ClipboardList },
  { label: 'Báo cáo', icon: Activity },
  { label: 'Kiểm toán', icon: ShieldCheck },
  { label: 'Cấu hình', icon: Settings2 },
]

const pageMeta: Record<string, { title: string; subtitle: string }> = {
  '/': {
    title: 'Tổng quan vận hành',
    subtitle: 'Sức khỏe hệ thống và cấu hình trạm cổng tại hai nhà máy',
  },
  '/stations': {
    title: 'Trạm cổng',
    subtitle: 'Danh mục trạm, kiosk và ánh xạ cổng quay',
  },
}

export function AppShell() {
  const [today] = useState(() => new Date())
  const location = useLocation()
  const meta = pageMeta[location.pathname] ?? {
    title: 'Không tìm thấy trang',
    subtitle: 'Đường dẫn không hợp lệ',
  }
  const systemQuery = useSystemStatus()
  const databaseUp = systemQuery.data?.database === 'UP'
  const connectionLabel = systemQuery.isPending
    ? 'Đang kết nối'
    : databaseUp
      ? 'Hệ thống ổn định'
      : 'Mất kết nối'

  return (
    <div className="min-h-screen bg-slate-100 lg:grid lg:grid-cols-[272px_minmax(0,1fr)]">
      <aside className="sticky top-0 hidden h-screen flex-col border-r border-slate-800/80 bg-slate-950 text-slate-300 lg:flex">
        <div className="flex items-center gap-3 px-6 py-6">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-500 font-bold text-slate-950">
            T
          </div>
          <div>
            <p className="text-sm font-semibold tracking-wide text-white">TMC-AACS</p>
            <p className="text-[11px] uppercase tracking-[0.18em] text-slate-500">
              Access &amp; Attendance
            </p>
          </div>
        </div>

        <nav className="flex-1 space-y-6 overflow-y-auto px-3 py-2">
          <div>
            <p className="px-3 pb-2 text-[10px] font-semibold uppercase tracking-[0.2em] text-slate-600">
              Vận hành
            </p>
            <div className="space-y-1">
              {primaryNav.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) =>
                    clsx(
                      'group flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                      isActive
                        ? 'bg-slate-800 text-white'
                        : 'text-slate-400 hover:bg-slate-900 hover:text-slate-200',
                    )
                  }
                >
                  <item.icon className="h-4 w-4" aria-hidden="true" />
                  <span className="flex-1">{item.label}</span>
                  <ChevronRight className="h-3.5 w-3.5 opacity-0 transition-opacity group-hover:opacity-60" />
                </NavLink>
              ))}
            </div>
          </div>

          <div>
            <p className="px-3 pb-2 text-[10px] font-semibold uppercase tracking-[0.2em] text-slate-600">
              Sắp triển khai
            </p>
            <div className="space-y-1">
              {plannedNav.map((item) => (
                <div
                  key={item.label}
                  className="flex cursor-not-allowed items-center gap-3 rounded-lg px-3 py-2.5 text-sm text-slate-600"
                  aria-disabled="true"
                >
                  <item.icon className="h-4 w-4" aria-hidden="true" />
                  {item.label}
                </div>
              ))}
            </div>
          </div>
        </nav>

        <div className="border-t border-slate-800/80 p-4">
          <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-3">
            <div className="flex items-center gap-2 text-xs font-medium text-slate-300">
              <span
                className={clsx(
                  'h-2 w-2 rounded-full',
                  systemQuery.isPending
                    ? 'bg-slate-500'
                    : databaseUp
                      ? 'bg-emerald-400'
                      : 'bg-rose-400',
                )}
              />
              Máy chủ trung tâm
            </div>
            <p className="mt-1 text-[11px] text-slate-500">
              {systemQuery.data?.environment ?? 'local'} · PostgreSQL{' '}
              {systemQuery.data?.database ?? '—'}
            </p>
          </div>
          <p className="mt-3 px-1 text-[10px] text-slate-600">
            TMC · Bình Dương &amp; Long An
          </p>
        </div>
      </aside>

      <div className="flex min-h-screen flex-col">
        <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/85 backdrop-blur">
          <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-4 sm:px-6 lg:px-10">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-950 text-sm font-bold text-amber-400 lg:hidden">
                T
              </div>
              <div>
                <h1 className="text-base font-semibold tracking-tight text-slate-950 sm:text-lg">
                  {meta.title}
                </h1>
                <p className="text-xs text-slate-500">{meta.subtitle}</p>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <span
                className={clsx(
                  'inline-flex items-center gap-2 rounded-full border px-3 py-1.5 text-xs font-medium',
                  systemQuery.isPending
                    ? 'border-slate-200 bg-slate-50 text-slate-500'
                    : databaseUp
                      ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
                      : 'border-rose-200 bg-rose-50 text-rose-700',
                )}
              >
                <span
                  className={clsx(
                    'h-1.5 w-1.5 rounded-full',
                    systemQuery.isPending
                      ? 'bg-slate-400'
                      : databaseUp
                        ? 'animate-pulse bg-emerald-500'
                        : 'bg-rose-500',
                  )}
                />
                {connectionLabel}
              </span>
              <span className="hidden items-center gap-2 text-xs text-slate-500 md:inline-flex">
                <CalendarDays className="h-3.5 w-3.5" aria-hidden="true" />
                {formatDateLong(today)}
              </span>
            </div>
          </div>

          <nav className="flex gap-1 overflow-x-auto px-4 pb-3 sm:px-6 lg:hidden">
            {primaryNav.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  clsx(
                    'inline-flex items-center gap-2 rounded-lg px-3 py-1.5 text-xs font-medium',
                    isActive
                      ? 'bg-slate-950 text-white'
                      : 'bg-slate-100 text-slate-600',
                  )
                }
              >
                <item.icon className="h-3.5 w-3.5" aria-hidden="true" />
                {item.label}
              </NavLink>
            ))}
          </nav>
        </header>

        <main className="flex-1 px-4 py-6 sm:px-6 lg:px-10 lg:py-8">
          <div className="mx-auto w-full max-w-[1400px]">
            <Outlet />
          </div>
        </main>

        <footer className="border-t border-slate-200 px-4 py-4 text-xs text-slate-400 sm:px-6 lg:px-10">
          <div className="mx-auto flex max-w-[1400px] flex-wrap items-center justify-between gap-2">
            <span>TMC Attendance &amp; Access Control System</span>
            <span className="inline-flex items-center gap-1.5">
              <LockKeyhole className="h-3 w-3" aria-hidden="true" />
              Dữ liệu chấm công là chứng cứ chỉ thêm
            </span>
          </div>
        </footer>
      </div>
    </div>
  )
}
