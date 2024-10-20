import copy

import pandas as pd
import os
import json

from has_multiple_refactorings import filter_refactorings
from collections import defaultdict, Counter

def group_by_file(combined_data):
    file_refactoring_map = defaultdict(list)
    for commit in combined_data['commits']:
        for refactoring in commit['refactorings']:
            fileChanged = refactoring['leftSideLocations'][0]['filePath']
            refactoring['commitUrl'] = commit['commitUrl']
            file_refactoring_map[fileChanged].append(refactoring)
    return file_refactoring_map

class RefKey:
    def __init__(self, filename, commitUrl):
        self.filename = filename
        self.commitUrl = commitUrl

    def __hash__(self):
        return hash(self.filename + self.commitUrl)

    def __eq__(self, other):
        if isinstance(other, RefKey):
            return other.commitUrl == self.commitUrl and other.filename == self.filename
        return False

def group_by_commit(file_refactoring_map):
    new_map = defaultdict(list)
    for filename in file_refactoring_map:
        for ref in file_refactoring_map[filename]:
            commit_url = ref['commitUrl']
            new_map[RefKey(filename, commit_url)].append(ref)
    return new_map

def isPure(refactoring):
    if 'isPure' not in refactoring:
        return True
    return refactoring['isPure']
def filter_only_pure(file_refactoring_map):
    new_file_refactoring_map = {
        filename:refactorings for filename, refactorings in file_refactoring_map.items()
        if all([isPure(r) for r in refactorings])
    }
    return new_file_refactoring_map

def find_purity_pct(file_refactoring_map_filtered):
    refactoring_count = 0
    purity_count = 0
    purity_key_count = 0
    purity_key_true_count = 0
    for filename in file_refactoring_map_filtered:
        refactorings = file_refactoring_map_filtered[filename]
        for r in refactorings:
            if 'isPure' not in r:
                purity_count += 1
            else:
                purity_key_count += 1
                if r['isPure']:
                    purity_count += 1
                    purity_key_true_count += 1
            refactoring_count +=1
    print(f"{purity_count/refactoring_count=}")
    print(f"{purity_key_true_count/purity_key_count=}")

def print_project_stats(file_refactoring_map):
    projects = []
    for file in file_refactoring_map:
        refactoring0 = file_refactoring_map[file][0]
        commit_url = refactoring0['commitUrl']
        projects.append("/".join(commit_url.split('/')[3:5]))
    return Counter(projects)

def refactoring_type_stats(file_refactoring_map):
    refactorings = []
    for file in file_refactoring_map:
        for refactoring in file_refactoring_map[file]:
            refactorings.append(refactoring['type'])
    return Counter(refactorings)


def filter_only(file_refactoring_map, *only):
    new_map = {}
    for k in file_refactoring_map:
        new_list = []
        for ref in file_refactoring_map[k]:
            if any([ref['type']==o for o in only]):
                new_list.append(ref)
        new_map[k] = new_list
    return new_map

def add_refactoring_id(file_refactoring_map):
    count = 0
    for k in file_refactoring_map:
        for ref in file_refactoring_map[k]:
            ref['refID'] = count
            count += 1
    return file_refactoring_map

def transform_file_refactoring_map(file_refactoring_map):
    file_refactoring_map_copy = copy.deepcopy(file_refactoring_map)
    ref_data = []
    bad = 0
    for k in file_refactoring_map_copy:
        commitUrl = file_refactoring_map_copy[k][0]['commitUrl']
        if not all([r['commitUrl'] == commitUrl for r in file_refactoring_map_copy[k]]):
            bad +=1
        ref_data.append(
            {
                "file": k.filename,
                "commitUrl": k.commitUrl,
                "refactorings" : [r for r in file_refactoring_map_copy[k] if r['commitUrl']==commitUrl]
            }
        )
    print(f"{bad=}")
    return ref_data

purityOutDir = "/Users/abhiram/Documents/TBE/PurityChecker/purityOut_copy"
purity_out_files = os.listdir(purityOutDir)

commits_data = []
combined_data = {"commits": commits_data}

for file in purity_out_files:
    with open(purityOutDir+"/"+file) as f:
        try:
            commits_data.append(json.load(f))
        except:
            continue

filter_refactorings(combined_data)
file_refactoring_map = group_by_file(combined_data)
file_refactoring_map_filtered = {k:v for k,v in file_refactoring_map.items() if len(v)>=5}
find_purity_pct(file_refactoring_map_filtered)
file_refactoring_map_pure = filter_only_pure(file_refactoring_map_filtered)
project_stats = print_project_stats(file_refactoring_map_pure)
refactoring_stats = refactoring_type_stats(file_refactoring_map_pure)
print(file_refactoring_map_pure)

selected_projects = ['neo4j/neo4j', 'robovm/robovm', 'infinispan/infinispan', 'apache/cassandra']
file_refactoring_map_filter_projects = file_refactoring_map
file_refactoring_map_filter_projects = filter_only(file_refactoring_map,
            "Extract Method", 'Rename Parameter', 'Move Method',
            'Rename Method', 'Rename Variable', 'Move And Rename Method')
file_refactoring_map_filter_projects = group_by_commit(file_refactoring_map_filter_projects)
file_refactoring_map_filter_projects = {k:v for k,v in file_refactoring_map_filter_projects.items()
                                 if len(v)>=3 and any([i in v[0]['commitUrl'] for i in selected_projects])}
find_purity_pct(file_refactoring_map_filter_projects)
total_refactorings = sum([len(i) for i in file_refactoring_map_filter_projects.values()])
refactoring_stats_projects = refactoring_type_stats(file_refactoring_map_filter_projects)

add_refactoring_id(file_refactoring_map_filter_projects)
v1_dataset = [ i for i in
    transform_file_refactoring_map(file_refactoring_map_filter_projects)
               if len(i['refactorings']) >=3
]
print(f"{total_refactorings=}")
with open("v1_dataset.json", "w") as f:
    json.dump(v1_dataset, f, indent=4)



var = {'Extract Method': 307, 'Rename Parameter': 178, 'Move Method': 91, 'Rename Method': 69, 'Rename Variable': 43,
       'Pull Up Attribute': 37, 'Pull Up Method': 32, 'Move Attribute': 24, 'Extract Class': 19, 'Inline Variable': 19,
       'Parameterize Variable': 18, 'Rename Attribute': 16, 'Extract Variable': 15, 'Inline Method': 14,
       'Move Class': 14,
       'Move And Rename Method': 10, 'Move And Inline Method': 8, 'Inline Attribute': 7, 'Extract Superclass': 6,
       'Localize Parameter': 5, 'Extract And Move Method': 5, 'Move And Rename Class': 5, 'Rename Class': 4,
       'Move Code': 4,
       'Merge Class': 3, 'Push Down Attribute': 3, 'Split Parameter': 2, 'Push Down Method': 2,
       'Replace Anonymous With Lambda': 2, 'Replace Variable With Attribute': 2, 'Replace Attribute With Variable': 1,
       'Merge Parameter': 1, 'Replace Anonymous With Class': 1, 'Extract Subclass': 1, 'Parameterize Attribute': 1}

