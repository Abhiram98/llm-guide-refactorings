import json
import os
import re
import traceback

from MethodSignature import MethodSignature
from MoveMethodValidator import MoveMethodValidator
from mm_analyser import data_folder
from mm_analyser.env import PROJECTS_BASE_PATH, PROJECT_ALIAS_MAP
count = 0

def is_tp_extractmovemethod(refactoring):
    original_class = refactoring['description'].split(' & moved to class')[0].split('.')[-1]
    right_code_element = \
        [i for i in refactoring['rightSideLocations'] if i['description'] == "extracted method declaration"] \
            [0]['codeElement'].split(":")[0]
    return re.search(r'\b' + re.escape(original_class) + r'\b', right_code_element) is not None





def filter_fp_moves(data, project_path):
    tp_moves = []
    for i, commit in enumerate(data['commits']):
        print(f"processing commit ({i}/{len(data['commits'])})")
        print(f"{len(tp_moves)=}")
        try:
            tp_moves += MoveMethodValidator(commit, project_path).get_valid_moves()
        except Exception as e:
            print(f"failed to process commit: {e}")
            print(traceback.format_exc())
        if i%100==0:
            with open(os.path.join(filtered_path, fname), "w") as f:
                json.dump(tp_moves, f, indent=4)

    return tp_moves


if __name__ == '__main__':
    filtered_path = data_folder.joinpath("refminer_data/filter_fp")
    mm_path = data_folder.joinpath("refminer_data/contains_a_mm")
    files = [i for i in os.listdir(mm_path) if i.endswith(".json")]
    for fname in files:
        print(fname)
        project_name = PROJECT_ALIAS_MAP.get(fname)
        if project_name=='' or project_name is None:
            print(f"skipping {fname}")
            continue

        with open(os.path.join(mm_path, fname)) as f:
            move_data = json.load(f)
        filtered_moves = filter_fp_moves(move_data, os.path.join(PROJECTS_BASE_PATH, project_name))
        with open(os.path.join(filtered_path, fname), "w") as f:
            json.dump(filtered_moves, f, indent=4)
