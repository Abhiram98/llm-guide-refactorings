# Evaluation

This document describes how to run MM-assist on large number of classes automatically. 
As MM-assist is a plugin which must be triggered manually on each class, it would be tedious to evaluate against it in this way. 
To trigger the automated run on mm-assist, follow the steps below:

1. Clone and build the project. Change to the "evaluate-move-method" branch.
    
     This is important, as files on the project path are needed to automate the running of mm-assist.

2. Write the names of classes you would like to run MM-assist on to [this](/src/main/resources/plugin_input_files/classes_and_commits.json) resource file. 

    Follow the same schema as the JSON file and provide a list of file paths and commit hashes to MM-assist on.

    Optional: Use [this helper](/src/main/python/mm_analyser/refactoring_miner_processing/automation_helpers/write_file4plugin.py) script to write to the file.
3. Launch the plugin by executing the "runIde" gradle target.
4. De-anonymize telemetry. Open Main | Settings | Tools | Large Language Models | Uncheck "Anonymize Telemetry Data"
5. Right click on _any_ class | Show Intention Actions | "Move-Method Assistant" 

    Ensure to click on the intention which displays the following text when you hover over it:

   Apply plugin on file and commit list.
   [
   {"file_path": ..., "commit_hash": ...}
   ]

6. Each entry in the telemetry log corresponds with the (file_path, commit_hash) pair in the [input](/src/main/resources/plugin_input_files/classes_and_commits.json) resource file
    
   Optional: Use [this helper script](/src/main/python/mm_analyser/refactoring_miner_processing/automation_helpers/read_from_telemetry.py) to read from the MM-assist telemetry data
