import mm_analyser.hmove.compute_input_synthetic as hmove_in
import mm_analyser
import mm_analyser.refactoring_miner_processing.oracle as rminer_oracle

from collections import defaultdict
import json
import git
from typing import Optional


class HMovePreparerRW(hmove_in.HMovePreparer):

    def __init__(self, project_directory_path: str, specific_projects: Optional[list[str]] = None, exclude_projects: Optional[list[str]]=None):
        self.exclude_projects = exclude_projects
        self.specific_project = specific_projects
        self.source_patterns = {
            'dbeaver': ['src'],
            'graal': ['src']
        }
        super().__init__(project_directory_path)

    def compute(self):
        hmove_data: dict[str, dict[int, list[hmove_in.HMoveInput]]] = defaultdict(lambda: defaultdict(list))
        for oracle in rminer_oracle.get_instance_oracle():
            if (self.specific_project is not None
                    and oracle.project_name not in self.specific_project):
                print(f"Skipping {oracle.ref_id} because {oracle.project_name} not in {self.specific_project}")
                continue
            if (self.exclude_projects is not None
                    and oracle.project_name in self.exclude_projects):
                print(f"Skipping {oracle.ref_id} because {oracle.project_name} in {self.exclude_projects}")
                continue

            project_directory = self.project_directory_path.joinpath(oracle.project_name)
            project_git = git.Repo(project_directory.joinpath('.git'))
            project_git.git.checkout(oracle.project_branch_name)  # Checkout the appropriate branch.

            self.source_dirs = self.find_source_dirs(project_directory,
                                                     source_patterns=self.source_patterns.get(oracle.project_name))
            self.source_dirs = [i for i in self.source_dirs if i.exists()]

            print(f"{oracle.move_method_ref.original_class=}")
            print(f"{oracle.move_method_ref.target_class=}")
            # source_class_path = self.get_path_from_qualname(
            #     oracle.move_method_ref.original_class, self.source_dirs)
            # oracle_target_class_path = self.get_path_from_qualname(
            #     oracle.move_method_ref.target_class, self.source_dirs)
            print(f"{oracle.move_method_ref.left_file_path=}")
            print(f"{oracle.move_method_ref.right_file_path=}")
            print()

            methods_in_class = self.get_methods_in_class(
                oracle.move_method_ref.original_class,
                project_directory.joinpath(oracle.move_method_ref.left_file_path),
                self.source_dirs)
            print(methods_in_class)
            class_fields = self.get_class_fields(
                oracle.move_method_ref.original_class,
                project_directory.joinpath(oracle.move_method_ref.left_file_path),
                self.source_dirs)
            print(class_fields)

            for method in methods_in_class:
                # method_params = self.get_method_params(method, data.source_class)
                #     method_params = self.filter_classes_in_proj(method_params, self.source_dirs)
                target_classes = [i.field_type for i in class_fields] + [i.param_type for i in method.parameter_types]
                for target_class in target_classes:
                    try:
                        target_class_path = self.get_path_from_qualname(target_class, self.source_dirs)
                    except:
                        known_names = ['java.', 'javax.', 'boolean', 'int', 'T', 'long', 'double', 'short']
                        if not any(k in target_class for k in known_names):
                            print(f"Failed to find path of {target_class}")
                        # print(f"Failed to find path of {target_class}")
                        continue

                    hmove_data[oracle.project_name][oracle.ref_id].append(
                        hmove_in.HMoveInput(
                            method_information=method,
                            source_class_path=oracle.move_method_ref.left_file_path,
                            target_class_path=str(
                                target_class_path.relative_to(self.project_directory_path))
                        ).model_dump(mode='json')
                    )

            with open(mm_analyser.data_folder.joinpath(
                    f"refminer_data/hmove/input/{oracle.project_name}.json"), "w") as f:
                json.dump(hmove_data[oracle.project_name], f, indent=4)


if __name__ == '__main__':
    import os
    from mm_analyser.env import PROJECT_ALIAS_MAP, PROJECTS_BASE_PATH

    HMovePreparerRW(project_directory_path=PROJECTS_BASE_PATH,
                    specific_projects=['selenium']).compute()
