package com.bambiloff.kvantor

data class ModuleDto(
    var id: String = "",
    var title: String = "",
    var pages: List<PageDto> = emptyList()
) {
    fun toModule(documentId: String = ""): Module {
        return Module(
            id = id.ifBlank { documentId },
            title = title,
            pages = pages.mapNotNull { it.toPageOrNull() }
        )
    }
}

fun PageDto.toPageOrNull(): Page? {
    return try {
        when (type.lowercase()) {
            "theory" -> Page.Theory(text ?: theory ?: "")
            "test" -> Page.Test(
                question = question ?: "",
                answers = answers ?: emptyList(),
                correctAnswerIndex = correctAnswerIndex ?: 0,
                hint     = hint
            )
            "coding" -> Page.CodingTask(
                description = description ?: "",
                expectedCode = expectedCode ?: "",
                codeReviewPlaceholder = codeReviewPlaceholder ?: "",
                hint     = hint
            )
            "final" -> Page.Final(message ?: "Congratulations! You completed the module.")
            else -> null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
