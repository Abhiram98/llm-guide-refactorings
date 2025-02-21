import json
import pathlib
from pydantic import BaseModel, Field

class JMoveOracle(BaseModel):
    source_class: str = Field(description="source class fully qualified.")
    target_class: str = Field(description="source class fully qualified.")
    method_signature: str = Field(description="method signature")
    project_name: str = Field(description="project which contains the oracle")
    method_size: str = Field(description="Size of the method, as defined by the JMove authors")

from mm_analyser import data_folder

with open(f"{data_folder}/synthetic_corpus_comparison/oracle/oracle.json") as f:
    oracle_raw_data = json.load(f)


oracle_data: list[JMoveOracle] = []
for project_name in oracle_raw_data:
    for size in oracle_raw_data[project_name]:
        for oracle_point in oracle_raw_data[project_name][size].split('\n'):
            if oracle_point == '':
                continue
            # print(oracle_point)
            method_class, target_class_ = oracle_point.split(' need move ')
            source_class_, method_signature = method_class.split("::")
            target_class = target_class_.split(' ')[-1]
            source_class = source_class_.split('method ')[-1]

            oracle_data.append(
                JMoveOracle(source_class=source_class, target_class=target_class, method_signature=method_signature,
                            project_name=project_name, method_size=size)
            )

# print(oracle_data)