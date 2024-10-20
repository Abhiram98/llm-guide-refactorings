import os
import json
from mm_analyser import data_folder


rminer_data_path = f"{data_folder}/refminer_data/filter_emm"
files = os.listdir(rminer_data_path)
json_files = [i for i in files if i.endswith(".json")]

data = []
oracle_count = 0
instance_moves_stats = {}

for fname in json_files:
    with open(os.path.join(rminer_data_path, fname)) as f:
        move_data = json.load(f)
        instance_moves = [i for i in move_data if not i['move_method_refactoring']['isStatic']]
    oracle_count += len(move_data)
    instance_moves_stats[fname] = instance_moves

print(f"{oracle_count=}")
per_project_numbers = {k:len(v) for k,v in instance_moves_stats.items()}
print(f"{per_project_numbers=}")
print(f"total={sum(per_project_numbers.values())}")
all_moves = []
for data in instance_moves_stats.values():
    all_moves += [i['move_method_refactoring']['description'].split('extracted from')[0] for i in data]
all_moves_per_file = []
for data in instance_moves_stats.values():
    all_moves_per_file += [
        i['move_method_refactoring']['description'].split('extracted from')[0] +
        i['move_method_refactoring']['leftSideLocations'][0]['filePath']
        for i in data]
print(f"{len(all_moves)=}")
print(f"{len(set(all_moves))=}")
print(f"{len(set(all_moves_per_file))=}")