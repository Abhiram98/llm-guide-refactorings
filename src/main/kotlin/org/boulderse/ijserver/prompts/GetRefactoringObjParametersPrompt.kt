package org.boulderse.ijserver.prompts

import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.UserMessage

/*
This prompt passes the documentation of a refactoring API and
asks the llm to fill out the parameters and make a valid API call.
 */
class GetRefactoringObjParametersPrompt {

    companion object{

        fun get(improvementDescription: String,
                refactoringType: String,
                refactoringApiDocumentation: String
                ): List<ChatMessage>{

            return mutableListOf(
                UserMessage.from("user", """
                
                You suggested to improve my code by doing the following: \"$improvementDescription\". 
                Please provide a function call to a python function that performs this $refactoringType refactoring(see documentation below). 
                Respond with ONLY a JSON list of calls to the function below (ex: ["func1(param1)", "func(param2)"]):
                Here is the signature of the function:
                $refactoringApiDocumentation
                   
                """.trimIndent())
            )
        }
    }


}