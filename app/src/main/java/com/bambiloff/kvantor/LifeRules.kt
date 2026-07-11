package com.bambiloff.kvantor

data class LifeState(
    val lives: Int,
    val lastLifeTimestampSeconds: Long?
)

enum class PurchaseResult {
    SUCCESS,
    INSUFFICIENT_COINS,
    FULL_LIVES,
    FAILURE
}

object LifeRules {
    fun spendLife(
        lives: Int,
        lastLifeTimestampSeconds: Long?,
        nowSeconds: Long
    ): LifeState? {
        val currentLives = lives.coerceIn(0, GameConfig.MAX_LIVES)
        if (currentLives <= 0) return null

        val nextTimestamp = if (lastLifeTimestampSeconds == null || currentLives == GameConfig.MAX_LIVES) {
            nowSeconds
        } else {
            lastLifeTimestampSeconds
        }

        return LifeState(
            lives = currentLives - 1,
            lastLifeTimestampSeconds = nextTimestamp
        )
    }

    fun restoreLives(
        lives: Int,
        lastLifeTimestampSeconds: Long?,
        nowSeconds: Long
    ): LifeState {
        val currentLives = lives.coerceIn(0, GameConfig.MAX_LIVES)
        if (currentLives >= GameConfig.MAX_LIVES) {
            return LifeState(GameConfig.MAX_LIVES, null)
        }

        val last = lastLifeTimestampSeconds ?: return LifeState(currentLives, nowSeconds)
        val elapsed = (nowSeconds - last).coerceAtLeast(0)
        val intervals = elapsed / GameConfig.LIFE_RESTORE_INTERVAL
        if (intervals <= 0) return LifeState(currentLives, last)

        val missingLives = GameConfig.MAX_LIVES - currentLives
        val restoredLives = intervals.coerceAtMost(missingLives.toLong()).toInt()
        val nextLives = (currentLives + restoredLives).coerceAtMost(GameConfig.MAX_LIVES)
        val nextTimestamp = if (nextLives >= GameConfig.MAX_LIVES) {
            null
        } else {
            last + intervals * GameConfig.LIFE_RESTORE_INTERVAL
        }

        return LifeState(nextLives, nextTimestamp)
    }

    fun lifePurchaseResult(lives: Int, coins: Int): PurchaseResult = when {
        lives >= GameConfig.MAX_LIVES -> PurchaseResult.FULL_LIVES
        coins < GameConfig.LIFE_COST -> PurchaseResult.INSUFFICIENT_COINS
        else -> PurchaseResult.SUCCESS
    }

    fun hintPurchaseResult(coins: Int): PurchaseResult = when {
        coins < GameConfig.HINT_COST -> PurchaseResult.INSUFFICIENT_COINS
        else -> PurchaseResult.SUCCESS
    }
}
