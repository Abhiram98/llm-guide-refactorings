import pathlib
import subprocess
import os
import git
import json

import mm_analyser.refactoring_miner_processing.MethodSignature as MethodSignature


class Field:
    def __init__(self, name: str, type: str):
        self.name = name
        self.type = type


class MoveMethodRef:
    def __init__(self, right_file_path, left_file_path,
                 right_signature: MethodSignature, left_signature: MethodSignature,
                 original_class, target_class,
                 description
                 ):
        self.left_file_path = left_file_path
        self.right_file_path = right_file_path
        self.right_signature = right_signature
        self.left_signature = left_signature
        self.original_class = original_class
        self.target_class = target_class
        self.description = description

    def __str__(self):
        return (f"{self.left_signature.method_name}: "
                f"{self.original_class.split('.')[-1]} -> {self.target_class.split('.')[-1]}")

    @staticmethod
    def create_from(refminer_move):
        split_from_class = refminer_move['description'].split("from class ")
        original_class = split_from_class[1].split(" to ")[0]
        target_class = split_from_class[-1]
        left_code_element = refminer_move['leftSideLocations'][0]['codeElement']
        right_code_element = refminer_move['rightSideLocations'][0]['codeElement']
        left_signature = MethodSignature.MethodSignature.get_method_signature_parts(left_code_element)
        right_signature = MethodSignature.MethodSignature.get_method_signature_parts(right_code_element)
        left_filepath = refminer_move['leftSideLocations'][0]['filePath']
        right_filepath = refminer_move['rightSideLocations'][0]['filePath']

        return MoveMethodRef(right_filepath, left_filepath,
                             right_signature, left_signature,
                             original_class, target_class,
                             refminer_move['description'])


class MoveMethodValidator:
    gradle_path = "/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/gradlew"

    def __init__(self, refminer_commit_data, project_basepath):
        self.refminer_commit_data = refminer_commit_data
        self.tp_moves = []

        self.project_basepath = project_basepath
        self.repo = git.Repo(project_basepath)
        self.commit_after = refminer_commit_data['sha1']
        self.commit_before = self.repo.commit(self.commit_after).parents[0]

    def validate(self):
        return True

    def preconditions(self, moveref: MoveMethodRef):
        # checkout before after
        self.repo.git.checkout(self.commit_after, force=True)
        both_files_exist = (os.path.exists(
            os.path.join(self.project_basepath, moveref.right_file_path)
        ) and os.path.exists(
            os.path.join(self.project_basepath, moveref.left_file_path)
        ) and self.class_exists(moveref.original_class, moveref.left_file_path)
          and self.class_exists(moveref.target_class, moveref.right_file_path))

        if not both_files_exist:
            return False

        # checkout before before
        self.repo.git.checkout(self.commit_before, force=True)
        return (os.path.exists(
            os.path.join(self.project_basepath, moveref.right_file_path)
        ) and os.path.exists(
            os.path.join(self.project_basepath, moveref.left_file_path)
        ) and self.class_exists(moveref.target_class, moveref.right_file_path)
          and self.class_exists(moveref.original_class, moveref.left_file_path))

    def get_valid_moves(self):

        for ref in self.refminer_commit_data['refactorings']:
            if ref['type'] == 'Move Method':

                movemethod_obj = MoveMethodRef.create_from(ref)

                if not self.preconditions(movemethod_obj):
                    continue  # don't add to list
                if self.isStaticMove(movemethod_obj):
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

    def isStaticMove(self, ref: MoveMethodRef):
        self.repo.git.checkout(self.commit_before, force=True)
        outputpath = "/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/refminer_data/isStaticOut.txt"
        filepath = os.path.join(self.project_basepath, ref.left_file_path)
        result = subprocess.run([
            MoveMethodValidator.gradle_path,
            "-p", str(pathlib.Path(MoveMethodValidator.gradle_path).parent),
            "run",
            f"--args="
            f"checkIfStatic -i {filepath} -o {outputpath} -s \'{ref.left_signature}\'"
        ], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode != 0:
            # return False
            raise Exception("Failed to find if method is static")
        with open(outputpath) as f1:
            isStatic = f1.read() == 'true'
        subprocess.run(['rm', outputpath])

        return isStatic

    def is_tp_instance_move(self, movemethod_obj):
        """
        MM is a TP if any of the following are true:
        1. Moved to a type in the original parameter list
        2. Moved to a type in the original class' fields
        3. New method signature contains a parameter of the original class.
        """

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

    def is_tp_static_move(self, movemethod_obj: MoveMethodRef):
        return True

    @staticmethod
    def create_new_data(i, ref):
        new_data = {
            **i,
            "move_method_refactoring": ref
        }
        new_data.pop("refactorings")
        return new_data

    def find_field_types(self, left_file_path, original_class):
        self.repo.git.checkout(self.commit_before, force=True)
        outputpath = "/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/refminer_data/fieldTypes.json"
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
        outputpath = "/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/refminer_data/isClassStatic.txt"
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
        outputpath = "/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/refminer_data/classExists.txt"
        filepath = os.path.join(self.project_basepath, file_path)
        result = subprocess.run([
            MoveMethodValidator.gradle_path,
            "-p", str(pathlib.Path(MoveMethodValidator.gradle_path).parent),
            "run",
            f"--args="
            f"checkIfClassExists -i {filepath} -o {outputpath} -c \'{class_qualified_name}\'"
        ], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode != 0:
            raise Exception(f"Failed to find if class {class_qualified_name} exists.")
        with open(outputpath) as f:
            is_static = f.read() == 'true'
        subprocess.run(['rm', outputpath])
        return is_static

    def checkout_before(self):
        self.repo.git.checkout(self.commit_before, force=True)

    def checkout_after(self):
        self.repo.git.checkout(self.commit_after, force=True)
