import json
import os

import mm_analyser
import mm_analyser.hmove.compute_input_synthetic as hmove_computer
import mm_analyser.jmove_dataset.oracle as jmove_oracle

def check():
    hmove_input_folder = mm_analyser.data_folder.joinpath('synthetic_corpus_comparison/hmove/input')
    json_files = [i for i in os.listdir(hmove_input_folder) if i.endswith('.json')]


    for filename in json_files:
        with open(hmove_input_folder.joinpath(filename)) as f:
            hmove_input_data = json.load(f)

        for oracle_key in hmove_input_data:
            source_class_method, target_class = oracle_key.split('->')
            source_class, method_signature = source_class_method.split('::')
            found_oracle = False
            oracle_matches = [data for data in jmove_oracle.oracle_data
                      if data == jmove_oracle.JMoveOracle(
                    source_class=source_class, target_class=target_class, method_signature=method_signature, method_size='?', project_name='?')]

            assert len(oracle_matches) == 1
            oracle = oracle_matches[0]

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
    print("Validation complete!")


if __name__=='__main__':
    check()