from mm_analyser import data_folder
import json
import os
import numpy as np

user_study_path = f"{data_folder}/user_study/"
user_study_files = [i for i in os.listdir(user_study_path) if i.endswith('.jsonl')]

user_data = []
for fname in user_study_files:
    with open(user_study_path+fname) as f:
        user_data += [json.loads(i) for i in f.read().split('\n') if i!='']

avg_time = np.mean([i['processingTime']['totalTime']/1000 for i in user_data if 'processingTime' in i and i['candidatesTelemetryData']['numberOfSuggestions']!=0])
llm_times = np.mean([sum([j['llm_response_time']/1000 for j in i['targetClassMap'].values() if 'llm_response_time' in j ]) for i in user_data])
print(f"{avg_time=}")
print(f"{llm_times=}")