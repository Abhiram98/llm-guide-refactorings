import json
import git

with open("v1_dataset_neo4j_neo4j.json") as f:
    v1_dataset = json.load(f)
#
# for ref_file in v1_dataset:
#     commit_url = ref_file['commitUrl']
#     commit_parts = commit_url.split('/')
#     project_name = "/".join(commit_parts[3:5])
#     project_folder_name = commit_parts[4]
#
#     v2_hash = commit_parts[-1]
#
#     repo = git.Repo(f"/Users/abhiram/Documents/TBE/evaluation_projects/{project_folder_name}")
#     v1_hash = repo.commit(v2_hash).parents[0].hexsha
#
#     ref_file["v2_hash"] = v2_hash
#     ref_file["project_name"] = project_name
#     ref_file["v1_hash"] = v1_hash
with open("commit_map.json") as f:
    commit_map = json.load(f)

with open("refactoring_status_map.json") as f:
    undo_status = json.load(f)

for ref in v1_dataset:
    filename = ref['file'].split('/')[-1].split('.')[0]
    commit = ref['v2_hash'][:7]
    ref['v2_prime_branch'] = f"undo-{filename}-{commit}"

    if ref['file'] in commit_map:
        ref['v2_prime_hash'] = commit_map[ref['file']]['second']

        for r in ref['refactorings']:
            if str(r['refID']) in undo_status:
                undone= undo_status[str(r['refID'])]['first']
                r['undone'] = undone
                if not undone:
                    r['undo_status'] = undo_status[str(r['refID'])]['second']



print(commit_map)

with open("v1_dataset_neo4j_neo4j_augmented.json", "w") as f:
    json.dump(v1_dataset, f, indent=4)

# push branches
git_repo = git.Repo("/Users/abhiram/Documents/TBE/evaluation_projects/neo4j/.git")
for r in v1_dataset:
    branch = r["v2_prime_branch"]
    try:
        git_repo.git.checkout(branch)
        git_repo.git.push("origin", "-u", branch, "--force")
    except:
        pass