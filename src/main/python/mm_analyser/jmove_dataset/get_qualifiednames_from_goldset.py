import json
import os
import sys
from mm_analyser import data_folder

# project_name = "drjava"
# size = "large"
# temperature = "1_temp"
print(sys.argv)
project_name = sys.argv[1]
size = sys.argv[2]
temperature = sys.argv[3]

gold_file = f"/Users/abhiram/Documents/TBE/jmove/dataset-tse/gold_sets/{project_name}/{size}.txt"
dest_file_name = f"comparison_{project_name}_{size}"
dest_dir = f"/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/synthetic_corpus_comparison/temp_{str(temperature).replace('.', '_')}"
try:
    os.makedirs(dest_dir)
except FileExistsError:
    pass
dest_file = f"{dest_dir}/{dest_file_name}.json"
with open(gold_file) as f:
    ant_small = f.read()

class_names = []
for i in ant_small.split("\n"):
    if i=='':
        continue
    class_names.append(i.split('::')[0].split(' ')[-1])

print("\n".join(class_names))
with open(f"{data_folder}/qualified_classes_{project_name}_{size}.txt", "w") as f:
    f.write("\n".join(class_names))


telemetry_file = '/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/build/idea-sandbox/system/log/ref_plugin_logs/ref_telemetry_data.jsonl'
with open(telemetry_file) as f:
    telemetry = [json.loads(i) for i in f.read().split('\n') if i!=''][-len(class_names):]
data = []
for class_name, tele, oracle in zip(class_names, telemetry, ant_small.split('\n')):
    operated_class_name = tele['hostFunctionTelemetryData']['filePath'].split('.java')[0].split('/')[-1]
    print(f"{operated_class_name=}")
    print(f"{class_name=}")
    assert operated_class_name in class_name
    data.append(
        {
            "oracle": oracle,
            "class_name": class_name,
            "telemetry": tele
        }
    )

with open(dest_file, "w") as f:
    json.dump(data, f, indent=4)