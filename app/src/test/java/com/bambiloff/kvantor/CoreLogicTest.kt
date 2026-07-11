package com.bambiloff.kvantor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreLogicTest {

    @Test
    fun courseCompletionUsesActualModuleIds() {
        val actualModuleIds = listOf("intro", "vars", "loops")

        assertFalse(
            CourseProgressRules.isCourseCompleted(
                actualModuleIds = actualModuleIds,
                completedModuleIds = listOf("intro", "vars", "vars")
            )
        )

        assertTrue(
            CourseProgressRules.isCourseCompleted(
                actualModuleIds = actualModuleIds,
                completedModuleIds = listOf("loops", "intro", "vars", "extra")
            )
        )
    }

    @Test
    fun completedModuleIdsRemainUnique() {
        val actualModuleIds = listOf("intro", "vars")
        val first = CourseProgressRules.markModuleCompleted(
            progress = CourseProgressState(),
            moduleId = "intro",
            actualModuleIds = actualModuleIds
        )
        val duplicate = CourseProgressRules.markModuleCompleted(
            progress = first,
            moduleId = "intro",
            actualModuleIds = actualModuleIds
        )

        assertEquals(setOf("intro"), duplicate.completedModuleIds)
    }

    @Test
    fun moduleDtoFallsBackToFirestoreDocumentId() {
        val module = ModuleDto(
            id = "",
            title = "Intro",
            pages = emptyList()
        ).toModule(documentId = "firestore-intro")

        assertEquals("firestore-intro", module.id)
    }

    @Test
    fun pythonAndJavaScriptProgressStaySeparate() {
        val progress = mapOf(
            "python" to CourseProgressState(moduleIndex = 1, pageIndex = 2),
            "javascript" to CourseProgressState(moduleIndex = 0, pageIndex = 0)
        )

        val updated = progress + (
            "python" to CourseProgressRules.markModuleCompleted(
                progress = progress.getValue("python"),
                moduleId = "py-intro",
                actualModuleIds = listOf("py-intro")
            )
            )

        assertEquals(setOf("py-intro"), updated.getValue("python").completedModuleIds)
        assertTrue(updated.getValue("javascript").completedModuleIds.isEmpty())
        assertEquals(0, updated.getValue("javascript").moduleIndex)
        assertEquals(0, updated.getValue("javascript").pageIndex)
    }

    @Test
    fun lifeRestoreHandlesMultipleIntervalsAndMaxLives() {
        val start = 1_000L

        val restored = LifeRules.restoreLives(
            lives = 5,
            lastLifeTimestampSeconds = start,
            nowSeconds = start + GameConfig.LIFE_RESTORE_INTERVAL * 3 + 5
        )
        assertEquals(8, restored.lives)
        assertEquals(start + GameConfig.LIFE_RESTORE_INTERVAL * 3, restored.lastLifeTimestampSeconds)

        val capped = LifeRules.restoreLives(
            lives = 9,
            lastLifeTimestampSeconds = start,
            nowSeconds = start + GameConfig.LIFE_RESTORE_INTERVAL * 3
        )
        assertEquals(GameConfig.MAX_LIVES, capped.lives)
        assertNull(capped.lastLifeTimestampSeconds)
    }

    @Test
    fun spendLifeNeverGoesNegativeAndDoesNotRestartActiveTimer() {
        val now = 5_000L
        val started = LifeRules.spendLife(
            lives = GameConfig.MAX_LIVES,
            lastLifeTimestampSeconds = null,
            nowSeconds = now
        )
        assertEquals(GameConfig.MAX_LIVES - 1, started?.lives)
        assertEquals(now, started?.lastLifeTimestampSeconds)

        val activeTimer = LifeRules.spendLife(
            lives = 9,
            lastLifeTimestampSeconds = 1_000L,
            nowSeconds = now
        )
        assertEquals(8, activeTimer?.lives)
        assertEquals(1_000L, activeTimer?.lastLifeTimestampSeconds)

        assertNull(
            LifeRules.spendLife(
                lives = 0,
                lastLifeTimestampSeconds = 1_000L,
                nowSeconds = now
            )
        )
    }

    @Test
    fun quizIncorrectAnswerDoesNotAllowNext() {
        val result = QuizProgressRules.answer(
            pageId = "intro:1",
            selectedAnswerIndex = 0,
            correctAnswerIndex = 1,
            rewardedPageIds = emptySet()
        )

        assertFalse(result.correct)
        assertFalse(result.canProceed)
        assertTrue(result.spendLife)
        assertEquals(0, result.rewardCoins)
    }

    @Test
    fun quizCorrectAnswerAllowsNextAndRewardsOnce() {
        val first = QuizProgressRules.answer(
            pageId = "intro:1",
            selectedAnswerIndex = 1,
            correctAnswerIndex = 1,
            rewardedPageIds = emptySet()
        )
        val second = QuizProgressRules.answer(
            pageId = "intro:1",
            selectedAnswerIndex = 1,
            correctAnswerIndex = 1,
            rewardedPageIds = first.rewardedPageIds
        )

        assertTrue(first.correct)
        assertTrue(first.canProceed)
        assertEquals(GameConfig.QUIZ_REWARD_COINS, first.rewardCoins)
        assertEquals(0, second.rewardCoins)
        assertEquals(setOf("intro:1"), second.rewardedPageIds)
    }

    @Test
    fun rewardPageIdsAreStableAndCourseScoped() {
        val pythonFirst = CourseProgressRules.rewardPageId("python", "intro", 0)
        val pythonSecond = CourseProgressRules.rewardPageId("python", "intro", 1)
        val jsFirst = CourseProgressRules.rewardPageId("javascript", "intro", 0)

        assertEquals("python:intro:0", pythonFirst)
        assertFalse(pythonFirst == pythonSecond)
        assertFalse(pythonFirst == jsFirst)
    }

    @Test
    fun rewardRecordingIsIdempotent() {
        val rewardId = CourseProgressRules.rewardPageId("python", "intro", 0)

        assertTrue(CourseProgressRules.shouldAwardQuizReward(rewardId, emptySet()))

        val once = CourseProgressRules.recordRewardedQuizPageId(rewardId, emptySet())
        val twice = CourseProgressRules.recordRewardedQuizPageId(rewardId, once)

        assertFalse(CourseProgressRules.shouldAwardQuizReward(rewardId, twice))
        assertEquals(setOf(rewardId), twice)
    }

    @Test
    fun courseCompletionStateIsComputedBeforeAchievementFailure() {
        val update = CourseProgressRules.completeModuleAndEvaluate(
            progress = CourseProgressState(completedModuleIds = setOf("intro")),
            moduleId = "final",
            actualModuleIds = listOf("intro", "final")
        )
        val achievementUnlock = runCatching { error("unlock failed") }

        assertTrue(update.courseCompleted)
        assertEquals(setOf("intro", "final"), update.progress.completedModuleIds)
        assertTrue(achievementUnlock.isFailure)
    }

    @Test
    fun staleStoredCourseCompletedIsIgnored() {
        val storedCourseCompleted = true
        val recomputedCourseCompleted = CourseProgressRules.isCourseCompleted(
            actualModuleIds = listOf("intro", "final"),
            completedModuleIds = listOf("intro")
        )

        assertTrue(storedCourseCompleted)
        assertFalse(recomputedCourseCompleted)
    }

    @Test
    fun alreadyRewardedQuizFeedbackDoesNotShowCoins() {
        val pageId = CourseProgressRules.rewardPageId("python", "intro", 0)
        val first = QuizProgressRules.answer(
            pageId = pageId,
            selectedAnswerIndex = 1,
            correctAnswerIndex = 1,
            rewardedPageIds = emptySet()
        )
        val alreadyRewarded = QuizProgressRules.answer(
            pageId = pageId,
            selectedAnswerIndex = 1,
            correctAnswerIndex = 1,
            rewardedPageIds = first.rewardedPageIds
        )

        assertEquals(GameConfig.QUIZ_REWARD_COINS, first.rewardCoins)
        assertEquals("✅ Правильно (+10₵)", QuizProgressRules.resultMessage(first))
        assertEquals(0, alreadyRewarded.rewardCoins)
        assertEquals("✅ Правильно", QuizProgressRules.resultMessage(alreadyRewarded))
    }
}
