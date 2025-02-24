import json
import mm_analyser
import mm_analyser.jmove_dataset.oracle as jmove_oracle
from pydantic import BaseModel, Field
from pathlib import Path
import xml.etree.ElementTree as ET
import subprocess
from collections import defaultdict
import os

import mm_analyser.refactoring_miner_processing.MethodSignature as method_signature


class MethodInformation(BaseModel):
    line_num: int = Field(description="line num of the method, usually where the docstring begins.")
    method_name: str = Field(description="name of method")
    parameter_types: list[method_signature.Parameter] = Field(description="fully qualified parameter names and types.")
    method_name_start_line: int = Field(description="startline of the method name (typically, after the docstring)")
    return_type: str = Field(description="return type")
    modifier: str = Field(description="modifier (public/private, ...)")

    model_config = {'arbitrary_types_allowed': True}


class FieldInfo(BaseModel):
    field_name: str = Field(description="field name")
    field_type: str = Field(description="field type")
    field_declaration: str = Field(description="field declaration statement")


class HMoveInput(BaseModel):
    method_information: MethodInformation = Field(description="line number of the method2move.")
    source_class_path: str = Field(description="path to the source class")
    target_class_path: str = Field(description="path to the destination class")


class HMovePreparer:
    output_dir = f"{mm_analyser.data_folder}/refminer_data"
    gradle_path = f"{mm_analyser.project_root}/gradlew"

    def __init__(self, project_directory_path: str):
        self.source_dirs: list[str] = []
        self.project_directory_path: Path = Path(project_directory_path)

    def get_methods_in_class(self, source_class: str,
                             file_path: Path, source_dirs: list[Path]) -> list[MethodInformation]:
        """
        Return method name + signature + line number for all methods in class.
        :return:
        """
        source_dirs_ = ";".join([str(i) for i in source_dirs])
        outputpath = f"{HMovePreparer.output_dir}/methodInformation.json"
        result = subprocess.run([
            HMovePreparer.gradle_path,
            "-p", str(Path(HMovePreparer.gradle_path).parent),
            "run",
            f"--args="
            f"methodInformation -i \'{file_path}\' -o {outputpath} -c \'{source_class}\' -s \'{source_dirs_}\'"
        ], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode != 0:
            print(result.stderr.decode('utf-8'))
            print(result.stdout.decode('utf-8'))
            raise Exception(f"Failed to find methods of class {source_class}")
        with open(outputpath) as f:
            field_data = json.load(f)
        subprocess.run(['rm', outputpath])
        return [
            MethodInformation(method_name=field['methodName'],
                              method_name_start_line=field['methodNameStartLine'],
                              line_num=field['startLineNum'],
                              return_type=field['returnType'],
                              parameter_types=[method_signature.Parameter(param_type=p['type'], param_name=p['name'])
                                               for p in
                                               field['paramsList']],
                              modifier=field['modifier']
                              )
            for field in field_data]

    def compute(self):
        hmove_data: dict[dict[str, list[HMoveInput]]] = defaultdict(lambda: defaultdict(list))
        for data in jmove_oracle.oracle_data:
            outer_path = (self.project_directory_path
                          .joinpath(data.project_name)
                          .joinpath(data.method_size)
                          .joinpath('big' if data.method_size == 'large' else 'small'))
            proj_ = [i for i in os.listdir(outer_path) if data.project_name in i.lower()][0]
            project_directory: Path = outer_path.joinpath(proj_)

            # Find all source directories associated with the project.
            self.source_dirs = self.find_source_dirs(project_directory)

            print(f"{data.source_class=}")
            print(f"{data.target_class=}")
            source_class_path = self.get_path_from_qualname(data.source_class, self.source_dirs)
            oracle_target_class_path = self.get_path_from_qualname(data.target_class, self.source_dirs)
            print(f"{source_class_path=}")
            print(f"{oracle_target_class_path=}")
            print()

            methods_in_class = self.get_methods_in_class(data.source_class, source_class_path, self.source_dirs)
            print(methods_in_class)
            class_fields = self.get_class_fields(data.source_class, source_class_path, self.source_dirs)
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

                    hmove_data[data.project_name+data.method_size][f"{data.source_class}::{data.method_signature}->{data.target_class}"].append(
                        HMoveInput(method_information=method,
                                   source_class_path=str(source_class_path.relative_to(self.project_directory_path)),
                                   target_class_path=str(target_class_path.relative_to(self.project_directory_path))
                                   ).model_dump(mode='json')
                    )


            with open(mm_analyser.data_folder.joinpath(f"synthetic_corpus_comparison/hmove/input/{data.project_name}_{data.method_size}.json"), "w") as f:
                json.dump(hmove_data[data.project_name+data.method_size], f, indent=4)
            # print(data.method_signature)

    def get_class_fields(self, source_class: str, file_path: Path, source_dirs: list[Path]):
        source_dirs_ = ";".join([str(i) for i in source_dirs])
        outputpath = f"{HMovePreparer.output_dir}/classFieldInfo.json"
        result = subprocess.run([
            HMovePreparer.gradle_path,
            "-p", str(Path(HMovePreparer.gradle_path).parent),
            "run",
            f"--args="
            f"findQualFieldTypes -i \'{file_path}\' -o \'{outputpath}\' -c \'{source_class}\' -s \'{source_dirs_}\'"
        ], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode != 0:
            print(result.stderr.decode('utf-8'))
            print(result.stdout.decode('utf-8'))
            raise Exception(f"Failed to find field types of class {source_class}")
        with open(outputpath) as f:
            field_data = json.load(f)
        subprocess.run(['rm', outputpath])
        return [FieldInfo(**field) for field in field_data]

    def get_method_params(self, method, source_class):
        return []

    def get_path_from_qualname(self, qualified_name: str, source_dirs: list[Path]):
        path_parts = qualified_name.split('.')
        lower_ = [i for i in path_parts if i[0].islower()]
        upper_ = [i for i in path_parts if i[0].isupper()]
        if len(upper_):
            lower_ += [upper_[0]]
        rel_file_path = "/".join(lower_) + '.java'

        candidates = []
        for src_dir in source_dirs:
            if src_dir.joinpath(rel_file_path).exists():
                candidates.append(src_dir.joinpath(rel_file_path))

        if len(candidates) == 1:
            return candidates[0]
        elif len(candidates) > 1:
            raise Exception(f"too many options: {candidates}")
        raise Exception("Couldn't find file.")

    def find_source_dirs(self, directory: Path) -> list[Path]:
        classpath_file = directory.joinpath('.classpath')
        if os.path.exists(classpath_file):
            # XML parse and read src directories.
            parsed_file = ET.parse(classpath_file)
            src_dirs = []
            for tag in parsed_file.findall('classpathentry'):
                if tag.get('kind') == 'src':
                    src_dirs.append(
                        directory.joinpath(tag.get('path'))
                    )
            return src_dirs
        return self.find_sources_bruteforce(directory)

    def filter_classes_in_proj(self, class_fields: list[str], source_dirs) -> list[str]:
        '''Checks to see which of the classes are actually in the project.'''
        return class_fields

    def find_sources_bruteforce(self, directory) -> list[Path]:
        """
        Searches for directories containing the structure src/main/java, or src/test java.
        """
        matching_directories = []

        for item in os.walk(directory):
            current_path, sub_dirs, files = item
            if 'src' in sub_dirs:
                if Path(current_path).joinpath('src/main/java').exists():
                    matching_directories.append(Path(current_path).joinpath('src/main/java'))
                if Path(current_path).joinpath('src/test/java').exists():
                    matching_directories.append(Path(current_path).joinpath('src/test/java'))
            if 'java' in sub_dirs:
                if Path(current_path).joinpath('java/java/src').exists():
                    matching_directories.append(Path(current_path).joinpath('java/java/src'))
        return matching_directories

if __name__ == '__main__':

    jmove_directory_path = os.getenv('JMOVE_DIRECTORY_PATH')
    HMovePreparer(project_directory_path=jmove_directory_path).compute()
