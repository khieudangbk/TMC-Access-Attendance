import { Route, Routes } from 'react-router-dom'

import { AppShell } from '@/app/AppShell'
import { NotFoundPage } from '@/app/NotFoundPage'
import { OverviewPage } from '@/features/overview/OverviewPage'
import { StationsPage } from '@/features/stations/StationsPage'

export default function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<OverviewPage />} />
        <Route path="stations" element={<StationsPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
