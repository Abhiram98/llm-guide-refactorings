package com.intellij.ml.llm.template.prompts

import com.google.gson.Gson
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
            **Task**: Identify methods that violate good software design practices, such as the Single Responsibility Principle or low coupling. Specifically, find methods that do not logically belong to their current class and suggest an appropriate class to move them to. 
            Provide only suggestions that involve moving methods (i.e., "Move Method" refactoring).
            
            **Instructions**:
            
            Ensure that your recommendations are specific to this method/class and are actionable immediately. 
            Your response should be formatted as a JSON list of objects. Each object should comprise of the following fields. 
            The first field, named "method_name", should be the name of the method that needs to move.
            The second field, names "method_signature", should be the signature of the method that needs to move.
            The third field, "target_class" is the class it should move to.
            The fourth field "rationale", is the reason why it should be moved.
            
            **Example**:
            
            class Order {
                private Customer customer;
                private double amount;
            
                public double calculateDiscount() {
                    if (customer.getLoyaltyPoints() > 1000) {
                        return amount * 0.1;
                    } else if (customer.getMembershipLevel().equals("Gold")) {
                        return amount * 0.05;
                    } else {
                        return 0;
                    }
                }
            }
                     """.trimIndent()),
            AiMessage.from("""
                            [
                                {
                                    "method_name":"calculateDiscount",
                                    "method_signature": "public calculateDiscount(double amount): double",
                                    "target_class": "Customer",
                                    "rationale": "calculateDiscount() relies heavily on the Customer class, so it might be more appropriate to move this method to the Customer class.",
                                }
                            ]
                            """.trimIndent()),
            UserMessage.from(methodCode)
        )
    }
    fun askForMethodPriorityPrompt(methodCode: String, moveMethodSuggetions: List<ApplyMoveMethodInteractiveIntention.MoveMethodSuggestion>): MutableList<ChatMessage> {
        return mutableListOf(
            SystemMessage.from("You are an expert Java developer who prioritises move-method refactoring suggestions based on your expertise."),
            UserMessage.from("""
                Here is a java class:
                ${methodCode}
                
                Please rank the following move-method suggestions:
                 ${
                     Gson().toJson(moveMethodSuggetions.map{it.methodName})
                 }
                    
                Respond in a JSON list, with the most important move-method suggestion at the beginning of the list. 
                If you think it is not important to move some any of these methods, exclude them from the response list.
                     """.trimIndent()),
        )
    }

    fun askForTargetClassPriorityPrompt(methodCode: String,
                                        movePivots: List<MoveMethodFactory.MovePivot>): MutableList<ChatMessage>{
        return mutableListOf(
            SystemMessage.from("You are an expert Java developer. " +
                    "You are told that a certain method doesn't belong to a class," +
                    " and it is your responsibility to decide which class the method should move to," +
                    " based on your expertise. "),
            UserMessage.from("""
                Here is the method that needs to move:
                ${methodCode}
                
                Please decide which target class is the best option:
                   ${Gson().toJson(movePivots.map { it.psiClass.name }) }} 
                Respond with ONLY a JSON list of objects (with keys "target_class" and "rationale"), with the most important target class suggestion at the beginning of the list. 
                Ex:
                 [
                    {
                        "target_class": "Customer",
                        "rationale": "calculateDiscount() relies heavily on the Customer class, so it might be more appropriate to move this method to the Customer class.",
                    }
                ]
                     """.trimIndent()),
        )
    }

}