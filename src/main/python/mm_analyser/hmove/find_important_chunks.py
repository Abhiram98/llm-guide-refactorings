import json
import mm_analyser
import os
import mm_analyser.refactoring_miner_processing.oracle as rw_oracle
from collections import defaultdict

def find_important_files():
    """If there are more than 500 combinations of input for a data-point, keep only 500 of them."""
    MAX_SIZE = 500
    hmove_input_folder = mm_analyser.data_folder.joinpath('refminer_data/hmove/input/chunked/chunked')
    json_files = [i for i in os.listdir(hmove_input_folder) if i.endswith('.json')]
    real_world_oracle = rw_oracle.get_instance_oracle()
    important_files = []

    for filename in json_files:
        with open(hmove_input_folder.joinpath(filename)) as f:
            hmove_input_data = json.load(f)

        hmove_input_data_new = defaultdict(list)
        found_important = False
        for ref_id in hmove_input_data:
            ref_id_int = int(ref_id)
            oracle_matches = [data for data in real_world_oracle
                              if data.ref_id == ref_id_int]

            assert len(oracle_matches) == 1
            oracle = oracle_matches[0]

            for hmove_in in hmove_input_data[ref_id]:
                if hmove_in['method_information']['method_name'] in [oracle.method_name, oracle.alias_method_name]:
                    print(f"important file: {filename}")
                    found_important = True
                    important_files.append(filename)

                    with open(hmove_input_folder.parent.joinpath('important').joinpath(filename), "w") as f:
                        json.dump(hmove_input_data, f, indent=4)

                    break

            if found_important:
                break

    print("Completed adding branch data.")
    print(f"{important_files=}")
    print(f"{len(important_files)=}")



if __name__ == '__main__':
    find_important_files()