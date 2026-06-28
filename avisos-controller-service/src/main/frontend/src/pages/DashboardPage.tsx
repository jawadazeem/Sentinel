import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { useSubscription } from "../hooks/useSubscription";
import { DataCard } from "../components/ui/DataCard";
import { StatusBadge } from "../components/ui/StatusBadge";
import type {
  NodeRecord,
  AlarmRecord,
  AlarmAnalysisRecord,
  SystemHealthReport,
  SystemStats,
  VisionEvent,
  AnomalyReport,
  FleetMetrics,
} from "../types/models";
import "./DashboardPage.css";

export function DashboardPage() {
  const [health, setHealth] = useState<SystemHealthReport | null>(null);
  const [nodes, setNodes] = useState<NodeRecord[]>([]);
  const [alarms, setAlarms] = useState<AlarmRecord[]>([]);
  const [stats, setStats] = useState<SystemStats | null>(null);
  const [analyses, setAnalyses] = useState<AlarmAnalysisRecord[]>([]);
  const [anomalyReport, setAnomalyReport] = useState<AnomalyReport | null>(null);
  const [fleetMetrics, setFleetMetrics] = useState<FleetMetrics | null>(null);
  const [error, setError] = useState<string | null>(null);

  const nodeUpdate = useSubscription<NodeRecord>("/topic/nodes");
  const alarmUpdate = useSubscription<AlarmRecord>("/topic/alarms");
  const visionUpdate = useSubscription<VisionEvent>("/topic/vision");
  const analysisUpdate = useSubscription<AlarmAnalysisRecord>("/topic/alarm");

  useEffect(() => {
    Promise.all([
      api.getHealth(),
      api.getNodes(),
      api.getAlarms(),
      api.getStats(),
      api.getAnalyses(),
      api.getLatestAnomalyReport().catch(() => null),
      api.getLatestFleetMetrics().catch(() => null),
    ])
      .then(([h, n, a, s, an, ar, fm]) => {
        setHealth(h);
        setNodes(n);
        setAlarms(a);
        setStats(s);
        setAnalyses(an);
        setAnomalyReport(ar);
        setFleetMetrics(fm);
      })
      .catch((e) => setError(e.message));
  }, []);

  useEffect(() => {
    if (!nodeUpdate) return;
    setNodes((prev) => {
      const idx = prev.findIndex((n) => n.uuid === nodeUpdate.uuid);
      if (idx >= 0) {
        const updated = [...prev];
        updated[idx] = nodeUpdate;
        return updated;
      }
      return [...prev, nodeUpdate];
    });
  }, [nodeUpdate]);

  useEffect(() => {
    if (!alarmUpdate) return;
    setAlarms((prev) => upsertAlarm(prev, alarmUpdate));
  }, [alarmUpdate]);

  useEffect(() => {
    if (!analysisUpdate) return;
    setAnalyses((prev) => [analysisUpdate, ...prev]);
  }, [analysisUpdate]);

  if (error) {
    return <div className="dash-error">Error: {error}</div>;
  }

  const responsive = nodes.filter((n) => n.status === "RESPONSIVE").length;
  const activeAlarms = alarms.filter((a) => a.status === "ACTIVE");
  const critical = activeAlarms.filter((a) => a.severity === "CRITICAL").length;
  const warnings = activeAlarms.filter((a) => a.severity === "WARNING").length;
  const withEvidence = activeAlarms.filter((a) => Boolean(a.s3ImageKey)).length;

  return (
    <div className="dashboard">
      <div className="dash-grid">
        <DataCard title="System Health" accent={health?.overallStatus === "HEALTHY" ? "green" : health?.overallStatus === "DEGRADED" ? "amber" : "red"}>
          {health ? (
            <div className="health-panel">
              <div className="health-overall">
                <StatusBadge status={health.overallStatus} />
              </div>
              <div className="health-components">
                {health.components.map((c) => (
                  <div key={c.component} className="health-row">
                    <span className="comp-name">{c.component}</span>
                    <StatusBadge status={c.status} />
                    <span className="comp-latency">{c.latencyMs}ms</span>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <span className="loading">Loading...</span>
          )}
        </DataCard>

        <DataCard title="Node Overview" accent="blue">
          <div className="stat-grid">
            <div className="stat-item">
              <span className="stat-value green">{responsive}</span>
              <span className="stat-label">Online</span>
            </div>
            <div className="stat-item">
              <span className="stat-value amber">{nodes.length - responsive}</span>
              <span className="stat-label">Offline</span>
            </div>
            <div className="stat-item">
              <span className="stat-value">{nodes.length}</span>
              <span className="stat-label">Total</span>
            </div>
          </div>
          
          <div style={{ marginTop: "16px", paddingTop: "16px", borderTop: "1px solid var(--border-color)" }}>
            <h4 style={{ margin: "0 0 12px 0", color: "var(--text-secondary)", fontSize: "0.9em", textTransform: "uppercase" }}>Fleet Analysis</h4>
            {anomalyReport ? (
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                <span style={{ fontSize: "0.9em" }}>Anomaly Status:</span>
                <span className={anomalyReport.isAnomaly ? "red" : "green"} style={{ fontWeight: 600, fontSize: "0.9em" }}>
                  {anomalyReport.isAnomaly ? "DETECTED" : "NORMAL"}
                </span>
              </div>
            ) : <div style={{ fontSize: "0.9em", color: "var(--text-muted)" }}>No anomaly report</div>}
            
            {fleetMetrics ? (
              <div style={{ fontSize: '0.85em', color: 'var(--text-secondary)', display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span>Avg Battery &gt; 50%:</span>
                  <span>{(fleetMetrics.fleetMetrics.batteryAbove50Ratio * 100).toFixed(0)}%</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span>Responsive Ratio:</span>
                  <span>{(fleetMetrics.fleetMetrics.responsiveRatio * 100).toFixed(0)}%</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span>Evaluated Nodes:</span>
                  <span>{fleetMetrics.fleetMetrics.totalNodesEvaluated}</span>
                </div>
              </div>
            ) : null}
          </div>
        </DataCard>

        <DataCard title="Active Alarms" accent={critical > 0 ? "red" : warnings > 0 ? "amber" : "green"}>
          <div className="alarm-card-content">
            <div className="stat-grid">
              <div className="stat-item">
                <span className="stat-value red">{critical}</span>
                <span className="stat-label">Critical</span>
              </div>
              <div className="stat-item">
                <span className="stat-value amber">{warnings}</span>
                <span className="stat-label">Warnings</span>
              </div>
              <div className="stat-item">
                <span className="stat-value">{activeAlarms.length}</span>
                <span className="stat-label">Total Active</span>
              </div>
              <div className="stat-item">
                <span className="stat-value green">{withEvidence}</span>
                <span className="stat-label">Images</span>
              </div>
            </div>
            <Link className="alarm-page-link" to="/alarms">
              Open Alarms
            </Link>
          </div>
        </DataCard>

        <DataCard title="Sherwood — AI Analyst" accent="blue">
          <div className="sherwood-card-content">
            <div className="stat-grid">
              <div className="stat-item">
                <span className="stat-value">{analyses.length}</span>
                <span className="stat-label">Analyses</span>
              </div>
              <div className="stat-item">
                <span className="stat-value" style={{ fontSize: 14 }}>
                  {analyses.length > 0 ? analyses[0].alarmId.substring(0, 8) : "-"}
                </span>
                <span className="stat-label">Latest ID</span>
              </div>
            </div>
            {analyses.length > 0 && (
              <p className="sherwood-preview">{analyses[0].analysisText.substring(0, 120)}...</p>
            )}
            <Link className="alarm-page-link" to="/sherwood">
              Open Sherwood
            </Link>
          </div>
        </DataCard>

        <DataCard title="System Stats" accent="blue">
          {stats ? (
            <div className="stats-panel">
              <div className="stats-row">
                <span>Heap</span>
                <span className="stats-value">{stats.heapUsedMb}/{stats.heapMaxMb} MB</span>
              </div>
              <div className="stats-row">
                <span>Threads</span>
                <span className="stats-value">{stats.activeThreads}</span>
              </div>
              <div className="stats-row">
                <span>CPUs</span>
                <span className="stats-value">{stats.availableProcessors}</span>
              </div>
              <div className="stats-row">
                <span>Uptime</span>
                <span className="stats-value">{formatUptime(stats.uptimeSeconds)}</span>
              </div>
              <div className="stats-row">
                <span>Java</span>
                <span className="stats-value">{stats.javaVersion}</span>
              </div>
            </div>
          ) : (
            <span className="loading">Loading...</span>
          )}
        </DataCard>

        {visionUpdate && (
          <DataCard title="Last Vision Analysis" accent="amber">
            <div className="vision-panel">
              <div className="stats-row">
                <span>Node</span>
                <span className="stats-value">{visionUpdate.nodeId}</span>
              </div>
              <div className="vision-labels">
                {visionUpdate.response.predictions?.map((p, i) => (
                  <div key={i} className="vision-label">
                    <span>{p.label}</span>
                    <span className="confidence">{(p.confidence * 100).toFixed(1)}%</span>
                  </div>
                ))}
              </div>
            </div>
          </DataCard>
        )}
      </div>
    </div>
  );
}

function formatUptime(seconds: number): string {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = Math.floor(seconds % 60);
  return `${h}h ${m}m ${s}s`;
}

function upsertAlarm(existing: AlarmRecord[], incoming: AlarmRecord): AlarmRecord[] {
  const index = existing.findIndex((alarm) => alarm.id === incoming.id);
  if (index < 0) return [incoming, ...existing];
  const updated = [...existing];
  updated[index] = incoming;
  return updated;
}
