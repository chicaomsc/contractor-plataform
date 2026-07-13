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
        "mx-auto w-full min-w-0 px-5 md:px-8 lg:px-16",
        size === "wide"
          ? "max-w-[min(var(--container-max),100%)]"
          : "max-w-[min(48rem,100%)]",
        className,
      )}
    >
      {children}
    </div>
  );
}
