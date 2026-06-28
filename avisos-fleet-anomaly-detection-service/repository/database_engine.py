from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

fleet_engine = create_engine("postgresql+psycopg2://avisos:avisos@postgres:5432/avisos_anomaly_detection")
FleetSession = sessionmaker(bind=fleet_engine)