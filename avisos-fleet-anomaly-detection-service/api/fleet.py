import requests
from fastapi import APIRouter, Depends, HTTPException, status, BackgroundTasks
from pydantic import BaseModel, Field
from model.models import FleetMetricRecord, FleetAnomalyReport
from service.anomaly_detection_service import AnomalyDetectionService
from dependencies import get_anomaly_service
from datetime import datetime

import os

fleet_router = APIRouter()
JAVA_CALLBACK_URL = os.getenv("JAVA_CALLBACK_URL", "http://controller:8083/api/fleet-metrics/anomaly-report/latest")

def run_analysis_and_callback(
        payload: FleetMetricRecord,
        service: AnomalyDetectionService
        ):
    try:
        result = service.detect_anomalies(payload.fleet_metrics)
        response = requests.put(JAVA_CALLBACK_URL, json=result.to_json())
        response.raise_for_status()
    except requests.exceptions.RequestException as e:
        print(f"Failed to callback to Java: {e}")

@fleet_router.post("/fleet-health", status_code=status.HTTP_201_CREATED)
def ingest(
    payload: FleetMetricRecord,
    background_tasks: BackgroundTasks,
    service: AnomalyDetectionService = Depends(get_anomaly_service)
):
    background_tasks.add_task(run_analysis_and_callback, payload, service)
    return {"message": "Analysis accepted"}