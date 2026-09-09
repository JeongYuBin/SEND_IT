import { useEffect } from 'react'

type Props = {
  open: boolean
  title: string
  description: string
  confirmLabel?: string
  cancelLabel?: string
  pending?: boolean
  onConfirm: () => void
  onCancel: () => void
}

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = '삭제',
  cancelLabel = '취소',
  pending = false,
  onConfirm,
  onCancel,
}: Props) {
  useEffect(() => {
    if (!open) return

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !pending) onCancel()
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [open, pending, onCancel])

  if (!open) return null

  return (
    <div className="confirm-dialog-backdrop" role="presentation" onMouseDown={() => !pending && onCancel()}>
      <section
        className="confirm-dialog"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-dialog-title"
        aria-describedby="confirm-dialog-description"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="confirm-dialog-icon" aria-hidden="true">×</div>
        <h2 id="confirm-dialog-title">{title}</h2>
        <p id="confirm-dialog-description">{description}</p>
        <div className="confirm-dialog-actions">
          <button type="button" disabled={pending} onClick={onCancel}>{cancelLabel}</button>
          <button className="danger" type="button" disabled={pending} autoFocus onClick={onConfirm}>
            {pending ? '삭제 중…' : confirmLabel}
          </button>
        </div>
      </section>
    </div>
  )
}
