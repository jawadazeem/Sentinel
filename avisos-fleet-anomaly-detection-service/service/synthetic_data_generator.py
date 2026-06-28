from datetime import datetime, timedelta
import numpy as np
import pandas as pd

num_records = 100000
start_time = datetime.now()

np.random.seed(42)

total_nodes = np.full(shape=num_records, fill_value=100)

responsive_ratio = np.random.uniform(
    low=1, high=1.00, size=num_records
)
battery_ratio = np.random.uniform(
    low=50, high=92, size=num_records
)
avg_seconds = np.random.uniform(
    low=30, high=45, size=num_records
)

data = {
    "totalNodesEvaluated": total_nodes,
    "responsiveRatio": np.round(responsive_ratio, 0),
    "batteryAbove50Ratio": np.round(battery_ratio, 0),
    "avgSecondsSinceLastSeen": np.round(avg_seconds, 2),
}

df = pd.DataFrame(data)
df.to_csv("data/fleet_training_data.csv", index=False)

print("Dataset successfully generated! Preview:")
print(df.head(3))
