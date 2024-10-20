import json
import os
import pandas as pd

from mm_analyser2 import data_folder
import mm_analyser.refactoring_miner_processing.MoveMethodValidator as MoveMethodValidator
from mm_analyser2.env import PROJECTS_BASE_PATH, PROJECT_ALIAS_MAP

rminer_data_path = f"{data_folder}/refminer_data/contains_a_mm"
files = os.listdir(rminer_data_path)
json_files = [i for i in files if i.endswith(".json")]

data = []
total_count = 0
# same_package = 0
# source_class_inner = 0
# target_class_inner = 0
# source_file_is_test = 0
# target_file_is_test = 0

for fname in json_files:
    with open(os.path.join(rminer_data_path, fname)) as f:
        move_data = json.load(f)
    total_count += len(move_data['commits'])

    for j, m in enumerate(move_data):
        print((j, len(move_data)))
        if not m['move_method_refactoring']['isStatic']:
            continue

        oracle = m['move_method_refactoring']
        mm_obj = MoveMethodValidator.MoveMethodRef.create_from(oracle)

        orig_package = ".".join([i for i in mm_obj.original_class.split('.') if i[0].islower()])
        target_package = ".".join([i for i in mm_obj.target_class.split('.') if i[0].islower()])

        if orig_package == target_package:
            same_package = True
        else:
            same_package = False

        source_class_inner = len([i for i in mm_obj.original_class.split('.') if i[0].isupper()]) > 1
        target_class_inner = len([i for i in mm_obj.target_class.split('.') if i[0].isupper()]) > 1

        source_file_is_test = 'test' in mm_obj.left_file_path
        target_file_is_test = 'test' in mm_obj.right_file_path

        validator = MoveMethodValidator.MoveMethodValidator(
            m,
            os.path.join(PROJECTS_BASE_PATH, PROJECT_ALIAS_MAP[fname])
        )
        source_class_static = validator.is_class_static(mm_obj.original_class, mm_obj.left_file_path)
        target_class_static = validator.is_class_static(mm_obj.target_class, mm_obj.right_file_path)

        validator.checkout_before()
        source_class_exists = validator.class_exists(mm_obj.original_class, mm_obj.left_file_path)
        validator.checkout_before()
        target_class_exists = validator.class_exists(mm_obj.target_class, mm_obj.right_file_path)

        data.append(
            (
                m['url'],
                mm_obj.description,
                same_package,
                source_class_inner,
                target_class_inner,
                source_file_is_test,
                target_file_is_test,
                source_class_static,
                target_class_static,
                source_class_exists,
                target_class_exists
            )
        )

columns = ["url", "description",
           "same_package", "source_class_inner",
           "target_class_inner", "source_file_is_test",
           "target_file_is_test", "source_class_static",
           "target_class_static", "source_class_exists",
           "target_class_exists"]

df = pd.DataFrame(data, columns=columns)
print(df.head())
df.to_csv(data_folder.joinpath("refminer_data/stats.csv"), index=False)
