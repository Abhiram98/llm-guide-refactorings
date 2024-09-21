import git
import os
import json
from collections import defaultdict
from mm_analyser.env import PROJECTS_BASE_PATH
from mm_analyser import data_folder, resources_folder

project_name = "springboot"
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
refminer_filtered_file = f"{data_folder}/refminer_data/filter_fp/{project_name}_res.json"
repo = git.Repo(os.path.join(project_basepath, project_basepath_map.get(project_name)))

with open(refminer_filtered_file) as f:
    refdata = json.load(f)

write_data = []
duplication_counter = set()
for ref in refdata:
    parent_commit = repo.commit(ref['sha1']).parents[0]
    file_path = ref['move_method_refactoring']['leftSideLocations'][0]['filePath']
    if (parent_commit, file_path) in duplication_counter:
        continue
    write_data.append({
        "file_path": file_path,
        "commit_hash": parent_commit.hexsha,
        "new_commit_hash": ref['sha1']
    })
    duplication_counter.add((parent_commit, file_path))

with open(f"{resources_folder}/plugin_input_files/classes_and_commits.json", "w") as f:
    json.dump(write_data, f, indent=4)





