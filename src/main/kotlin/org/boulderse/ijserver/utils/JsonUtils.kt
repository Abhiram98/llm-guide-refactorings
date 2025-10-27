package org.boulderse.ijserver.utils

class JsonUtils {
    companion object {
        fun sanitizeJson(jsonText: String): String {
            val regex = Regex("```json(.*?)```", RegexOption.DOT_MATCHES_ALL)
            val match = regex.find(jsonText)
            if (match != null) {
                return match.groups.get(1)!!.value
            }

            val regex2 = Regex("```JSON(.*?)```", RegexOption.DOT_MATCHES_ALL)
            val match2 = regex2.find(jsonText)
            if (match2 != null) {
                return match2.groups.get(1)!!.value
            }

            return jsonText
        }
    }
}
