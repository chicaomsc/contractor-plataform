import { LoginForm } from "@/features/auth/components/LoginForm";

export const metadata = {
  title: "Platform Admin Login",
};

export default function AdminLoginPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-6 py-12">
      <LoginForm variant="admin" />
    </main>
  );
}
