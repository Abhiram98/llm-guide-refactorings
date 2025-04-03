import mm_analyser

import git
import os
import json
from pathlib import Path
from pydantic import BaseModel, Field, computed_field
import pandas as pd
import mm_analyser.refactoring_miner_processing.filter.ExtractMoveMethodValidator as em_ref
import mm_analyser.refactoring_miner_processing.filter.MoveMethodRef as mm_ref
from mm_analyser.env import PROJECTS_BASE_PATH

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

    @computed_field
    @property
    def alias_method_name(self) -> str:
        return self.move_method_ref.right_signature.method_name +'1'


    @computed_field
    @property
    def method_name(self) -> str:
        return self.move_method_ref.right_signature.method_name


class RealWorldStaticOraclePoint(RealWorldInstanceOraclePoint):
    move_method_ref: mm_ref.MoveMethodRef = Field(description="the extract-move-method refactoring"
                                                                 " object to be emulated")
    ref_id: str = Field(description="refactoring id.")
    head_commit_hash: str = Field(description="commit hash after move-method is complete")
    prev_commit_hash: str = Field(description="commit hash before move-method is done")

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

def get_static_oracle() -> list[RealWorldStaticOraclePoint]:
    static_oracle = pd.read_csv(mm_analyser.data_folder.joinpath('refminer_data/static_moves.csv').absolute())
    oracle_descriptions = list(static_oracle['description_x'])
    static_data_path = mm_analyser.data_folder.joinpath('refminer_data/filter_fp_2')
    json_files = [i for i in os.listdir(static_data_path) if i.endswith('.json')]
    oracle: list[RealWorldStaticOraclePoint] = []
    descriptions_found = []

    ref_id_counter = 1
    for fname in json_files:
        with open(static_data_path.joinpath(fname)) as f:
            data = json.load(f)

        for d in data:
            if d['move_method_refactoring']['description'] in oracle_descriptions:
                descriptions_found.append(d['move_method_refactoring']['description'])
                # print("found one")
                move_method = mm_ref.MoveMethodRef.create_from(d['move_method_refactoring'])
                repo_url = d['repository']
                project_name = repo_url.split('.git')[0].split('/')[-1]
                repo = git.Repo(Path(PROJECTS_BASE_PATH).joinpath(project_name))
                oracle.append(
                    RealWorldStaticOraclePoint(
                        move_method_ref=move_method,
                        ref_id=f"static-{ref_id_counter}",
                        project_git=d['repository'],
                        prev_commit_hash=str(repo.commit(d['sha1']).parents[0]),
                        head_commit_hash=d['sha1'],
                        project_branch_name="unknown"
                    )
                )
                ref_id_counter += 1
    print(f"descriptions not found = {set(oracle_descriptions) - set(descriptions_found)})")
    return oracle


if __name__ == '__main__':
    oracle = get_instance_oracle()
    for i in oracle:
        print(i.move_method_ref.left_file_path)
        print(i.move_method_ref.original_class)
        print("-"*20)
    print(oracle)

    static_oracle = get_static_oracle()
    print(f"{len(static_oracle)=}")
