import json
from mm_analyser import data_folder, resources_folder
from mm_analyser.env import TELEMETRY_FILE_PATH
from mm_analyser.refactoring_miner_processing.automation_helpers.AutmationHelpers import MmHelper, EmmHelper

project_name = "kafka"
# helper = MmHelper(project_name)
helper = EmmHelper()
refminer_filtered_file = f"{data_folder}/refminer_data/{helper.directory}/{project_name}_res.json"
mm_assist_outfile = f"{data_folder}/refminer_data/{helper.outdir}/{project_name}_res.json"
with open(f"{data_folder}/plugin_input_files/classes_and_commits.json") as f:
    mm_assist_runs = json.load(f)

with open(refminer_filtered_file) as f:
    refdata = json.load(f)

with open(TELEMETRY_FILE_PATH) as f:
    telemetry = [json.loads(i) for i in f.read().split('\n') if i!=''][-len(mm_assist_runs):]

# print(f"Length of refdata: {len(refdata)}")
# print(f"Length of mm_assist_runs: {len(mm_assist_runs)}")
# print(f"Length of telemetry: {len(telemetry)}")

for ref in refdata:
    if not ref['extraction_results']['success']:
        continue
    matches = [1 if i['new_commit_hash'] == ref['sha1'] and i['file_path'] ==
                    ref['move_method_refactoring']['leftSideLocations'][0]['filePath'] and
                    ref['extraction_results']['newCommitHash'] == i['commit_hash']
               else 0 for i in mm_assist_runs]
    try:
        index = matches.index(1)
    except:
        continue
    ref["telemetry"] = telemetry[index]

for ref in refdata:
    if 'telemetry' in ref:
        assert ref['telemetry']['hostFunctionTelemetryData']['filePath'].endswith(
            ref['move_method_refactoring']['leftSideLocations'][0]['filePath']
        )

with open(mm_assist_outfile, "w") as f:
    json.dump(refdata, f, indent=4)