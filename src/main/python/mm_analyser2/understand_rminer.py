import json
import pandas as pd
import re
from collections import Counter

def remove_invalid_refs(d):
    for i in list(d.keys()):
        if any([val in i for val in
                ["Type", "Add", "Remove", "AccessModifier", "Annotation"]]
               ):
            del d[i]
        # if "Type" in i or "Add" in i or "Remove" in i:
        #     del d[i]

def find_project(commit_urls):
    return "/".join(commit_urls.split('/')[3:5])

df = pd.read_csv("/Users/abhiram/Downloads/refactoring_stats3.csv")
df['refTypes'] = df['Refactoring Types'].apply(
    lambda x: eval(re.sub("\w+", (lambda m: "\"%s\""%(m.group(0))) , x.replace('=', ':')))
)
df['project'] = df['Commit URL'].apply(find_project)

ge_10_refactorings = df[df['#refactoring']>=5]

ge_10_refactorings["refTypes"].apply(remove_invalid_refs)


ge_10_refactorings_filtered = ge_10_refactorings[
    ge_10_refactorings["refTypes"].apply(
        lambda x: sum([int(i) for i in x.values()])) >= 5
]

counter = Counter()
for i in ge_10_refactorings_filtered['refTypes']:
    counter += Counter({k:int(v) for k,v in i.items()})
print(counter)

hot_projects = Counter(list(ge_10_refactorings_filtered['project']))
print(hot_projects)

print(ge_10_refactorings_filtered)
ge_10_refactorings_filtered['#refactorings'] = ge_10_refactorings_filtered["refTypes"].apply(
        lambda x: sum([int(i) for i in x.values()]))
ge_10_refactorings_filtered.to_csv("~/Downloads/refactoring_stats4.csv", index=False)

