import type { LucideIcon } from 'lucide-react'
import clsx from 'clsx'

const tones = {
  slate: 'bg-slate-100 text-slate-700',
  emerald: 'bg-emerald-50 text-emerald-700',
  amber: 'bg-amber-50 text-amber-700',
  sky: 'bg-sky-50 text-sky-700',
} as const

interface KpiCardProps {
  label: string
  value: string | number
  hint: string
  icon: LucideIcon
  tone?: keyof typeof tones
}

export function KpiCard({ label, value, hint, icon: Icon, tone = 'slate' }: KpiCardProps) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm shadow-slate-200/50">
      <div className="flex items-start justify-between gap-3">
        <p className="text-sm font-medium text-slate-500">{label}</p>
        <span className={clsx('rounded-xl p-2', tones[tone])}>
          <Icon className="h-4 w-4" aria-hidden="true" />
        </span>
      </div>
      <p className="mt-4 text-3xl font-semibold tracking-tight text-slate-950">{value}</p>
      <p className="mt-1 text-xs text-slate-500">{hint}</p>
    </div>
  )
}
