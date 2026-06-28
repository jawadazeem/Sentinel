import { useEffect, useMemo, useState } from "react";
import { api } from "../api/client";
import { useSubscription } from "../hooks/useSubscription";
import { StatusBadge } from "../components/ui/StatusBadge";
import type { NodeRecord, AnomalyReport, FleetMetrics } from "../types/models";
import { dateTimeMillis, formatDateTime } from "../utils/dateTime";
import "./NodesPage.css";

const PAGE_SIZE = 20;

export function NodesPage() {
  const [nodes, setNodes] = useState<NodeRecord[]>([]);
  const [anomalyReport, setAnomalyReport] = useState<AnomalyReport | null>(null);
  const [fleetMetrics, setFleetMetrics] = useState<FleetMetrics | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const nodeUpdate = useSubscription<NodeRecord>("/topic/nodes");

  useEffect(() => {
    api.getNodes().then(setNodes).catch((e) => setError(e.message));
    api.getLatestAnomalyReport().then(setAnomalyReport).catch(() => null);
    api.getLatestFleetMetrics().then(setFleetMetrics).catch(() => null);
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

  const sortedNodes = useMemo(
    () =>
      [...nodes].sort((a, b) => {
        const seen = dateTimeMillis(b.lastSeen) - dateTimeMillis(a.lastSeen);
        if (Number.isFinite(seen) && seen !== 0) return seen;
        return a.name.localeCompare(b.name);
      }),
    [nodes],
  );

  const pageCount = Math.max(1, Math.ceil(sortedNodes.length / PAGE_SIZE));
  const currentPage = Math.min(page, pageCount);
  const pageStart = (currentPage - 1) * PAGE_SIZE;
  const visibleNodes = sortedNodes.slice(pageStart, pageStart + PAGE_SIZE);

  useEffect(() => {
    if (page > pageCount) {
      setPage(pageCount);
    }
  }, [page, pageCount]);

  if (error) return <div className="page-error">Error: {error}</div>;

  return (
    <div className="nodes-page">
      <div className="page-header">
        <div>
          <h2 className="page-title">Registered Nodes</h2>
          <span className="page-subtitle">
            {nodes.length} total / {nodes.filter((node) => node.status === "RESPONSIVE").length} responsive
          </span>
        </div>
        <div className="pagination-summary">
          Showing {nodes.length === 0 ? 0 : pageStart + 1}-{Math.min(pageStart + PAGE_SIZE, sortedNodes.length)} of{" "}
          {sortedNodes.length}
        </div>
      </div>
      
      {(anomalyReport || fleetMetrics) && (
        <div style={{ marginBottom: "24px", padding: "16px", backgroundColor: "var(--bg-card)", borderRadius: "8px", border: "1px solid var(--border-color)" }}>
          <h3 style={{ margin: "0 0 16px 0", fontSize: "1.1em" }}>Fleet Analysis & Metrics</h3>
          <div style={{ display: "flex", gap: "32px", flexWrap: "wrap" }}>
            {anomalyReport && (
              <div style={{ flex: 1, minWidth: "250px" }}>
                <h4 style={{ margin: "0 0 8px 0", fontSize: "0.95em", color: "var(--text-secondary)" }}>Latest Anomaly Report</h4>
                <div style={{ marginBottom: "4px" }}><strong style={{ width: "120px", display: "inline-block", fontSize: "0.9em" }}>Status:</strong> <StatusBadge status={anomalyReport.isAnomaly ? "UNHEALTHY" : "HEALTHY"} /></div>
                <div style={{ marginBottom: "4px", fontSize: "0.9em" }}><strong style={{ width: "120px", display: "inline-block" }}>Report Time:</strong> {new Date(Number(anomalyReport.timestamp) > 9999999999 ? Number(anomalyReport.timestamp) : Number(anomalyReport.timestamp) * 1000).toLocaleString()}</div>
                {anomalyReport.diagnosticReason && <div style={{ fontSize: "0.9em" }}><strong style={{ width: "120px", display: "inline-block" }}>Reason:</strong> <span className="red">{anomalyReport.diagnosticReason}</span></div>}
              </div>
            )}
            {fleetMetrics && (
              <div style={{ flex: 1, minWidth: "250px" }}>
                <h4 style={{ margin: "0 0 8px 0", fontSize: "0.95em", color: "var(--text-secondary)" }}>Latest Fleet Metrics</h4>
                <div style={{ marginBottom: "4px", fontSize: "0.9em" }}><strong style={{ width: "180px", display: "inline-block" }}>Nodes Evaluated:</strong> {fleetMetrics.fleetMetrics.totalNodesEvaluated}</div>
                <div style={{ marginBottom: "4px", fontSize: "0.9em" }}><strong style={{ width: "180px", display: "inline-block" }}>Responsive Ratio:</strong> {(fleetMetrics.fleetMetrics.responsiveRatio * 100).toFixed(1)}%</div>
                <div style={{ marginBottom: "4px", fontSize: "0.9em" }}><strong style={{ width: "180px", display: "inline-block" }}>Battery &gt; 50% Ratio:</strong> {(fleetMetrics.fleetMetrics.batteryAbove50Ratio * 100).toFixed(1)}%</div>
                <div style={{ fontSize: "0.9em" }}><strong style={{ width: "180px", display: "inline-block" }}>Avg Last Seen:</strong> {fleetMetrics.fleetMetrics.avgSecondsSinceLastSeen.toFixed(1)}s</div>
              </div>
            )}
          </div>
        </div>
      )}

      <div className="table-shell">
        <table className="scada-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Type</th>
              <th>Status</th>
              <th>Battery</th>
              <th>Last Seen</th>
              <th>UUID</th>
            </tr>
          </thead>
          <tbody>
            {visibleNodes.map((node) => (
              <tr key={node.uuid}>
                <td className="node-name">{node.name}</td>
                <td>{node.type}</td>
                <td>
                  <StatusBadge status={node.status} />
                </td>
                <td>
                  <div className="battery-cell">
                    <div className="battery-bar-container">
                      <div
                        className={`battery-bar ${batteryColor(node.batteryLevel)}`}
                        style={{ width: `${Math.min(100, Math.max(0, node.batteryLevel))}%` }}
                      />
                      <span className="battery-text">{node.batteryLevel.toFixed(0)}%</span>
                    </div>
                  </div>
                </td>
                <td className="timestamp">{formatDateTime(node.lastSeen)}</td>
                <td className="uuid">{node.uuid}</td>
              </tr>
            ))}
            {nodes.length === 0 && (
              <tr>
                <td colSpan={6} className="empty-row">
                  No nodes registered
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="pagination-controls" aria-label="Node list pagination">
        <button type="button" onClick={() => setPage(1)} disabled={currentPage === 1}>
          First
        </button>
        <button type="button" onClick={() => setPage((value) => Math.max(1, value - 1))} disabled={currentPage === 1}>
          Previous
        </button>
        <span className="pagination-page">
          Page {currentPage} of {pageCount}
        </span>
        <button
          type="button"
          onClick={() => setPage((value) => Math.min(pageCount, value + 1))}
          disabled={currentPage === pageCount}
        >
          Next
        </button>
        <button type="button" onClick={() => setPage(pageCount)} disabled={currentPage === pageCount}>
          Last
        </button>
      </div>
    </div>
  );
}

function batteryColor(level: number): string {
  if (level > 50) return "battery-green";
  if (level > 20) return "battery-amber";
  return "battery-red";
}
