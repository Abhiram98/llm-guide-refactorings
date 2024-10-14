import git
import os
import json
from collections import defaultdict
from mm_analyser.env import PROJECTS_BASE_PATH
from mm_analyser import data_folder, resources_folder
from mm_analyser.refactoring_miner_processing.automation_helpers.AutmationHelpers import EmmHelper, MmHelper
from mm_analyser.refactoring_miner_processing.filter.ExtractMoveMethodValidator import ExtractMoveMethodRef

project_name = "vue_pro"
project_basepath_map = {
        'vue_pro': 'ruoyi-vue-pro',
        'flink': 'flink',
        'halo': 'halo',
        'elastic': 'elasticsearch',
        'redisson': 'redisson',
        'spring_framework': 'spring-framework',
        'springboot': 'spring-boot',
        'stirling': 'Stirling-PDF',
        'selenium': 'selenium',
        'ghidra': 'ghidra',
        'dbeaver': 'dbeaver',
        'kafka': 'kafka',
        "graal": 'graal',
        'dataease': 'dataease'
    }

project_basepath = str(PROJECTS_BASE_PATH)
helper = EmmHelper()
# helper = MmHelper(os.path.join(project_basepath, project_basepath_map.get(project_name)))

# filter_dir = "filter_emm" if emm_file else "filter_fp"
filter_dir = helper.directory
refminer_filtered_file = f"{data_folder}/refminer_data/{filter_dir}/{project_name}_res.json"
repo = git.Repo(os.path.join(project_basepath, project_basepath_map.get(project_name)))

with open(refminer_filtered_file) as f:
    refdata = json.load(f)

write_data = []
duplication_counter = set()
for ref in refdata:
    if not helper.include_static and ref['move_method_refactoring']['isStatic']:
        continue

    parent_commit = helper.get_parent_commit(ref)
    if parent_commit is None:
        continue
    file_path = ref['move_method_refactoring']['leftSideLocations'][0]['filePath']
    emm_obj = ExtractMoveMethodRef.create_from(ref['move_method_refactoring'])
    full_class_name = emm_obj.original_class
    class_name = full_class_name.split(".")[-1]
    if (parent_commit, file_path) in duplication_counter:
        continue
    write_data.append({
        "file_path": file_path,
        "class_name": class_name,
        "commit_hash": parent_commit,
        "new_commit_hash": ref['sha1']
    })
    duplication_counter.add((parent_commit, file_path))

with open(f"{data_folder}/plugin_input_files/classes_and_commits.json", "w") as f:
    json.dump(write_data, f, indent=4)





