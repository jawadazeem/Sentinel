from repository.fleet_anomaly_report_repository import FleetAnomalyReportRepository
from model.models import FleetAnomalyReport, FleetMetrics
from entity.entities import FleetAnomalyReportEntity
from datetime import datetime
import pandas as pd
from util.model_loader import MODEL
import uuid
import time
from datetime import datetime

class AnomalyDetectionService:
    def __init__(self, repository: FleetAnomalyReportRepository):
        self.repository = repository
        self.ml_model = MODEL

    def detect_anomalies(self, fleet_metrics: FleetMetrics) -> FleetAnomalyReport:
        df = pd.DataFrame([{
            "totalNodesEvaluated": fleet_metrics.total_nodes_evaluated,
            "responsiveRatio": fleet_metrics.responsive_ratio,
            "batteryAbove50Ratio": fleet_metrics.battery_above_50_ratio,
            "avgSecondsSinceLastSeen": fleet_metrics.avg_seconds_since_last_seen,
        }])

        detect = self.ml_model.predict(df.values)
        is_anomaly = bool(detect[0] < 0)

        # For now, there is no way to determine the reason of the anomaly.
        # This is something to add.
        fleet_anomaly_report = FleetAnomalyReportEntity(
            timestamp=datetime.now(),
            is_anomaly=is_anomaly,
            diagnostic_reason=None,
            raw_payload=fleet_metrics.model_dump()
        )

        self.repository.save(fleet_anomaly_report)
        
        report_dto = FleetAnomalyReport(
            id=str(fleet_anomaly_report.id),
            timestamp=fleet_anomaly_report.timestamp,
            is_anomaly=fleet_anomaly_report.is_anomaly,
            diagnostic_reason=fleet_anomaly_report.diagnostic_reason,
            raw_payload=fleet_anomaly_report.raw_payload
        )

        print(report_dto)
        return report_dto