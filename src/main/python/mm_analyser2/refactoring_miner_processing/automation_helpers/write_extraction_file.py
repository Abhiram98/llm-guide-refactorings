import json
import git

from mm_analyser.env import PROJECT_ALIAS_MAP, PROJECTS_BASE_PATH
from mm_analyser import data_folder, resources_folder
from mm_analyser.refactoring_miner_processing.filter.ExtractMoveMethodValidator import ExtractMoveMethodRef


plugin_file_name = "extraction_files_and_ranges.json"
plugin_file_path = f"{data_folder}/plugin_input_files/{plugin_file_name}"

PROJECT_ALIAS_MAP_FLIPPED = {v:k for k,v in PROJECT_ALIAS_MAP.items()}
project_name = "ruoyi-vue-pro"
emm_data_path = f"{data_folder}/refminer_data/filter_emm/{PROJECT_ALIAS_MAP_FLIPPED[project_name]}"
repo = git.Repo(f"{PROJECTS_BASE_PATH}/{project_name}")
with open(emm_data_path) as f:
    emm_data = json.load(f)


file_data = []
one_liners = 0

for mm in emm_data:
    emm_ref = ExtractMoveMethodRef.create_from(mm["move_method_refactoring"])
    extracted_range = emm_ref.extracted_range
    commit_hash = mm['sha1']
    prev_commit = str(repo.commit(commit_hash).parents[0])
    one_liners += emm_ref.extracted_range.start_line == emm_ref.extracted_range.end_line
    file_data.append(
        {
            "ref_id": mm['ref_id'],
            "commit_hash": commit_hash,
            "prev_commit": prev_commit,
            "file_path": emm_ref.left_file_path,
            "method_extracted_from": str(emm_ref.left_signature),
            "extracted_start_line": extracted_range.start_line,
            "extracted_start_column": extracted_range.start_column,
            "extracted_end_line": extracted_range.end_line,
            "extracted_end_column": extracted_range.end_column,
            "extracted_method_name": emm_ref.right_signature.method_name
        }
    )
print(f"{len(file_data)=}")
print(f"{one_liners=}")

with open(plugin_file_path, "w") as f:
    json.dump(file_data, f, indent=4)






