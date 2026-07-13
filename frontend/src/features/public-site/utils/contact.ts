const PHONE_DIGIT_PATTERN = /\d/g;

export function getPhoneHref(phone: string | null): string | null {
  if (!phone) {
    return null;
  }

  const digits = phone.match(PHONE_DIGIT_PATTERN)?.join("") ?? "";
  return digits.length >= 6 ? `tel:+${digits}` : null;
}

export function getWhatsAppHref(
  whatsapp: string | null,
  message = "Pedido de orçamento",
): string | null {
  if (!whatsapp) {
    return null;
  }

  const digits = whatsapp.match(PHONE_DIGIT_PATTERN)?.join("") ?? "";
  if (digits.length < 8) {
    return null;
  }

  return `https://wa.me/${digits}?text=${encodeURIComponent(message)}`;
}
