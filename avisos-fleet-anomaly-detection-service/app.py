from fastapi import FastAPI
from api.fleet import fleet_router
from repository.database_engine import fleet_engine
from entity.entities import Base

app = FastAPI(
    title="Fleet Anomaly Detection Service",
    version="1.0.0"
)

app.include_router(fleet_router)

Base.metadata.create_all(bind=fleet_engine)