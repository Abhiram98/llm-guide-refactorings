package com.intellij.ml.llm.template.prompts

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage

class SuggestRefactoringPrompt: MethodPromptBase() {
    override fun getPrompt(methodCode: String): MutableList<ChatMessage> {
        return mutableListOf(
            SystemMessage.from("You are an expert programmer performing refactoring operations."),
            UserMessage.from("""
                    Please provide suggestions to improve the following Java method/class. 
                    Only provide suggestions that are: 
                    1. Extract Method. 
                    2. Rename Variable 
                    3. Use Enhanced For Loop
                    4. Convert For Loop to While Loop
                    5. Convert For loop to use Java Streams 
                    6. Use Enhanced Switch Statement
                    7. Convert If Statement to Switch Statement (and vice versa)
                    8. Convert If Statement to Ternary Operator (and vice versa)
                    9. Use String Builder
                    10. Move Method
                    
                    Ensure that your recommendations are specific to this method/class and are actionable immediately. 
                    Your response should be formatted as a JSON object comprising two main fields. The first field, named 'improvements', should be a list of JSON objects, each with the following attributes: 'shortDescription' providing a brief summary of the improvement, 'longDescription' offering a detailed explanation of the improvement, 'start', indicating the starting line number where the improvement should be applied, 'end', indicating the ending line number where the improvement should be applied.
                    
                     1.    public static int calculateSum(int[] arr) {
                     2.        int sum = 0;
                     3.        for (int i = 0; i < arr.length; i++) {
                     4.            sum += arr[i]; 
                     5.        }
                     6.        return sum;
                     7.    }
                     """.trimIndent()),
            AiMessage.from("""
{
    "improvements": [
        {
            "shortDescription": "Convert For Loop to Use Enhanced For Loop",
            "longDescription": "Instead of using a traditional for loop to iterate over `arr`, use an enhanced for loop.",
            "start": 3,
            "end": 3
        },
        {
            "shortDescription": "Rename Variable `arr` to `numbers`",
            "longDescription": "Rename `arr` to `numbers` to indicate that the array contains numeric values.",
            "start": 1,
            "end": 4
        }
    ]
}
"""
            ),
            UserMessage.from(methodCode)
        )
    }
}