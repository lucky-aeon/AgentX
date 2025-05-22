import { ReactNode } from "react";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import { LucideIcon } from "lucide-react";

interface EmptyToolsStateProps {
  icon: LucideIcon;
  title: string;
  description?: string;
  iconClassName?: string;
  action?: {
    label: string;
    icon?: LucideIcon;
    href?: string;
    onClick?: () => void;
  };
}

export function EmptyToolsState({
  icon: Icon,
  title,
  description,
  iconClassName = "bg-primary/5 text-primary",
  action
}: EmptyToolsStateProps) {
  return (
    <div className="text-center py-12 bg-gray-50 rounded-lg border border-dashed border-gray-200">
      <div className={`${iconClassName} p-3 rounded-full inline-flex mb-4`}>
        <Icon className="h-8 w-8" />
      </div>
      
      <h3 className="text-lg font-medium mb-2">
        {title}
      </h3>
      
      {description && (
        <p className="text-muted-foreground mb-4 max-w-md mx-auto">
          {description}
        </p>
      )}
      
      {action && (
        <Button 
          variant="outline" 
          asChild={!!action.href}
          onClick={action.onClick}
        >
          {action.href ? (
            <Link href={action.href}>
              {action.icon && <action.icon className="mr-2 h-4 w-4" />}
              {action.label}
            </Link>
          ) : (
            <>
              {action.icon && <action.icon className="mr-2 h-4 w-4" />}
              {action.label}
            </>
          )}
        </Button>
      )}
    </div>
  );
} 