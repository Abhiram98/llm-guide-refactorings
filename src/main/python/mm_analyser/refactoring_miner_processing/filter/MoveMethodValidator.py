import pathlib
import subprocess
import os
import json

import mm_analyser.refactoring_miner_processing.MethodSignature as MethodSignature
import mm_analyser.refactoring_miner_processing.filter.RminerValidator as rm
import mm_analyser.refactoring_miner_processing.filter.MoveMethodRef as MoveMethodRef


class Field:
    def __init__(self, name: str, type: str):
        self.name = name
        self.type = type


class MoveMethodValidator(rm.RminerValidator):
    type = "Move Method"

    def preconditions(self, moveref: MoveMethodRef.MoveMethodRef):
        # return True
        # checkout before after
        self.repo.git.checkout(self.commit_after, force=True)
        both_files_exist = (os.path.exists(
            os.path.join(self.project_basepath, moveref.right_file_path)
        ) and os.path.exists(
            os.path.join(self.project_basepath, moveref.left_file_path)
        )
            # and 'test' not in moveref.left_file_path
            # and 'test' not in moveref.right_file_path
            and self.class_exists(moveref.original_class, moveref.left_file_path)
            and self.class_exists(moveref.target_class, moveref.right_file_path)
        )

        if not both_files_exist:
            return False

        # checkout commit before
        self.repo.git.checkout(self.commit_before, force=True)
        return (os.path.exists(
            os.path.join(self.project_basepath, moveref.right_file_path)
        ) and os.path.exists(
            os.path.join(self.project_basepath, moveref.left_file_path)
        )
          and self.class_exists(moveref.target_class, moveref.right_file_path)
          and self.class_exists(moveref.original_class, moveref.left_file_path)
                )

    def get_valid_moves(self):

        for ref in self.refminer_commit_data['refactorings']:
            if ref['type'] == 'Move Method':

                movemethod_obj = MoveMethodRef.MoveMethodRef.create_from(ref)

                if not self.preconditions(movemethod_obj):
                    continue  # don't add to list
                if self.is_method_before_static(movemethod_obj):
                    ref['isStatic'] = True
                    if self.is_tp_static_move(movemethod_obj):
                        new_data = MoveMethodValidator.create_new_data(
                            self.refminer_commit_data, ref)
                        self.tp_moves.append(new_data)

                else:
                    ref['isStatic'] = False
                    if self.is_tp_instance_move(movemethod_obj):
                        print("Found instance move!")
                        new_data = MoveMethodValidator.create_new_data(
                            self.refminer_commit_data, ref)
                        self.tp_moves.append(new_data)
                    else:
                        print("False-positive.")
        return self.tp_moves

    def is_tp_instance_move(self, movemethod_obj):
        """
        MM is a TP if any of the following are true:
        1. Moved to a type in the original parameter list
        2. Moved to a type in the original class' fields
        3. New method signature contains a parameter of the original class.
        """
        # return True
        for param in movemethod_obj.right_signature.params:
            if param.param_type == movemethod_obj.original_class.split('.')[-1]:
                return True

        for param in movemethod_obj.left_signature.params:
            if param.param_type == movemethod_obj.target_class.split('.')[-1]:
                return True

        try:
            original_class_fields = self.find_field_types(
                movemethod_obj.left_file_path, movemethod_obj.original_class)
        except Exception as e:
            print(e)
            raise e

        return any(
            [field.type == movemethod_obj.target_class.split('.')[-1] for field in original_class_fields]
        )

    def is_tp_static_move(self, movemethod_obj: MoveMethodRef.MoveMethodRef):
        return True

    def find_field_types(self, left_file_path, original_class):
        self.repo.git.checkout(self.commit_before, force=True)
        outputpath = f"{MoveMethodValidator.output_dir}/fieldTypes.json"
        filepath = os.path.join(self.project_basepath, left_file_path)
        result = subprocess.run([
            MoveMethodValidator.gradle_path,
            "-p", str(pathlib.Path(MoveMethodValidator.gradle_path).parent),
            "run",
            f"--args="
            f"findFieldTypes -i {filepath} -o {outputpath} -c \'{original_class}\'"
        ], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode != 0:
            raise Exception(f"Failed to find field types of class {original_class}")
        with open(outputpath) as f:
            field_data = json.load(f)
        subprocess.run(['rm', outputpath])
        return [Field(field['field_name'], field['field_type']) for field in field_data]

    def is_class_static(self, class_qualified_name, file_path):
        self.repo.git.checkout(self.commit_before, force=True)
        outputpath = f"{MoveMethodValidator.output_dir}/isClassStatic.txt"
        filepath = os.path.join(self.project_basepath, file_path)
        result = subprocess.run([
            MoveMethodValidator.gradle_path,
            "-p", str(pathlib.Path(MoveMethodValidator.gradle_path).parent),
            "run",
            f"--args="
            f"checkIfClassStatic -i {filepath} -o {outputpath} -c \'{class_qualified_name}\'"
        ], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode != 0:
            raise Exception(f"Failed to find if class {class_qualified_name} is static.")
        with open(outputpath) as f:
            is_static = f.read() == 'true'
        subprocess.run(['rm', outputpath])
        return is_static

    def class_exists(self, class_qualified_name, file_path):
        outputpath = f"{MoveMethodValidator.output_dir}/classExists.txt"
        filepath = os.path.join(self.project_basepath, file_path)
        result = subprocess.run([
            MoveMethodValidator.gradle_path,
            "-p", str(pathlib.Path(MoveMethodValidator.gradle_path).parent),
            "run",
            f"--args="
            f"checkIfClassExists -i {filepath} -o {outputpath} -c \'{class_qualified_name}\'"
        ], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode != 0:
            raise Exception(f"Failed to find if class {class_qualified_name}\
                                 exists.")
        with open(outputpath) as f:
            class_exists = f.read() == 'true'
        subprocess.run(['rm', outputpath])
        return class_exists
