package com.bambiloff.kvantor

import android.annotation.SuppressLint
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object GameManager {

    @SuppressLint("StaticFieldLeak")
    private val db = FirebaseFirestore.getInstance()

    private fun ref(uid: String) =
        db.collection("users").document(uid)

    /* ------------ додати життя / підказки ------------ */
    suspend fun addLives(uid: String, delta: Int) = withContext(Dispatchers.IO) {
        val ref = db.collection("users").document(uid)
        db.runTransaction { tx ->
            val cur = (tx.get(ref).getLong("lives") ?: 0).toInt()
            tx.update(ref, "lives", (cur + delta).coerceIn(0, GameConfig.MAX_LIVES))
        }.await()
    }

    suspend fun addHints(uid: String, delta: Int) = withContext(Dispatchers.IO) {
        val ref = db.collection("users").document(uid)
        db.runTransaction { tx ->
            val cur = (tx.get(ref).getLong("hints") ?: 0).toInt()
            tx.update(ref, "hints", cur + delta)
        }.await()
    }

    /* ---------- lives ---------- */

    /** −1 life. Повертає false, якщо життя вже =0 */
    suspend fun spendLife(uid: String): Boolean = withContext(Dispatchers.IO) {
        db.runTransaction { tx ->
            val userRef = ref(uid)
            val doc = tx.get(userRef)
            val lives = (doc.getLong("lives") ?: 0).toInt()
            val last = doc.getTimestamp("lastLifeTS")?.seconds
            val next = LifeRules.spendLife(lives, last, Timestamp.now().seconds)
                ?: return@runTransaction false

            tx.update(userRef, "lives", next.lives)
            tx.update(
                userRef,
                "lastLifeTS",
                next.lastLifeTimestampSeconds?.let { Timestamp(it, 0) }
            )
            true
        }.await()
    }

    /** Повертає життя, якщо минуло ≥10 хв і їх <10 */
    suspend fun maybeRestoreLife(uid: String, maxLives: Int = GameConfig.MAX_LIVES) =
        withContext(Dispatchers.IO) {
            db.runTransaction { tx ->
                val userRef = ref(uid)
                val doc = tx.get(userRef)
                val lives = (doc.getLong("lives") ?: maxLives.toLong()).toInt()
                val last = doc.getTimestamp("lastLifeTS")?.seconds
                val next = LifeRules.restoreLives(lives, last, Timestamp.now().seconds)

                if (next.lives != lives || next.lastLifeTimestampSeconds != last) {
                    tx.update(userRef, "lives", next.lives.coerceAtMost(maxLives))
                    tx.update(
                        userRef,
                        "lastLifeTS",
                        next.lastLifeTimestampSeconds?.let { Timestamp(it, 0) }
                    )
                }
            }.await()
        }

    /* ---------- hints ---------- */

    /** −1 hint. Повертає false, якщо підказки =0 */
    suspend fun spendHint(uid: String): Boolean =
        withContext(Dispatchers.IO) {
            db.runTransaction { tx ->
                val doc   = tx.get(ref(uid))
                val hints = (doc.getLong("hints") ?: 0).toInt()
                if (hints > 0) {
                    tx.update(ref(uid), "hints", hints - 1)
                    true
                } else false
            }.await()
        }

    /* ---------- coins ---------- */

    suspend fun addCoins(uid: String, amount: Int) =
        ref(uid).update("coins", FieldValue.increment(amount.toLong())).await()

    suspend fun awardQuizRewardIfNeeded(
        uid: String,
        courseType: String,
        rewardPageId: String
    ): QuizRewardResult = withContext(Dispatchers.IO) {
        runCatching {
            db.runTransaction { tx ->
                val userRef = ref(uid)
                val doc = tx.get(userRef)
                val rewardedIds = (doc.get("progress.$courseType.rewardedQuizPageIds") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?: emptyList()

                if (!CourseProgressRules.shouldAwardQuizReward(rewardPageId, rewardedIds)) {
                    return@runTransaction QuizRewardResult.ALREADY_REWARDED
                }

                val updatedRewardedIds = CourseProgressRules
                    .recordRewardedQuizPageId(rewardPageId, rewardedIds)
                    .sorted()

                tx.update(userRef, "coins", FieldValue.increment(GameConfig.QUIZ_REWARD_COINS.toLong()))
                tx.set(
                    userRef,
                    mapOf(
                        "progress" to mapOf(
                            courseType to mapOf("rewardedQuizPageIds" to updatedRewardedIds)
                        )
                    ),
                    SetOptions.merge()
                )
                QuizRewardResult.AWARDED
            }.await()
        }.getOrElse { QuizRewardResult.FAILURE }
    }

    suspend fun buyLife(uid: String): PurchaseResult = withContext(Dispatchers.IO) {
        runCatching {
            db.runTransaction { tx ->
                val userRef = ref(uid)
                val doc = tx.get(userRef)
                val coins = (doc.getLong("coins") ?: 0).toInt()
                val lives = (doc.getLong("lives") ?: 0).toInt()

                when (val result = LifeRules.lifePurchaseResult(lives, coins)) {
                    PurchaseResult.SUCCESS -> {
                        tx.update(userRef, "coins", coins - GameConfig.LIFE_COST)
                        tx.update(userRef, "lives", (lives + 1).coerceAtMost(GameConfig.MAX_LIVES))
                        result
                    }
                    else -> result
                }
            }.await()
        }.getOrElse { PurchaseResult.FAILURE }
    }

    suspend fun buyHint(uid: String): PurchaseResult = withContext(Dispatchers.IO) {
        runCatching {
            db.runTransaction { tx ->
                val userRef = ref(uid)
                val doc = tx.get(userRef)
                val coins = (doc.getLong("coins") ?: 0).toInt()
                val hints = (doc.getLong("hints") ?: 0).toInt()

                when (val result = LifeRules.hintPurchaseResult(coins)) {
                    PurchaseResult.SUCCESS -> {
                        tx.update(userRef, "coins", coins - GameConfig.HINT_COST)
                        tx.update(userRef, "hints", hints + 1)
                        result
                    }
                    else -> result
                }
            }.await()
        }.getOrElse { PurchaseResult.FAILURE }
    }
}
