export function ApiUnavailable() {
  return (
    <div
      role="status"
      className="border-l-4 border-warning bg-surface px-5 py-4 text-sm text-foreground"
    >
      Os dados públicos não estão disponíveis neste momento. A estrutura da
      página foi carregada com valores de fallback.
    </div>
  );
}
