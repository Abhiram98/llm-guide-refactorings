import json
import mm_analyser
import os
import mm_analyser.refactoring_miner_processing.oracle as rw_oracle
import mm_analyser.jmove_dataset.compute_accuracy_hmove as hmove_acc

from collections import defaultdict


def compute_recall(oracle: rw_oracle.RealWorldInstanceOraclePoint, hmove_output) -> hmove_acc.HMoveRecall:
    recall = hmove_acc.HMoveRecall(recall_m_index=-1, recall_c_index=-1, recall_mc_index=-1)

    valid_suggestions = [i for i in hmove_output if
                         i['probability'] is not None
                         and i['probability'] > 0.5
                         ]
    hmove_recommendations_sorted = sorted(valid_suggestions, key=lambda x: x['probability'])


    # Compute recall_mc
    for i, rec in enumerate(hmove_recommendations_sorted):
        # sanitse paths
        if '\\' in rec['method_info']['target_path']:
            rec['method_info']['target_path'] = rec['method_info']['target_path'].replace('\\', '/')

        if '\\' in rec['method_info']['source_path']:
            rec['method_info']['source_path'] = rec['method_info']['source_path'].replace('\\', '/')

        if (  # Check if any match the oracle
                (oracle.method_name == rec['method_info']['method_name']
                 or oracle.alias_method_name == rec['method_info']['method_name'])
                and oracle.move_method_ref.right_file_path in rec['method_info']['target_path']
                and oracle.move_method_ref.left_file_path in rec['method_info']['source_path']
        ):
            recall.recall_mc_index = i
            break

    # Compute recall_c
    suggestions_for_oracle_method = [i for i in hmove_recommendations_sorted if
                                     i['method_info']['method_name'] in [oracle.method_name, oracle.alias_method_name]]
    for i, rec in enumerate(suggestions_for_oracle_method):
        if oracle.move_method_ref.right_file_path in rec['method_info']['target_path']:
            recall.recall_c_index = i
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
            recall.recall_m_index = i
            break

    return recall


def present_recalls(recall_positions: list[hmove_acc.HMoveRecall]):
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


def compute():
    hmove_output_folder = mm_analyser.data_folder.joinpath('refminer_data/hmove/output')
    json_files = [i for i in os.listdir(hmove_output_folder) if i.endswith('.json')]

    real_world_oracle = rw_oracle.get_instance_oracle()
    oracle_not_found = []
    inner_target_class_count = 0

    hmove_output_data = defaultdict(list)

    for filename in json_files:
        with open(hmove_output_folder.joinpath(filename)) as f:
            hmove_output_data_ = json.load(f)
            for key in hmove_output_data_:
                hmove_output_data[key] += hmove_output_data_[key]

    recalls = {}
    for ref_id in hmove_output_data:
        # source_class_method, target_class = oracle_key.split('->')
        # source_class, method_signature = source_class_method.split('::')
        ref_id_int = int(ref_id)
        found_oracle = False
        oracle_matches = [data for data in real_world_oracle
                          if data.ref_id == ref_id_int]

        assert len(oracle_matches) == 1
        oracle = oracle_matches[0]

        recalls[ref_id_int] = compute_recall(oracle, hmove_output_data[ref_id])

        # compute recall.

    present_recalls(list(recalls.values()))


    METHOD_THRESHOLD = 15
    with open(mm_analyser.data_folder.joinpath("refminer_data/method_count_data.json")) as f:
        method_count_data = json.load(f)
        small_class_ids = [i['ref_id'] for i in method_count_data if i['method_count'] < METHOD_THRESHOLD]
        big_class_ids = [i['ref_id'] for i in method_count_data if i['method_count'] >= METHOD_THRESHOLD]



    print("-----results for small classes-----")
    present_recalls([v for k,v in recalls.items() if k in small_class_ids])

    print("-----results for large classes-----")
    present_recalls([v for k, v in recalls.items() if k in big_class_ids])


if __name__ == '__main__':
    compute()