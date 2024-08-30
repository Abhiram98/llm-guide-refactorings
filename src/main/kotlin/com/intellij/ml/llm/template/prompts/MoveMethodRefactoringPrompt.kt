package com.intellij.ml.llm.template.prompts

import com.intellij.ml.llm.template.intentions.ApplyMoveMethodInteractiveIntention
import com.intellij.ml.llm.template.refactoringobjects.movemethod.MoveMethodFactory
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage

class MoveMethodRefactoringPrompt: MethodPromptBase() {
    override fun getPrompt(methodCode: String): MutableList<ChatMessage> {
        return mutableListOf(
            SystemMessage.from(SuggestRefactoringPrompt.systemMessageText),
            UserMessage.from("""
            Please provide suggestions to improve the following Java method/class. 
            Your task is to identify methods that do not belong to the class and suggest an appropriate class to move them.
            Only provide suggestions that are: Move Method.
                
                
            Ensure that your recommendations are specific to this method/class and are actionable immediately. 
            Your response should be formatted as a JSON list of objects. Each object should comprise of the following fields. 
            The first field, named "method_name", should be the name of the method that needs to move.
            The second field, names "method_signature", should be the signature of the method that needs to move.
            The third field, "target_class" is the class it should move to.
            The fourth field "rationale", is the reason why it should be moved.
                
            TODO: EXAMPLE
                     """.trimIndent()),
            AiMessage.from("""
                            [
                                {
                                    "method_name":"...",
                                    "method_signature": "...",
                                    "target_class": "...",
                                    "rationale": "////",
                                },
                                {
                                    "method_name":"...",
                                    "method_signature": "...",
                                    "target_class": "...",
                                    "rationale": "////",
                                }
                            ]
                            """.trimIndent()),
            UserMessage.from(methodCode)
        )
    }
    fun askForMethodPriorityPrompt(methodCode: String, moveMethodSuggetions: List<ApplyMoveMethodInteractiveIntention.MoveMethodSuggestion>): MutableList<ChatMessage> {
        return mutableListOf(
            SystemMessage.from("You are an expert Java developer who prioritises move-method refactoring suggestions based on your expertise. "),
            UserMessage.from("""
                Here is a java class:
                ${methodCode}
                
                Please rank the following move-method suggestions:
                 ${
                     moveMethodSuggetions.forEach { it.methodName }
                 }
                    
                Respond in a JSON list, with the most important move-method suggestion at the beginning of the list. 
                If you think it is not important to move any of these methods, respond with an empty list.
                     """.trimIndent()),
        )
    }

    fun askForTargetClassPriorityPrompt(methodCode: String,
                                        movePivots: List<MoveMethodFactory.MovePivot>): MutableList<ChatMessage>{
        return mutableListOf(
            SystemMessage.from("You are an expert Java developer. " +
                    "You are told that a certain method doesn't belong to a class," +
                    " and it is your responsibility to decides which class the method should move to," +
                    " based on your expertise. "),
            UserMessage.from("""
                Here is the method that needs to move:
                ${methodCode}
                
                Please decide which target class is the best option:
                   ${movePivots.map { it.psiClass.name }.joinToString(", ") }} 
                Respond with ONLY a JSON list, with the most important class suggestion at the beginning of the list. 
                     """.trimIndent()),
        )
    }

}