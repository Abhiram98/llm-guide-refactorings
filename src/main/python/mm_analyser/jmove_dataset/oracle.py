import json
import pathlib
from pydantic import BaseModel, Field, computed_field
from typing import Any


class JMoveOracle(BaseModel):
    source_class: str = Field(description="source class fully qualified.")
    target_class: str = Field(description="source class fully qualified.")
    method_signature: str = Field(description="method signature")
    project_name: str = Field(description="project which contains the oracle")
    method_size: str = Field(description="Size of the method, as defined by the JMove authors")


    @computed_field
    @property
    def method_name(self) -> str:
        return self.method_signature.split('(')[0]

    @computed_field
    @property
    def alias_method_name(self) -> str:
        return self.method_signature.split('(')[0] + '2'

    def __hash__(self):
        return hash(self.source_class + self.target_class + self.method_signature)

    def __eq__(self, other):
        return (isinstance(other, JMoveOracle) and
                other.method_signature == self.method_signature and
                other.source_class == self.source_class and
                other.target_class == self.target_class
                )


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
