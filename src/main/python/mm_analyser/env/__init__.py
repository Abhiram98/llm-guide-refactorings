import pathlib
import configparser

env_file_name = "evaluation.ini"
file = pathlib.Path(__file__).parent.parent.parent.parent.parent.parent.joinpath(env_file_name)

config = configparser.ConfigParser()
config.read(file)

PROJECTS_BASE_PATH = config['projects']['PROJECTS_BASE_PATH']
TELEMETRY_FILE_PATH = config['telemetry']['TELEMETRY_FILE_PATH']

PROJECT_ALIAS_MAP = {
        'vue_pro_res.json': 'ruoyi-vue-pro',
        'flink_res.json': 'flink',
        'halo_res.json': 'halo',
        'elastic_res.json': 'elasticsearch',
        'redisson_res.json': 'redisson',
        'spring_framework_res.json': 'spring-framework',
        'springboot_res.json': 'spring-boot',
        'stirling_res.json': 'Stirling-PDF',
        'selenium_res.json': 'selenium',
        'ghidra_res.json': 'ghidra',
        'dbeaver_res.json': 'dbeaver',
        'kafka_res.json': 'kafka',
        "graal_res.json": 'graal',
        'dataease_res.json': 'dataease'
    }