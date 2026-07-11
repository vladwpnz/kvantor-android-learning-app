package com.bambiloff.kvantor

data class QuizAttemptResult(
    val correct: Boolean,
    val canProceed: Boolean,
    val rewardCoins: Int,
    val spendLife: Boolean,
    val rewardedPageIds: Set<String>
)

enum class QuizRewardResult {
    AWARDED,
    ALREADY_REWARDED,
    FAILURE
}

object QuizProgressRules {
    fun answer(
        pageId: String,
        selectedAnswerIndex: Int,
        correctAnswerIndex: Int,
        rewardedPageIds: Set<String>
    ): QuizAttemptResult {
        if (selectedAnswerIndex < 0) {
            return QuizAttemptResult(
                correct = false,
                canProceed = false,
                rewardCoins = 0,
                spendLife = false,
                rewardedPageIds = rewardedPageIds
            )
        }

        val correct = selectedAnswerIndex == correctAnswerIndex
        val shouldReward = correct && pageId !in rewardedPageIds

        return QuizAttemptResult(
            correct = correct,
            canProceed = correct,
            rewardCoins = if (shouldReward) GameConfig.QUIZ_REWARD_COINS else 0,
            spendLife = !correct,
            rewardedPageIds = if (shouldReward) rewardedPageIds + pageId else rewardedPageIds
        )
    }

    fun resultMessage(result: QuizAttemptResult): String = when {
        result.correct && result.rewardCoins > 0 -> "✅ Правильно (+10₵)"
        result.correct -> "✅ Правильно"
        else -> "❌ Неправильно (-1 ❤️)"
    }
}
