import json
import numpy as np
from collections import Counter
from typing import Union, Any

import mm_analyser.refactoring_miner_processing.filter.ExtractMoveMethodValidator as emref
from collections import defaultdict
import pandas as pd
from mm_analyser import data_folder


class RecallPosition:
    def __init__(self, recall_method_position=-1, recall_class_position=-1):
        self.method_position = recall_method_position
        self.class_position = recall_class_position

    def __repr__(self):
        return str(self)

    def __str__(self):
        return f"{self.method_position=}, {self.class_position=}"


def myindex(list, ele, default=-1):
    try:
        return list.index(ele)
    except ValueError:
        return default


def present_recall(combined_output):
    recall_method_and_class_1 = len(
        [i for i in combined_output if
         i['recall_position'].method_position == 0 and i['recall_position'].class_position == 0]) / len(
        combined_output)
    recall_method_1 = len([i for i in combined_output if i['recall_position'].method_position == 0]) / len(
        combined_output)

    recall_method_and_class_2 = len([i for i in combined_output if
                                     i['recall_position'].method_position in [0, 1] and i[
                                         'recall_position'].class_position == 0]) / len(combined_output)
    recall_method_2 = len([i for i in combined_output if i['recall_position'].method_position in [0, 1]]) / len(
        combined_output)

    recall_method_and_class_3 = len([i for i in combined_output if
                                     i['recall_position'].method_position in [0, 1, 2] and i[
                                         'recall_position'].class_position == 0]) / len(combined_output)
    recall_method_3 = len([i for i in combined_output if i['recall_position'].method_position in [0, 1, 2]]) / len(
        combined_output)

    recall_method_and_class_all = len([i for i in combined_output if
                                       i['recall_position'].method_position != -1 and i[
                                           'recall_position'].class_position != -1]) / len(combined_output)
    recall_method_all = len([i for i in combined_output if i['recall_position'].method_position != -1]) / len(
        combined_output)

    print(f"dataset size = {len(combined_output)}")
    oracle_size = 200
    print(f"{oracle_size=}?")

    print("recalling the correct MoveMethod:")
    print(f"recall method&class @1 = {recall_method_and_class_1}")
    print(f"recall method&class @2 = {recall_method_and_class_2}")
    print(f"recall method&class @3 = {recall_method_and_class_3}")
    print(f"recall method&class @inf = {recall_method_and_class_all}")
    print()

    print("recalling the correct method only (identifying method out of place)")
    print(f"recall method @1 = {recall_method_1}")
    print(f"recall method @2 = {recall_method_2}")
    print(f"recall method @3 = {recall_method_3}")
    print(f"recall method @inf = {recall_method_all}")
    print()

    recalled_methods = [i for i in combined_output if i['recall_position'].method_position != -1]
    if len(recalled_methods) == 0:
        print("0 methods were recalled")
        return
    recall_class_1 = len([i for i in recalled_methods if i['recall_position'].class_position == 0]) / len(
        recalled_methods)
    recall_class_2 = len([i for i in recalled_methods if i['recall_position'].class_position in [0, 1]]) / len(
        recalled_methods)
    recall_class_3 = len([i for i in recalled_methods if i['recall_position'].class_position in [0, 1, 2]]) / len(
        recalled_methods)
    recall_class_inf = len([i for i in recalled_methods if i['recall_position'].class_position != -1]) / len(
        recalled_methods)
    print(f"recall of class for a recalled method. there were {len(recalled_methods)} recalled at any position.")
    print(f"recall class @1 = {recall_class_1}")
    print(f"recall class @2 = {recall_class_2}")
    print(f"recall class @3 = {recall_class_3}")
    print(f"recall class @inf = {recall_class_inf}")


