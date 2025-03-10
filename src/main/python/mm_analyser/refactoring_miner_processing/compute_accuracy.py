import json
from collections import Counter
import MoveMethodValidator
from collections import defaultdict
import pandas as pd
from mm_analyser import data_folder
import argparse
import datetime
import sys
import os

# Add argument parsing
parser = argparse.ArgumentParser()
parser.add_argument('--input_directory', required=True, help='Directory for input files')
parser.add_argument('--output_file_path', required=True, help='Path for output CSV file')
parser.add_argument('--log_path', required=True, help='Path to the log file')
args = parser.parse_args()

# Redirect stdout to the log file
sys.stdout = open(args.log_path, 'w')

# Add datetime stamp as an indicator
current_time = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
print(f"#############################################################")
print(f"############### Log started at {current_time} ###############")
print(f"#############################################################")
print()

def myindex(list, ele):
    try:
        idx = list.index(ele)
        return idx if idx <= 2 else -1
    except ValueError:
        return -1
    
def get_source_java_filename(file_path):
    """Extract the Java file name from the full file path."""
    return os.path.basename(file_path)

def calculate_vanilla_llm_recalls(telemetry, method_name, target_class, evaluation_data):
    # Calculating recall for iteration-3
    vanilla_llm_suggestions = [i for i in telemetry['iterationData'] if i['iteration_num'] == 3]
    # print(f"Number of vanilla LLM suggestions for iteration 3 (method): {len(vanilla_llm_suggestions)}")
    if (len(vanilla_llm_suggestions) == 0):
        evaluation_data['vanilla_recall_method_position'] = -1
        evaluation_data['vanilla_recall_method_class_position'] = -1
        return
    method_order = [i.get('method_name', '') for i in vanilla_llm_suggestions[0].get('suggested_move_methods', [])]
    evaluation_data['vanilla_recall_method_position'] = myindex(method_order, method_name)

    if evaluation_data['vanilla_recall_method_position'] == -1:
        evaluation_data['vanilla_recall_method_class_position'] = -1
        return
    
    target_classes = [i.get('target_class', '') for i in vanilla_llm_suggestions[0].get('suggested_move_methods', [])]
    # print(f"Number of vanilla LLM suggestions for iteration 3 (class): {len(vanilla_llm_suggestions)}")
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
    try:
        with open(f'{data_folder}/refminer_data/{args.input_directory}/{file_name}') as f:
            data = json.load(f)
        combined_output += data
        print(f"Successfully processed {file_name}")
    except FileNotFoundError:
        print(f"File not found: {file_name}. Skipping...")
    except json.JSONDecodeError:
        print(f"Error decoding JSON in file: {file_name}. Skipping...")

combined_output = [i for i in combined_output if 'telemetry' in i
                   and len(i['telemetry'].keys())
                   and i['move_method_refactoring']['isStatic'] == True]
print(f"Total items after loading all files: {len(combined_output)}")

oracle_group_by_file = defaultdict(list)
for evaluation_data in combined_output:
    oracle = evaluation_data['move_method_refactoring']
    mm_obj = MoveMethodValidator.MoveMethodRef.create_from(oracle)
    method_name = mm_obj.left_signature.method_name
    target_class = mm_obj.target_class.split('.')[-1]
    oracle_key = (mm_obj.left_file_path, evaluation_data['sha1'])
    # unique_oracle_files.append(oracle_key)
    oracle_group_by_file[oracle_key].append(mm_obj)
    evaluation_data['method_count'] = evaluation_data['telemetry'].get('methodCount', None)

