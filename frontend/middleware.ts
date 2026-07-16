import { NextResponse, type NextRequest } from "next/server";

const SESSION_COOKIE = "contractor_session";

export function middleware(request: NextRequest) {
  const isDashboard = request.nextUrl.pathname.startsWith("/dashboard");
  const hasSession = request.cookies.get(SESSION_COOKIE)?.value === "active";

  if (isDashboard && !hasSession) {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("next", request.nextUrl.pathname);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/dashboard/:path*"],
};
