import type { PetInfo } from '../store/useAppStore';

// 체크리스트 아이템 타입
export interface ChecklistItem {
  id: number;
  task: string;
  time: string;
  completed: boolean;
  type: 'medicine' | 'care' | 'health';
}

// 건강 지표 타입
export interface HealthMetric {
  label: string;
  value: string;
  change: string;
  trend: 'up' | 'down' | 'stable';
  status: 'good' | 'warning';
}

// 복약 알림 타입
export interface MedicationItem {
  name: string;
  schedule: string;
  dose: string;
}

// 케어 팁 타입
export interface CareTip {
  text: string;
}

/** 나이 구간 분류 */
function getAgeGroup(age: number): 'puppy' | 'young' | 'adult' | 'senior' {
  if (age <= 1) return 'puppy';
  if (age <= 3) return 'young';
  if (age <= 7) return 'adult';
  return 'senior';
}

/** pet.age / pet.size 기반 동적 체크리스트 생성 */
export function generateChecklist(pet: PetInfo): ChecklistItem[] {
  const ageGroup = getAgeGroup(pet.age);
  const items: ChecklistItem[] = [];
  let id = 1;

  // 공통 항목: 수분 섭취
  const waterInterval = pet.size === 'LARGE' ? '매 1시간' : pet.size === 'MEDIUM' ? '매 1.5시간' : '매 2시간';
  items.push({ id: id++, task: '수분 섭취 체크', time: waterInterval, completed: false, type: 'health' });

  // 나이별 분기
  if (ageGroup === 'puppy') {
    items.push({ id: id++, task: '영양제 급여', time: '09:00', completed: false, type: 'medicine' });
    items.push({ id: id++, task: '사회화 훈련', time: '11:00', completed: false, type: 'care' });
    items.push({ id: id++, task: '체중 기록', time: '저녁', completed: false, type: 'health' });
  } else if (ageGroup === 'young') {
    items.push({ id: id++, task: '종합 영양제', time: '09:00', completed: false, type: 'medicine' });
    items.push({ id: id++, task: '양치질', time: '21:00', completed: false, type: 'care' });
    if (pet.activity === 'HIGH') {
      items.push({ id: id++, task: '활동 후 발바닥 체크', time: '산책 후', completed: false, type: 'health' });
    }
  } else if (ageGroup === 'adult') {
    items.push({ id: id++, task: '종합 영양제', time: '09:00', completed: false, type: 'medicine' });
    items.push({ id: id++, task: '양치질', time: '21:00', completed: false, type: 'care' });
    items.push({ id: id++, task: '피부·모질 체크', time: '그루밍 시', completed: false, type: 'health' });
    if (pet.size === 'LARGE') {
      items.push({ id: id++, task: '관절 영양제', time: '아침 식후', completed: false, type: 'medicine' });
    }
  } else {
    // 시니어
    items.push({ id: id++, task: '아침 약 복용', time: '09:00', completed: false, type: 'medicine' });
    items.push({ id: id++, task: '저녁 약 복용', time: '19:00', completed: false, type: 'medicine' });
    items.push({ id: id++, task: '관절 마사지', time: '14:00', completed: false, type: 'care' });
    items.push({ id: id++, task: '배변 상태 체크', time: '매 외출 후', completed: false, type: 'health' });
    if (pet.size !== 'SMALL') {
      items.push({ id: id++, task: '관절 영양제', time: '아침 식후', completed: false, type: 'medicine' });
    }
  }

  return items;
}

