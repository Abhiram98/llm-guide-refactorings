import mm_analyser.hmove.compute_input_synthetic as hmove_in
import mm_analyser
import mm_analyser.refactoring_miner_processing.oracle as rminer_oracle

from collections import defaultdict
import json


class HMovePreparerRW(hmove_in.HMovePreparer):

    def compute(self):
        hmove_data: dict[str, dict[int, list[hmove_in.HMoveInput]]] = defaultdict(lambda: defaultdict(list))
        for oracle in rminer_oracle.get_instance_oracle():
            project_directory = self.project_directory_path.joinpath(oracle.project_name)

            self.source_dirs = self.find_source_dirs(project_directory)

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
                oracle.move_method_ref.left_file_path,
                self.source_dirs)
            print(methods_in_class)
            class_fields = self.get_class_fields(
                oracle.move_method_ref.original_class,
                oracle.move_method_ref.left_file_path,
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
                        print(f"Failed to find path of {target_class}")
                        continue

                    hmove_data[oracle.project_name][oracle.ref_id].append(
                        hmove_in.HMoveInput(
                            method_information=method,
                            source_class_path=str(
                                oracle.move_method_ref.left_file_path.relative_to(self.project_directory_path)),
                            target_class_path=str(
                                target_class_path.relative_to(self.project_directory_path))
                        ).model_dump(mode='json')
                    )

            with open(mm_analyser.data_folder.joinpath(
                    f"synthetic_corpus_comparison/hmove/input_rw/{oracle.project_name}.json"), "w") as f:
                json.dump(hmove_data[oracle.project_name], f, indent=4)


if __name__ == '__main__':
    import os
    eval_projects_path = os.getenv('EVALUATION_PROJECTS_PATH')
    HMovePreparerRW(project_directory_path=eval_projects_path).compute()
