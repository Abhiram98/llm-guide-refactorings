import pathlib
data_folder = pathlib.Path(__file__).parent.parent.parent.parent.parent.joinpath('data').absolute()
resources_folder = pathlib.Path(__file__).parent.parent.parent.joinpath('resources').absolute()
project_root = pathlib.Path(__file__).parent.parent.parent.parent.parent.absolute()