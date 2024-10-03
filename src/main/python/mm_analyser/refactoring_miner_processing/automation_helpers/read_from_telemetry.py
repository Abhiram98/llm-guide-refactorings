import json
import sys
from mm_analyser import data_folder, resources_folder
from mm_analyser.env import TELEMETRY_FILE_PATH
from mm_analyser.env import TELEMETRY_FILE_PATH_BASE
import argparse
import os 

# Add argument parsing
parser = argparse.ArgumentParser(description='Process project name.')
parser.add_argument('--project_name', type=str, required=True, help='Name of the project')
parser.add_argument('--mm_assist_outfile_base', type=str, required=True, help='Name of the project')
args = parser.parse_args()

try:
    os.makedirs(os.path.dirname(f'{data_folder}/refminer_data/{args.mm_assist_outfile_base}/'), exist_ok=True)
    print(f"Successfully created directory: {args.mm_assist_outfile_base}")
except Exception as e:
    print(f"Error creating directory: {e}")

refminer_filtered_file = f"{data_folder}/refminer_data/filter_fp/{args.project_name}_res.json"
mm_assist_outfile = f"{data_folder}/refminer_data/{args.mm_assist_outfile_base}/{args.project_name}_res.json"

with open(f"{resources_folder}/plugin_input_files/classes_and_commits.json") as f:
    mm_assist_runs = json.load(f)

with open(refminer_filtered_file) as f:
    refdata = json.load(f)

with open(f"{TELEMETRY_FILE_PATH_BASE}/ref_telemetry_data_{args.project_name}.jsonl") as f:
    telemetry = [json.loads(i) for i in f.read().split('\n') if i!=''][-len(mm_assist_runs):]

# print(f"Length of refdata: {len(refdata)}")
# print(f"Length of mm_assist_runs: {len(mm_assist_runs)}")
# print(f"Length of telemetry: {len(telemetry)}")

for ref in refdata:
    matches = [1 if i['new_commit_hash']==ref['sha1'] and i['file_path']==ref['move_method_refactoring']['leftSideLocations'][0]['filePath'] else 0 for i in mm_assist_runs]
    
    try:
        index = matches.index(1)
        if index < len(telemetry):
            ref["telemetry"] = telemetry[index]
        else:
            print(f"Warning: Telemetry index {index} out of range. Skipping this entry.")
    except ValueError:
        print(f"Warning: No matching entry found for commit {ref['sha1']}. Skipping this entry.")
        continue

for ref in refdata:
    if 'telemetry' in ref:
        assert ref['telemetry']['hostFunctionTelemetryData']['filePath'].endswith(
            ref['move_method_refactoring']['leftSideLocations'][0]['filePath']
        )

with open(mm_assist_outfile, "w") as f:
    json.dump(refdata, f, indent=4)