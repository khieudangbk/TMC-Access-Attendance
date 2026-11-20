import { AlertTriangle, RefreshCw } from 'lucide-react'

interface ErrorPanelProps {
  message: string
  onRetry?: () => void
}

export function ErrorPanel({ message, onRetry }: ErrorPanelProps) {
  return (
    <div className="flex flex-col items-start gap-4 rounded-2xl border border-rose-200 bg-rose-50 p-5 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex items-start gap-3">
        <AlertTriangle className="mt-0.5 h-5 w-5 text-rose-600" aria-hidden="true" />
        <div>
          <p className="text-sm font-semibold text-rose-800">Không tải được dữ liệu</p>
          <p className="mt-1 text-sm text-rose-700">{message}</p>
        </div>
      </div>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="inline-flex items-center gap-2 rounded-lg bg-rose-600 px-3 py-2 text-sm font-medium text-white transition-colors hover:bg-rose-700"
        >
          <RefreshCw className="h-4 w-4" aria-hidden="true" />
          Thử lại
        </button>
      )}
    </div>
  )
}