def present_vanilla_lmm_recall(combined_output):
    print("---Vanilla LLM recall---")
    vanilla_recall_method_and_class_1 = []
    vanilla_recall_method_and_class_2 = []
    vanilla_recall_method_and_class_3 = []
    vanilla_recall_method_and_class_all = []
    vanilla_recall_method_1 = []
    vanilla_recall_method_2 = []
    vanilla_recall_method_3 = []
    vanilla_recall_method_all = []
    for index in range(MAX_ITERS):
        vanilla_recall_method_and_class_1.append(
            len(
                [i for i in combined_output if
                 i['vanilla_recall'][index].method_position == 0 and i['vanilla_recall'][index].class_position == 0]) / len(
                combined_output)
        )
        vanilla_recall_method_1.append(
            len([i for i in combined_output if i['vanilla_recall'][index].method_position == 0]) / len(
            combined_output))

        vanilla_recall_method_and_class_2.append(len([i for i in combined_output if
                                                 i['vanilla_recall'][index].method_position in [0, 1] and i['vanilla_recall'][index].class_position == 0]) / len(combined_output))
        vanilla_recall_method_2.append( len(
            [i for i in combined_output if i['vanilla_recall'][index].method_position in [0, 1]]) / len(
            combined_output))

        vanilla_recall_method_and_class_3.append(len([i for i in combined_output if
                                                 i['vanilla_recall'][index].method_position in [0, 1, 2] and i['vanilla_recall'][index].class_position == 0]) / len(combined_output))
        vanilla_recall_method_3.append(len(
            [i for i in combined_output if i['vanilla_recall'][index].method_position in [0, 1, 2]]) / len(
            combined_output))

        vanilla_recall_method_and_class_all.append(len([i for i in combined_output if
                                                   i['vanilla_recall'][index].method_position != -1 and i['vanilla_recall'][index].class_position != -1]) / len(combined_output))
        vanilla_recall_method_all.append(len(
            [i for i in combined_output if i['vanilla_recall'][index].method_position != -1]) / len(
            combined_output))

    print("recalling the correct MoveMethod:")
    print(
        f"recall method&class @1 = {vanilla_recall_method_and_class_1=}")
    print(
        f"recall method&class @2 = {vanilla_recall_method_and_class_2=}")
    print(
        f"recall method&class @3 = {vanilla_recall_method_and_class_3=}")
    print(
        f"recall method&class @inf = {vanilla_recall_method_and_class_all=}")


    print()

    print("recalling the correct method only (identifying method out of place)")
    print(f"recall method @1 = {vanilla_recall_method_1=}")
    print(f"recall method @2 = {vanilla_recall_method_2=}")
    print(f"recall method @3 = {vanilla_recall_method_3=}")
    print(f"recall method @inf = {vanilla_recall_method_all=}")
    print()

    vanilla_recall_class_1 = []
    vanilla_recall_class_2 = []
    vanilla_recall_class_3 = []
    vanilla_recall_class_inf = []
    for index in range(MAX_ITERS):
        vanilla_recalled_methods = [i for i in combined_output if i['vanilla_recall'][index].method_position != -1]
        if len(vanilla_recalled_methods) == 0:
            print("0 methods were recalled")
            continue
        vanilla_recall_class_1.append(len(
            [i for i in vanilla_recalled_methods if i['vanilla_recall'][index].class_position == 0]) / len(
            vanilla_recalled_methods))
        vanilla_recall_class_2.append(len(
            [i for i in vanilla_recalled_methods if i['vanilla_recall'][index].class_position in [0, 1]]) / len(
            vanilla_recalled_methods))
        vanilla_recall_class_3.append(len(
            [i for i in vanilla_recalled_methods if i['vanilla_recall'][index].class_position in [0, 1, 2]]) / len(
            vanilla_recalled_methods))
        vanilla_recall_class_inf.append(len(
            [i for i in vanilla_recalled_methods if i['vanilla_recall'][index].class_position != -1]) / len(
            vanilla_recalled_methods))
    print(
        f"recall of class for a recalled method. there were {len(vanilla_recalled_methods)} recalled at any position.")
    print(f"recall class @1 = {vanilla_recall_class_1=}")
    print(f"recall class @2 = {vanilla_recall_class_2=}")
    print(f"recall class @3 = {vanilla_recall_class_3=}")
    print(f"recall class @inf = {vanilla_recall_class_inf=}")