for evaluation_data in combined_output:

    oracle = evaluation_data['move_method_refactoring']
    mm_obj = MoveMethodValidator.MoveMethodRef.create_from(oracle)
    method_name = mm_obj.left_signature.method_name
    target_class = mm_obj.target_class.split('.')[-1]
    oracle_key = (mm_obj.left_file_path, evaluation_data['sha1'])
    # if len(oracle_group_by_file[oracle_key])>3:
    #     continue
    evaluation_data['method_count'] = evaluation_data['telemetry'].get('methodCount', None)
    evaluation_data['source_java_file'] = get_source_java_filename(mm_obj.left_file_path)

    telemetry = evaluation_data['telemetry']

    calculate_vanilla_llm_recalls(telemetry, method_name, target_class, evaluation_data)

    if 'llmMethodPriority' not in telemetry:
        evaluation_data['recall_method_and_class_position'] = -1
        evaluation_data['recall_method_position'] = -1
        continue

    vanilla_llm_suggestions = [i for i in telemetry['iterationData']]

    if len(vanilla_llm_suggestions) == 0:
        evaluation_data['recall_method_class_position'] = -1
        evaluation_data['recall_method_position'] = -1
        continue
    vanilla_methods = []
    method_order = [i.get('method_name', '') for i in vanilla_llm_suggestions[0].get('suggested_move_methods', [])]
    for m in [i['method_name'] for i in vanilla_llm_suggestions[1]['suggested_move_methods']]:
        # print(m)
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
        # print("nice")
        pass
    if evaluation_data['recall_method_position'] == -1:
        evaluation_data['recall_method_class_position'] = -1
        continue
    
    # evaluation_data['method_count'] = evaluation_data['telemetry'].get('methodCount', None)
    suggested_target_classes = telemetry['targetClassMap'].get(method_name, {}).get('target_classes_sorted_by_llm', [])
    evaluation_data['recall_method_class_position'] = \
        myindex(suggested_target_classes, target_class)


# combined_output = [i for i in combined_output if 'recall_method_position' in i]

df_mm_assist = pd.DataFrame(combined_output)
# print(df_mm_assist.head())
df_mm_assist['description'] = df_mm_assist['move_method_refactoring'].apply(lambda x: x['description'])
df_mm_assist['description-url'] = df_mm_assist['description'] + df_mm_assist['url']
df['description-url'] = df['description'] + df['url']
df_merged = pd.merge(df, df_mm_assist, on='description-url')
selected_columns = ['url_x', 'description_x', 'same_package', 'source_class_inner',
                    'target_class_inner', 'source_file_is_test', 'target_file_is_test',
                    'source_class_static', 'target_class_static', 'source_class_exists',
                    'target_class_exists', 'repository', 'sha1', 'method_count', 'source_java_file']
df_selected = df_merged[selected_columns]
df_selected = df_selected.rename(columns={"vanilla_recall_method_class_position": "vanilla_recall_class_position", "recall_method_class_position": "recall_class_position"})
df_selected.to_csv(f"{data_folder}/refminer_data/{args.output_file_path}", index=False)
df_filtered = df_merged[df_merged['target_class_exists'] == True]
# df_filtered = df_merged[df_merged['source_class_exists'] == True]


# Recreate combined_output from the filtered DataFrame
combined_output = df_filtered.to_dict('records')
combined_output_less_than_15 = [i for i in combined_output if i.get('method_count', 0) > 0 and i.get('method_count', 0) < 15]
combined_output_greater_than_15 = [i for i in combined_output if i.get('method_count', 0) >= 15]

def safe_division(numerator, denominator):
    return numerator / denominator if denominator != 0 else 0

