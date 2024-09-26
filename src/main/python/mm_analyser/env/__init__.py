import pathlib
import configparser
from dotenv import load_dotenv
import os

# Attempt to load from .env file
load_dotenv()

# Attempt to load from INI file
config = configparser.ConfigParser()
env_file_name = "evaluation.ini"
file = pathlib.Path(__file__).parent.parent.parent.parent.parent.parent.joinpath(env_file_name)
config.read(file)

def get_env_value(key):
    # First, try to get from os.environ (set by either .env or system environment)
    value = os.getenv(key)
    if value is not None:
        return value
    
    # If not found, try to get from INI file
    try:
        return config['DEFAULT'][key]
    except KeyError:
        return None

PROJECTS_BASE_PATH = get_env_value('PROJECTS_BASE_PATH')
TELEMETRY_FILE_PATH = get_env_value('TELEMETRY_FILE_PATH')
TELEMETRY_FILE_PATH_BASE = get_env_value('TELEMETRY_FILE_PATH_BASE')

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


