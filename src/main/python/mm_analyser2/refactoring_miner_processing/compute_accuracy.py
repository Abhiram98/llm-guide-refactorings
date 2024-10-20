import json
from collections import Counter
import mm_analyser.refactoring_miner_processing.filter.MoveMethodRef as mref
from collections import defaultdict
import pandas as pd
from mm_analyser import data_folder

def myindex(list, ele):
    try:
        return list.index(ele)
    except ValueError:
        return -1


def calculate_vanilla_llm_recalls(telemetry, method_name, target_class, evaluation_data):
    # Calculating recall for iteration-3
    vanilla_llm_suggestions = [i for i in telemetry['iterationData'] if i['iteration_num'] == 3]
    if (len(vanilla_llm_suggestions) == 0):
        evaluation_data['vanilla_recall_method_position'] = -1
        evaluation_data['vanilla_recall_method_class_position'] = -1
        return
    method_order = [i['method_name'] for i in vanilla_llm_suggestions[0]['suggested_move_methods']]
    evaluation_data['vanilla_recall_method_position'] = myindex(method_order, method_name)

    if evaluation_data['vanilla_recall_method_position'] == -1:
        evaluation_data['vanilla_recall_method_class_position'] = -1
        return
    target_classes = [i.get('target_class', '') for i in vanilla_llm_suggestions[0]['suggested_move_methods']]
    if target_class in target_classes:
        evaluation_data['vanilla_recall_method_class_position'] = 0
    else:
        evaluation_data['vanilla_recall_method_class_position'] = -1


plugin_outfiles = [
    'vue_pro_res.json',
    'elastic_res.json',
    'dbeaver_res.json',
    'flink_res.json',
    'spring_framework_res.json',
    'halo_res.json',
    'redisson_res.json',
    'kafka_res.json',
    'springboot_res.json'
]

combined_output = []
df = pd.read_csv(f'{data_folder}/refminer_data/static_moves.csv')
for file_name in plugin_outfiles:
    with open(f'{data_folder}/refminer_data/mm-assist/{file_name}') as f:
        data = json.load(f)
    combined_output += data
combined_output = [i for i in combined_output if 'telemetry' in i
                   and len(i['telemetry'].keys())
                   and i['move_method_refactoring']['isStatic'] == True]

oracle_group_by_file = defaultdict(list)
for evaluation_data in combined_output:
    oracle = evaluation_data['move_method_refactoring']
    mm_obj = mref.MoveMethodRef.create_from(oracle)
    method_name = mm_obj.left_signature.method_name
    target_class = mm_obj.target_class.split('.')[-1]
    oracle_key = (mm_obj.left_file_path, evaluation_data['sha1'])
    # unique_oracle_files.append(oracle_key)
    oracle_group_by_file[oracle_key].append(mm_obj)

for evaluation_data in combined_output:

    oracle = evaluation_data['move_method_refactoring']
    mm_obj = mref.MoveMethodRef.create_from(oracle)
    method_name = mm_obj.left_signature.method_name
    target_class = mm_obj.target_class.split('.')[-1]
    oracle_key = (mm_obj.left_file_path, evaluation_data['sha1'])
    # if len(oracle_group_by_file[oracle_key])>3:
    #     continue

    telemetry = evaluation_data['telemetry']

    calculate_vanilla_llm_recalls(telemetry, method_name, target_class, evaluation_data)

    # if 'llmMethodPriority' not in telemetry:
    #     evaluation_data['recall_method_and_class_position'] = -1
    #     evaluation_data['recall_method_position'] = -1
    #     continue

    vanilla_llm_suggestions = [i for i in telemetry['iterationData']]
    if len(vanilla_llm_suggestions) == 0:
        evaluation_data['recall_method_class_position'] = -1
        evaluation_data['recall_method_position'] = -1
        continue
    vanilla_methods = []
    for m in [i['method_name'] for i in vanilla_llm_suggestions[0]['suggested_move_methods']]:
        if m not in vanilla_methods:
            vanilla_methods.append(m)

    other_oracle_methods = [o.left_signature.method_name for o in oracle_group_by_file[oracle_key] if
                            o.left_signature.method_name != mm_obj.left_signature.method_name]
    priority_method_minus_other_oracles = [m for m in vanilla_methods if m not in other_oracle_methods]
    filtered_llm_priority = [m for m in priority_method_minus_other_oracles
                             if
                             m in telemetry["targetClassMap"] and len(telemetry["targetClassMap"][m]['target_classes'])]
    evaluation_data['recall_method_position'] = myindex(filtered_llm_priority, method_name)
    if len(vanilla_methods) != len(priority_method_minus_other_oracles):
        print("nice")
    if evaluation_data['recall_method_position'] == -1:
        evaluation_data['recall_method_class_position'] = -1
        continue

    suggested_target_classes = telemetry['targetClassMap'][method_name]['target_classes_sorted_by_llm']
    evaluation_data['recall_method_class_position'] = \
        myindex(suggested_target_classes, target_class)

# combined_output = [i for i in combined_output if 'recall_method_position' in i]

