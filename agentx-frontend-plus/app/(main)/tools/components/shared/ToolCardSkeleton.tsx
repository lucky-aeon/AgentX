import { Card, CardHeader, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

interface ToolCardSkeletonProps {
  count?: number;
}

export function ToolCardSkeleton({ count = 1 }: ToolCardSkeletonProps) {
  return (
    <>
      {Array.from({ length: count }).map((_, index) => (
        <Card key={index} className="overflow-hidden border border-gray-100 shadow-sm min-h-[180px]">
          <CardHeader className="pb-2">
            <div className="flex items-center gap-2">
              <Skeleton className="h-10 w-10 rounded-md" />
              <div>
                <Skeleton className="h-5 w-32 mb-1" />
                <Skeleton className="h-4 w-24" />
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <Skeleton className="h-4 w-full mb-2" />
            <Skeleton className="h-4 w-4/5 mb-4" />
            <div className="mb-2 flex flex-wrap gap-1">
              <Skeleton className="h-5 w-16 rounded-full" />
              <Skeleton className="h-5 w-20 rounded-full" />
              <Skeleton className="h-5 w-14 rounded-full" />
            </div>
          </CardContent>
        </Card>
      ))}
    </>
  );
} 