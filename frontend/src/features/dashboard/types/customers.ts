import { z } from "zod";

const nullableString = z.string().nullable();
const emptyToNull = (value: unknown) => (value === "" ? null : value);
const nullableTextInput = (max: number) =>
  z.preprocess(emptyToNull, z.string().max(max).nullable());

export const customerAddressDtoSchema = z.object({
  street: nullableString,
  city: nullableString,
  postalCode: nullableString,
  region: nullableString,
  country: nullableString,
});

export const customerDtoSchema = z.object({
  id: z.string(),
  companyId: z.string(),
  name: z.string(),
  email: nullableString,
  phone: nullableString,
  taxNumber: nullableString,
  address: customerAddressDtoSchema.nullable(),
  notes: nullableString,
  active: z.boolean(),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export const customersDtoSchema = z.array(customerDtoSchema);

const customerAddressFormSchema = z.object({
  street: nullableTextInput(255),
  city: nullableTextInput(100),
  postalCode: nullableTextInput(20),
  region: nullableTextInput(100),
  country: nullableTextInput(2),
});

/**
 * Mirrors the backend's own rule (CustomerService#requireContactInfo): at least one of
 * email/phone must be present. The backend re-validates this regardless — this check only
 * saves the user a round trip.
 */
export const quickCustomerFormSchema = z
  .object({
    name: z.string().min(1, "Indique o nome do cliente.").max(255),
    email: z.preprocess(
      emptyToNull,
      z.string().email("Indique um email válido.").max(255).nullable(),
    ),
    phone: nullableTextInput(50),
    taxNumber: nullableTextInput(50),
    address: customerAddressFormSchema,
    notes: nullableTextInput(2000),
  })
  .superRefine((data, ctx) => {
    if (!data.email && !data.phone) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: "Indique pelo menos um contacto: email ou telefone.",
        path: ["phone"],
      });
    }
  });

export type CustomerDto = z.infer<typeof customerDtoSchema>;
export type QuickCustomerFormInput = z.infer<typeof quickCustomerFormSchema>;
