# Avisos Fleet Anomaly Detection Service

Small Python/FastAPI service that analyzes fleet-wide node metrics for unusual behavior. In the AVISOS demo architecture, the Java controller owns orchestration and persistence for the operator experience, while this service focuses on the machine-learning concern: accepting fleet health metrics, running a scikit-learn Isolation Forest model, and sending the resulting anomaly report back to the controller.

This service exists to keep ML experimentation out of the controller service without making the overall system heavy. It is intentionally practical and modest: FastAPI for the HTTP boundary, Pydantic for request/response models, SQLAlchemy for local persistence, and joblib/scikit-learn for model loading and inference.

## Responsibilities

- Accept fleet metric payloads at `POST /fleet-health`.
- Run anomaly detection over the submitted fleet metrics.
- Callback to the controller with the latest anomaly report.
- Support local model training and synthetic data generation for demos.

## Local Notes

Install dependencies from `requirements.txt`, then run the service with Uvicorn from this directory:

```bash
uvicorn app:app --reload
```

The controller callback URL defaults to `http://controller:8083/api/fleet-metrics/anomaly-report/latest` and can be overridden with `JAVA_CALLBACK_URL`.

## Project Role

This is not the source of truth for node state. The controller remains responsible for the operator-facing API and dashboard. This service is a supporting ML component used to enrich that view with fleet-level anomaly signals.
