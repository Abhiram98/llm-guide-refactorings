import re

class Parameter:
    def __init__(self, param_type, param_name):
        self.param_type = param_type
        self.param_name = param_name

class MethodSignature:
    def __init__(self,
                 original_signature: str,
                 method_name: str,
                 params: list[Parameter],
                 return_type: str,
                 modifier: str):
        self.original_signature = original_signature
        self.method_name = method_name
        self.params = params
        self.return_type = return_type
        self.modifier = modifier

    def __str__(self):
        return self.original_signature

    @staticmethod
    def get_method_signature_parts(method_signature: str):
        method_signature_regex = re.compile(r"(\w+) (\w+)(\(.*\)) : ((\w(<\w+>)?)+)")
        m = method_signature_regex.search(method_signature)

        if m:
            modifier = "packageLocal" if m.group(1) == "package" else m.group(1)
            method_name = m.group(2)
            method_params = m.group(3)
            return_type = m.group(4)
            return MethodSignature(
                method_signature,
                method_name,
                MethodSignature.get_params_list(method_params),
                return_type,
                modifier)

        return None

    @staticmethod
    def get_params_list(method_params: str):
        if method_params == "()":
            return []

        params_list = method_params[1:-1].split(", ")

        return [Parameter(param.split(" ")[1], param.split(" ")[0])
                for param in params_list]



