package org.boulderse.ijserver.suggestrefactoring

import com.google.gson.annotations.SerializedName


//{
//    "Improvements": [
//    {
//        "Improvement": "Avoid empty catch blocks",
//        "Change_Diff": "- catch (final InterruptedException e) {\n+ catch (final InterruptedException e) {\n+     e.printStackTrace();\n  }",
//        "Description": "An empty catch block can hide the occurrence of exceptions that you may need to handle. Instead, it's better to at least log the exception so it can be debugged.",
//        "Start": 8,
//        "End": 9
//    },
//    {
//        "Improvement": "Reduce System.out.println statements",
//        "Change_Diff": "- System.out.println(...);\n+ this.log(...);",
//        "Description": "Multiple System.out.println statements can make the code hard to read and maintain. Consolidate these by creating a helper method for log display.",
//        "Start": 2,
//        "End": 57
//    },
//    {
//        "Improvement": "Extract duplicate code into helper methods",
//        "Change_Diff": "- catch (final AmazonServiceException e) {...}\n+ handleException(e, stackName, finalStackStatus);",
//        "Description": "There is duplicate code for handling exceptions and logging. Extract these processes into separate helper methods to reduce code redundancy and improve maintainability.",
//        "Start": 23,
//        "End": 58
//    }
//    ],
//    "Final code": "1. protected void waitForStack(final Context context,final String stackName,final FinalStatus finalStackStatus){\n2.   log('waitForStack[' + stackName + ']: to reach status '+ finalStackStatus.finalStatus);\n3.   final List<StackEvent> eventsDisplayed=new ArrayList<>();\n4.   while (true) {\n5.     try {\n6.       Thread.sleep(20000);\n7.     }\n8.  catch (final InterruptedException e) {\n9.     e.printStackTrace();\n10.   }\n11.     final List<StackEvent> events=getStackEvents(stackName);\n12.     for (final StackEvent event : events) {\n13.       boolean displayed=false;\n14.       for (final StackEvent eventDisplayed : eventsDisplayed) {\n15.         if (event.getEventId().equals(eventDisplayed.getEventId())) {\n16.           displayed=true;\n17.         }\n18.       }\n19.       if (!displayed) {\n20.         log('waitForStack[' + stackName + ']: '+ event.getTimestamp().toString()+ ' '+ event.getLogicalResourceId()+ ' '+ event.getResourceStatus()+ ' '+ event.getResourceStatusReason());\n21.         eventsDisplayed.add(event);\n22.       }\n23.     }\n24.     try {\n25.       final DescribeStacksResult res=this.cf.describeStacks(new DescribeStacksRequest().withStackName(stackName));\n26.       final StackStatus currentStatus=StackStatus.fromValue(res.getStacks().get(0).getStackStatus());\n27.       if (finalStackStatus.finalStatus == currentStatus) {\n28.         log('waitForStack[' + stackName + ']: final status reached.');\n29.         return;\n30.       }\n31.  else {\n32.         if (finalStackStatus.intermediateStatus.contains(currentStatus)) {\n33.           log('waitForStack[' + stackName + ']: continue to wait (still in intermediate status '+ currentStatus+ ') ...');\n34.         }\n35.  else {\n36.           context.reportStackFailure(stackName);\n37.           throw new RuntimeException('waitForStack[' + stackName + ']: reached invalid intermediate status '+ currentStatus+ '.');\n38.         }\n39.       }\n40.     }\n41.  catch (final AmazonServiceException e) {\n42.       handleException(e, stackName, finalStackStatus);\n43.     }\n44.   }\n45. }\n\n46. private void log(String message) {\n47.   System.out.println(message);\n48. }\n\n49. private void handleException(final AmazonServiceException e, final String stackName, final FinalStatus finalStackStatus) {\n50.   if (e.getErrorMessage().equals('Stack with id ' + stackName + ' does not exist')) {\n51.     if (finalStackStatus.notFoundIsFinalStatus) {\n52.       log('waitForStack[' + stackName + ']: final  reached (not found).');\n53.       return;\n54.     }\n55.  else {\n56.       if (finalStackStatus.notFoundIsIntermediateStatus) {\n57.         log('waitForStack[' + stackName + ']: continue to wait (stack not found) ...');\n58.       }\n59.  else {\n60.         context.reportStackFailure(stackName);\n61.         throw new RuntimeException('waitForStack[' + stackName + ']: stack not found.');\n62.       }\n63.     }\n64.   }\n65.  else {\n66.     throw e;\n67.   }\n68. }"
//}

data class RefactoringSuggestion(
    @SerializedName("improvements")
    val improvements:List<AtomicSuggestion>,

) {
}

data class AtomicSuggestion(
    @SerializedName("shortDescription")
    val shortDescription: String,

    @SerializedName("longDescription")
    val longDescription: String,

    @SerializedName("start")
    val start: Int,

    @SerializedName("end")
    val end: Int

    ){
    fun getSerialized(): String{
        return shortDescription+longDescription+start+end
    }
}