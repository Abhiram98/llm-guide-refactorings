import json
import MoveMethodValidator
from collections import defaultdict
import os

def myindex(list, ele):
    try:
        return list.index(ele)
    except ValueError:
        return -1


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

combined_output = []
for file_name in plugin_outfiles:
    with open(f'../../../../data/refminer_data/mm-assist/{file_name}') as f:
        data = json.load(f)
    combined_output += data
combined_output = [i for i in combined_output
                   if 'telemetry' in i and len(i['telemetry'].keys())
                   and i['move_method_refactoring']['isStatic']
                   ]

fetruth_dir = "/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/refminer_data/feTruth"
fetruth_files = [i for i in os.listdir(fetruth_dir) if i.endswith(".json")]
fe_data = {}
for fe in fetruth_files:
    commit_hash = fe.split("_")[-1].rstrip(".json")
    with open(os.path.join(fetruth_dir, fe)) as f:
        fe_data[commit_hash] = json.load(f)

print(len(fe_data))

oracle_group_by_file = defaultdict(list)
for evaluation_data in combined_output:
    oracle = evaluation_data['move_method_refactoring']
    mm_obj = MoveMethodValidator.MoveMethodRef.create_from(oracle)
    method_name = mm_obj.left_signature.method_name
    target_class = mm_obj.target_class.split('.')[-1]
    oracle_key = (mm_obj.left_file_path, evaluation_data['sha1'])
    # unique_oracle_files.append(oracle_key)
    oracle_group_by_file[oracle_key].append(mm_obj)

fe_hits = []
for evaluation_data in combined_output:

    oracle = evaluation_data['move_method_refactoring']
    mm_obj = MoveMethodValidator.MoveMethodRef.create_from(oracle)
    method_name = mm_obj.left_signature.method_name
    target_class = mm_obj.target_class.split('.')[-1]
    oracle_key = (mm_obj.left_file_path, evaluation_data['sha1'])
    # if len(oracle_group_by_file[oracle_key])>3:
    #     continue

    telemetry = evaluation_data['telemetry']

    fetruth_recs = fe_data.get(evaluation_data['sha1'])
    if fetruth_recs is None:
        continue
    fe_method_and_classes = []
    fe_hits.append(evaluation_data['sha1'])

    fetruth_recs_source_class = [
        i for i in fetruth_recs if i['source_class'] == mm_obj.original_class
    ]
    # print(f"{len(fetruth_recs_source_class)=}")
    if len(fetruth_recs_source_class) == 0:
        evaluation_data['recall_method_position'] = -1
        evaluation_data['recall_method_class_position'] = -1
        continue

    fe_methods = [i['source_method'] for i in fetruth_recs_source_class]
    evaluation_data['recall_method_position'] = myindex(fe_methods, method_name)
    if evaluation_data['recall_method_position'] == -1:
        evaluation_data['recall_method_class_position'] = -1
        continue
    method_index = evaluation_data['recall_method_position']
    fe_method = fetruth_recs_source_class[method_index]['source_method']
    fe_target_classes = [i['target_class'].split('.')[-1] for i in fetruth_recs_source_class if
                         i['source_method'] == fe_method]
    evaluation_data['recall_method_class_position'] = myindex(fe_target_classes, target_class)

combined_output = [i for i in combined_output if 'recall_method_position' in i]
recall_method_and_class_1 = len(
    [i for i in combined_output if i['recall_method_position'] == 0 and i['recall_method_class_position'] == 0]) / len(
    combined_output)
recall_method_1 = len([i for i in combined_output if i['recall_method_position'] == 0]) / len(combined_output)

recall_method_and_class_2 = len([i for i in combined_output if
                                 i['recall_method_position'] in [0, 1] and i['recall_method_class_position'] == 0]) / len(combined_output)
recall_method_2 = len([i for i in combined_output if i['recall_method_position'] in [0, 1]]) / len(combined_output)

recall_method_and_class_3 = len([i for i in combined_output if
                                 i['recall_method_position'] in [0, 1, 2] and i['recall_method_class_position'] == 0]) / len(combined_output)
recall_method_3 = len([i for i in combined_output if i['recall_method_position'] in [0, 1, 2]]) / len(combined_output)

recall_method_and_class_all = len([i for i in combined_output if
                                 i['recall_method_position']!=-1 and i['recall_method_class_position'] !=-1]) / len(combined_output)
recall_method_all = len([i for i in combined_output if i['recall_method_position'] !=-1]) / len(combined_output)

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

recalled_methods = [i for i in combined_output if i['recall_method_position'] != -1]
recall_class_1 = len([i for i in recalled_methods if i['recall_method_class_position'] == 0]) / len(recalled_methods)
recall_class_2 = len([i for i in recalled_methods if i['recall_method_class_position'] in [0, 1]]) / len(recalled_methods)
recall_class_3 = len([i for i in recalled_methods if i['recall_method_class_position'] in [0, 1, 2]]) / len(recalled_methods)
recall_class_inf = len([i for i in recalled_methods if i['recall_method_class_position'] != -1]) / len(recalled_methods)
print(f"recall of class for a recalled method. there were {len(recalled_methods)} recalled at any position.")
print(f"recall class @1 = {recall_class_1}")
print(f"recall class @2 = {recall_class_2}")
print(f"recall class @3 = {recall_class_3}")
print(f"recall class @inf = {recall_class_inf}")