def calculate_and_print_recall(combined_output):
    total_count = len(combined_output)
    
    recall_method_and_class_1 = safe_division(
        len([i for i in combined_output if i['recall_method_position'] == 0 and i['recall_method_class_position'] == 0]),
        total_count
    )
    recall_method_1 = safe_division(len([i for i in combined_output if i['recall_method_position'] == 0]), total_count)

    recall_method_and_class_2 = safe_division(
        len([i for i in combined_output if i['recall_method_position'] in [0, 1] and i['recall_method_class_position'] == 0]),
        total_count
    )
    recall_method_2 = safe_division(len([i for i in combined_output if i['recall_method_position'] in [0, 1]]), total_count)

    recall_method_and_class_3 = safe_division(
        len([i for i in combined_output if i['recall_method_position'] in [0, 1, 2] and i['recall_method_class_position'] == 0]),
        total_count
    )
    recall_method_3 = safe_division(len([i for i in combined_output if i['recall_method_position'] in [0, 1, 2]]), total_count)

    recall_method_and_class_all = safe_division(
        len([i for i in combined_output if i['recall_method_position'] != -1 and i['recall_method_class_position'] != -1]),
        total_count
    )
    recall_method_all = safe_division(len([i for i in combined_output if i['recall_method_position'] != -1]), total_count)

    print(f"dataset size = {total_count}")
    # oracle_size = 200
    # print(f"{oracle_size=}?")
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

    recalled_methods = [i for i in combined_output if i['recall_method_position'] in [0,1,2]]
    if recalled_methods:
        recall_class_1 = len([i for i in recalled_methods if i['recall_method_class_position'] == 0]) / len(recalled_methods)
        recall_class_2 = len([i for i in recalled_methods if i['recall_method_class_position'] in [0, 1]]) / len(recalled_methods)
        recall_class_3 = len([i for i in recalled_methods if i['recall_method_class_position'] in [0, 1, 2]]) / len(recalled_methods)
        recall_class_inf = len([i for i in recalled_methods if i['recall_method_class_position'] != -1]) / len(recalled_methods)
    else:
        recall_class_1 = recall_class_2 = recall_class_3 = recall_class_inf = 0
    print(f"recall of class for a recalled method. there were {len(recalled_methods)} recalled at any position.")
    print(f"recall class @1 = {recall_class_1}")
    print(f"recall class @2 = {recall_class_2}")
    print(f"recall class @3 = {recall_class_3}")
    print(f"recall class @inf = {recall_class_inf}")

calculate_and_print_recall(combined_output_less_than_15)
calculate_and_print_recall(combined_output_greater_than_15)

def calculate_and_print_vanilla_recall(combined_output, dataset_name):
    print(f"\n---Embedding Ranked recall for {dataset_name}---")
    total_count = len(combined_output)

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

    print(f"dataset size = {total_count}")
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
    if vanilla_recalled_methods:
        vanilla_recall_class_1 = len([i for i in vanilla_recalled_methods if i['vanilla_recall_method_class_position'] == 0]) / len(vanilla_recalled_methods)
        vanilla_recall_class_2 = len([i for i in vanilla_recalled_methods if i['vanilla_recall_method_class_position'] in [0, 1]]) / len(vanilla_recalled_methods)
        vanilla_recall_class_3 = len([i for i in vanilla_recalled_methods if i['vanilla_recall_method_class_position'] in [0, 1, 2]]) / len(vanilla_recalled_methods)
        vanilla_recall_class_inf = len([i for i in vanilla_recalled_methods if i['vanilla_recall_method_class_position'] != -1]) / len(vanilla_recalled_methods)
    else:
        vanilla_recall_class_1 = vanilla_recall_class_2 = vanilla_recall_class_3 = vanilla_recall_class_inf = 0
    print(f"recall of class for a recalled method. there were {len(vanilla_recalled_methods)} recalled at any position.")
    print(f"recall class @1 = {vanilla_recall_class_1}")
    print(f"recall class @2 = {vanilla_recall_class_2}")
    print(f"recall class @3 = {vanilla_recall_class_3}")
    print(f"recall class @inf = {vanilla_recall_class_inf}")

calculate_and_print_vanilla_recall(combined_output_less_than_15, "Methods < 15")
calculate_and_print_vanilla_recall(combined_output_greater_than_15, "Methods >= 15")

