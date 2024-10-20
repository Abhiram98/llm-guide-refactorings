import json
import git

from mm_analyser.env import PROJECT_ALIAS_MAP, PROJECTS_BASE_PATH
from mm_analyser import data_folder
from mm_analyser.refactoring_miner_processing.filter.ExtractMoveMethodValidator import ExtractMoveMethodRef


plugin_outfile_path = f"{data_folder}/plugin_output/extraction_results.json"
with open(plugin_outfile_path) as f:
    extraction_results = json.load(f)



PROJECT_ALIAS_MAP_FLIPPED = {v:k for k,v in PROJECT_ALIAS_MAP.items()}
project_name = "ruoyi-vue-pro"
emm_data_path = f"{data_folder}/refminer_data/filter_emm/{PROJECT_ALIAS_MAP_FLIPPED[project_name]}"
repo = git.Repo(f"{PROJECTS_BASE_PATH}/{project_name}")
with open(emm_data_path) as f:
    emm_data = json.load(f)

for data in emm_data:
    if str(data['ref_id']) in extraction_results:
        data['extraction_results'] = extraction_results[str(data['ref_id'])]

with open(emm_data_path, "w") as f:
    json.dump(emm_data, f, indent=4)