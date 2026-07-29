import type { TransitRoute } from './types'

const stepLabels: Record<string, string> = {
  WALKING: '도보',
  BUS: '버스',
  SUBWAY: '지하철',
  TRAIN: '기차',
}

function distance(meters: number) {
  return meters >= 1000 ? `${(meters / 1000).toFixed(1)}km` : `${meters}m`
}

export function TransitRouteGuide({ route }: { route: TransitRoute }) {
  return (
    <details className="transit-guide">
      <summary>
        <strong>대중교통 약 {route.totalMinutes}분</strong>
        <span>환승 {route.transfers}회</span>
        {route.fare > 0 && <span>예상 {route.fare.toLocaleString()}원</span>}
        <span>{distance(route.totalDistanceMeters)}</span>
      </summary>
      <ol>
        {route.steps.map((step, index) => (
          <li key={`${step.type}-${index}`}>
            <span className={`transit-step-type transit-step-${step.type.toLowerCase()}`}>
              {stepLabels[step.type] ?? step.type}
            </span>
            <div>
              <strong>{step.guidance || `${stepLabels[step.type] ?? step.type} 이동`}</strong>
              {step.vehicles.length > 0 && <span>{step.vehicles.join(', ')}</span>}
              {step.startStop && (
                <span>
                  {step.type === 'BUS' ? '승차 정류장: ' : '출발: '}
                  {step.startStop}
                  {step.endStop && step.endStop !== step.startStop
                    ? ` · 하차: ${step.endStop}`
                    : ''}
                </span>
              )}
            </div>
            <small>{step.minutes}분 · {distance(step.distanceMeters)}</small>
          </li>
        ))}
      </ol>
      {route.landingUrl && (
        <a href={route.landingUrl} target="_blank" rel="noreferrer">
          카카오맵에서 전체 경로 확인
        </a>
      )}
      <p>
        카카오 API는 지정한 미래 출발 시각별 도착 예정 버스 정보는 제공하지 않습니다.
        노선 번호와 승하차 정류장은 위 안내를 참고하고, 실제 도착 정보는 출발 전에 카카오맵에서 확인해 주세요.
      </p>
    </details>
  )
}
