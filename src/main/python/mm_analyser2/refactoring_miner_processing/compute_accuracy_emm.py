import json
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

def myindex(list, ele):
    try:
        return list.index(ele)
    except ValueError:
        return -1


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
    if len(recalled_methods)==0:
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
    vanilla_recall_method_and_class_1 = len(
        [i for i in combined_output if
         i['vanilla_recall'].method_position == 0 and i['vanilla_recall'].class_position == 0]) / len(
        combined_output)
    vanilla_recall_method_1 = len([i for i in combined_output if i['vanilla_recall'].method_position == 0]) / len(
        combined_output)

    vanilla_recall_method_and_class_2 = len([i for i in combined_output if
                                             i['vanilla_recall'].method_position in [0, 1] and i[
                                                 'vanilla_recall'].class_position == 0]) / len(combined_output)
    vanilla_recall_method_2 = len([i for i in combined_output if i['vanilla_recall'].method_position in [0, 1]]) / len(
        combined_output)

    vanilla_recall_method_and_class_3 = len([i for i in combined_output if
                                             i['vanilla_recall'].method_position in [0, 1, 2] and i[
                                                 'vanilla_recall'].class_position == 0]) / len(combined_output)
    vanilla_recall_method_3 = len(
        [i for i in combined_output if i['vanilla_recall'].method_position in [0, 1, 2]]) / len(
        combined_output)

    vanilla_recall_method_and_class_all = len([i for i in combined_output if
                                               i['vanilla_recall'].method_position != -1 and i[
                                                   'vanilla_recall'].class_position != -1]) / len(combined_output)
    vanilla_recall_method_all = len([i for i in combined_output if i['vanilla_recall'].method_position != -1]) / len(
        combined_output)

    print("recalling the correct MoveMethod:")
    print(f"recall method&class @1 = {vanilla_recall_method_and_class_1}")
    print(f"recall method&class @2 = {vanilla_recall_method_and_class_2}")
    print(f"recall method&class @3 = {vanilla_recall_method_and_class_3}")
    print(f"recall method&class @inf = {vanilla_recall_method_and_class_all}")
    print()

    print("recalling the correct method only (identifying method out of place)")
    print(f"recall method @1 = {vanilla_recall_method_1}")
    print(f"recall method @2 = {vanilla_recall_method_2}")
    print(f"recall method @3 = {vanilla_recall_method_3}")
    print(f"recall method @inf = {vanilla_recall_method_all}")
    print()

    vanilla_recalled_methods = [i for i in combined_output if i['vanilla_recall'].method_position != -1]
    if len(vanilla_recalled_methods)==0:
        print("0 methods were recalled")
        return
    vanilla_recall_class_1 = len(
        [i for i in vanilla_recalled_methods if i['vanilla_recall'].class_position == 0]) / len(
        vanilla_recalled_methods)
    vanilla_recall_class_2 = len(
        [i for i in vanilla_recalled_methods if i['vanilla_recall'].class_position in [0, 1]]) / len(
        vanilla_recalled_methods)
    vanilla_recall_class_3 = len(
        [i for i in vanilla_recalled_methods if i['vanilla_recall'].class_position in [0, 1, 2]]) / len(
        vanilla_recalled_methods)
    vanilla_recall_class_inf = len(
        [i for i in vanilla_recalled_methods if i['vanilla_recall'].class_position != -1]) / len(
        vanilla_recalled_methods)
    print(
        f"recall of class for a recalled method. there were {len(vanilla_recalled_methods)} recalled at any position.")
    print(f"recall class @1 = {vanilla_recall_class_1}")
    print(f"recall class @2 = {vanilla_recall_class_2}")
    print(f"recall class @3 = {vanilla_recall_class_3}")
    print(f"recall class @inf = {vanilla_recall_class_inf}")


def calculate_vanilla_llm_recalls(telemetry, method_name, target_class, evaluation_data):
    # Calculating recall for all iterations
    vanilla_llm_suggestions_ = [i for i in telemetry['iterationData']
                                if i['iteration_num'] == -2
                                ]
    if (len(vanilla_llm_suggestions_) == 0):
        evaluation_data['vanilla_recall'] = RecallPosition()
        return evaluation_data['vanilla_recall']
    method_order = [i['method_name'] for i in vanilla_llm_suggestions_[0]['suggested_move_methods']]
    # method_order = telemetry['llmMethodPriority']['priority_method_names']
    alias_method_name = method_name + '1'
    vanilla_recall: int = myindex([1 if (method_name in i or alias_method_name in i) else 0 for i in method_order], 1)


    if vanilla_recall == -1:
        evaluation_data['vanilla_recall'] = RecallPosition()
        return evaluation_data['vanilla_recall']
    target_classes = [i.get('target_class', '') for i in vanilla_llm_suggestions_[0]['suggested_move_methods']]
    if target_class in target_classes:
        evaluation_data['vanilla_recall'] = RecallPosition(vanilla_recall, 0)
    else:
        evaluation_data['vanilla_recall'] = RecallPosition(vanilla_recall, -1)
    
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


