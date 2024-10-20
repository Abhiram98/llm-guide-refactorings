"""
pre-process refactoring miner output
1. filter out moves which occur along with "move class", "extract class" refactorings
2. More than one MM in a class.

2. Add refactoring Id

"""
import json
import os
count = 0
def filter_fp(data):
    global count
    bad_refs = ['Move Class', 'Extract Class']
    filtered_commits = []
    for commit in data['commits']:
        bad = False
        for ref in commit['refactorings']:
            count += 1
            if ref['type'] in bad_refs:
                bad=True
                break
            ref['refID'] = count
        if not bad:
            filtered_commits.append(commit)

    return {"commits": filtered_commits}



mm_path = "../../../../data/refminer_data/contains_a_mm"
filtered_path = "../../../../data/refminer_data/preprocessed"
files = [i for i in os.listdir(mm_path) if i.endswith(".json")]
for fname in files:
    print(fname)
    with open(os.path.join(mm_path, fname)) as f:
        move_data = json.load(f)
    filtered_moves = filter_fp(move_data)
    with open(os.path.join(filtered_path, fname), "w") as f:
        json.dump(filtered_moves, f, indent=4)