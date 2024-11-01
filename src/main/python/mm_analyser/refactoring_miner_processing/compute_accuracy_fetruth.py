import json
# import MoveMethodValidator
from collections import defaultdict
import os
from mm_analyser.refactoring_miner_processing.filter.ExtractMoveMethodValidator import ExtractMoveMethodRef

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

plugin_outfiles = [
    'vue_pro_res.json',
    'elastic_res.json',
    'dbeaver_res.json',
    'flink_res.json',
    'kafka_res.json',
    'spring_framework_res.json',
    'halo_res.json',
    'redisson_res.json',
    'springboot_res.json'
]

oracle_data = []
for file_name in plugin_outfiles:
    with open(f'{data_folder}/refminer_data/filter_emm/{file_name}') as f:
        data = json.load(f)
    oracle_data += data
# combined_output = [i for i in combined_output
                   # if 'telemetry' in i and len(i['telemetry'].keys())
                   # and i['move_method_refactoring']['isStatic']


fetruth_dir = f"{data_folder}/refminer_data/feTruthInstanceMethodSmall"
fetruth_files = [i for i in os.listdir(fetruth_dir) if i.endswith(".json")]
fe_data = {}
for fe in fetruth_files:
    commit_hash = fe.split("_")[-1].rstrip(".json")
    with open(os.path.join(fetruth_dir, fe)) as f:
        fe_data[commit_hash] = json.load(f)

print(len(fe_data))


fe_hits = []
for oracle_point in oracle_data:

    oracle = oracle_point['move_method_refactoring']
    mm_obj = ExtractMoveMethodRef.create_from(oracle)
    method_name = mm_obj.right_signature.method_name
    target_class = mm_obj.target_class.split('.')[-1]

    # telemetry = evaluation_data['telemetry']
    branch_name = oracle_point['extraction_results']['newBranchName']
    fetruth_recs = fe_data.get(branch_name)
    if fetruth_recs is None:
        continue
    fe_method_and_classes = []
    fe_hits.append(oracle_point['ref_id'])

    fetruth_recs_source_class = [
        i for i in fetruth_recs if i['source_class'] == mm_obj.original_class
    ]
    # print(f"{len(fetruth_recs_source_class)=}")
    if len(fetruth_recs_source_class) == 0:
        oracle_point['recall_position'] = RecallPosition()
        continue

    fe_methods = [i['source_method'] for i in fetruth_recs_source_class]
    recall_method_position = myindex(fe_methods, method_name)
    if recall_method_position == -1:
        oracle_point['recall_position'] = RecallPosition()
        continue
    method_index = recall_method_position
    fe_method = fetruth_recs_source_class[method_index]['source_method']
    fe_target_classes = [i['target_class'].split('.')[-1] for i in fetruth_recs_source_class if
                         i['source_method'] == fe_method]
    recall_method_class_position = myindex(fe_target_classes, target_class)
    oracle_point['recall_position'] = RecallPosition(recall_method_position, recall_method_class_position)

combined_output = [i for i in oracle_data if 'recall_position' in i]
present_recall(combined_output)
# recall_method_and_class_1 = len(
#     [i for i in combined_output if i['recall_position'].method_position == 0 and i['recall_position'] == 0]) / len(
#     combined_output)
# recall_method_1 = len([i for i in combined_output if i['recall_method_position'] == 0]) / len(combined_output)
#
# recall_method_and_class_2 = len([i for i in combined_output if
#                                  i['recall_method_position'] in [0, 1] and i['recall_method_class_position'] == 0]) / len(combined_output)
# recall_method_2 = len([i for i in combined_output if i['recall_method_position'] in [0, 1]]) / len(combined_output)
#
# recall_method_and_class_3 = len([i for i in combined_output if
#                                  i['recall_method_position'] in [0, 1, 2] and i['recall_method_class_position'] == 0]) / len(combined_output)
# recall_method_3 = len([i for i in combined_output if i['recall_method_position'] in [0, 1, 2]]) / len(combined_output)
#
# recall_method_and_class_all = len([i for i in combined_output if
#                                  i['recall_method_position']!=-1 and i['recall_method_class_position'] !=-1]) / len(combined_output)
# recall_method_all = len([i for i in combined_output if i['recall_method_position'] !=-1]) / len(combined_output)
#
# print(f"dataset size = {len(combined_output)}")
# oracle_size = 200
# print(f"{oracle_size=}?")
#
# print("recalling the correct MoveMethod:")
# print(f"recall method&class @1 = {recall_method_and_class_1}")
# print(f"recall method&class @2 = {recall_method_and_class_2}")
# print(f"recall method&class @3 = {recall_method_and_class_3}")
# print(f"recall method&class @inf = {recall_method_and_class_all}")
# print()
#
# print("recalling the correct method only (identifying method out of place)")
# print(f"recall method @1 = {recall_method_1}")
# print(f"recall method @2 = {recall_method_2}")
# print(f"recall method @3 = {recall_method_3}")
# print(f"recall method @inf = {recall_method_all}")
# print()
#
# recalled_methods = [i for i in combined_output if i['recall_method_position'] != -1]
# recall_class_1 = len([i for i in recalled_methods if i['recall_method_class_position'] == 0]) / len(recalled_methods)
# recall_class_2 = len([i for i in recalled_methods if i['recall_method_class_position'] in [0, 1]]) / len(recalled_methods)
# recall_class_3 = len([i for i in recalled_methods if i['recall_method_class_position'] in [0, 1, 2]]) / len(recalled_methods)
# recall_class_inf = len([i for i in recalled_methods if i['recall_method_class_position'] != -1]) / len(recalled_methods)
# print(f"recall of class for a recalled method. there were {len(recalled_methods)} recalled at any position.")
# print(f"recall class @1 = {recall_class_1}")
# print(f"recall class @2 = {recall_class_2}")
# print(f"recall class @3 = {recall_class_3}")
# print(f"recall class @inf = {recall_class_inf}")
