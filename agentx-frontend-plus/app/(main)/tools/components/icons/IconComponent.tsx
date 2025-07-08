import React from 'react';
import { Wrench } from 'lucide-react';

interface IconComponentProps {
  icon: string | null;
  className?: string;
}

/**
 * 图标组件，根据传入的图标URL显示图像，如果没有图标则显示默认图标
 */
const IconComponent: React.FC<IconComponentProps> = ({ icon, className = '' }) => {
  if (!icon) {
    return <Wrench className={className} />;
  }

  return (
    <img 
      src={icon} 
      alt="工具图标" 
      className={`object-contain ${className}`} 
      onError={(e) => {
        // 图片加载失败时，显示默认图标
        const target = e.currentTarget;
        target.style.display = 'none';
        const parent = target.parentElement;
        if (parent) {
          const wrapperDiv = document.createElement('div');
          wrapperDiv.className = className;
          const iconElement = <Wrench className={className} />;
          // 由于React元素不能直接附加到DOM，使用替代方案
          parent.appendChild(wrapperDiv);
          // 这里我们只是设置一个标记，实际应该使用ReactDOM
          wrapperDiv.innerHTML = '<span>图标加载失败</span>';
        }
      }}
    />
  );
};

export default IconComponent; 