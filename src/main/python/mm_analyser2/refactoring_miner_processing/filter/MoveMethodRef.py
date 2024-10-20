import mm_analyser.refactoring_miner_processing.MethodSignature as MethodSignature


class MoveMethodRef:
    def __init__(self, right_file_path, left_file_path,
                 right_signature: MethodSignature.MethodSignature, left_signature: MethodSignature.MethodSignature,
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
