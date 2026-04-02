import React, { useEffect, useState } from 'react';
import { ChevronUp, ChevronDown } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

interface ScrollButtonsProps {
  scrollRef: React.RefObject<HTMLElement | null>;
  withNav?: boolean;
}

export function ScrollButtons({ scrollRef, withNav = false }: ScrollButtonsProps) {
  const [showUp, setShowUp] = useState(false);
  const [showDown, setShowDown] = useState(false);

  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;

    const update = () => {
      setShowUp(el.scrollTop > 80);
      setShowDown(el.scrollTop + el.clientHeight < el.scrollHeight - 80);
    };

    update();
    el.addEventListener('scroll', update, { passive: true });
    const ro = new ResizeObserver(update);
    ro.observe(el);

    return () => {
      el.removeEventListener('scroll', update);
      ro.disconnect();
    };
  }, [scrollRef]);

  const scrollUp = () => scrollRef.current?.scrollBy({ top: -320, behavior: 'smooth' });
  const scrollDown = () => scrollRef.current?.scrollBy({ top: 320, behavior: 'smooth' });

  return (
    <div className={`absolute right-3 z-40 flex flex-col gap-2 pointer-events-none ${withNav ? 'bottom-[90px]' : 'bottom-6'}`}>
      <AnimatePresence>
        {showUp && (
          <motion.button
            key="up"
            initial={{ opacity: 0, y: 6, scale: 0.85 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 6, scale: 0.85 }}
            transition={{ type: 'spring', damping: 22, stiffness: 320 }}
            onClick={scrollUp}
            className="pointer-events-auto w-9 h-9 bg-white/90 backdrop-blur-sm rounded-full shadow-md border border-gray-100 flex items-center justify-center text-gray-400 hover:text-primary hover:shadow-lg transition-spring active:scale-90"
            aria-label="위로 스크롤"
          >
            <ChevronUp size={18} />
          </motion.button>
        )}
      </AnimatePresence>
      <AnimatePresence>
        {showDown && (
          <motion.button
            key="down"
            initial={{ opacity: 0, y: -6, scale: 0.85 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -6, scale: 0.85 }}
            transition={{ type: 'spring', damping: 22, stiffness: 320 }}
            onClick={scrollDown}
            className="pointer-events-auto w-9 h-9 bg-white/90 backdrop-blur-sm rounded-full shadow-md border border-gray-100 flex items-center justify-center text-gray-400 hover:text-primary hover:shadow-lg transition-spring active:scale-90"
            aria-label="아래로 스크롤"
          >
            <ChevronDown size={18} />
          </motion.button>
        )}
      </AnimatePresence>
    </div>
  );
}
