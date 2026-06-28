from pydantic import BaseModel, Field
from datetime import datetime, timezone

# DTOs
class FleetMetrics(BaseModel):
    total_nodes_evaluated: int = Field(alias="totalNodesEvaluated")
    responsive_ratio: float = Field(alias="responsiveRatio")
    battery_above_50_ratio: float = Field(alias="batteryAbove50Ratio")
    avg_seconds_since_last_seen: float = Field(alias="avgSecondsSinceLastSeen")

    model_config = {"populate_by_name": True}

class FleetMetricRecord(BaseModel):
    timestamp: datetime
    fleet_metrics: FleetMetrics = Field(alias="fleetMetrics")

    model_config = {"populate_by_name": True}

class FleetAnomalyReport(BaseModel):
    id: int
    timestamp: datetime
    is_anomaly: bool = Field(alias="isAnomaly")
    diagnostic_reason: str | None = Field(alias="diagnosticReason")
    raw_payload: FleetMetrics = Field(alias="rawPayload")

    model_config = {"populate_by_name": True}

    def to_json(self):
        data = self.model_dump(mode='json', by_alias=True)
        # Force the timestamp to UTC and append 'Z'
        if isinstance(self.timestamp, datetime):
            data['timestamp'] = self.timestamp.replace(tzinfo=timezone.utc).isoformat().replace('+00:00', 'Z')
        return data