import { adminApiRequest } from "@/lib/api/admin-http-client";
import {
  customerDtoSchema,
  customersDtoSchema,
  type CustomerDto,
  type QuickCustomerFormInput,
} from "../types/customers";

export async function fetchCustomers(accessToken: string): Promise<CustomerDto[]> {
  const response = await adminApiRequest<unknown>("/customers", {
    accessToken,
  });
  return customersDtoSchema.parse(response);
}

export async function createCustomer(
  accessToken: string,
  payload: QuickCustomerFormInput,
): Promise<CustomerDto> {
  const response = await adminApiRequest<unknown>("/customers", {
    method: "POST",
    accessToken,
    body: JSON.stringify(payload),
  });
  return customerDtoSchema.parse(response);
}
