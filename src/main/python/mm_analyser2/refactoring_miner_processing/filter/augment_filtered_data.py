import json
import os
import pandas as pd

from mm_analyser import data_folder
import mm_analyser.refactoring_miner_processing.filter.ExtractMoveMethodValidator as emval
from mm_analyser.env import PROJECTS_BASE_PATH, PROJECT_ALIAS_MAP

rminer_data_path = f"{data_folder}/refminer_data/mm-assist-emm"
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
        # if not m['move_method_refactoring']['isStatic']:
        #     continue
        ref_id = m['ref_id']
        oracle = m['move_method_refactoring']
        mm_obj = emval.ExtractMoveMethodRef.create_from(oracle)

        validator = emval.ExtractMoveMethodValidator(
            m,
            os.path.join(PROJECTS_BASE_PATH, PROJECT_ALIAS_MAP[fname])
        )

        validator.checkout_before()
        method_count = validator.get_method_count(mm_obj.left_file_path)

        data.append(
            (
                ref_id,
                m['url'],
                mm_obj.description,
                method_count
            )
        )

columns = ["ref_id", "url", "description",
           "method_count"]

df = pd.DataFrame(data, columns=columns)
print(df.head())
df.to_csv(data_folder.joinpath("refminer_data/emm_method_count.csv"), index=False)
