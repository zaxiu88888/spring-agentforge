import { message } from "antd";

export const BASE_URL = "http://localhost:8080/api";
export const SSE_BASE_URL = "http://localhost:8080/sse"; // 注意无 /api 前缀（契约 §8）

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const resp = await fetch(BASE_URL + path, {
    method,
    headers: { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const json = (await resp.json()) as ApiResponse<T>;
  if (json.code !== 200) {
    message.error(json.message || "请求失败");
    throw new Error(json.message);
  }
  return json.data;
}

export const get = <T,>(path: string) => request<T>("GET", path);
export const post = <T,>(path: string, body?: unknown) => request<T>("POST", path, body);
export const patch = <T,>(path: string, body?: unknown) => request<T>("PATCH", path, body);
export const del = <T,>(path: string) => request<T>("DELETE", path);

/** multipart 上传（契约 §5：kbId + file） */
export async function upload<T>(path: string, form: FormData): Promise<T> {
  const resp = await fetch(BASE_URL + path, { method: "POST", body: form });
  const json = (await resp.json()) as ApiResponse<T>;
  if (json.code !== 200) {
    message.error(json.message || "上传失败");
    throw new Error(json.message);
  }
  return json.data;
}