def calculate_vanilla_llm_recalls(telemetry, method_name, target_class, evaluation_data):


    evaluation_data['vanilla_recall'] = []

    for i in range(MAX_ITERS):
        VANILLA_ITER_NUM = i  # only perform computation on LLM's first response.
        # Calculating recall for all iterations
        vanilla_llm_suggestions_ = [i for i in telemetry['iterationData']
                                    if i['iteration_num'] > 0
                                    ]
        if (len(vanilla_llm_suggestions_) == 0):
            evaluation_data['vanilla_recall'].append(RecallPosition())
            continue
        method_order = [i['method_name'] for i in vanilla_llm_suggestions_[VANILLA_ITER_NUM]['suggested_move_methods']]
        # method_order = telemetry['llmMethodPriority']['priority_method_names']
        alias_method_name = method_name + '1'
        vanilla_recall_m: int = myindex(
            [1 if (method_name in i or alias_method_name in i) else 0 for i in method_order], 1)

        if vanilla_recall_m == -1:
            evaluation_data['vanilla_recall'].append(RecallPosition())
            continue
        vanilla_target_class = vanilla_llm_suggestions_[VANILLA_ITER_NUM]['suggested_move_methods'] \
            [vanilla_recall_m]['target_class']
        if target_class == vanilla_target_class:
            evaluation_data['vanilla_recall'].append(RecallPosition(vanilla_recall_m, 0))
        else:
            evaluation_data['vanilla_recall'].append(RecallPosition(vanilla_recall_m, -1))

    return evaluation_data['vanilla_recall']


def calculate_priority_sort_recall(telemetry, method_name, target_class, evaluation_data):
    # Calculating recall for all iterations
    vanilla_llm_suggestions_ = [i for i in telemetry['iterationData']
                                if i['iteration_num'] == -2
                                ]
    if (len(vanilla_llm_suggestions_) == 0):
        evaluation_data['vanilla_recall'] = RecallPosition()
        return evaluation_data['vanilla_recall']
    # method_order = [i['method_name'] for i in vanilla_llm_suggestions_[0]['suggested_move_methods']]
    method_order = telemetry['llmMethodPriority']['priority_method_names']
    alias_method_name = method_name + '1'
    vanilla_recall: int = max(myindex(method_order, method_name),
                              myindex(method_order, alias_method_name))

    if vanilla_recall == -1:
        evaluation_data['vanilla_recall'] = RecallPosition()
        return evaluation_data['vanilla_recall']
    target_classes = [i.get('target_class', '') for i in vanilla_llm_suggestions_[0]['suggested_move_methods']]
    if target_class in target_classes:
        evaluation_data['vanilla_recall'] = RecallPosition(vanilla_recall, 0)
    else:
        evaluation_data['vanilla_recall'] = RecallPosition(vanilla_recall, -1)

    return evaluation_data['vanilla_recall']


