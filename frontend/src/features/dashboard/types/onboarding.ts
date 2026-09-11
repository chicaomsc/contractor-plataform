import { z } from "zod";

export const onboardingStatusSchema = z
  .object({
    companyCompleted: z.boolean(),
    brandingCompleted: z.boolean(),
    servicesCompleted: z.boolean(),
    customerCompleted: z.boolean(),
    estimateCompleted: z.boolean(),
    teamCompleted: z.boolean(),
  })
  .strict();

export type OnboardingStatus = z.infer<typeof onboardingStatusSchema>;
