import mm_analyser

import os
import json
from pydantic import BaseModel, Field, computed_field
import mm_analyser.refactoring_miner_processing.filter.ExtractMoveMethodValidator as em_ref


rminer_data_path = f"{mm_analyser.data_folder}/refminer_data/contains_a_mm"
files = os.listdir(rminer_data_path)
json_files = [i for i in files if i.endswith(".json")]

data = []
total_count = 0


class RealWorldInstanceOraclePoint(BaseModel):
    move_method_ref: em_ref.ExtractMoveMethodRef = Field(description="the extract-move-method refactoring"
                                                                     " object to be emulated")
    ref_id: int = Field(description="refactoring id.")
    project_git: str = Field(description="git url of the project")
    project_branch_name: str = Field(description="name of the branch on which to operate")

    model_config = {'arbitrary_types_allowed': True}

    @computed_field
    @property
    def project_name(self) -> str:
        return self.project_git.split('.git')[0].split('/')[-1]

def get_instance_oracle() -> list[RealWorldInstanceOraclePoint]:
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
    for file_name in plugin_outfiles:
        with open(f'{mm_analyser.data_folder}/refminer_data/mm-assist-emm/{file_name}') as f:
            data = json.load(f)
        combined_output += data

    IGNORED_IDS = [40, 42, 44, 46, 583, 584]
    combined_output = [i for i in combined_output if 'telemetry' in i
                       and len(i['telemetry'].keys())
                       and i['move_method_refactoring']['isStatic'] == False
                       and i['ref_id'] not in IGNORED_IDS]
    print(f"{len(combined_output)=}")

    return [
        RealWorldInstanceOraclePoint(
            move_method_ref=em_ref.ExtractMoveMethodRef.create_from(evaluation_data['move_method_refactoring']),
            ref_id=evaluation_data['ref_id'],
            project_git=evaluation_data['repository'],
            project_branch_name=evaluation_data['extraction_results']['newBranchName']
        )
        for evaluation_data in combined_output
    ]

if __name__ == '__main__':
    oracle = get_instance_oracle()
    for i in oracle:
        print(i.move_method_ref.left_file_path)
        print(i.move_method_ref.original_class)
        print("-"*20)
    print(oracle)
