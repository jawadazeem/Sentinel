from sqlalchemy.orm import Session
from fastapi import Depends
from repository.database_engine import FleetSession
from repository.fleet_anomaly_report_repository import FleetAnomalyReportRepository
from service.anomaly_detection_service import AnomalyDetectionService

def get_db():
    db = FleetSession()
    try:
        yield db
    finally:
        db.close()

def get_fleet_repository(db: Session = Depends(get_db)):
    return FleetAnomalyReportRepository(db)

def get_anomaly_service(
    repo: FleetAnomalyReportRepository = Depends(get_fleet_repository)
):
    return AnomalyDetectionService(repo)
