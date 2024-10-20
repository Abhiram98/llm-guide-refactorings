import json
import os
import re
import traceback
from typing import Union

from MoveMethodValidator import MoveMethodValidator
from ExtractMoveMethodValidator import ExtractMoveMethodValidator
from mm_analyser import data_folder
from mm_analyser.env import PROJECTS_BASE_PATH, PROJECT_ALIAS_MAP

count = 0


class Processor:
    def __init__(self, data, project_path, refactoring_type):
        self.data = data
        self.project_path = project_path
        self.validator: Union[MoveMethodValidator, ExtractMoveMethodValidator]
        if refactoring_type == MoveMethodValidator.type:
            self.validator = MoveMethodValidator
        elif refactoring_type == ExtractMoveMethodValidator.type:
            self.validator = ExtractMoveMethodValidator

    def filter_fp_moves(self):
        tp_moves = []
        for i, commit in enumerate(self.data['commits']):
            print(f"processing commit ({i}/{len(self.data['commits'])})")
            print(f"{len(tp_moves)=}")
            try:
                tp_moves += self.validator(commit, self.project_path).get_valid_moves()
            except Exception as e:
                print(f"failed to process commit: {e}")
                print(traceback.format_exc())
            if i % 100 == 0:
                with open(os.path.join(filtered_path, fname), "w") as f:
                    json.dump(tp_moves, f, indent=4)

        return tp_moves

    def count_refactorings(self):
        count = 0
        for commit in self.data['commits']:
            for ref in commit['refactorings']:
                if ref['type'] == self.validator.type:
                    count += 1
        return count


if __name__ == '__main__':
    refactoring_type = ExtractMoveMethodValidator.type

    filtered_path = data_folder.joinpath("refminer_data/filter_emm")
    mm_path = data_folder.joinpath("refminer_data/contains_a_mm")
    files = [i for i in os.listdir(mm_path) if i.endswith(".json")]
    SELECTED_PROJECTS = [
        'vue_pro_res.json',
        'flink_res.json',
        'halo_res.json',
        'elastic_res.json',
        'graal_res.json',
        'kafka_res.json',
        'redisson_res.json',
        'spring_framework_res.json',
        'springboot_res.json',
        'stirling_res.json',
        'selenium_res.json',
        'ghidra_res.json',
        'dbeaver_res.json',
        'dataease_res.json'
    ]
    for fname in files:
        print(fname)
        project_name = PROJECT_ALIAS_MAP.get(fname)
        if project_name == '' or project_name is None or fname not in SELECTED_PROJECTS:
            print(f"skipping {fname}")
            continue

        with open(os.path.join(mm_path, fname)) as f:
            move_data = json.load(f)
        processor = Processor(move_data, os.path.join(PROJECTS_BASE_PATH, project_name), refactoring_type)
        print(f"Project {project_name} has {processor.count_refactorings()} {refactoring_type} refactorings.")
        filtered_moves = processor.filter_fp_moves()
        with open(os.path.join(filtered_path, fname), "w") as f:
            json.dump(filtered_moves, f, indent=4)
