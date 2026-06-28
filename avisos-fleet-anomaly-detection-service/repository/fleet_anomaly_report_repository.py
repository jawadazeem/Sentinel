from entity.entities import FleetAnomalyReportEntity
from sqlalchemy.orm import Session
from sqlalchemy import select, insert

class FleetAnomalyReportRepository:
    def __init__(self, db: Session):
        self.db = db
    
    def get_by_id(self, id: int) -> FleetAnomalyReportEntity | None:
        return self.db.get(FleetAnomalyReportEntity, id)
    
    def get_latest(self) -> FleetAnomalyReportEntity | None:
        stmt = select(FleetAnomalyReportEntity).order_by(FleetAnomalyReportEntity.created_at.desc())
        return self.db.scalars(stmt).first()
    
    def save(self, report: FleetAnomalyReportEntity):
        self.db.add(report)
        self.db.commit()
        self.db.refresh(report)