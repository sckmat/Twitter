import { API_BASE } from "../config/env";

type HttpMethod = "GET" | "POST" | "PUT" | "DELETE" | "PATCH";

export async function request<T>(
    path: string,
    options: {
        method?: HttpMethod;
        body?: unknown;
        headers?: Record<string, string>;
    } = {}
): Promise<T> {
    const token = localStorage.getItem("token");
    const method = options.method ?? "GET";

    const hasBody = options.body !== undefined && method !== "GET";

    const headers: Record<string, string> = {
        ...(hasBody ? { "Content-Type": "application/json" } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(options.headers ?? {}),
    };

    const res = await fetch(`${API_BASE}${path}`, {
        method,
        headers,
        body: hasBody ? JSON.stringify(options.body) : undefined,
    });

    if (!res.ok) {
        const text = await res.text().catch(() => "");
        if (res.status === 401) throw new Error("401 Unauthorized");
        throw new Error(text || `HTTP ${res.status}`);
    }

    if (res.status === 204) return undefined as T;
    return (await res.json()) as T;
}
