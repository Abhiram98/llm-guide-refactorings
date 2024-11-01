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
            Your task is to identify methods that do not belong to the class and suggest an appropriate class to move them.
            Only provide suggestions that are: Move Method.
                
                
            Ensure that your recommendations are specific to this method/class and are actionable immediately. 
            Your response should be formatted as a JSON list of objects. Each object should comprise of the following fields. 
            The first field, named "method_name", should be the name of the method that needs to move.
            The second field, names "method_signature", should be the signature of the method that needs to move.
            The third field, "target_class" is the class it should move to.
            The fourth field "rationale", is the reason why it should be moved.
                
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
    fun askForMethodPriorityPrompt(classCode: String, moveMethodSuggetions: List<ApplyMoveMethodInteractiveIntention.MoveMethodSuggestion>, methodSimilarity: List<Pair<ApplyMoveMethodInteractiveIntention.MoveMethodSuggestion, Double>>): MutableList<ChatMessage> {
        return mutableListOf(
            SystemMessage.from("You are an expert Java developer who prioritises move-method refactoring suggestions based on your expertise."),
            UserMessage.from("""
                Here is a java class:
                ${classCode}



                Please rank the following move-method suggestions:
                 ${
                     Gson().toJson(moveMethodSuggetions.map{it.methodName})
                 }

                Respond in a JSON list, with the most important move-method suggestion at the beginning of the list.
                If you think it is not important to move some any of these methods, exclude them from the response list.
                     """.trimIndent()),
        )
    }

    fun askForMethodPriorityPromptWithSimilarity(
        classCode: String,
        methodSimilarity: List<Pair<ApplyMoveMethodInteractiveIntention.MoveMethodSuggestion, Double>>
    ): MutableList<ChatMessage> {
        // Construct a list of methods with their similarity scores
        val methodsWithScores = methodSimilarity.map { (suggestion, score) ->
            mapOf(
                "methodName" to suggestion.methodName,
                "similarityScore" to score
            )
        }

        // Convert the list to a JSON string for structured representation
        val methodsJson = Gson().toJson(methodsWithScores)

        // Construct the prompt messages
        return mutableListOf(
            SystemMessage.from("You are an expert Java developer who specializes in move-method refactoring."),
            UserMessage.from("""
            Here is a Java class:
            ```java
            ${classCode}
            ```
            
            Below are the move-method suggestions along with their similarity scores:
            ```json
            $methodsJson
            ```
            
            Please rank the following move-method suggestions based on their importance for refactoring. 
            Consider both the provided similarity scores and your own expertise to determine the priority.
            
            Respond with a JSON list of method names ordered from highest to lowest priority. 
        """.trimIndent())
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
                Gson().toJson(moveMethodSuggetions.map{it.methodSignature})
            }
                    
                Respond in a JSON list, with the most important move-method suggestion at the beginning of the list. 
                     """.trimIndent()),
        )
    }

    fun askForMethodPriorityPrompt(
        classCode: String,
        moveMethodSuggestions: List<ApplyMoveMethodInteractiveIntention.MoveMethodSuggestion>,
        limit: Int
    ): MutableList<ChatMessage> {
        return mutableListOf(
            SystemMessage.from("You are an expert Java developer who specializes in move-method refactoring."),
            UserMessage.from("""
            Analyze this class and prioritize all candidate methods for potential movement. Follow these exact steps:

            1. First, analyze each candidate method using this format:
               - Method: [name]
               - Purpose: [one-line description of what it does]
               - Dependencies: [what data/methods it uses from current class vs other classes]
               - Cohesion: [how related is it to class's main purpose]

            2. Then, summarize:
               - The main responsibility of this class
               - Which methods align with this responsibility
               - Which methods could be better placed elsewhere

            3. Finally, provide a JSON array of the top **$limit** candidate method signatures ordered by priority (highest priority first).
               **Ensure that exactly $limit method signatures are included.**

            **DO NOT include any summary or analysis in the output. Only return the JSON array.**

            Class code:
            ${classCode}

            Candidate method signatures:
            ${moveMethodSuggestions.mapIndexed { index, suggestion -> "${index + 1}. ${suggestion.methodSignature}" }.joinToString("\n")}
                     
            **Your final output should be only a JSON list of the top $limit method signatures ordered from highest priority to lowest priority, like the example below:**

            [
                "public void calculateResults(String input)",
                "private int fetchData(String query)",
                "protected List<String> processItems(List<Integer> items)"
                // ... up to $limit method signatures
            ]
        """.trimIndent())
        )
    }

    fun askForTargetClassPriorityPrompt(methodCode: String,
                                        potentialClassBody: String?,
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
                Include detailed information in the rationale, including why the method needs to move out the existing class, and why it should move to the target class.  
                Ex:
                 [
                    {
                        "target_class": "Customer",
                        "rationale": "calculateDiscount() relies heavily on the Customer class, so it might be more appropriate to move this method to the Customer class.",
                    }
                ]
                
                Here are the class body of the potential target classes for your reference:
                    ${potentialClassBody}
                    
                     """.trimIndent()),
        )
    }

}