import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { createCollection, getCollections } from './savedApi'
import { CollectionChoiceCancelled, recommendedCollection, registerCollectionChooser, type CollectionChoiceRequest } from './collectionChoice'
import './collection-picker.css'

export function CollectionPicker({ category, collectionId, onConfirm, pending = false }: {
  category?: string | null; collectionId?: number; onConfirm: (id: number) => Promise<unknown>; pending?: boolean
}) {
  const cache = useQueryClient()
  const collections = useQuery({ queryKey: ['collections'], queryFn: getCollections })
  const [choice, setChoice] = useState<string | null>(null)
  const [newName, setNewName] = useState('')
  const [adding, setAdding] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const suggested = recommendedCollection(category)
  const defaultName = collections.data?.find((item) => item.id === collectionId)?.name ?? suggested
  const selected = choice ?? defaultName
  const names = [...new Set([suggested, ...(collections.data ?? []).map((item) => item.name), ...(choice ? [choice] : [])])]
  const confirm = async () => {
    setBusy(true); setError('')
    try {
      const item = collections.data?.find((item) => item.name === selected) ?? await createCollection(selected)
      await cache.invalidateQueries({ queryKey: ['collections'] })
      await onConfirm(item.id)
      await cache.invalidateQueries({ queryKey: ['saved-places'] })
    } catch { setError('저장하지 못했습니다. 다시 시도해 주세요.') }
    finally { setBusy(false) }
  }
  return <div className="collection-picker">
    <p>추천 컬렉션 <b>{suggested}</b> · 원하는 컬렉션으로 바꿀 수 있어요.</p>
    {collections.isLoading && <p role="status">컬렉션을 불러오는 중…</p>}
    {collections.isError && <p role="alert">컬렉션을 불러오지 못했습니다. <button onClick={() => collections.refetch()}>다시 시도</button></p>}
    <div className="collection-picker-options" role="group" aria-label="저장할 컬렉션">
      {names.map((name) => <button key={name} type="button" disabled={busy || pending} aria-pressed={selected === name} onClick={() => setChoice(name)}>
        <span aria-hidden="true">{selected === name ? '✓' : '☆'}</span>{name}
      </button>)}
      <button type="button" disabled={busy || pending} onClick={() => setAdding(!adding)}>＋ 새 컬렉션</button>
    </div>
    {adding && <form className="collection-picker-new" onSubmit={(event) => {
      event.preventDefault(); if (newName.trim()) { setChoice(newName.trim()); setAdding(false); setNewName('') }
    }}><input autoFocus aria-label="새 컬렉션 이름" maxLength={100} value={newName} onChange={(event) => setNewName(event.target.value)} placeholder="예: 주말 데이트" />
      <button disabled={!newName.trim() || busy || pending}>선택</button></form>}
    {error && <p role="alert">{error}</p>}
    <button className="collection-picker-confirm" type="button" disabled={busy || pending || !collections.isSuccess} onClick={() => void confirm()}>
      {busy || pending ? '저장 중…' : `‘${selected}’에 저장`}
    </button>
  </div>
}

export function CollectionSaveDialog() {
  const [request, setRequest] = useState<CollectionChoiceRequest | null>(null)
  const pending = useRef<{ resolve: (id: number) => void; reject: (error: Error) => void } | null>(null)
  const dialogRef = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    const unregister = registerCollectionChooser((value) => new Promise<number>((resolve, reject) => {
      if (pending.current) { reject(new CollectionChoiceCancelled()); return }
      pending.current = { resolve, reject }; setRequest(value)
    }))
    return () => { unregister(); pending.current?.reject(new CollectionChoiceCancelled()); pending.current = null }
  }, [])
  useEffect(() => {
    if (!request) return
    dialogRef.current?.showModal()
    const previous = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => { document.body.style.overflow = previous }
  }, [request])
  const cancel = () => { pending.current?.reject(new CollectionChoiceCancelled()); pending.current = null; setRequest(null) }
  if (!request) return null
  return createPortal(<dialog ref={dialogRef} className="collection-save-dialog" onCancel={(event) => { event.preventDefault(); cancel() }} aria-labelledby="collection-save-title">
    <header><h2 id="collection-save-title">어디에 담을까요?</h2><button type="button" aria-label="저장 취소" onClick={cancel}>×</button></header>
    <p className="collection-save-name">{request.name}</p>
    <CollectionPicker category={request.category} onConfirm={async (id) => {
      pending.current?.resolve(id); pending.current = null; setRequest(null)
    }} />
  </dialog>, document.body)
}
