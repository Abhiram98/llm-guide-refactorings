import re


class MethodInvocation:
    def __init__(self, var_name, method_name, *args):
        self.var_name = var_name
        self.method_name = method_name
        self.args = args

    @staticmethod
    def create_from_call_str(call_str: str):
        method_call_regex = re.compile(r"(\w+\.)?(\w+)(\(.*\))")
        m = method_call_regex.search(call_str)

        if m:
            var_name = m.group(1)[:-1] if m.group(1) is not None else None
            method_name = m.group(2)
            args_str = m.group(3)[1:-1]
            args = args_str.split(",") if args_str != "" else None
            if args is not None:
                return MethodInvocation(var_name, method_name, *args)
            else:
                return MethodInvocation(var_name, method_name)
