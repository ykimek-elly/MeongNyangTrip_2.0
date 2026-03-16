import React, { useState } from 'react';
import { TreeDeciduous, Bed, Coffee } from 'lucide-react';

const CATEGORY_PLACEHOLDER: Record<string, { bg: string; icon: React.ElementType }> = {
  PLACE:  { bg: 'bg-green-50',  icon: TreeDeciduous },
  STAY:   { bg: 'bg-yellow-50', icon: Bed },
  DINING: { bg: 'bg-orange-50', icon: Coffee },
};

interface PlaceImageProps {
  imageUrl: string | null | undefined;
  category: string;
  className: string;
  iconSize?: number;
  alt?: string;
}

export function PlaceImage({ imageUrl, category, className, iconSize = 28, alt = '' }: PlaceImageProps) {
  const [hasError, setHasError] = useState(false);
  const ph = CATEGORY_PLACEHOLDER[category] ?? CATEGORY_PLACEHOLDER['PLACE'];
  const Icon = ph.icon;

  if (imageUrl && !hasError) {
    return (
      <img
        src={imageUrl}
        className={className}
        alt={alt}
        onError={() => setHasError(true)}
      />
    );
  }

  return (
    <div className={`${className} ${ph.bg} flex items-center justify-center`}>
      <Icon size={iconSize} className="text-gray-300" />
    </div>
  );
}
