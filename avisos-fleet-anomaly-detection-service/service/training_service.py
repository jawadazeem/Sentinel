from sklearn.ensemble import IsolationForest
from sklearn.model_selection import train_test_split
import io
from pathlib import Path
import pandas as pd
import joblib

class TrainingService:
    def get_training_data_as_dataframe(self) -> pd.DataFrame:
        csv_path = Path("data/fleet_training_data.csv")

        if not csv_path.exists():
            raise FileNotFoundError(f"Fatal Error: '{csv_path}' was not found. This file is required for training.")
        
        return pd.read_csv(csv_path)
    
    def train_ml_model(self) -> Path:
        """Trains the model on clean data and saves it to disk.
        
        Returns:
            Path: The filesystem path to the saved joblib asset.
        """
        df = self.get_training_data_as_dataframe()
        X = df.values
        
        clf = IsolationForest(contamination="auto", random_state=42)
        clf.fit(X)
        predictions = clf.predict(X)
        path = Path("ml_model/fleet_isolation_forest.joblib")
        joblib.dump(clf, path)
        
        print("Successfully trained and dumped ML model")
        return path