def calculate_mm_assist_recall(telemetry):
    # vanilla_llm_suggestions = [i for i in telemetry['iterationData']]
    # if len(vanilla_llm_suggestions) == 0:
    #     return RecallPosition()
    # vanilla_methods = []
    # for m in [i['method_name'] for i in vanilla_llm_suggestions[0]['suggested_move_methods']]:
    #     if m not in vanilla_methods:
    #         vanilla_methods.append(m)

    # other_oracle_methods = [o.right_signature.method_name for o in oracle_group_by_file[oracle_key] if
    #                         o.right_signature.method_name != mm_obj.right_signature.method_name]
    # priority_method_minus_other_oracles = [m for m in vanilla_methods if m not in other_oracle_methods]

    # rank the methods by tf-idf, voyage or by llm.
    tf_idf_ranking = [i['suggested_move_methods'] for i in telemetry['iterationData'] if i['iteration_num'] == -2][0]
    tf_idf_methods = [i['method_name'] for i in tf_idf_ranking]
    voyage_ranking = sorted(
        [(v['first']['method_name'], v['second']) for v in telemetry['methodCompatibilityScores']['voyage'].values()],
        key=lambda x: x[1])
    voyage_methods = [i[0] for i in voyage_ranking]

    sorted_methods = tf_idf_methods
    # sorted_methods = voyage_methods

    # check which of those method could actually be moved and filter the others out.
    filtered_llm_priority = [m for m in sorted_methods
                             if
                             m in telemetry["targetClassMap"]
                             and
                             len(telemetry["targetClassMap"][m]['target_classes'])
                             ]
    if len(filtered_llm_priority) > 3 and telemetry['llmMethodPriority']['tf-idf']['priority_method_names'] != []:
        priority_method_order = telemetry['llmMethodPriority']['tf-idf']['priority_method_names']
        # priority_method_order = [i.split('(')[0].split(' ')[-1] for i in priority_method_order]
        filtered_llm_priority.sort(key=lambda x: myindex([1 if x in i else 0 for i in priority_method_order], 1,
                                                         default=len(sorted_methods) + 1))
    recall_method_position = max(myindex(filtered_llm_priority, method_name),
                                 myindex(filtered_llm_priority, alias_method_name))
    # -1)
    if recall_method_position == -1:
        return RecallPosition()

    suggested_target_classes = telemetry['targetClassMap'].get(
        method_name, telemetry['targetClassMap'].get(alias_method_name)
    )['target_classes_sorted_by_llm']
    recall_method_class_position = myindex(suggested_target_classes, target_class)
    return RecallPosition(recall_method_position, recall_method_class_position)


