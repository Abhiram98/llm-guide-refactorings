import os
import subprocess
import pathlib
import json
import functools

import mm_analyser.refactoring_miner_processing.filter.RminerValidator as rv
import mm_analyser.refactoring_miner_processing.filter.MoveMethodRef as mm
import mm_analyser.refactoring_miner_processing.MethodSignature as MethodSignature
import mm_analyser.refactoring_miner_processing.MethodInvocation as mi


class ExtractedRange:
    def __init__(self, start_line, start_column, end_line, end_column):
        self.start_line = start_line
        self.start_column = start_column
        self.end_line = end_line
        self.end_column = end_column

    def __eq__(self, other):
        if not isinstance(other, ExtractedRange):
            return False
        return (other.start_line == self.end_line
                and other.start_column == self.start_column
                and other.end_line == self.end_line
                and other.end_column == self.end_column
                )

    def __gt__(self, other):
        if not isinstance(other, ExtractedRange):
            return NotImplemented
        other_size = other.end_line - other.start_line
        this_size = self.end_line - self.start_line
        if other_size == this_size:
            this_start = self.start_column
            other_start = other.start_column
            return other_start > this_start if this_start != other_size \
                else other.end_column > self.end_column
        return other_size < this_size

    def __str__(self):
        return f"{self.start_line}:{self.start_column} - {self.end_line}:{self.end_column}"

    def __repr__(self):
        return str(self)


class ExtractMoveMethodRef(mm.MoveMethodRef):

    def __init__(self, right_file_path, left_file_path, right_signature: mm.MethodSignature,
                 left_signature: mm.MethodSignature, original_class, target_class, description,
                 extracted_range: ExtractedRange, extracted_method_invocation: mi.MethodInvocation
                 ):
        super().__init__(right_file_path, left_file_path, right_signature, left_signature, original_class, target_class,
                         description)
        self.extracted_range = extracted_range
        self.extracted_method_invocation = extracted_method_invocation

    @staticmethod
    def create_from(ref):
        split_from_class = ref['description'].split(" & moved to class ")
        original_class = split_from_class[0].split(" in class ")[-1]
        target_class = split_from_class[-1]
        source_method_decl = [i for i in ref['leftSideLocations']
                              if i['description'] == "source method declaration before extraction"][0]
        left_code_element = source_method_decl['codeElement']
        target_method_decl = [i for i in ref['rightSideLocations']
                              if i['description'] == "extracted method declaration"][0]
        right_code_element = target_method_decl['codeElement']
        left_signature = MethodSignature.MethodSignature.get_method_signature_parts(left_code_element)
        right_signature = MethodSignature.MethodSignature.get_method_signature_parts(right_code_element)

        left_filepath = source_method_decl['filePath']
        right_filepath = target_method_decl['filePath']

        extracted_code = [i for i in ref['leftSideLocations']
                          if i['description'] == "extracted code from source method declaration"]

        extracted_ranges = [ExtractedRange(ec['startLine'], ec['startColumn'],
                                           ec['endLine'], ec['endColumn'])
                            for ec in extracted_code]
        extracted_range = max(extracted_ranges)

        extracted_method_invocation = [i for i in ref['rightSideLocations']
                                       if i['description'] == 'extracted method invocation'][0]
        method_invocation = mi.MethodInvocation.create_from_call_str(extracted_method_invocation['codeElement'])
        return ExtractMoveMethodRef(right_filepath, left_filepath,
                                    right_signature, left_signature,
                                    original_class, target_class,
                                    ref['description'],
                                    extracted_range,
                                    method_invocation)

    def key(self):
        return self.left_file_path + self.right_signature.method_name


class ExtractMoveMethodValidator(rv.RminerValidator):
    type = "Extract And Move Method"

    def get_valid_moves(self):
        for ref in self.refminer_commit_data['refactorings']:
            if ref['type'] == ExtractMoveMethodValidator.type:

                emm_obj = ExtractMoveMethodRef.create_from(ref)

                if not self.preconditions(emm_obj):
                    continue  # don't add to list
                if self.is_method_after_static(emm_obj):
                    ref['isStatic'] = True
                    if self.is_tp_static_move(emm_obj):
                        new_data = super().create_new_data(
                            self.refminer_commit_data, ref)
                        self.tp_moves.append(new_data)

                else:
                    ref['isStatic'] = False
                    if self.is_tp_instance_move(emm_obj):
                        print("Found instance move!")
                        new_data = super().create_new_data(
                            self.refminer_commit_data, ref)
                        self.tp_moves.append(new_data)
                    else:
                        print("False-positive.")
        return self.tp_moves

    def preconditions(self, emm_obj: ExtractMoveMethodRef):
        return True

    def is_tp_instance_move(self, emm_obj):
        return emm_obj.extracted_method_invocation.var_name is not None \
            and emm_obj.extracted_method_invocation.var_name[0].islower()

    def is_tp_static_move(self, emm_obj):
        return True

    def findTypesInRange(self, emm_obj: ExtractMoveMethodRef, file_path):

        outputpath = f"{ExtractMoveMethodValidator.output_dir}/typesInRange.json"
        filepath = os.path.join(self.project_basepath, file_path)
        result = subprocess.run([
            super().gradle_path,
            "-p", str(pathlib.Path(super().gradle_path).parent),
            "run",
            f"--args="
            f"findTypesInRange -i {filepath} -o {outputpath} -s {emm_obj.extraction_start} -e {emm_obj.extraction_end}"
        ], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode != 0:
            raise Exception(f"Failed to find if types in range for {filepath}")
        with open(outputpath) as f:
            type_in_range = json.load(f)
        subprocess.run(['rm', outputpath])
        return type_in_range
