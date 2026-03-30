import React from 'react';

interface IconProps {
  size?: number;
  className?: string;
}

/**
 * Filled 아이콘 팩토리 — fill 기반 (Material Design 등)
 */
export function createFilledIcon(displayName: string, pathD: string, viewBox = '0 0 24 24') {
  const Icon = ({ size = 24, className }: IconProps) => (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={size}
      height={size}
      viewBox={viewBox}
      fill="none"
      role="presentation"
      className={className}
    >
      <path d={pathD} fill="currentColor" fillRule="evenodd" clipRule="evenodd" />
    </svg>
  );
  Icon.displayName = displayName;
  return Icon;
}

/**
 * Outline 아이콘 팩토리 — stroke 기반 (Heroicons 등)
 * paths: path d 값 배열 (여러 path 지원)
 */
export function createOutlineIcon(displayName: string, paths: string[], strokeWidth = 1.5, viewBox = '0 0 24 24') {
  const Icon = ({ size = 24, className }: IconProps) => (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={size}
      height={size}
      viewBox={viewBox}
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      role="presentation"
      className={className}
    >
      {paths.map((d, i) => <path key={i} d={d} />)}
    </svg>
  );
  Icon.displayName = displayName;
  return Icon;
}

// ─── 아이콘 등록 ───────────────────────────────────────────

/** 지도 아이콘 — 비활성 Outline (Heroicons) */
export const MapNavOutlineIcon = createOutlineIcon(
  'MapNavOutlineIcon',
  [
    'M15 10.5a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z',
    'M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1 1 15 0Z',
  ]
);

/** 지도 아이콘 — 활성 Filled (Heroicons) */
export const MapNavFilledIcon = createFilledIcon(
  'MapNavFilledIcon',
  'm11.54 22.351.07.04.028.016a.76.76 0 0 0 .723 0l.028-.015.071-.041a16.975 16.975 0 0 0 1.144-.742 19.58 19.58 0 0 0 2.683-2.282c1.944-1.99 3.963-4.98 3.963-8.827a8.25 8.25 0 0 0-16.5 0c0 3.846 2.02 6.837 3.963 8.827a19.58 19.58 0 0 0 2.682 2.282 16.975 16.975 0 0 0 1.145.742ZM12 13.5a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z'
);
