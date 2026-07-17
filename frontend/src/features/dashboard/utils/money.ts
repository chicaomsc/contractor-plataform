/**
 * Formatting only — never computes a monetary value. Every number passed in must already
 * come from a backend response (EstimateResponse/EstimateItemResponse/MaterialResponse).
 */
export function formatMoney(value: number, currency: string) {
  return new Intl.NumberFormat("pt-PT", {
    style: "currency",
    currency: currency || "EUR",
  }).format(value);
}

export function formatDate(value: string | null) {
  if (!value) {
    return "—";
  }

  return new Intl.DateTimeFormat("pt-PT", { dateStyle: "medium" }).format(
    new Date(value),
  );
}
