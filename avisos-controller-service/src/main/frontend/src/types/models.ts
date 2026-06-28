export type NodeStatus = "RESPONSIVE" | "UNRESPONSIVE" | "OFFLINE";
export type ApiDateTime = string | number[];

export interface NodeRecord {
  uuid: string;
  name: string;
  type: string;
  status: NodeStatus;
  batteryLevel: number;
  lastSeen: ApiDateTime | null;
}

export type AlarmSeverity = "CRITICAL" | "WARNING" | "NONE";
export type AlarmStatus = "ACTIVE" | "RESOLVED";

export interface AlarmRecord {
  id: string;
  deviceUuid: string;
  severity: AlarmSeverity;
  reason: string;
  status: AlarmStatus;
  triggeredAtTimestamp: ApiDateTime;
  resolvedAtTimestamp: ApiDateTime | null;
  s3ImageKey: string | null;
}

export type HealthStatusLevel = "HEALTHY" | "DEGRADED" | "UNHEALTHY";

export interface ComponentHealth {
  component: string;
  status: HealthStatusLevel;
  message: string;
  latencyMs: number;
}

export interface SystemHealthReport {
  overallStatus: HealthStatusLevel;
  components: ComponentHealth[];
}

export interface SystemStats {
  heapUsedMb: number;
  heapMaxMb: number;
  availableProcessors: number;
  activeThreads: number;
  uptimeSeconds: number;
  javaVersion: string;
}

export interface SystemAbout {
  name: string;
  description: string;
  version: string;
  architecture: string;
}

export interface CliResponse {
  command: string;
  output: string;
  executionMs: number;
}

export interface VisionEvent {
  nodeId: string;
  response: {
    predictions: { label: string; confidence: number }[];
  };
}

export interface StaffRecord {
  staffId: string;
  name: string;
  email: string;
  phone: string;
  role: string;
  jurisdiction: string;
  primaryZone: string;
  shift: string;
  createdAt: ApiDateTime;
  updatedAt: ApiDateTime;
}

export interface AlarmAnalysisRecord {
  alarmId: string;
  analysisText: string;
  promptVersion: string;
  createdAt: ApiDateTime;
}

export interface AnomalyReport {
  id: number;
  timestamp: string;
  isAnomaly: boolean;
  diagnosticReason: string | null;
  rawPayload: {
    totalNodesEvaluated: number,
    responsiveRatio: number,
    batteryAbove50Ratio: number,
    avgSecondsSinceLastSeen: number
  }
}
export interface FleetMetrics {
  timestamp: string;
  fleetMetrics: {
    totalNodesEvaluated: number,
    responsiveRatio: number,
    batteryAbove50Ratio: number,
    avgSecondsSinceLastSeen: number
  }
}
