import json
import os
from collections import defaultdict

import mm_analyser
import mm_analyser.hmove.compute_input_synthetic as hmove_computer
import mm_analyser.refactoring_miner_processing.oracle as rw_oracle


def add_branch_data():
    CHUNK_SIZE = 200
    print(f"Chunking hmove input into size of {CHUNK_SIZE}.")
    hmove_input_folder = mm_analyser.data_folder.joinpath('refminer_data/hmove/input')
    json_files = [i for i in os.listdir(hmove_input_folder) if i.endswith('.json')]
    real_world_oracle = rw_oracle.get_instance_oracle()


    for filename in json_files:
        with open(hmove_input_folder.joinpath(filename)) as f:
            hmove_input_data = json.load(f)

        for ref_id in hmove_input_data:
            ref_id_int = int(ref_id)
            oracle_matches = [data for data in real_world_oracle
                              if data.ref_id == ref_id_int]

            assert len(oracle_matches) == 1
            oracle = oracle_matches[0]

            for hmove_in in hmove_input_data[ref_id]:
                hmove_in['branch_name'] = oracle.project_branch_name

        with open(hmove_input_folder.joinpath(filename), "w") as f:
            json.dump(hmove_input_data, f, indent=4)

    print("Completed adding branch data.")

if __name__ == '__main__':
    add_branch_data()
