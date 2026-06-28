from fastapi import APIRouter, status
from pydantic import BaseModel

infra_router = APIRouter(tags=["System Health"])

class HealthCheck(BaseModel):
    status: str = "OK"

@infra_router.get(
    "/health",
    summary="Infrastructure Liveness Probe",
    status_code=status.HTTP_200_OK,
    response_model=HealthCheck
)
def get_health() -> HealthCheck:
    return HealthCheck(status="OK")