def count_suggestions(combined_output):
    total_vanilla_suggestions = 0
    total_final_suggestions = 0
    total_files = len(combined_output)
    
    for evaluation_data in combined_output:
        telemetry = evaluation_data['telemetry']
        
        # Count vanilla LLM suggestions (iteration 3)
        vanilla_llm_suggestions = [i for i in telemetry['iterationData'] if i['iteration_num'] == 3]
        if vanilla_llm_suggestions:
            suggestions = vanilla_llm_suggestions[0].get('suggested_move_methods', [])
            total_vanilla_suggestions += len(suggestions)
        
        # Count final suggestions
        if 'llmMethodPriority' in telemetry:
            vanilla_suggestions = [i for i in telemetry['iterationData']]
            if vanilla_suggestions:
                vanilla_methods = []
                for m in [i['method_name'] for i in vanilla_suggestions[1]['suggested_move_methods']]:
                    if m not in vanilla_methods:
                        vanilla_methods.append(m)
                total_final_suggestions += len(vanilla_methods)
    
    print(f"Analysis of {total_files} files:")
    print(f"Total vanilla LLM suggestions (iteration 3): {total_vanilla_suggestions}")
    print(f"Average vanilla suggestions per file: {total_vanilla_suggestions/total_files:.2f}")
    print(f"Total final suggestions: {total_final_suggestions}")
    print(f"Average final suggestions per file: {total_final_suggestions/total_files:.2f}")

# Add these lines after loading the combined_output
print("\nAnalyzing all inputs:")
count_suggestions(combined_output)

print("\nAnalyzing inputs with method count < 15:")
count_suggestions(combined_output_less_than_15)

print("\nAnalyzing inputs with method count >= 15:")
count_suggestions(combined_output_greater_than_15)


def count_suggestions_and_hallucinations(combined_output):
    total_files = len(combined_output)
    total_vanilla_suggestions = 0
    total_hallucinations = 0
    total_valid_suggestions = 0
    
    for evaluation_data in combined_output:
        telemetry = evaluation_data['telemetry']
        
        # Get valid suggestions from bruteforce
        valid_suggestions = []
        bruteforce_data = [i for i in telemetry['iterationData'] if i['iteration_num'] == -1]
        if bruteforce_data:
            valid_suggestions = [i.get('method_name', '') for i in bruteforce_data[0].get('suggested_move_methods', [])]
        
        # Get vanilla LLM suggestions (iteration 3)
        vanilla_llm_data = [i for i in telemetry['iterationData'] if i['iteration_num'] == 3]
        if vanilla_llm_data:
            vanilla_suggestions = [i.get('method_name', '') for i in vanilla_llm_data[0].get('suggested_move_methods', [])]
            
            # Count suggestions and hallucinations
            num_suggestions = len(vanilla_suggestions)
            total_vanilla_suggestions += num_suggestions
            
            # Count hallucinations (suggestions not in valid_suggestions)
            hallucinations = [s for s in vanilla_suggestions if s not in valid_suggestions]
            num_hallucinations = len(hallucinations)
            total_hallucinations += num_hallucinations
            
            # Count valid suggestions
            num_valid = num_suggestions - num_hallucinations
            total_valid_suggestions += num_valid
    
    # Calculate percentages
    hallucination_rate = (total_hallucinations / total_vanilla_suggestions * 100) if total_vanilla_suggestions > 0 else 0
    valid_rate = (total_valid_suggestions / total_vanilla_suggestions * 100) if total_vanilla_suggestions > 0 else 0
    
    print(f"\nAnalysis of {total_files} files:")
    print(f"Total vanilla LLM suggestions: {total_vanilla_suggestions}")
    print(f"Total hallucinations: {total_hallucinations}")
    print(f"Total valid suggestions: {total_valid_suggestions}")
    print(f"Average suggestions per file: {total_vanilla_suggestions/total_files:.2f}")
    print(f"Hallucination rate: {hallucination_rate:.2f}%")
    print(f"Valid suggestion rate: {valid_rate:.2f}%")

# Add these lines after loading the combined_output
print("\nAnalyzing all inputs:")
count_suggestions_and_hallucinations(combined_output)

print("\nAnalyzing inputs with method count < 15:")
count_suggestions_and_hallucinations(combined_output_less_than_15)

print("\nAnalyzing inputs with method count >= 15:")
count_suggestions_and_hallucinations(combined_output_greater_than_15)
# Close the log file
sys.stdout.close()

# Restore stdout
sys.stdout = sys.__stdout__