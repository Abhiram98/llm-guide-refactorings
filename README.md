# Move-Method Assistant

<!-- Plugin description -->
CoRename Agent helps you perform Coordinated Renaming throughout your codebase. 
Coordinated renaming refers to the process of consistently updating the names of various identifiers—such as class names, methods, parameters, and fields—that share a semantic relationship or naming pattern. 
Unlike a simple text-based rename, coordinated renaming ensures naming consistency across your project.
<!-- Plugin description end -->

### Table of contents

- [Installation](#installation)
- [Trigger Move-Method Assistant](#triggering-move-method-assistant)
- [Telemetry Data](#telemetry-data)

## Installation

- Download Intellij IDEA 2024.1
- Download the plugin from this repository: [refactoring-assistant-plugin.zip](dist%2Frefactoring-assistant-plugin.zip)
- Then, follow the instructions [here](https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_disk) to “install plugin from disk” in your IDE. Be sure to restart the IDE.
  
- Go to Settings | Tools | Large Language Models and enter your API key in the "API Key" field. Select the appropriate LLM in the dropdown list.
  ![api_key.gif](readme-images/gifs/api_key.gif)

## Triggering Move Method Assistant

- Find a Java class you’d like to improve its modularity, and move methods that are misplaced.
-  Right-click on the class name -> Select "Show Context Actions" -> Select "Move-Method Refactoring Assistant"
   ![trigger_plugin.gif](readme-images/gifs/trigger_plugin.gif)
- If you liked any of the suggestions, select that particular suggestion and execute it by pushing the Refactor button! You can execute as many (or none) of the shown suggestions.
- Hit the ESC button when you’re done, to close the popup window with suggested refactorings.


#### Optional: View the logs of the assistant

-  Click on View | Tool Windows | Refactoring Assistant Logs
   ![trigger_plugin.gif](readme-images/gifs/open_logs.gif)


## Telemetry data

The plugin stores anonymous telemetry data on your local machine. 
It does NOT contain any personal information about you or about the code that you develop. 
Manually sending this telemetry data to us, will help us understand how developers think about the plugin suggestions. 
The recorded information is in a human-readable format, so you can proofread it before sending it to us.


Telemetry data is collected in a file on the local machine of the developer, under the IntelliJ Idea folder
`ref_plugin_logs/ref_telemetry_data.jsonl`. In my case, the full path
is `~/Library/Logs/JetBrains/IntelliJIdea2023.1/ef_plugin_logs/ef_telemetry_data.log`.
The data in stored in *JSONL* format. 
Each line in the telemetry file represents a JSON object, parseable by a json parser. You can use [this](https://pprint-ndjson.neverendingqs.com) online formatter to view the contents of your telemetry data before you send it to us.

### How to find the telemetry data file?

 - Click Help | Show Log in Finder
  ![idea_logs.gif](readme-images/gifs/open_idea_logs.gif)
 - Locate the telemetry file under "ref_plugin_logs/ref_telemetry_data.jsonl"
  ![telemetry.gif](readme-images/gifs/find_logs.gif)

