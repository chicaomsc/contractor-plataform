import { NextResponse, type NextRequest } from "next/server";

const SESSION_COOKIE = "contractor_session";
const ROLE_COOKIE = "contractor_role";

export function middleware(request: NextRequest) {
  const isDashboard = request.nextUrl.pathname.startsWith("/dashboard");
  const isAdmin = request.nextUrl.pathname.startsWith("/admin");
  const isAdminLogin = request.nextUrl.pathname === "/admin/login";
  const hasSession = request.cookies.get(SESSION_COOKIE)?.value === "active";
  const role = request.cookies.get(ROLE_COOKIE)?.value;

  if (isDashboard && !hasSession) {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("next", request.nextUrl.pathname);
    return NextResponse.redirect(loginUrl);
  }

  if (isAdmin && !isAdminLogin && !hasSession) {
    const loginUrl = new URL("/admin/login", request.url);
    loginUrl.searchParams.set("next", request.nextUrl.pathname);
    return NextResponse.redirect(loginUrl);
  }

  if (isAdmin && !isAdminLogin && role === "OWNER") {
    return NextResponse.redirect(new URL("/dashboard", request.url));
  }

  if (isDashboard && role === "SUPER_ADMIN") {
    return NextResponse.redirect(new URL("/admin", request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/dashboard/:path*", "/admin/:path*"],
};
