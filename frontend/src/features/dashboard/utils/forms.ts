export function emptyToNull(value: string | null | undefined) {
  const normalized = value?.trim();
  return normalized ? normalized : null;
}

export function nullableText(value: string | null | undefined) {
  return value ?? "";
}

export function latestIsoDate(dates: Array<string | null | undefined>) {
  const timestamps = dates
    .filter((date): date is string => Boolean(date))
    .map((date) => Date.parse(date))
    .filter((timestamp) => !Number.isNaN(timestamp));

  if (!timestamps.length) {
    return null;
  }

  return new Date(Math.max(...timestamps)).toISOString();
}

export function formatDateTime(value: string | null) {
  if (!value) {
    return "Sem atualização registada";
  }

  return new Intl.DateTimeFormat("pt-PT", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
