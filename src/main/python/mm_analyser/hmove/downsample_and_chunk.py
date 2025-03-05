import json
import os
from collections import defaultdict

import mm_analyser
import mm_analyser.hmove.compute_input_synthetic as hmove_computer
import mm_analyser.refactoring_miner_processing.oracle as rw_oracle
import mm_analyser.jmove_dataset.oracle as jmove_oracle

def downsample():
    """If there are more than 500 combinations of input for a data-point, keep only 500 of them."""
    MAX_SIZE = 500
    hmove_input_folder = mm_analyser.data_folder.joinpath('refminer_data/hmove/input')
    json_files = [i for i in os.listdir(hmove_input_folder) if i.endswith('.json')]
    real_world_oracle = rw_oracle.get_instance_oracle()

    for filename in json_files:
        with open(hmove_input_folder.joinpath(filename)) as f:
            hmove_input_data = json.load(f)

        hmove_input_data_new = defaultdict(list)

        for ref_id in hmove_input_data:
            ref_id_int = int(ref_id)
            oracle_matches = [data for data in real_world_oracle
                              if data.ref_id == ref_id_int]

            assert len(oracle_matches) == 1
            oracle = oracle_matches[0]
            if len(hmove_input_data[ref_id]) > MAX_SIZE:
                # perform down sampling.

                oracle_method_inputs = [i for i in
                                        hmove_input_data[ref_id] if
                                        i['method_information']['method_name'] in [oracle.method_name, oracle.alias_method_name]]
                other_inputs = [i for i in
                                        hmove_input_data[ref_id] if
                                        i['method_information']['method_name'] not in [oracle.method_name, oracle.alias_method_name]]
                remaining_count = MAX_SIZE - len(oracle_method_inputs)

                hmove_input_data_new[ref_id] = oracle_method_inputs + other_inputs[:remaining_count]
            else:
                hmove_input_data_new[ref_id] = hmove_input_data[ref_id]



        with open(hmove_input_folder.joinpath(filename), "w") as f:
            json.dump(hmove_input_data_new, f, indent=4)
    print("Completed adding branch data.")


def chunk_data():
    CHUNK_SIZE = 200
    print(f"Chunking hmove input into size of {CHUNK_SIZE}.")
    hmove_input_folder = mm_analyser.data_folder.joinpath('refminer_data/hmove/input')
    json_files = [i for i in os.listdir(hmove_input_folder) if i.endswith('.json')]
    real_world_oracle = rw_oracle.get_instance_oracle()

    for filename in json_files:
        with open(hmove_input_folder.joinpath(filename)) as f:
            hmove_input_data = json.load(f)

        file_counter = 0
        counter = 0
        hmove_input_data_new = defaultdict(list)
        for ref_id in hmove_input_data:
            for hmove_in in hmove_input_data[ref_id]:
                counter += 1
                hmove_input_data_new[ref_id].append(hmove_in)

                if counter % CHUNK_SIZE == 0:
                    # write to new file.
                    file_counter += 1
                    filename_ = filename.split('.json')[0]
                    with open(hmove_input_folder
                                      .joinpath('chunked')
                                      .joinpath(f"{filename_}-{file_counter}.json"), "w") as f:
                        json.dump(hmove_input_data_new, f, indent=4)

                    hmove_input_data_new = defaultdict(list) # reset the data.

        if len(hmove_input_data_new):
            # Saving last chunk
            file_counter += 1
            filename_ = filename.split('.json')[0]
            with open(hmove_input_folder
                              .joinpath('chunked')
                              .joinpath(f"{filename_}-{file_counter}.json"), "w") as f:
                json.dump(hmove_input_data_new, f, indent=4)

    print("Completed chunking the data.")


def downsample_synthetic():
    MAX_SIZE = 500

    print("downsampling synthetic data")
    hmove_input_folder = mm_analyser.data_folder.joinpath('synthetic_corpus_comparison/hmove/input')
    json_files = [i for i in os.listdir(hmove_input_folder) if i.endswith('.json')]

    inner_target_class_count = 0

    for filename in json_files:
        with open(hmove_input_folder.joinpath(filename)) as f:
            hmove_input_data = json.load(f)

        for oracle_key in hmove_input_data:
            source_class_method, target_class = oracle_key.split('->')
            source_class, method_signature = source_class_method.split('::')
            found_oracle = False
            oracle_matches = [data for data in jmove_oracle.oracle_data
                              if data == jmove_oracle.JMoveOracle(
                    source_class=source_class, target_class=target_class, method_signature=method_signature,
                    method_size='?', project_name='?')]

            assert len(oracle_matches) == 1
            oracle = oracle_matches[0]
            inner_target_class_count += 1 if len(
                [i for i in oracle.target_class.split('.') if i[0].isupper()]) > 1 else 0

            if len(hmove_input_data[oracle_key]) > MAX_SIZE:
                print(f"too many combinations of inputs for {oracle_key}")

                oracle_method_inputs = [i for i in
                                        hmove_input_data[oracle_key] if
                                        i['method_information']['method_name'] in [oracle.method_name, oracle.alias_method_name]]
                other_inputs = [i for i in
                                        hmove_input_data[oracle_key] if
                                        i['method_information']['method_name'] not in [oracle.method_name, oracle.alias_method_name]]
                remaining_count = MAX_SIZE - len(oracle_method_inputs)

                hmove_input_data[oracle_key] = oracle_method_inputs + other_inputs[:remaining_count]

                with open(hmove_input_folder.joinpath(filename), "w") as f:
                    json.dump(hmove_input_data, f, indent=4)



if __name__ == '__main__':
    # downsample()
    chunk_data()
    # downsample_synthetic()
