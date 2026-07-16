export function resolveAdminAssetUrl(url: string | null) {
  if (!url) {
    return null;
  }

  if (/^https?:\/\//i.test(url)) {
    return url;
  }

  const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;

  if (!apiBaseUrl) {
    return url;
  }

  try {
    return new URL(url, apiBaseUrl).toString();
  } catch {
    return url;
  }
}
