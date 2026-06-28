from sqlalchemy import BigInteger, DateTime, Boolean, String, JSON
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from datetime import datetime

class Base(DeclarativeBase):
    pass

class FleetAnomalyReportEntity(Base):
    __tablename__ = "fleet_anomaly_reports"
    
    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    timestamp: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    is_anomaly: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    diagnostic_reason: Mapped[str | None] = mapped_column(String(500), nullable=True)
    raw_payload: Mapped[dict] = mapped_column(JSON, nullable=False)

