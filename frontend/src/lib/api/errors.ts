export type ApiErrorBody = {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
};

export class ApiError extends Error {
  readonly status: number;
  readonly body: ApiErrorBody | null;
  readonly code: "http" | "timeout" | "network" | "invalid-response";

  constructor(
    message: string,
    status: number,
    body: ApiErrorBody | null = null,
    code: "http" | "timeout" | "network" | "invalid-response" = "http",
  ) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
    this.code = code;
  }
}
