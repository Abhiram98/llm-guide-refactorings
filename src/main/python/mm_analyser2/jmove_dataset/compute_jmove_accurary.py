import json
from collections import Counter, defaultdict


def myindex(list, ele):
    try:
        return list.index(ele)
    except ValueError:
        return -1


def get_jmove_rec(jmove_line):
    ref, source_method_class, target_class, similarity = jmove_line.split("	")
    source_class, source_method = source_method_class.split("::")
    return (ref,
            source_method.split('(')[0],
            source_class.split('.')[-1],
            target_class.split('.')[-1],
            float(similarity))


jmove_out_files = ['jmove_ant_large.txt',
                   'jmove_ant_small.txt',
                   'jmove_derby_large.txt',
                   'jmove_derby_small.txt',
                   'jmove_drjava_large.txt',
                   'jmove_drjava_small.txt',
                   'jmove_jfreechart_large.txt',
                   'jmove_jfreechart_small.txt',
                   'jmove_jgroups_large.txt',
                   'jmove_jgroups_small.txt',
                   'jmove_jhotdraw_large.txt',
                   'jmove_jhotdraw_small.txt',
                   'jmove_jtopen_large.txt',
                   'jmove_jtopen_small.txt',
                   'jmove_junit_large.txt',
                   'jmove_junit_small.txt',
                   'jmove_lucene_large.txt',
                   'jmove_lucene_small.txt',
                   'jmove_mvnforum_large.txt',
                   'jmove_mvnforum_small.txt',
                   'jmove_tapestry_large.txt',
                   'jmove_tapestry_small.txt'
                   ]

with open('../../../../data/synthetic_corpus_comparison/oracle/oracle.json') as f:
    oracle_data = json.load(f)

oracle_points = []
covered = []
for jmove_file in jmove_out_files:
    project = jmove_file.split('_')[1]
    size = jmove_file.split('_')[-1].rstrip('.txt')
    # print(oracle_data[project][size])
    with open(f'../../../../data/synthetic_corpus_comparison/jmove_run/{jmove_file}') as f:
        jmove_out = f.read()
        jmove_recs = [get_jmove_rec(i) for i in jmove_out.split('\n')[1:-1]]

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
        # find jmove recomendations for that host class.

        jmove_recs_source_class = [(jsource_method, jtarget_class, similarity)
                                   for _, jsource_method, jsource_class, jtarget_class, similarity in jmove_recs
                                   if jsource_class.split('.')[-1] == source_class]
        print(f"{len(jmove_recs_source_class)=}")
        if len(jmove_recs_source_class) == 0:
            oracle_result['recall_method_position'] = -1
            oracle_result['recall_method_class_position'] = -1
            continue

        jmove_recs_source_class = sorted(jmove_recs_source_class, key=lambda x: x[-1], reverse=True)
        jmove_methods = [jmove_method for jmove_method, _, _ in jmove_recs_source_class]
        oracle_result['recall_method_position'] = max(
            myindex(jmove_methods, method_name),
            myindex(jmove_methods, alias_method_name)
        )
        if oracle_result['recall_method_position'] == -1:
            oracle_result['recall_method_class_position'] = -1
            continue
        method_index = oracle_result['recall_method_position']
        jtarget_class = jmove_recs_source_class[method_index][1]
        if jtarget_class == target_class:
            oracle_result['recall_method_class_position'] = 0
        else:
            oracle_result['recall_method_class_position'] = -1

# covered_vals = Counter(covered)
# si = defaultdict(lambda :defaultdict(int))
# for p in oracle_data:
#     for s in oracle_data[p]:
#         si[p][s] = len([i for i in oracle_data[p][s].split('\n') if i!=''])
# assert (si == sum(covered_vals.values()))

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
