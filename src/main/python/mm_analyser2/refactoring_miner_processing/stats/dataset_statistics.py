import os
import json
from mm_analyser import data_folder


rminer_data_path = f"{data_folder}/refminer_data/contains_a_mm"
files = os.listdir(rminer_data_path)
json_files = [i for i in files if i.endswith(".json")]

data = []
oracle_count = 0

for fname in json_files:
    with open(os.path.join(rminer_data_path, fname)) as f:
        move_data = json.load(f)
    oracle_count += len(move_data)

print(f"{oracle_count=}")
