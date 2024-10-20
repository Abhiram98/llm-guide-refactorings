import json

def myindex(list, ele):
    try:
        return list.index(ele)
    except ValueError:
        return -1

files = ['ant_large.json',
         'ant_small.json',
         'derby_large.json',
         'derby_small.json',
         'drjava_large.json',
         'drjava_small.json',
         'jfreechart_large.json',
         'jfreechart_small.json',
         'jgroups_large.json',
         'jgroups_small.json',
         'jhotdraw_large.json',
         'jhotdraw_small.json',
         'jtopen_large.json',
         'jtopen_small.json',
         'junit_large.json',
         'junit_small.json',
         'lucene_large.json',
         'lucene_small.json',
         'mvnforum_large.json',
         'mvnforum_small.json',
         'tapestry_large.json',
         'tapestry_small.json']

with open('../../../../data/synthetic_corpus_comparison/oracle/oracle.json') as f:
    oracle_data = json.load(f)

oracle_points = []
covered = []

for file in files:

    project = file.split('_')[0]
    size = file.split('_')[-1].rstrip('.json')
    # print(oracle_data[project][size])
    with open(f'../../../../data/synthetic_corpus_comparison/feTruth_run/{file}') as f:
        fetruth_recs = json.load(f)

    for oracle in oracle_data[project][size].split('\n'):
        if oracle == '':
            continue
        covered.append((project, size))
        oracle_result = {"oracle": oracle}
        oracle_points.append(oracle_result)
        method_name = oracle.split("::")[1].split('(')[0]
        alias_method_name = method_name + '2'
        target_class = oracle.split('.')[-1]
        source_class = oracle.split("::")[0].split('.')[-1]
        # find fetruth recomendations for that host class.

        fetruth_recs_source_class = [
            i for i in fetruth_recs if i['source_class'].split('.')[-1] == source_class
        ]
        print(f"{len(fetruth_recs_source_class)=}")
        if len(fetruth_recs_source_class) == 0:
            oracle_result['recall_method_position'] = -1
            oracle_result['recall_method_class_position'] = -1
            continue

        fe_methods = [i['source_method'] for i in fetruth_recs_source_class]
        oracle_result['recall_method_position'] = max(
            myindex(fe_methods, method_name),
            myindex(fe_methods, alias_method_name)
        )
        if oracle_result['recall_method_position'] == -1:
            oracle_result['recall_method_class_position'] = -1
            continue
        method_index = oracle_result['recall_method_position']
        fe_method = fetruth_recs_source_class[method_index]['source_method']
        fe_target_classes = [i['target_class'].split('.')[-1] for i in fetruth_recs_source_class if i['source_method'] == fe_method]
        oracle_result['recall_method_class_position'] = myindex(fe_target_classes, target_class)


recall_method_and_class_1 = len(
    [i for i in oracle_points if i['recall_method_position'] == 0 and i['recall_method_class_position'] == 0]) / len(
    oracle_points)
recall_method_1 = len([i for i in oracle_points if i['recall_method_position'] == 0]) / len(oracle_points)

recall_method_and_class_2 = len([i for i in oracle_points if
                                 i['recall_method_position'] in [0, 1] and i[
                                     'recall_method_class_position'] == 0]) / len(oracle_points)
recall_method_2 = len([i for i in oracle_points if i['recall_method_position'] in [0, 1]]) / len(oracle_points)

recall_method_and_class_3 = len([i for i in oracle_points if
                                 i['recall_method_position'] in [0, 1, 2] and i[
                                     'recall_method_class_position'] == 0]) / len(oracle_points)
recall_method_3 = len([i for i in oracle_points if i['recall_method_position'] in [0, 1, 2]]) / len(oracle_points)

recall_method_and_class_all = len([i for i in oracle_points if
                                   i['recall_method_position'] != -1 and i[
                                       'recall_method_class_position'] != -1]) / len(oracle_points)
recall_method_all = len([i for i in oracle_points if i['recall_method_position'] != -1]) / len(oracle_points)

print(f"dataset size = {len(oracle_points)}")
oracle_size = 235
print(f"{oracle_size=}")

print("recalling the correct MoveMethod:")
print(f"recall method&class @1 = {recall_method_and_class_1}")
print(f"recall method&class @2 = {recall_method_and_class_2}")
print(f"recall method&class @3 = {recall_method_and_class_3}")
print(f"recall method&class @inf = {recall_method_and_class_all}")
print()

print("recalling the correct method only (identifying method out of place)")
print(f"recall method @1 = {recall_method_1}")
print(f"recall method @2 = {recall_method_2}")
print(f"recall method @3 = {recall_method_3}")
print(f"recall method @inf = {recall_method_all}")
print()

recalled_methods = [i for i in oracle_points if i['recall_method_position'] != -1]
recall_class_1 = len([i for i in recalled_methods if i['recall_method_class_position'] == 0]) / len(recalled_methods)
recall_class_2 = len([i for i in recalled_methods if i['recall_method_class_position'] in [0, 1]]) / len(
    recalled_methods)
recall_class_3 = len([i for i in recalled_methods if i['recall_method_class_position'] in [0, 1, 2]]) / len(
    recalled_methods)
recall_class_inf = len([i for i in recalled_methods if i['recall_method_class_position'] != -1]) / len(recalled_methods)
print(f"recall of class for a recalled method. there were {len(recalled_methods)} recalled at any position.")
print(f"recall class @1 = {recall_class_1}")
print(f"recall class @2 = {recall_class_2}")
print(f"recall class @3 = {recall_class_3}")
print(f"recall class @inf = {recall_class_inf}")
