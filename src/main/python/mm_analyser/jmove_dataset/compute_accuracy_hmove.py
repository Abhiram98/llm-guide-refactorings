import json

import mm_analyser
import mm_analyser.jmove_dataset.oracle as jmove_oracle
import mm_analyser.env as env
import mm_analyser.hmove.compute_input_synthetic as hmove_computer
import mm_analyser.refactoring_miner_processing.compute_accuracy_emm as emm_acc

from pydantic import BaseModel, Field

class HMoveRecall(BaseModel):
    recall_m_index: int = Field(description="index that the oracle-method was found at.")
    recall_c_index: int = Field(description="index that the oracle-target-class was found at.")
    recall_mc_index: int = Field(description="index that the oracle was found at.")

def compute_recall_from_json(hmove_results_json) -> list[HMoveRecall]:
    recall_positions: list[HMoveRecall] = []
    incorrect_run = 0

    for oracle_key in hmove_results_json:
        recall_ = HMoveRecall(recall_m_index=-1, recall_c_index=-1, recall_mc_index=-1)
        source_class_method, target_class = oracle_key.split('->')
        source_class, method_signature = source_class_method.split('::')
        found_oracle = False
        oracle_matches = [data for data in jmove_oracle.oracle_data
                          if data == jmove_oracle.JMoveOracle(
                source_class=source_class, target_class=target_class, method_signature=method_signature,
                method_size='?',
                project_name='?')]

        assert len(oracle_matches) == 1
        oracle = oracle_matches[0]

        lower_ = [i for i in oracle.target_class.split('.') if i[0].islower()]
        upper_ = [i for i in oracle.target_class.split('.') if i[0].isupper()]
        target_ = upper_[0] if len(upper_) else lower_[-1]

        lower_ = [i for i in oracle.source_class.split('.') if i[0].islower()]
        upper_ = [i for i in oracle.source_class.split('.') if i[0].isupper()]
        source_ = upper_[0] if len(upper_) else lower_[-1]

        # Keep only the suggestions which have a probablity value greater than half.
        valid_suggestions = [i for i in hmove_results_json[oracle_key] if
                             i['probability'] is not None
                             and i['probability'] > 0.5
                             ]

        # check if hmove ran correctly every time

        # has_incorrect = False
        # for rec in valid_suggestions:
        #     if 'Subprocess output: Parent path as string:' not in rec['stdout']:
        #         incorrect_run += 1
        #         has_incorrect = True
        # if has_incorrect:
        #     continue


        nomove_suggestions = [i for i in hmove_results_json[oracle_key] if
                              i['probability'] is not None
                              and not i['final_decision']
                              ]

        hmove_recommendations_sorted = sorted(valid_suggestions, key=lambda x: x['probability'])
        print(f"{len(hmove_recommendations_sorted)=}")

        # Compute recall_mc
        for i, rec in enumerate(hmove_recommendations_sorted):
            if ( # Check if any match the oracle
                    (oracle.method_name == rec['method_info']['method_name']
                     or oracle.alias_method_name == rec['method_info']['method_name'])
                    and target_ in rec['method_info']['target_path']
                    and source_ in rec['method_info']['source_path']
            ):
                recall_.recall_mc_index = i
                break

        # Compute recall_c
        suggestions_for_oracle_method = [i for i in hmove_recommendations_sorted if i['method_info']['method_name'] in [oracle.method_name, oracle.alias_method_name]]
        for i, rec in enumerate(suggestions_for_oracle_method):
            if target_ in rec['method_info']['target_path']:
                recall_.recall_c_index = i
                break

        # Compute recall_m
        best_suggestion_for_method = []
        covered_methods = []
        for i, rec in enumerate(hmove_recommendations_sorted):
            if rec['method_info']['method_name'] not in covered_methods:
                best_suggestion_for_method.append((rec['method_info']['method_name'], rec['probability']))
                covered_methods.append(rec['method_info']['method_name'])

        for i, (method_name, probability) in enumerate(best_suggestion_for_method):
            if method_name == oracle.method_name or method_name == oracle.alias_method_name:
                recall_.recall_m_index = i
                break
        recall_positions.append(recall_)

    print(f"{incorrect_run=}")

    return recall_positions


def compute_recall():
    files = [
        'result_ant_large.json',
        'result_ant_small.json',
        'result_derby_large.json',
        'result_derby_small.json',
        'result_jhotdraw_large.json',
        'result_jhotdraw_small.json',
        'result_junit_large.json',
        'result_junit_small.json',
        'result_lucene_large.json',
        'result_lucene_small.json',
        'result_mvnforum_large.json',
        'result_mvnforum_small.json',
        'result_tapestry_large.json',
        'result_tapestry_small.json',
        'result_jfreechart_large.json',
        'result_jfreechart_small.json',
        'result_jgroups_large.json'
    ]

    hmove_results_path = mm_analyser.data_folder.joinpath('synthetic_corpus_comparison/hmove/output')
    recall_positions: list[HMoveRecall] = []
    for filename in files:
        with open(hmove_results_path.joinpath(filename)) as f:
            hmove_results = json.load(f)

        recall_positions += compute_recall_from_json(hmove_results)


    # Recall_M
    recall_1 = [i for i in recall_positions if i.recall_m_index == 0]
    recall_2 = [i for i in recall_positions if -1 < i.recall_m_index <= 1]
    recall_3 = [i for i in recall_positions if -1 < i.recall_m_index <= 2]
    recall_inf = [i for i in recall_positions if i.recall_m_index > -1]

    print()
    print("-----results-----")
    print(f"{len(recall_positions)=}")
    print(f"recall_m@1={len(recall_1) / len(recall_positions)}")
    print(f"recall_m@2={len(recall_2) / len(recall_positions)}")
    print(f"recall_m@3={len(recall_3) / len(recall_positions)}")
    print(f"recall_m@inf={len(recall_inf) / len(recall_positions)}")


    # Recall_C
    recall_1 = [i for i in recall_positions if i.recall_c_index == 0]
    recall_2 = [i for i in recall_positions if -1 < i.recall_c_index <= 1]
    recall_3 = [i for i in recall_positions if -1 < i.recall_c_index <= 2]
    recall_inf = [i for i in recall_positions if i.recall_c_index > -1]

    print(f"{len(recall_positions)=}")
    print(f"recall_c@1={len(recall_1) / len(recall_positions)}")
    print(f"recall_c@2={len(recall_2) / len(recall_positions)}")
    print(f"recall_c@3={len(recall_3) / len(recall_positions)}")
    print(f"recall_c@inf={len(recall_inf) / len(recall_positions)}")

    # Recall_MC
    recall_1 = [i for i in recall_positions if i.recall_mc_index == 0]
    recall_2 = [i for i in recall_positions if -1 < i.recall_mc_index <= 1]
    recall_3 = [i for i in recall_positions if -1 < i.recall_mc_index <= 2]
    recall_inf = [i for i in recall_positions if i.recall_mc_index > -1]

    print(f"{len(recall_positions)=}")
    print(f"recall_mc@1={len(recall_1) / len(recall_positions)}")
    print(f"recall_mc@2={len(recall_2) / len(recall_positions)}")
    print(f"recall_mc@3={len(recall_3) / len(recall_positions)}")
    print(f"recall_mc@inf={len(recall_inf) / len(recall_positions)}")


if __name__ == '__main__':
    compute_recall()
