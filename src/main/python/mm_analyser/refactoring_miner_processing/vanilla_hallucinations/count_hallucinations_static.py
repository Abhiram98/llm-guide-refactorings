import json
import os

from mm_analyser import data_folder


class MoveMethodSuggestion:

    def __init__(self, method_name, target_class, static=False):
        self.target_class = target_class
        self.method_name = method_name
        self.static = static

    def __hash__(self):
        return hash(self.method_name + self.target_class)

    def __eq__(self, other):
        if isinstance(other, MoveMethodSuggestion):
            return other.method_name == self.method_name and other.target_class == self.target_class
        return False

    def __str__(self):
        return self.method_name + "->" + self.target_class

def java_file_in_root(base_path):
    java_files = []
    for root, dirs, files in os.walk(base_path):
        for f in files:
            if f.endswith('.java'):
                java_files.append(f)
    return java_files

# java_files = java_file_in_root("/Users/abhiram/Documents/TBE/evaluation_projects")
with open("/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/file_map.json") as f:
    file_map = json.load(f)
projects = list(file_map.keys())
emm_path = data_folder.joinpath("refminer_data/voyage_results_static")
emm_files = [i for i in os.listdir(emm_path) if i.endswith('.jsonl')]
mm_assist_data = []

total_suggestions = 0
plausible_suggestions = 0
critique_accepted = 0
class_doesnt_exist = 0
mech_inf = 0
for fname in emm_files:
    emm_data = []
    with open(emm_path.joinpath(fname)) as f:
        emm_data += [json.loads(i) for i in f.read().split('\n') if i!='']
        mm_assist_data += emm_data

    # project_data = [i for i in emm_data if 'telemetry' in i and len(i['telemetry'].keys())]
    for data in emm_data:
        # ref_id = data['ref_id']
        telemetry = data
        vanilla_suggestions = set()
        for iteration in telemetry["iterationData"]:
            if iteration['iteration_num'] <= 0:
                continue
            for mm in iteration["suggested_move_methods"]:
                # if 'static' not in mm['method_signature']:
                #     continue
                vanilla_suggestions.add(
                    MoveMethodSuggestion(
                        mm["method_name"],
                        mm["target_class"],
                        'static' in mm['method_signature']
                    )
                )

        # try:
        brute_force_methods = [i for i in telemetry["iterationData"] if i['iteration_num'] == -1][0]
        # brute_force_methods['suggested_move_methods']

        priority_methods = [j['method_name'] for j in brute_force_methods['suggested_move_methods']]
        # except:
        #     continue
        total_suggestions += len(vanilla_suggestions)
        vanilla_suggestions_priority = [i for i in vanilla_suggestions
                                        if (i.method_name in priority_methods)]
        critique_accepted += len(vanilla_suggestions_priority)

        for suggetion in vanilla_suggestions_priority:
            matched_project = [i for i in projects if i in telemetry["hostFunctionTelemetryData"]['filePath']][0]
            source_class_code = telemetry['hostFunctionTelemetryData']['sourceCode']
            # if (suggetion.method_name in telemetry["targetClassMap"] and suggetion.target_class in
            #         telemetry["targetClassMap"][suggetion.method_name]["target_classes_sorted_by_llm"]):
            #     plausible_suggestions += 1
            if suggetion.target_class+".java" not in file_map[matched_project]:
                class_doesnt_exist += 1
            else:
                if suggetion.static:
                    plausible_suggestions +=1
                elif (suggetion.method_name in telemetry["targetClassMap"] and suggetion.target_class in
                    telemetry["targetClassMap"][suggetion.method_name]["target_classes_sorted_by_llm"]):
                    plausible_suggestions += 1
                else:
                    mech_inf +=1


plausible_rate = plausible_suggestions / total_suggestions
print(f"{plausible_rate=}")
print(f"{1-plausible_rate=}")
print(f"{plausible_suggestions=}")
print(f"{total_suggestions=}")
print(f"{critique_accepted=}")
print(f"{class_doesnt_exist=}")
print(f"{mech_inf=}")
