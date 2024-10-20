import json
from collections import defaultdict

with open("v1_dataset.json") as f:
    v1_dataset = json.load(f)

data_by_project = defaultdict(list)

for ref in v1_dataset:
    pname = ref['project_name']
    data_by_project[pname].append(ref)

for project_name in data_by_project:
    pname_ = project_name.replace('/','_')
    with open(f"v1_dataset_{pname_}.json", "w") as f:
        json.dump(data_by_project[project_name], f, indent=4)