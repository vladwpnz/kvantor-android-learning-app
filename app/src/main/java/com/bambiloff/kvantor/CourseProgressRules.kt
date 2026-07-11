package com.bambiloff.kvantor

data class CourseProgressState(
    val moduleIndex: Int = 0,
    val pageIndex: Int = 0,
    val completedModuleIds: Set<String> = emptySet(),
    val rewardedQuizPageIds: Set<String> = emptySet(),
    val courseCompleted: Boolean = false
)

data class CompletionUpdate(
    val progress: CourseProgressState,
    val courseCompleted: Boolean
)

object CourseProgressRules {
    fun defaultState() = CourseProgressState()

    fun sanitizePosition(
        moduleIndex: Int,
        pageIndex: Int,
        modules: List<Module>
    ): CourseProgressState {
        val safeModuleIndex = moduleIndex.coerceIn(0, modules.lastIndex.coerceAtLeast(0))
        val pagesLastIndex = modules.getOrNull(safeModuleIndex)?.pages?.lastIndex ?: 0
        return CourseProgressState(
            moduleIndex = safeModuleIndex,
            pageIndex = pageIndex.coerceIn(0, pagesLastIndex)
        )
    }

    fun markModuleCompleted(
        progress: CourseProgressState,
        moduleId: String,
        actualModuleIds: Collection<String>
    ): CourseProgressState {
        if (moduleId.isBlank() || moduleId !in actualModuleIds.toSet()) return progress
        return progress.copy(completedModuleIds = progress.completedModuleIds + moduleId)
    }

    fun completeModuleAndEvaluate(
        progress: CourseProgressState,
        moduleId: String,
        actualModuleIds: Collection<String>
    ): CompletionUpdate {
        val updatedProgress = markModuleCompleted(progress, moduleId, actualModuleIds)
        val completed = isCourseCompleted(actualModuleIds, updatedProgress.completedModuleIds)
        return CompletionUpdate(
            progress = updatedProgress.copy(courseCompleted = completed),
            courseCompleted = completed
        )
    }

    fun isCourseCompleted(
        actualModuleIds: Collection<String>,
        completedModuleIds: Collection<String>
    ): Boolean {
        val actual = actualModuleIds.filter { it.isNotBlank() }.toSet()
        val completed = completedModuleIds.filter { it.isNotBlank() }.toSet()
        return actual.isNotEmpty() && completed.containsAll(actual)
    }

    fun mergeCompatibleCompletedIds(
        courseCompletedIds: Collection<String>,
        legacyCompletedIds: Collection<String>,
        actualModuleIds: Collection<String>
    ): Set<String> {
        val actual = actualModuleIds.toSet()
        return (courseCompletedIds + legacyCompletedIds)
            .filter { it in actual }
            .toSet()
    }

    fun rewardPageId(courseType: String, moduleId: String, pageIndex: Int): String =
        "${courseType.trim()}:${moduleId.trim()}:$pageIndex"

    fun shouldAwardQuizReward(
        rewardPageId: String,
        rewardedQuizPageIds: Collection<String>
    ): Boolean = rewardPageId.isNotBlank() && rewardPageId !in rewardedQuizPageIds.toSet()

    fun recordRewardedQuizPageId(
        rewardPageId: String,
        rewardedQuizPageIds: Collection<String>
    ): Set<String> =
        if (rewardPageId.isBlank()) rewardedQuizPageIds.toSet()
        else rewardedQuizPageIds.toSet() + rewardPageId
}
