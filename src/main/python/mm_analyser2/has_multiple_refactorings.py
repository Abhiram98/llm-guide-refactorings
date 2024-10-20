import pandas as pd
import json
from collections import defaultdict


def filter_refactorings(rminer_out):
    new_commits = []
    for commit in list(rminer_out['commits']):
        f = []
        for i in commit['refactorings']:
            if any([val in i['type'] for val in
                    ["Type", "Add", "Remove", "Access Modifier", "Annotation"]]
                   ):
                continue
            f.append(i)
        if len(f):
            commit['refactorings'] = f
            new_commits.append(commit)
    rminer_out['commits'] = new_commits


if __name__ == '__main__':
    with open("/Users/abhiram/Documents/TBE/evaluation_projects/cassandra-ref-miner.json") as f:
        rminer_out = json.load(f)

    filter_refactorings(rminer_out)

    commit_file_changed_map = defaultdict(set)

    for commit in rminer_out['commits']:
        for r in commit['refactorings']:
            try:
                commit_file_changed_map[commit['sha1']].add(
                    r['leftSideLocations'][0]['filePath']
                )
            except:
                continue

    print(commit_file_changed_map)
    file_commit_map = defaultdict(set)
    for k, v in commit_file_changed_map.items():
        for filePath in v:
            file_commit_map[filePath].add(k)

    ge_2_commits = {k: v for k, v in file_commit_map.items() if len(v) >= 2}
    one_commit = {k: v for k, v in file_commit_map.items() if len(v) == 1}
    print(file_commit_map)
    # interesting_file = 'x-pack/plugin/esql/src/main/java/org/elasticsearch/xpack/esql/parser/LogicalPlanBuilder.java'
    # interesting_file = 'server/src/main/java/org/elasticsearch/search/vectors/KnnSearchBuilder.java'
    # interesting_file = 'x-pack/plugin/esql/src/test/java/org/elasticsearch/xpack/esql/expression/function/AbstractScalarFunctionTestCase.java'
    # interesting_file = 'x-pack/plugin/esql/src/main/java/org/elasticsearch/xpack/esql/io/stream/PlanNamedTypes.java'
    # interesting_commits = ge_2_commits[interesting_file]
    file_refactoring_map = {}
    for interesting_file in one_commit:
        interesting_commits = one_commit[interesting_file]
        refactorings_performed_in_commits = [
            {**i, "refactorings": [j for j in i['refactorings'] if
                                   len(j['leftSideLocations']) and j['leftSideLocations'][0]['filePath'] == interesting_file]}
            for i in rminer_out['commits'] if i['sha1'] in interesting_commits]
        file_refactoring_map[interesting_file] = refactorings_performed_in_commits

    print(file_refactoring_map)
    file_refactoring_map_ge_5_refactorings = \
        {k:v for k,v in file_refactoring_map.items() if len(v[0]['refactorings'])>=5}

    # ge_2_commits[
    #     'x-pack/plugin/inference/src/main/java/org/elasticsearch/xpack/inference/services/elasticsearch/MultilingualE5SmallInternalServiceSettings.java']

    with open("/Users/abhiram/Documents/TBE/evaluation_projects/casandra-interesting-files.json", "w") as f:
        json.dump(file_refactoring_map_ge_5_refactorings, f, indent=4)
