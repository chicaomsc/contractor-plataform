import type { FieldErrors, Resolver } from "react-hook-form";
import type { z } from "zod";

export function zodResolver<TSchema extends z.ZodType>(
  schema: TSchema,
): Resolver<z.infer<TSchema>> {
  return async (values) => {
    const result = schema.safeParse(values);

    if (result.success) {
      return {
        values: result.data,
        errors: {},
      };
    }

    const errors: Record<string, unknown> = {};

    result.error.issues.forEach((issue) => {
      let target = errors;
      issue.path.forEach((part, index) => {
        const key = String(part);
        const isLeaf = index === issue.path.length - 1;

        if (isLeaf) {
          target[key] = { message: issue.message, type: issue.code };
          return;
        }

        target[key] = (target[key] ?? {}) as Record<string, unknown>;
        target = target[key] as Record<string, unknown>;
      });
    });

    return {
      values: {},
      errors: errors as FieldErrors<z.infer<TSchema>>,
    };
  };
}