df_mm_assist = pd.DataFrame(combined_output)
df_mm_assist['description'] = df_mm_assist['move_method_refactoring'].apply(lambda x: x['description'])
df_mm_assist['description-url'] = df_mm_assist['description'] + df_mm_assist['url']
df['description-url'] = df['description'] + df['url']
df_merged = pd.merge(df, df_mm_assist, on='description-url')
selected_columns = ['url_x', 'description_x', 'same_package', 'source_class_inner',
                    'target_class_inner', 'source_file_is_test', 'target_file_is_test',
                    'source_class_static', 'target_class_static', 'source_class_exists',
                    'target_class_exists', 'repository', 'sha1',
                    'vanilla_recall_method_position',
                    'vanilla_recall_method_class_position', 'recall_method_position',
                    'recall_method_class_position']
df_selected = df_merged[selected_columns]
df_selected = df_selected.rename(columns={"vanilla_recall_method_class_position": "vanilla_recall_class_position", "recall_method_class_position": "recall_class_position"})
df_selected.to_csv(f"{data_folder}/refminer_data/static_methods_2.csv", index=False)
recall_method_and_class_1 = len(
    [i for i in combined_output if i['recall_method_position'] == 0 and i['recall_method_class_position'] == 0]) / len(
    combined_output)
recall_method_1 = len([i for i in combined_output if i['recall_method_position'] == 0]) / len(combined_output)

recall_method_and_class_2 = len([i for i in combined_output if
                                 i['recall_method_position'] in [0, 1] and i[
                                     'recall_method_class_position'] == 0]) / len(combined_output)
recall_method_2 = len([i for i in combined_output if i['recall_method_position'] in [0, 1]]) / len(combined_output)

recall_method_and_class_3 = len([i for i in combined_output if
                                 i['recall_method_position'] in [0, 1, 2] and i[
                                     'recall_method_class_position'] == 0]) / len(combined_output)
recall_method_3 = len([i for i in combined_output if i['recall_method_position'] in [0, 1, 2]]) / len(combined_output)

recall_method_and_class_all = len([i for i in combined_output if
                                   i['recall_method_position'] != -1 and i[
                                       'recall_method_class_position'] != -1]) / len(combined_output)
recall_method_all = len([i for i in combined_output if i['recall_method_position'] != -1]) / len(combined_output)

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
recall_class_2 = len([i for i in recalled_methods if i['recall_method_class_position'] in [0, 1]]) / len(
    recalled_methods)
recall_class_3 = len([i for i in recalled_methods if i['recall_method_class_position'] in [0, 1, 2]]) / len(
    recalled_methods)
recall_class_inf = len([i for i in recalled_methods if i['recall_method_class_position'] != -1]) / len(recalled_methods)
print(f"recall of class for a recalled method. there were {len(recalled_methods)} recalled at any position.")
print(f"recall class @1 = {recall_class_1}")
print(f"recall class @2 = {recall_class_2}")
print(f"recall class @3 = {recall_class_3}")
print(f"recall class @inf = {recall_class_inf}")

print("---Vanilla LLM recall---")
vanilla_recall_method_and_class_1 = len(
    [i for i in combined_output if
     i['vanilla_recall_method_position'] == 0 and i['vanilla_recall_method_class_position'] == 0]) / len(
    combined_output)
vanilla_recall_method_1 = len([i for i in combined_output if i['vanilla_recall_method_position'] == 0]) / len(
    combined_output)

vanilla_recall_method_and_class_2 = len([i for i in combined_output if
                                         i['vanilla_recall_method_position'] in [0, 1] and i[
                                             'vanilla_recall_method_class_position'] == 0]) / len(combined_output)
vanilla_recall_method_2 = len([i for i in combined_output if i['vanilla_recall_method_position'] in [0, 1]]) / len(
    combined_output)

vanilla_recall_method_and_class_3 = len([i for i in combined_output if
                                         i['vanilla_recall_method_position'] in [0, 1, 2] and i[
                                             'vanilla_recall_method_class_position'] == 0]) / len(combined_output)
vanilla_recall_method_3 = len([i for i in combined_output if i['vanilla_recall_method_position'] in [0, 1, 2]]) / len(
    combined_output)

vanilla_recall_method_and_class_all = len([i for i in combined_output if
                                           i['vanilla_recall_method_position'] != -1 and i[
                                               'vanilla_recall_method_class_position'] != -1]) / len(combined_output)
vanilla_recall_method_all = len([i for i in combined_output if i['vanilla_recall_method_position'] != -1]) / len(
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

vanilla_recalled_methods = [i for i in combined_output if i['vanilla_recall_method_position'] != -1]
vanilla_recall_class_1 = len(
    [i for i in vanilla_recalled_methods if i['vanilla_recall_method_class_position'] == 0]) / len(
    vanilla_recalled_methods)
vanilla_recall_class_2 = len(
    [i for i in vanilla_recalled_methods if i['vanilla_recall_method_class_position'] in [0, 1]]) / len(
    vanilla_recalled_methods)
vanilla_recall_class_3 = len(
    [i for i in vanilla_recalled_methods if i['vanilla_recall_method_class_position'] in [0, 1, 2]]) / len(
    vanilla_recalled_methods)
vanilla_recall_class_inf = len(
    [i for i in vanilla_recalled_methods if i['vanilla_recall_method_class_position'] != -1]) / len(
    vanilla_recalled_methods)
print(f"recall of class for a recalled method. there were {len(vanilla_recalled_methods)} recalled at any position.")
print(f"recall class @1 = {vanilla_recall_class_1}")
print(f"recall class @2 = {vanilla_recall_class_2}")
print(f"recall class @3 = {vanilla_recall_class_3}")
print(f"recall class @inf = {vanilla_recall_class_inf}")