if __name__ == '__main__':
    VANILLA_ONLY = False
    MAX_ITERS = 3

    plugin_outfiles = [
        'vue_pro_res.json',
        'elastic_res.json',
        'dbeaver_res.json',
        'flink_res.json',
        'spring_framework_res.json',
        # 'halo_res.json',
        'redisson_res.json',
        'kafka_res.json',
        'springboot_res.json',
        "graal_res.json",
        "ghidra_res.json",
        "selenium_res.json"
    ]

    combined_output = []
    # df = pd.read_csv(f'{data_folder}/refminer_data/static_moves.csv')
    for file_name in plugin_outfiles:
        # with open(f'{data_folder}/refminer_data/mm-assist-emm/{file_name}') as f:
        with open(f'{data_folder}/refminer_data/mm-assist-emm/{file_name}') as f:
            data = json.load(f)
        combined_output += data

    IGNORED_IDS = [40, 42, 44, 46, 583, 584
                   # , 796
                   ]
    # IGNORED_IDS = []
    combined_output = [i for i in combined_output if 'telemetry' in i
                       and len(i['telemetry'].keys())
                       and i['move_method_refactoring']['isStatic'] == False
                       and i['ref_id'] not in IGNORED_IDS]
    print(f"{len(combined_output)=}")
    # remove duplicates
    unique_data = []
    unique_emm = []
    for evaluation in combined_output:
        oracle = evaluation['move_method_refactoring']
        mm_obj = emref.ExtractMoveMethodRef.create_from(oracle)
        # if mm_obj.key() in unique_emm:
        #     continue
        unique_data.append(evaluation)
        unique_emm.append(mm_obj.key())
    print(f"{len(unique_emm)=}")
    print(f"{len(unique_data)=}")
    combined_output = unique_data

    for evaluation_data in combined_output:

        oracle = evaluation_data['move_method_refactoring']
        mm_obj = emref.ExtractMoveMethodRef.create_from(oracle)
        method_name = mm_obj.right_signature.method_name
        alias_method_name = method_name + '1'
        target_class = mm_obj.target_class.split('.')[-1]
        # oracle_key = (mm_obj.left_file_path, evaluation_data['sha1'])
        # if len(oracle_group_by_file[oracle_key])>3:
        #     continue

        telemetry = evaluation_data['telemetry']

        calculate_vanilla_llm_recalls(telemetry, method_name, target_class, evaluation_data)

        if 'llmMethodPriority' not in telemetry:
            evaluation_data['recall_position'] = RecallPosition()
            continue
        if not VANILLA_ONLY:
            evaluation_data['recall_position'] = calculate_mm_assist_recall(telemetry)
        else:
            evaluation_data['recall_position'] = RecallPosition()

    # combined_output = [i for i in combined_output if 'recall_method_position' in i]

    # df_mm_assist = pd.DataFrame(combined_output)
    # df_mm_assist['description'] = df_mm_assist['move_method_refactoring'].apply(lambda x: x['description'])
    # df_mm_assist['vanilla_recall_m'] = df_mm_assist['vanilla_recall'].apply(lambda x: x.method_position)
    # df_mm_assist['description-url'] = df_mm_assist['description'] + df_mm_assist['url']
    # df['description-url'] = df['description'] + df['url']
    # df_merged = pd.merge(df, df_mm_assist, on='description-url')
    # selected_columns = ['url_x', 'description_x', 'same_package', 'source_class_inner',
    #                     'target_class_inner', 'source_file_is_test', 'target_file_is_test',
    #                     'source_class_static', 'target_class_static', 'source_class_exists',
    #                     'target_class_exists', 'repository', 'sha1',
    #                     'vanilla_recall',
    #                     'vanilla_recall', 'recall_method_position',
    #                     'recall_method_class_position']
    # df_selected = df_merged[selected_columns]
    # df_selected = df_selected.rename(columns={"vanilla_recall": "vanilla_recall_class_position", "recall_method_class_position": "recall_class_position"})
    # df_selected.to_csv(f"{data_folder}/refminer_data/static_methods_2.csv", index=False)
    present_recall(combined_output)
    present_vanilla_lmm_recall(combined_output)

    METHOD_THRESHOLD = 15
    with open(data_folder.joinpath("refminer_data/method_count_data.json")) as f:
        method_count_data = json.load(f)
        small_class_ids = [i['ref_id'] for i in method_count_data if i['method_count'] < METHOD_THRESHOLD]
        big_class_ids = [i['ref_id'] for i in method_count_data if i['method_count'] >= METHOD_THRESHOLD]

    all_ref_ids = list([i['ref_id'] for i in combined_output])
    print(f"{all_ref_ids=}")

    small_refs = [i for i in combined_output if i['ref_id'] in small_class_ids]
    big_refs = [i for i in combined_output if i['ref_id'] in big_class_ids]
    print(f"{big_class_ids=}")

    print()
    print()
    print(f"Recall on Small classes (< {METHOD_THRESHOLD} methods)")
    present_recall(small_refs)
    present_vanilla_lmm_recall(small_refs)

    print()
    print()
    print(f"Recall on Large classes (>= {METHOD_THRESHOLD} methods)")
    present_recall(big_refs)
    present_vanilla_lmm_recall(big_refs)
    missed_big_ref_ids = [i['ref_id'] for i in big_refs if i['recall_position'].method_position == -1]
    print(f"{missed_big_ref_ids=}")

    # missed_methods_because_infeasible = [i['ref_id'] for i in combined_output
    #                                      if i['vanilla_recall'].method_position!=-1 and i['recall_position'].method_position == -1]
    # print(f"{missed_methods_because_infeasible=}")

    missed_small_ref_ids = [i["ref_id"] for i in small_refs if i['recall_position'].method_position == -1]
    print(f"{missed_small_ref_ids=}")

    missed_small_ref_recall_c = [i['ref_id'] for i in small_refs if i['recall_position'].method_position != -1 and i[
        'recall_position'].class_position == -1]
    print(f"{missed_small_ref_recall_c=}")

    missed_big_ref_recall_c = [i['ref_id'] for i in big_refs if i['recall_position'].method_position != -1 and i[
        'recall_position'].class_position == -1]
    print(f"{missed_big_ref_recall_c=}")
