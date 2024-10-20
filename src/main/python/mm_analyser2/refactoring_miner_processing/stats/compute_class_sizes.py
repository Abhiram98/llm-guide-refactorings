import json
import os
import pandas as pd

from mm_analyser import data_folder
import mm_analyser.refactoring_miner_processing.filter.MoveMethodRef as mmref
import mm_analyser.refactoring_miner_processing.filter.MoveMethodValidator as MoveMethodValidator
from mm_analyser.env import PROJECTS_BASE_PATH, PROJECT_ALIAS_MAP

rminer_data_path = f"{data_folder}/refminer_data/mm-assist"
files = os.listdir(rminer_data_path)
json_files = [i for i in files if i.endswith(".json")]

data = []
total_count = 0

for fname in json_files:
    with open(os.path.join(rminer_data_path, fname)) as f:
        move_data = json.load(f)
    # total_count += len(move_data['commits'])

    for j, m in enumerate(move_data):
        print((j, len(move_data)))
        if not m['move_method_refactoring']['isStatic']:
            continue

        oracle = m['move_method_refactoring']
        mm_obj = mmref.MoveMethodRef.create_from(oracle)

        orig_package = ".".join([i for i in mm_obj.original_class.split('.') if i[0].islower()])
        target_package = ".".join([i for i in mm_obj.target_class.split('.') if i[0].islower()])


        validator = MoveMethodValidator.MoveMethodValidator(
            m,
            os.path.join(PROJECTS_BASE_PATH, PROJECT_ALIAS_MAP[fname])
        )

        validator.checkout_before()
        method_count = validator.get_method_count(mm_obj.left_file_path)

        data.append(
            (
                m['url'],
                mm_obj.description,
               method_count
            )
        )

columns = ["url", "description",
           "method_count"]

df = pd.DataFrame(data, columns=columns)
print(df.head())
df.to_csv(data_folder.joinpath("refminer_data/method_count_stats.csv"), index=False)
df_static = pd.read_csv(f'{data_folder}/refminer_data/static_methods_2.csv')
df_static['description-url'] = df_static['description_x'] + df_static['url_x']
df['description-url'] = df['description'] + df['url']
df_merged = pd.merge(df, df_static, on='description-url')

selected_columns = ['url', 'description', 'same_package', 'source_class_inner',
                    'target_class_inner', 'source_file_is_test', 'target_file_is_test',
                    'source_class_static', 'target_class_static', 'source_class_exists',
                    'target_class_exists', 'repository', 'sha1',
                    'vanilla_recall_method_position',
                    'vanilla_recall_class_position', 'recall_method_position',
                    'recall_class_position', 'method_count']
df_selected = df_merged[selected_columns]
df_selected.to_csv(data_folder.joinpath("refminer_data/method_count_stats_2.csv"), index=False)