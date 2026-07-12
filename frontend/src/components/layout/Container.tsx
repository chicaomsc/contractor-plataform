import type { ReactNode } from "react";
import { cn } from "@/lib/utils/cn";

type ContainerProps = {
  children: ReactNode;
  size?: "wide" | "narrow";
  className?: string;
};

export function Container({
  children,
  size = "wide",
  className,
}: ContainerProps) {
  return (
    <div
      className={cn(
        "mx-auto w-full px-5 md:px-8 lg:px-16",
        size === "wide" ? "max-w-container" : "max-w-3xl",
        className,
      )}
    >
      {children}
    </div>
  );
}
