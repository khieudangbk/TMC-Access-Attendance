import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="flex min-h-[55vh] flex-col items-center justify-center rounded-3xl border border-dashed border-slate-300 bg-white/60 px-6 text-center">
      <p className="font-mono text-xs uppercase tracking-[0.24em] text-slate-400">404</p>
      <h2 className="mt-3 text-2xl font-semibold text-slate-950">Không tìm thấy trang</h2>
      <p className="mt-2 max-w-md text-sm text-slate-500">
        Đường dẫn không tồn tại hoặc màn hình chưa được triển khai trong giai đoạn hiện tại.
      </p>
      <Link
        to="/"
        className="mt-6 rounded-xl bg-slate-950 px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-slate-800"
      >
        Về tổng quan
      </Link>
    </div>
  )
}
