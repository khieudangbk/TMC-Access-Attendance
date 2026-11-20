import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { DirectionBadge, StatusBadge } from '@/shared/ui/StatusBadge'

describe('StatusBadge', () => {
  it('renders the Vietnamese label for an active station', () => {
    render(<StatusBadge status="ACTIVE" />)
    expect(screen.getByText('Hoạt động')).toBeInTheDocument()
  })

  it('renders the maintenance state distinctly', () => {
    render(<StatusBadge status="MAINTENANCE" />)
    expect(screen.getByText('Bảo trì')).toBeInTheDocument()
  })
})

describe('DirectionBadge', () => {
  it('renders IN and OUT labels', () => {
    render(
      <>
        <DirectionBadge direction="IN" />
        <DirectionBadge direction="OUT" />
      </>,
    )
    expect(screen.getByText('Vào')).toBeInTheDocument()
    expect(screen.getByText('Ra')).toBeInTheDocument()
  })
})