plugin_outfiles = [
    'vue_pro_res.json',
    # 'elastic_res.json',
    # 'dbeaver_res.json',
    'flink_res.json',
    # 'spring_framework_res.json',
    # 'halo_res.json',
    # 'redisson_res.json',
    'kafka_res.json',
    # 'springboot_res.json'
]

combined_output = []
# df = pd.read_csv(f'{data_folder}/refminer_data/static_moves.csv')
for file_name in plugin_outfiles:
    with open(f'{data_folder}/refminer_data/mm-assist-emm/{file_name}') as f:
        data = json.load(f)
    combined_output += data
combined_output = [i for i in combined_output if 'telemetry' in i
                   and len(i['telemetry'].keys())
                   and i['move_method_refactoring']['isStatic'] == False]
print(f"{len(combined_output)=}")
# remove duplicates
unique_data = []
unique_emm = []
for evaluation in combined_output:
    oracle = evaluation['move_method_refactoring']
    mm_obj = emref.ExtractMoveMethodRef.create_from(oracle)
    if mm_obj.key() in unique_emm:
        continue
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

    vanilla_llm_suggestions = [i for i in telemetry['iterationData']]
    if len(vanilla_llm_suggestions) == 0:
        evaluation_data['recall_position'] = RecallPosition()
        continue
    vanilla_methods = []
    for m in [i['method_name'] for i in vanilla_llm_suggestions[0]['suggested_move_methods']]:
        if m not in vanilla_methods:
            vanilla_methods.append(m)

    # other_oracle_methods = [o.right_signature.method_name for o in oracle_group_by_file[oracle_key] if
    #                         o.right_signature.method_name != mm_obj.right_signature.method_name]
    # priority_method_minus_other_oracles = [m for m in vanilla_methods if m not in other_oracle_methods]
    priority_method_minus_other_oracles = vanilla_methods
    filtered_llm_priority = [m for m in priority_method_minus_other_oracles
                             if
                             m in telemetry["targetClassMap"]
                             and
                             len(telemetry["targetClassMap"][m]['target_classes'])
                             ]
    recall_method_position = max(myindex(filtered_llm_priority, method_name),
                                 myindex(filtered_llm_priority, alias_method_name))
                                # -1)
    if len(vanilla_methods) != len(priority_method_minus_other_oracles):
        print("nice")
    if recall_method_position == -1:
        evaluation_data['recall_position'] = RecallPosition()
        continue

    suggested_target_classes = telemetry['targetClassMap'].get(
        method_name, telemetry['targetClassMap'].get(alias_method_name)
    )['target_classes_sorted_by_llm']
    recall_method_class_position = myindex(suggested_target_classes, target_class)
    evaluation_data['recall_position'] = RecallPosition(recall_method_position, recall_method_class_position)

# combined_output = [i for i in combined_output if 'recall_method_position' in i]

# df_mm_assist = pd.DataFrame(combined_output)
# df_mm_assist['description'] = df_mm_assist['move_method_refactoring'].apply(lambda x: x['description'])
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


df = pd.read_csv(data_folder.joinpath("refminer_data/emm_method_count.csv"))
METHOD_THRESHOLD = 15
small_ids = list(df[df['method_count'] < METHOD_THRESHOLD]['ref_id'])
small_refs = [i for i in combined_output if i['ref_id'] in small_ids]
big_refs = [i for i in combined_output if not i['ref_id'] in small_ids]
big_refs_ids = [i
                for i in  list(df[df['method_count'] >= METHOD_THRESHOLD]['ref_id'])]
print(f"{big_refs_ids=}")

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
missed_big_ref_ids = [i['ref_id'] for i in big_refs if i['vanilla_recall'].method_position==-1]
print(f"{missed_big_ref_ids=}")

missed_methods_because_infeasible = [i['ref_id'] for i in combined_output
                                     if i['vanilla_recall'].method_position!=-1 and i['recall_position'].method_position == -1]
print(f"{missed_methods_because_infeasible=}")

