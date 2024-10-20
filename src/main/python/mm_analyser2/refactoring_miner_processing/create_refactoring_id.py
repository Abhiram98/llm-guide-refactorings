import json
import os

from mm_analyser import data_folder

with open(f"{data_folder}/refminer_data/ref_id_map.json") as f:
    ref_id_map = json.load(f)
v=ref_id_map.values()
max_id = max([0] if len(v)==0 else v)

folder = f"{data_folder}/refminer_data/filter_emm"
data_files = [i for i in os.listdir(folder) if i.endswith('.json')]
count = 0
for filename in data_files:
    with open(f"{folder}/{filename}") as f:
        move_method_data = json.load(f)
    for m in move_method_data:
        count += 1
        id = ref_id_map.get(m['move_method_refactoring']['description'])
        if id is None:
            max_id += 1
            id = max_id
            ref_id_map[m['move_method_refactoring']['description']] = id
        m['ref_id'] = id
    with open(f"{folder}/{filename}", "w") as f:
        json.dump(move_method_data, f, indent=4)
print(f"{max_id=}")
print(f"{len(ref_id_map)=}")
print(f"{count=}")

with open(f"{data_folder}/refminer_data/ref_id_map.json", "w") as f:
    json.dump(ref_id_map, f, indent=4)