/** pet 기반 동적 건강 지표 생성 */
export function generateHealthMetrics(pet: PetInfo): HealthMetric[] {
  // 체중 기준값 (크기별)
  const weightRanges: Record<string, { ideal: number; unit: string }> = {
    SMALL: { ideal: 5, unit: 'kg' },
    MEDIUM: { ideal: 15, unit: 'kg' },
    LARGE: { ideal: 30, unit: 'kg' },
  };
  const weightInfo = weightRanges[pet.size];
  const displayWeight = pet.weight ? `${pet.weight}kg` : `${weightInfo.ideal}kg`;
  const weightDiff = pet.weight
    ? (pet.weight - weightInfo.ideal > 2 ? { change: '과체중 주의', status: 'warning' as const }
      : pet.weight - weightInfo.ideal < -2 ? { change: '저체중 주의', status: 'warning' as const }
      : { change: '정상 범위', status: 'good' as const })
    : { change: '측정 필요', status: 'good' as const };

  // 활동량 표시
  const activityMap = { LOW: '적음', NORMAL: '보통', HIGH: '활발' };
  const activityStatus = pet.activity === 'LOW' && getAgeGroup(pet.age) !== 'senior'
    ? 'warning' as const : 'good' as const;

  // 나이 기반 수면 시간
  const ageGroup = getAgeGroup(pet.age);
  const sleepHours = ageGroup === 'puppy' ? '18~20시간'
    : ageGroup === 'young' ? '12~14시간'
    : ageGroup === 'adult' ? '10~12시간'
    : '14~16시간';

  return [
    { label: '체중', value: displayWeight, change: weightDiff.change, trend: 'stable', status: weightDiff.status },
    { label: '활동량', value: activityMap[pet.activity], change: ageGroup === 'senior' ? '조절 필요' : '양호', trend: 'stable', status: activityStatus },
    { label: '식욕', value: '양호', change: '유지', trend: 'stable', status: 'good' },
    { label: '적정 수면', value: sleepHours, change: ageGroup === 'senior' ? '충분히 휴식' : '정상', trend: 'stable', status: 'good' },
  ];
}

/** pet 기반 동적 복약 알림 생성 */
export function generateMedications(pet: PetInfo): MedicationItem[] {
  const ageGroup = getAgeGroup(pet.age);
  const meds: MedicationItem[] = [];

  // 공통: 종합 영양제
  meds.push({ name: '종합 영양제', schedule: '매일 아침', dose: '1정' });

  if (ageGroup === 'senior' || (ageGroup === 'adult' && pet.size === 'LARGE')) {
    meds.push({ name: '관절 영양제', schedule: '매일 아침', dose: '1정' });
  }

  if (ageGroup === 'senior') {
    meds.push({ name: '심장 보조제', schedule: '매일 저녁', dose: '0.5정' });
    meds.push({ name: '유산균', schedule: '매일 아침', dose: '1포' });
  }

  if (pet.size === 'LARGE') {
    meds.push({ name: '고관절 보호제', schedule: '격일 저녁', dose: '1정' });
  }

  return meds;
}

/** pet 기반 동적 케어 팁 생성 */
export function generateCareTips(pet: PetInfo): CareTip[] {
  const ageGroup = getAgeGroup(pet.age);
  const tips: CareTip[] = [];

  // 나이별 팁
  if (ageGroup === 'puppy') {
    tips.push({ text: '예방접종 스케줄을 반드시 지켜주세요.' });
    tips.push({ text: '다양한 환경에서 사회화 훈련을 진행하세요.' });
    tips.push({ text: '이갈이 시기에 적절한 장난감을 제공하세요.' });
  } else if (ageGroup === 'young' || ageGroup === 'adult') {
    tips.push({ text: '정기 건강검진은 1년에 1회 이상 필수입니다.' });
    tips.push({ text: '양치질을 매일 해주면 치석 예방에 좋아요.' });
  } else {
    tips.push({ text: '정기 건강검진은 6개월마다 필수입니다.' });
    tips.push({ text: '급격한 체중 변화가 있다면 즉시 병원 방문이 필요해요.' });
    tips.push({ text: '관절 건강을 위해 부드러운 바닥에서 활동하세요.' });
  }

  // 크기별 팁
  if (pet.size === 'LARGE') {
    tips.push({ text: '대형견은 관절 부담이 크므로 계단 사용을 줄여주세요.' });
  } else if (pet.size === 'SMALL') {
    tips.push({ text: '소형견은 저혈당에 취약하니 간식을 규칙적으로 주세요.' });
  }

  // 활동량별 팁
  if (pet.activity === 'HIGH') {
    tips.push({ text: '활동량이 많은 아이는 발바닥 패드를 자주 확인해주세요.' });
  }

  // 공통 팁
  tips.push({ text: '수분 섭취를 자주 체크하고 충분히 마시게 해주세요.' });

  return tips;
}

/** 나이 기반 검진 주기 라벨 */
export function getCheckupCycleLabel(age: number): string {
  const ageGroup = getAgeGroup(age);
  if (ageGroup === 'puppy') return '월 1회 (예방접종 포함)';
  if (ageGroup === 'young' || ageGroup === 'adult') return '연 1회';
  return '6개월 1회';
}

/** 나이 그룹 한글 라벨 */
export function getAgeGroupLabel(age: number): string {
  const group = getAgeGroup(age);
  const labels = { puppy: '퍼피', young: '영년기', adult: '성년기', senior: '시니어' };
  return labels[group];
}
