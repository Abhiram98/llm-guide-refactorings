import json
import os
import numpy as np

import mm_analyser
import mm_analyser.hmove.compute_input_synthetic as hmove_computer
import mm_analyser.jmove_dataset.oracle as jmove_oracle
import mm_analyser.refactoring_miner_processing.oracle as rw_oracle

def check():
    print("validating synthetic data")
    hmove_input_folder = mm_analyser.data_folder.joinpath('synthetic_corpus_comparison/hmove/input')
    json_files = [i for i in os.listdir(hmove_input_folder) if i.endswith('.json')]

    inner_target_class_count = 0
    combinations_count = []

    for filename in json_files:
        with open(hmove_input_folder.joinpath(filename)) as f:
            hmove_input_data = json.load(f)

        for oracle_key in hmove_input_data:
            combinations_count.append(len(hmove_input_data[oracle_key]))
            source_class_method, target_class = oracle_key.split('->')
            source_class, method_signature = source_class_method.split('::')
            found_oracle = False
            oracle_matches = [data for data in jmove_oracle.oracle_data
                      if data == jmove_oracle.JMoveOracle(
                    source_class=source_class, target_class=target_class, method_signature=method_signature, method_size='?', project_name='?')]

            assert len(oracle_matches) == 1
            oracle = oracle_matches[0]
            inner_target_class_count += 1 if len([i for i in oracle.target_class.split('.') if i[0].isupper()])>1 else 0

            for data in hmove_input_data[oracle_key]:
                in_data = hmove_computer.HMoveInput(**data)

                lower_ = [i for i in oracle.target_class.split('.') if i[0].islower()]
                upper_ = [i for i in oracle.target_class.split('.') if i[0].isupper()]
                target_ = upper_[0] if len(upper_) else lower_[-1]

                lower_ = [i for i in oracle.source_class.split('.') if i[0].islower()]
                upper_ = [i for i in oracle.source_class.split('.') if i[0].isupper()]
                source_ = upper_[0] if len(upper_) else lower_[-1]

                exact_match = ((oracle.method_name == in_data.method_information.method_name
                                or oracle.alias_method_name==in_data.method_information.method_name)
                               and source_ in in_data.source_class_path
                               and target_ in in_data.target_class_path
                               )
                if exact_match:
                    found_oracle = True

            if found_oracle == False:
                print("Couln't find it.")
            assert found_oracle
    print(f"{inner_target_class_count=}")
    print(f"{np.mean(combinations_count)=}")
    print("Validation complete!")


def check_real_world():
    hmove_input_folder = mm_analyser.data_folder.joinpath('refminer_data/hmove/input')
    json_files = [i for i in os.listdir(hmove_input_folder) if i.endswith('.json')]

    real_world_oracle = rw_oracle.get_instance_oracle()
    oracle_not_found = []
    inner_target_class_count = 0
    combinations_count = []

    for filename in json_files:
        with open(hmove_input_folder.joinpath(filename)) as f:
            hmove_input_data = json.load(f)

        for ref_id in hmove_input_data:
            combinations_count.append(len(hmove_input_data[ref_id]))
            # source_class_method, target_class = oracle_key.split('->')
            # source_class, method_signature = source_class_method.split('::')
            ref_id_int = int(ref_id)
            found_oracle = False
            oracle_matches = [data for data in real_world_oracle
                              if data.ref_id == ref_id_int]

            assert len(oracle_matches) == 1
            oracle = oracle_matches[0]
            inner_target_class_count += 1 if len(
                [i for i in oracle.move_method_ref.target_class.split('.') if i[0].isupper()]) > 1 else 0

            for data in hmove_input_data[ref_id]:
                in_data = hmove_computer.HMoveInput(**data)

                lower_ = [i for i in oracle.move_method_ref.target_class.split('.') if i[0].islower()]
                upper_ = [i for i in oracle.move_method_ref.target_class.split('.') if i[0].isupper()]
                target_ = upper_[0] if len(upper_) else lower_[-1]

                lower_ = [i for i in oracle.move_method_ref.original_class.split('.') if i[0].islower()]
                upper_ = [i for i in oracle.move_method_ref.original_class.split('.') if i[0].isupper()]
                source_ = upper_[0] if len(upper_) else lower_[-1]

                exact_match = ((oracle.method_name == in_data.method_information.method_name
                                or oracle.alias_method_name == in_data.method_information.method_name)
                               and source_ in in_data.source_class_path
                               and target_ in in_data.target_class_path
                               )
                if exact_match:
                    found_oracle = True

            if not found_oracle:
                print("Couldn't find it.")
                oracle_not_found.append(ref_id_int)

            # assert found_oracle
    print("Validation complete!")
    print(f"{oracle_not_found=}")
    print(f"{inner_target_class_count=}")
    print(f"{np.mean(combinations_count)=}")
    print(f"{max(combinations_count)=}")
    assert len(oracle_not_found) == 0

if __name__=='__main__':
    check()
    check_real_world()