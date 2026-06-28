import type {
  NodeRecord,
  AlarmRecord,
  AlarmAnalysisRecord,
  StaffRecord,
  SystemHealthReport,
  SystemStats,
  SystemAbout,
  AnomalyReport,
  FleetMetrics,
} from "../types/models";

const BASE = "";

async function fetchJson<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${url}`, init);
  if (!res.ok) {
    const body = await res.text();
    throw new Error(`${res.status}: ${body}`);
  }
  const text = await res.text();
  return text ? JSON.parse(text) : (undefined as unknown as T);
}

export const api = {
  getNodes: () => fetchJson<NodeRecord[]>("/api/nodes"),
  getNode: (id: string) => fetchJson<NodeRecord>(`/api/nodes/${id}`),

  getAlarms: () => fetchJson<AlarmRecord[]>("/api/alarms"),
  resolveAlarm: (id: string) =>
    fetchJson<void>(`/api/alarms/${id}/resolve`, { method: "POST" }),

  getAnalyses: () => fetchJson<AlarmAnalysisRecord[]>("/api/ai/analyses"),
  getAnalysis: (id: string) => fetchJson<AlarmAnalysisRecord>(`/api/ai/analyses/${id}`),

  getStaff: () => fetchJson<StaffRecord[]>("/api/staff"),
  getStaffMember: (id: string) => fetchJson<StaffRecord>(`/api/staff/${id}`),

  getHealth: () => fetchJson<SystemHealthReport>("/api/health"),

  getStats: () => fetchJson<SystemStats>("/api/system/stats"),
  getAbout: () => fetchJson<SystemAbout>("/api/system/about"),
  getAlarmImageUrl: (alarmId: string) => `${BASE}/api/alarms/${encodeURIComponent(alarmId)}/image`,
  getLatestAnomalyReport: () => fetchJson<AnomalyReport>("/api/fleet-metrics/anomaly-report/latest"),
  getLatestFleetMetrics: () => fetchJson<FleetMetrics>("/api/fleet-metrics/metrics/latest"),
};
