import json
from mm_analyser import data_folder, resources_folder
from mm_analyser.env import TELEMETRY_FILE_PATH

project_name = "springboot"
refminer_filtered_file = f"{data_folder}/refminer_data/filter_fp/{project_name}_res.json"
mm_assist_outfile = f"{data_folder}/refminer_data/mm-assist/{project_name}_res.json"
with open(f"{resources_folder}/plugin_input_files/classes_and_commits.json") as f:
    mm_assist_runs = json.load(f)

with open(refminer_filtered_file) as f:
    refdata = json.load(f)

with open(TELEMETRY_FILE_PATH) as f:
    telemetry = [json.loads(i) for i in f.read().split('\n') if i!=''][-len(mm_assist_runs):]
for ref in refdata:
    matches = [1 if i['new_commit_hash']==ref['sha1'] and i['file_path']==ref['move_method_refactoring']['leftSideLocations'][0]['filePath'] else 0 for i in mm_assist_runs ]
    try:
        index = matches.index(1)
    except:
        break
    ref["telemetry"] = telemetry[index]

for ref in refdata:
    if 'telemetry' in ref:
        assert ref['telemetry']['hostFunctionTelemetryData']['filePath'].endswith(
            ref['move_method_refactoring']['leftSideLocations'][0]['filePath']
        )

with open(mm_assist_outfile, "w") as f:
    json.dump(refdata, f, indent=4)