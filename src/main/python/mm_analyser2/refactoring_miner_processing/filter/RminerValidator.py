import os
import pathlib
import subprocess

import git

from mm_analyser import data_folder, project_root
import mm_analyser.refactoring_miner_processing.filter.MoveMethodRef as MoveMethodRef

class RminerValidator:
    output_dir = f"{data_folder}/refminer_data"
    gradle_path = f"{project_root}/gradlew"

    def __init__(self, refminer_commit_data, project_basepath):
        self.refminer_commit_data = refminer_commit_data
        self.tp_moves = []

        self.project_basepath = project_basepath
        self.repo = git.Repo(project_basepath)
        self.commit_after = refminer_commit_data['sha1']
        self.commit_before = self.repo.commit(self.commit_after).parents[0]

    @staticmethod
    def create_new_data(i, ref):
        new_data = {
            **i,
            "move_method_refactoring": ref
        }
        new_data.pop("refactorings")
        return new_data

    def checkout_after(self):
        self.repo.git.checkout(self.commit_after, force=True)

    def checkout_before(self):
        self.repo.git.checkout(self.commit_before, force=True)

    def is_method_before_static(self, ref: MoveMethodRef.MoveMethodRef):
        self.repo.git.checkout(self.commit_before, force=True)
        return self.is_method_static(ref.left_file_path, ref.left_signature)

    def is_method_after_static(self, ref: MoveMethodRef.MoveMethodRef):
        self.repo.git.checkout(self.commit_after, force=True)
        return self.is_method_static(ref.right_file_path, ref.right_signature)

    def is_method_static(self, file_path, signature):
        outputpath = f"{RminerValidator.output_dir}/isStaticOut.txt"
        complete_filepath = os.path.join(self.project_basepath, file_path)
        result = subprocess.run([
            RminerValidator.gradle_path,
            "-p", str(pathlib.Path(RminerValidator.gradle_path).parent),
            "run",
            f"--args="
            f"checkIfStatic -i {complete_filepath} -o {outputpath} -s \'{signature}\'"
        ], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode != 0:
            # return False
            raise Exception("Failed to find if method is static")
        with open(outputpath) as f1:
            isStatic = f1.read() == 'true'
        subprocess.run(['rm', outputpath])
        return isStatic

    def get_method_count(self, file_path):
        outputpath = f"{RminerValidator.output_dir}/methodCount.txt"
        complete_filepath = os.path.join(self.project_basepath, file_path)
        result = subprocess.run([
            RminerValidator.gradle_path,
            "-p", str(pathlib.Path(RminerValidator.gradle_path).parent),
            "run",
            f"--args="
            f"methodCount -i {complete_filepath} -o {outputpath}"
        ], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode != 0:
            # return False
            raise Exception("Failed to find method count")
        with open(outputpath) as f1:
            method_count = int(f1.read())
        subprocess.run(['rm', outputpath])
        return method_count
