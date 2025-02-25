import os
from pathlib import Path

import mm_analyser.jmove_dataset.oracle as jmove_oracle


if __name__ == '__main__':
    jmove_directory_path = Path(os.getenv('JMOVE_DIRECTORY_PATH'))
    project_directories = []
    for data in jmove_oracle.oracle_data:
        outer_path = (jmove_directory_path
                      .joinpath(data.project_name)
                      .joinpath(data.method_size)
                      .joinpath('big' if data.method_size == 'large' else 'small'))
        proj_ = [i for i in os.listdir(outer_path) if data.project_name in i.lower()][0]
        project_directory: Path = outer_path.joinpath(proj_)
        if str(project_directory) not in project_directories:
            project_directories.append(str(project_directory))
    print("\n".join(project_directories))
    print()