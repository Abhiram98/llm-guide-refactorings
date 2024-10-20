import json


project_name = "jtopen"
size = "small"

gold_file = f"/Users/abhiram/Documents/TBE/jmove/dataset-tse/gold_sets/{project_name}/{size}.txt"
dest_file = f"comparison_{project_name}_{size}"
dest_file = f"/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/synthetic_corpus_comparison/{dest_file}.json"
with open(gold_file) as f:
    ant_small = f.read()

class_names = []
for i in ant_small.split("\n"):
    if i=='':
        continue
    class_names.append(i.split('::')[0].split(' ')[-1])

print("\n".join(class_names))
with open("../../../../data/qualified_classes.txt", "w") as f:
    f.write("\n".join(class_names))


telemetry_file = '/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/build/idea-sandbox/system/log/ref_plugin_logs/ref_telemetry_data.jsonl'
with open(telemetry_file) as f:
    telemetry = [json.loads(i) for i in f.read().split('\n') if i!=''][-len(class_names):]
data = []
for class_name, tele, oracle in zip(class_names, telemetry, ant_small.split('\n')):
    data.append(
        {
            "oracle": oracle,
            "class_name": class_name,
            "telemetry": tele
        }
    )

with open(dest_file, "w") as f:
    json.dump(data, f, indent=4)