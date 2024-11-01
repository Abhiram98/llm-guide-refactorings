import json
import os

from mm_analyser import data_folder


class MoveMethodSuggestion:

    def __init__(self, method_name, target_class):
        self.target_class = target_class
        self.method_name = method_name

    def __hash__(self):
        return hash(self.method_name + self.target_class)

    def __eq__(self, other):
        if isinstance(other, MoveMethodSuggestion):
            return other.method_name == self.method_name and other.target_class == self.target_class
        return False

    def __str__(self):
        return self.method_name + "->" + self.target_class


files = [
    'comparison_ant_large.json',
    'comparison_ant_small.json',
    'comparison_derby_large.json',
    'comparison_derby_small.json',
    'comparison_drjava_large.json',
    'comparison_drjava_small.json',
    'comparison_jfreechart_large.json',
    'comparison_jfreechart_small.json',
    'comparison_jgroups_large.json',
    'comparison_jgroups_small.json',
    'comparison_jhotdraw_large.json',
    'comparison_jhotdraw_small.json',
    'comparison_jtopen_large.json',  #incomplete
    'comparison_junit_large.json',
    'comparison_junit_small.json',
    'comparison_mvnforum_large.json',
    'comparison_mvnforum_small.json',
    'comparison_tapestry_large.json',
    'comparison_tapestry_small.json',
    'comparison_lucene_small.json',
    'comparison_lucene_large.json',
]

emm_path = data_folder.joinpath("refminer_data/mm-assist-emm")
emm_files = [i for i in os.listdir(emm_path) if i.endswith('.json')]
mm_assist_data = []

total_suggestions = 0
plausible_suggestions = 0
critique_accepted = 0
class_doesnt_exist = 0
mech_infeasiable = 0
for fname in emm_files:
    with open(emm_path.joinpath(fname)) as f:
        emm_data = json.load(f)
        mm_assist_data += emm_data

    project_data = [i for i in emm_data if 'telemetry' in i and len(i['telemetry'].keys())]
    for data in project_data:
        ref_id = data['ref_id']
        telemetry = data["telemetry"]
        vanilla_suggestions = set()
        for iteration in telemetry["iterationData"]:
            if iteration['iteration_num'] <= 0:
                continue
            for mm in iteration["suggested_move_methods"]:
                if 'static' in mm['method_signature']:
                    continue
                vanilla_suggestions.add(
                    MoveMethodSuggestion(
                        mm["method_name"],
                        mm["target_class"]
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
            source_class_code = telemetry['hostFunctionTelemetryData']['sourceCode']
            if (suggetion.method_name in telemetry["targetClassMap"] and suggetion.target_class in
                    telemetry["targetClassMap"][suggetion.method_name]["target_classes_sorted_by_llm"]):
                plausible_suggestions += 1
            elif suggetion.target_class not in source_class_code:
                class_doesnt_exist += 1
            else:
                mech_infeasiable +=1

plausible_rate = plausible_suggestions / total_suggestions
print(f"{plausible_rate=}")
print(f"{1-plausible_rate=}")
print(f"{plausible_suggestions=}")
print(f"{total_suggestions=}")
print(f"{critique_accepted=}")
print(f"{class_doesnt_exist=}")
print(f"{mech_infeasiable=}")
