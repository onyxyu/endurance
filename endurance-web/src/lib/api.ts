export interface SystemInfo {
  name: string;
  version: string;
  stage: string;
  modules: string[];
  capabilities: Record<string, boolean>;
}

export async function getSystemInfo(signal?: AbortSignal): Promise<SystemInfo> {
  const response = await fetch('/api/system/info', { signal });
  if (!response.ok) {
    throw new Error(`服务响应异常：HTTP ${response.status}`);
  }
  return response.json() as Promise<SystemInfo>;
}
