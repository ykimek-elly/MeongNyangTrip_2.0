import React, { useState, useRef, useEffect } from 'react';
import { Calendar, ChevronLeft, ChevronRight } from 'lucide-react';

interface DatePickerPopupProps {
  value: string;
  onChange: (value: string) => void;
}

const DAYS = ['일', '월', '화', '수', '목', '금', '토'];
const MONTHS = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'];

export function DatePickerPopup({ value, onChange }: DatePickerPopupProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [textValue, setTextValue] = useState('');
  const [viewDate, setViewDate] = useState(new Date());
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Sync textValue with value prop
  useEffect(() => {
    if (value) {
      const d = new Date(value);
      if (!isNaN(d.getTime())) {
        setTextValue(formatDisplay(value));
      }
    } else {
      setTextValue('');
    }
  }, [value]);

  // Close on outside click
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    }
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen]);

  function formatDisplay(dateStr: string) {
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return dateStr;
    return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`;
  }

  function handleTextChange(e: React.ChangeEvent<HTMLInputElement>) {
    const val = e.target.value;
    setTextValue(val);

    // Try to parse various formats: YYYY.MM.DD, YYYY-MM-DD, YYYY/MM/DD
    const cleaned = val.replace(/[./]/g, '-');
    const match = cleaned.match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/);
    if (match) {
      const year = parseInt(match[1]);
      const month = parseInt(match[2]);
      const day = parseInt(match[3]);
      if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
        const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        onChange(dateStr);
        setViewDate(new Date(year, month - 1));
      }
    }
  }

  function handleTextKeyDown(e: React.KeyboardEvent) {
    if (e.key === 'Enter') {
      setIsOpen(false);
    }
  }

  function handleDayClick(day: number) {
    const year = viewDate.getFullYear();
    const month = viewDate.getMonth();
    const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    onChange(dateStr);
    setIsOpen(false);
  }

  function prevMonth() {
    setViewDate(new Date(viewDate.getFullYear(), viewDate.getMonth() - 1));
  }

  function nextMonth() {
    setViewDate(new Date(viewDate.getFullYear(), viewDate.getMonth() + 1));
  }

  // Calendar grid
  function getDaysInMonth(year: number, month: number) {
    return new Date(year, month + 1, 0).getDate();
  }

  function getFirstDayOfMonth(year: number, month: number) {
    return new Date(year, month, 1).getDay();
  }

  const year = viewDate.getFullYear();
  const month = viewDate.getMonth();
  const daysInMonth = getDaysInMonth(year, month);
  const firstDay = getFirstDayOfMonth(year, month);
  const today = new Date();
  const selectedDate = value ? new Date(value) : null;

  const calendarDays: (number | null)[] = [];
  for (let i = 0; i < firstDay; i++) calendarDays.push(null);
  for (let d = 1; d <= daysInMonth; d++) calendarDays.push(d);

  const isSelected = (day: number) =>
    selectedDate &&
    selectedDate.getFullYear() === year &&
    selectedDate.getMonth() === month &&
    selectedDate.getDate() === day;

  const isToday = (day: number) =>
    today.getFullYear() === year &&
    today.getMonth() === month &&
    today.getDate() === day;

  return (
    <div ref={containerRef} className="relative w-[130px] shrink-0">
      <div
        className="bg-gray-50 border border-gray-200 rounded-2xl px-3 h-full flex items-center gap-2 w-full cursor-pointer"
        onClick={() => {
          setIsOpen(!isOpen);
          if (!isOpen) {
            setTimeout(() => inputRef.current?.focus(), 100);
            if (value) {
              setViewDate(new Date(value));
            } else {
              setViewDate(new Date());
            }
          }
        }}
      >
        <Calendar className="text-gray-400 shrink-0" size={18} />
        <input
          ref={inputRef}
          type="text"
          placeholder="날짜 선택"
          className="bg-transparent w-full outline-none text-gray-600 text-xs font-medium p-0"
          value={textValue}
          onChange={handleTextChange}
          onKeyDown={handleTextKeyDown}
          onFocus={() => {
            setIsOpen(true);
            if (value) {
              setViewDate(new Date(value));
            } else {
              setViewDate(new Date());
            }
          }}
          onClick={(e) => e.stopPropagation()}
        />
      </div>

      {isOpen && (
        <div className="absolute top-full left-1/2 -translate-x-1/2 mt-2 bg-white rounded-2xl shadow-xl border border-gray-100 p-4 z-50 w-[280px]">
          {/* Header */}
          <div className="flex items-center justify-between mb-3">
            <button
              className="w-7 h-7 flex items-center justify-center rounded-full hover:bg-gray-100 transition-spring"
              onClick={(e) => { e.stopPropagation(); prevMonth(); }}
            >
              <ChevronLeft size={16} className="text-gray-500" />
            </button>
            <span className="text-sm font-bold text-gray-800">
              {year}년 {MONTHS[month]}
            </span>
            <button
              className="w-7 h-7 flex items-center justify-center rounded-full hover:bg-gray-100 transition-spring"
              onClick={(e) => { e.stopPropagation(); nextMonth(); }}
            >
              <ChevronRight size={16} className="text-gray-500" />
            </button>
          </div>

          {/* Day headers */}
          <div className="grid grid-cols-7 gap-0 mb-1">
            {DAYS.map((d) => (
              <div key={d} className="text-center text-[10px] font-medium text-gray-400 py-1">
                {d}
              </div>
            ))}
          </div>

          {/* Days grid */}
          <div className="grid grid-cols-7 gap-0">
            {calendarDays.map((day, i) => (
              <div key={i} className="flex items-center justify-center">
                {day ? (
                  <button
                    className={`w-8 h-8 rounded-full text-xs font-medium transition-spring
                      ${isSelected(day)
                        ? 'bg-primary text-white font-bold'
                        : isToday(day)
                          ? 'bg-primary/10 text-primary font-bold'
                          : 'text-gray-700 hover:bg-gray-100'
                      }`}
                    onClick={(e) => { e.stopPropagation(); handleDayClick(day); }}
                  >
                    {day}
                  </button>
                ) : (
                  <div className="w-8 h-8" />
                )}
              </div>
            ))}
          </div>

          {/* Today button */}
          <div className="mt-2 pt-2 border-t border-gray-100 flex justify-center">
            <button
              className="text-xs text-primary font-medium hover:underline"
              onClick={(e) => {
                e.stopPropagation();
                const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
                onChange(todayStr);
                setIsOpen(false);
              }}
            >
              오늘
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
