import json
import os
from collections import Counter
from typing import Union, Any

import mm_analyser.refactoring_miner_processing.filter.ExtractMoveMethodValidator as emref
from collections import defaultdict
import pandas as pd
from mm_analyser import data_folder
from mm_analyser.refactoring_miner_processing.filter.MoveMethodRef import MoveMethodRef


class RecallPosition:
    def __init__(self, recall_method_position=-1, recall_class_position=-1):
        self.method_position = recall_method_position
        self.class_position = recall_class_position

    def __repr__(self):
        return str(self)

    def __str__(self):
        return f"{self.method_position=}, {self.class_position=}"


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
    if len(vanilla_recalled_methods) == 0:
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
                                if i['iteration_num'] > -1
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
    'elastic_res.json',
    'dbeaver_res.json',
    'flink_res.json',
    'spring_framework_res.json',
    'halo_res.json',
    'redisson_res.json',
    'kafka_res.json',
    'springboot_res.json',
    'ghidra_res.json',
    'graal_res.json',
    'selenium_res.json'
]

combined_output = []
# df = pd.read_csv(f'{data_folder}/refminer_data/static_moves.csv')
for file_name in plugin_outfiles:
    with open(f'{data_folder}/refminer_data/filter_emm/{file_name}') as f:
        data = json.load(f)
    combined_output += data
print(f"{len(combined_output)=}")

jmove_files = [i for i in os.listdir(f"{data_folder}/refminer_data/jmove") if i.endswith(".txt") and i.startswith("ref")]

jmove_results_map = {}
for f in jmove_files:
    ref_id = int(f.split('ref')[1].split('.txt')[0])
    with open(f"{data_folder}/refminer_data/jmove/{f}") as f1:
        jmove_results_map[ref_id] = f1.read().split('\n')[2:-1]
jmove_recall_data = {}
ignored_ids = [1, 52, 94, 569]
any_rec = 0
max_recs=[]
for evaluation_data in combined_output:
    ref_id = evaluation_data['ref_id']
    if ref_id==495:
        print(736)
    if ref_id not in jmove_results_map or ref_id in ignored_ids:
        continue

    oracle = evaluation_data['move_method_refactoring']
    mm_obj = emref.ExtractMoveMethodRef.create_from(oracle)
    method_name = mm_obj.right_signature.method_name
    alias_method_name = method_name + '1'
    target_class = mm_obj.target_class.split('.')[-1]

    recall_position = RecallPosition()
    # if len(jmove_results_map[ref_id]) > max_recs:
    #     max_recs = len(jmove_results_map[ref_id])
    count = 0
    for raw_result in jmove_results_map[ref_id]:
        _, source_method_and_class, target_class, similarity = raw_result.split("\t")
        source_class, source_method = source_method_and_class.split("::")
        # if len(jmove_results_map[ref_id]) > max_recs:
        #     max_recs = len(jmove_results_map[ref_id])
        if mm_obj.original_class.split('.')[-1] in source_class:
            any_rec += 1
            count += 1
        if mm_obj.original_class.split('.')[-1] in source_class and mm_obj.right_signature.method_name in source_method:
            recall_position.method_position = 0
            if mm_obj.target_class.split('.')[-1] in target_class:
                recall_position.class_position = 0
    max_recs.append(count)
    jmove_recall_data[ref_id] = recall_position


print(f"{any_rec=}")
print()
print(jmove_recall_data)
num_datapoints = len(jmove_recall_data)
print(f"{num_datapoints=}")
recalled_methods = [i for i in jmove_recall_data.values() if i.method_position!=-1]
recall_M = len(recalled_methods)/num_datapoints
print(f"{recall_M=}")
recall_C = len([i for i in recalled_methods if i.class_position!=-1])/len(recalled_methods)
print(f"{recall_C=}")

print(f"Recall_MC={recall_M * recall_C}")

with open(data_folder.joinpath("refminer_data/method_count_data.json")) as f:
    method_count_data = json.load(f)
method_count_map = {i['ref_id']:i['method_count'] for i in method_count_data}

METHOD_THRESHOLD = 15
jmove_small_classes = {k:v for k,v in jmove_recall_data.items() if method_count_map[k] < METHOD_THRESHOLD}
jmove_large_classes = {k:v for k,v in jmove_recall_data.items() if method_count_map[k] >= METHOD_THRESHOLD}
print("----Recall on small clases----")
print(f"{len(jmove_small_classes)=}")
recalled_methods = [i for i in jmove_small_classes.values() if i.method_position!=-1]
recall_M = len(recalled_methods)/len(jmove_small_classes)
print(f"{recall_M=}")
recall_C = len([i for i in recalled_methods if i.class_position!=-1])/len(recalled_methods)
print(f"{recall_C=}")
print(f"Recall_MC={recall_M * recall_C}")


print("----Recall on large clases----")
print(f"{len(jmove_large_classes)=}")
recalled_methods = [i for i in jmove_large_classes.values() if i.method_position!=-1]
recall_M = len(recalled_methods)/len(jmove_large_classes)
print(f"{recall_M=}")
recall_C = len([i for i in recalled_methods if i.class_position!=-1])/len(recalled_methods)
print(f"{recall_C=}")
print(f"Recall_MC={recall_M * recall_C}")

print(f"{max_recs=}")