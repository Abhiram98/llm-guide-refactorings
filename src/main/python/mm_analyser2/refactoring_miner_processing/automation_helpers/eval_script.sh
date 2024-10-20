#!/bin/bash

# Set the project name (you can change this to any of the supported projects)
PROJECT_NAMES=("vue_pro")

# Set the path to the Python scripts
WRITE_SCRIPT="./write_file4plugin.py"
READ_TELEMETRY_SCRIPT="./read_from_telemetry.py"
COMPUTE_RECALL_SCRIPT="../compute_accuracy.py"

# Generate the date and time once
CURRENT_DATE=$(date +'%d-%H:%M')
MM_ASSIST_OUTFILE="mm-assist-${CURRENT_DATE}"

INPUT_DIRECTORY=$MM_ASSIST_OUTFILE # used this name to make it readable for users
OUTPUT_FILE_PATH="static_methods_12.csv"
LOG_FILE_PATH="./${MM_ASSIST_OUTFILE}.log"


# Loop through each project name
for PROJECT_NAME in "${PROJECT_NAMES[@]}"; do
    # Execute write_file4plugin.py
    echo "Executing write_file4plugin.py for project: ${PROJECT_NAME}"
    python3 "${WRITE_SCRIPT}" --project_name "${PROJECT_NAME}"

    # Check if the previous command was successful
    if [ $? -ne 0 ]; then
        echo "Error: write_file4plugin.py failed for project: ${PROJECT_NAME}"
        exit 1
    fi

    # Execute read_from_telemetry.py
    echo "Executing read_from_telemetry.py for project: ${PROJECT_NAME}"

    python3 "${READ_TELEMETRY_SCRIPT}" --project_name "${PROJECT_NAME}" --mm_assist_outfile_bas "${MM_ASSIST_OUTFILE}"

    # Check if the previous command was successful
    if [ $? -ne 0 ]; then
        echo "Error: read_from_telemetry.py failed for project: ${PROJECT_NAME}"
        exit 1
    fi

    echo "Evaluation data is completed successfully for project: ${PROJECT_NAME}"
done

python3 "${COMPUTE_RECALL_SCRIPT}" --input_directory "${INPUT_DIRECTORY}" --output_file_path "${OUTPUT_FILE_PATH}" --log_path "${LOG_FILE_PATH}"
echo "Evaluation is completed successfully."
