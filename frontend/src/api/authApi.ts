import { request } from "./http";

type LoginPayload = {
    login: string;
    password: string;
};

type LoginResponse = {
    code: string;
    message: string;
    data?: {
        token?: string;
    };
};

export async function login(
    payload: LoginPayload
): Promise<{ token: string }> {
    const res = await request<LoginResponse>("/auth/login", {
        method: "POST",
        body: payload,
    });

    const token = res?.data?.token;
    if (!token) {
        throw new Error("Токен не получен");
    }

    return { token };
}