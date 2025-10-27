package org.boulderse.ijserver.utils

import org.junit.Test
import org.junit.jupiter.api.Assertions.*

class JsonUtilsTest {
    @Test
    fun testSanitise() {
        val str =
            "```json\n" +
                "[\n" +
                "    \"handleShareFetchSuccess\",\n" +
                "    \"handleShareFetchFailure\",\n" +
                "    \"handleShareAcknowledgeSuccess\",\n" +
                "    \"handleShareAcknowledgeFailure\",\n" +
                "    \"handleShareAcknowledgeCloseSuccess\",\n" +
                "    \"handleShareAcknowledgeCloseFailure\",\n" +
                "    \"isNodeFree\",\n" +
                "    \"isRequestStateInProgress\",\n" +
                "    \"maybeBuildRequest\"\n" +
                "]\n" +
                "```"
        val sanitised = JsonUtils.sanitizeJson(str)
        print(sanitised)
        assertTrue(
            sanitised == "\n" +
                "[\n" +
                "    \"handleShareFetchSuccess\",\n" +
                "    \"handleShareFetchFailure\",\n" +
                "    \"handleShareAcknowledgeSuccess\",\n" +
                "    \"handleShareAcknowledgeFailure\",\n" +
                "    \"handleShareAcknowledgeCloseSuccess\",\n" +
                "    \"handleShareAcknowledgeCloseFailure\",\n" +
                "    \"isNodeFree\",\n" +
                "    \"isRequestStateInProgress\",\n" +
                "    \"maybeBuildRequest\"\n" +
                "]\n",
        )
    }

    @Test
    fun testSanitise2() {
        val str =
            "```json\n" +
                "[\n" +
                "    \"handleShareFetchSuccess\",\n" +
                "    \"handleShareFetchFailure\",\n" +
                "    \"handleShareAcknowledgeSuccess\",\n" +
                "    \"handleShareAcknowledgeFailure\",\n" +
                "    \"handleShareAcknowledgeCloseSuccess\",\n" +
                "    \"handleShareAcknowledgeCloseFailure\",\n" +
                "    \"isNodeFree\",\n" +
                "    \"isRequestStateInProgress\",\n" +
                "    \"maybeBuildRequest\"\n" +
                "]\n" +
                "```\n" +
                "\n" +
                "### Explanation\n" +
                "\n" +
                "1. **Handle methods**: \n" +
                "   - \"handleShareFetchSuccess\", \"handleShareFetchFailure\", \"handleShareAcknowledgeSuccess\", \"handleShareAcknowledgeFailure\", \"handleShareAcknowledgeCloseSuccess\", and \"handleShareAcknowledgeCloseFailure\" are all specific to handling various responses. Moving these methods to their respective classes or handlers could make the code more modular and maintainable.\n" +
                "\n" +
                "2. **Utility methods**:\n" +
                "   - \"isNodeFree\" and \"isRequestStateInProgress\" are utility methods that provide state-checking functionalities. Moving them to a more appropriate utility class can keep the `ShareConsumeRequestManager` class cleaner.\n" +
                "   \n" +
                "3. **Request building**:\n" +
                "   - \"maybeBuildRequest\" deals with building requests and can likely be better managed in a class closely related to requests themselves.\n" +
                "\n" +
                "Methods like `processAcknowledgements` and `checkAndRemoveCompletedAcknowledgements` are more integral to the `ShareConsumeRequestManager`'s responsibilities and hence were excluded from the list.\n" +
                "No methods are important to move.\n"
        val sanitised = JsonUtils.sanitizeJson(str)
        print(sanitised)
        assertTrue(
            sanitised == "\n" +
                "[\n" +
                "    \"handleShareFetchSuccess\",\n" +
                "    \"handleShareFetchFailure\",\n" +
                "    \"handleShareAcknowledgeSuccess\",\n" +
                "    \"handleShareAcknowledgeFailure\",\n" +
                "    \"handleShareAcknowledgeCloseSuccess\",\n" +
                "    \"handleShareAcknowledgeCloseFailure\",\n" +
                "    \"isNodeFree\",\n" +
                "    \"isRequestStateInProgress\",\n" +
                "    \"maybeBuildRequest\"\n" +
                "]\n",
        )
    }

    @Test
    fun test3() {
        val json = "```json\n[\n    \"getTrueFunction\",\n    \"createEndingState\"\n]\n```\n\n### Explanation\n1. **getTrueFunction**: This method is a utility function within the `NFAFactoryCompiler` class and does not depend on the state or the primary attributes of `NFACompiler`. It is a small, self-contained utility that is a good candidate for being moved to a utility class or kept as an inner method of `NFAFactoryCompiler`.\n\n2. **createEndingState**: This method is a helper method within `NFAFactoryCompiler` for generating the NFA's ending state. It's tightly focused on the task of setting up states and could benefit from being encapsulated in `NFAFactoryCompiler` to aid in its cohesion and maintainability.\n\n### Not Included\n- **canProduceEmptyMatches**: This method is a static method that uses `NFAFactoryCompiler` to compile patterns and perform a check. Its purpose is well-defined in the context of the `NFACompiler` class, making it less important to move.\n\n- **compileFactory**: This method is a primary method of `NFACompiler` and orchestrates the main functionality of compiling a pattern into an NFA factory. It serves as a core component of the `NFACompiler` class and therefore should not be moved.\n```"
        val s = JsonUtils.sanitizeJson(json)
        print(s)
    }
}
