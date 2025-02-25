import json
import os
from collections import defaultdict

import mm_analyser
import mm_analyser.hmove.compute_input_synthetic as hmove_computer
import mm_analyser.jmove_dataset.oracle as jmove_oracle
import mm_analyser.refactoring_miner_processing.oracle as rw_oracle

def dedup_synthetic():
    print("validating synthetic data")
    hmove_input_folder = mm_analyser.data_folder.joinpath('synthetic_corpus_comparison/hmove/input')
    json_files = [i for i in os.listdir(hmove_input_folder) if i.endswith('.json')]

    inner_target_class_count = 0

    for filename in json_files:
        with open(hmove_input_folder.joinpath(filename)) as f:
            hmove_input_data = json.load(f)

        new_input_data = defaultdict(list)

        for oracle_key in hmove_input_data:
            seen_files = set()
            for data in hmove_input_data[oracle_key]:
                in_data = hmove_computer.HMoveInput(**data)
                if (in_data.method_information.method_name_start_line,
                    in_data.source_class_path,
                    in_data.target_class_path) in seen_files:
                    continue
                else:
                    seen_files.add((in_data.method_information.method_name_start_line,
                                    in_data.source_class_path,
                                    in_data.target_class_path))
                    new_input_data[oracle_key].append(data)

        with open(hmove_input_folder.joinpath(filename), "w") as f:
            json.dump(new_input_data, f, indent=4)

    print(f"{inner_target_class_count=}")
    print("Deduplication complete")


def dedup_rw():
    print("removing duplicates from real world data")
    hmove_input_folder = mm_analyser.data_folder.joinpath('refminer_data/hmove/input')
    json_files = [i for i in os.listdir(hmove_input_folder) if i.endswith('.json')]

    inner_target_class_count = 0

    for filename in json_files:
        with open(hmove_input_folder.joinpath(filename)) as f:
            hmove_input_data = json.load(f)

        new_input_data = defaultdict(list)

        for oracle_key in hmove_input_data:
            seen_files = set()
            for data in hmove_input_data[oracle_key]:
                in_data = hmove_computer.HMoveInput(**data)
                if (in_data.method_information.method_name_start_line,
                    in_data.source_class_path,
                    in_data.target_class_path) in seen_files:
                    continue
                else:
                    seen_files.add((in_data.method_information.method_name_start_line,
                                    in_data.source_class_path,
                                    in_data.target_class_path))
                    new_input_data[oracle_key].append(data)

        with open(hmove_input_folder.joinpath(filename), "w") as f:
            json.dump(new_input_data, f, indent=4)

    print(f"{inner_target_class_count=}")
    print("Deduplication complete")

if __name__ == '__main__':
    # dedup_synthetic()
    dedup_rw()
