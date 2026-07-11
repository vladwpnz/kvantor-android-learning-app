// File: app/src/main/java/com/bambiloff/kvantor/LessonViewModel.kt
package com.bambiloff.kvantor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LessonViewModel(
    private val courseType: String = "python"   // "python" або "javascript"
) : ViewModel() {

    /* ---------------- Firebase ---------------- */
    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /* ---------------- Data ---------------- */
    private val _modules            = MutableStateFlow<List<Module>>(emptyList())
    val           modules: StateFlow<List<Module>> = _modules

    private val _currentModuleIndex = MutableStateFlow(0)
    val           currentModuleIndex: StateFlow<Int> = _currentModuleIndex

    private val _currentPageIndex   = MutableStateFlow(0)
    val           currentPageIndex:  StateFlow<Int> = _currentPageIndex

    private val _completedModuleIds = MutableStateFlow<Set<String>>(emptySet())
    val           completedModuleIds: StateFlow<Set<String>> = _completedModuleIds

    private val _courseCompleted = MutableStateFlow(false)
    val           courseCompleted: StateFlow<Boolean> = _courseCompleted

    /* ----------  Gamification  ---------- */
    private val _lives     = MutableStateFlow(0)
    val           lives:    StateFlow<Int> = _lives

    private val _hints     = MutableStateFlow(0)
    val           hints:    StateFlow<Int> = _hints

    private val _coins     = MutableStateFlow(0)
    val           coins:    StateFlow<Int> = _coins

    private val _showHint  = MutableStateFlow<String?>(null)
    val           showHint: StateFlow<String?> = _showHint

    /* ----------  last life timestamp & таймер ---------- */
    private val _lastLifeTS     = MutableStateFlow<Timestamp?>(null)
    private val _timeToNextLife = MutableStateFlow(0L)      // сек
    val           timeToNextLife: StateFlow<Long> = _timeToNextLife

    /* (можна реагувати у UI) */
    sealed interface UiEvent {
        object NoLives  : UiEvent
        object NoHints  : UiEvent
        object NoCoins  : UiEvent
        object SaveFailed : UiEvent
        object AchievementUnlockFailed : UiEvent
        data class PurchaseFinished(
            val item: PurchaseItem,
            val result: PurchaseResult
        ) : UiEvent
    }
    private val _events = MutableSharedFlow<UiEvent>()
    val           events = _events.asSharedFlow()

    private var userListener: ListenerRegistration? = null
    private var rewardedQuizPageIds: Set<String> = emptySet()

    enum class PurchaseItem {
        LIFE,
        HINT
    }

    /* ---------------- Current module helper ---------------- */
    val currentModule = combine(_modules, _currentModuleIndex) { list, idx ->
        list.getOrNull(idx)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /* -------------------------------------------------------------------- */
    init {
        auth.currentUser?.uid?.let { uid ->
            /* ---- 1. live listener на документ користувача ---- */
            userListener = db.collection("users").document(uid)
                .addSnapshotListener { snap, _ ->
                    snap ?: return@addSnapshotListener
                    _lives.value      = (snap.getLong("lives") ?: 0).toInt().coerceIn(0, GameConfig.MAX_LIVES)
                    _hints.value      = (snap.getLong("hints") ?: 0).toInt()
                    _coins.value      = (snap.getLong("coins") ?: 0).toInt()
                    _lastLifeTS.value = snap.getTimestamp("lastLifeTS")
                }

            /* ---- 2. одноразова ініціалізація ---- */
            viewModelScope.launch {
                UserBootstrapper.ensureStats(uid)
                GameManager.maybeRestoreLife(uid)
            }

            /* ---- 3. тікер 1 сек: рахуємо до наступного ❤️ ---- */
            viewModelScope.launch {
                while (true) {
                    delay(1_000)

                    val livesNow = _lives.value
                    if (livesNow >= GameConfig.MAX_LIVES) {
                        _timeToNextLife.value = 0
                        continue
                    }

                    val last = _lastLifeTS.value
                    if (last == null) { _timeToNextLife.value = 0; continue }

                    val passed = (Timestamp.now().seconds - last.seconds).coerceAtLeast(0)
                    if (passed >= GameConfig.LIFE_RESTORE_INTERVAL) {
                        _timeToNextLife.value = 0
                        GameManager.maybeRestoreLife(uid, GameConfig.MAX_LIVES)
                    } else {
                        _timeToNextLife.value = GameConfig.LIFE_RESTORE_INTERVAL - passed
                    }
                }
            }
        }
    }

    /* ---------------- Modules loading ---------------- */
    fun loadModules() {
        viewModelScope.launch {
            val collection = if (courseType == "javascript") "modules_js" else "modules"
            try {
                val snapshot = db.collection(collection).get().await()
                _modules.value = snapshot.documents
                    .mapNotNull { doc ->
                        doc.toObject(ModuleDto::class.java)?.toModule(doc.id)
                    }
                    .sortedBy { it.id }

                restoreProgress()          // відновлюємо позицію
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /* ---------------- Progress save / restore ---------------- */
    fun saveProgress() {
        viewModelScope.launch {
            saveProgressNow()
        }
    }

    suspend fun saveProgressNow(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return runCatching {
            persistProgress(uid)
            true
        }.getOrElse {
            _events.emit(UiEvent.SaveFailed)
            false
        }
    }

    private suspend fun persistProgress(uid: String) {
        val progress = mapOf(
            "moduleIndex" to _currentModuleIndex.value,
            "pageIndex" to _currentPageIndex.value,
            "completedModuleIds" to _completedModuleIds.value.sorted(),
            "courseCompleted" to _courseCompleted.value
        )
        db.collection("users").document(uid)
            .set(
                mapOf("progress" to mapOf(courseType to progress)),
                SetOptions.merge()
            )
            .await()
    }

    private suspend fun restoreProgress() {
        auth.currentUser?.uid?.let { uid ->
            try {
                val doc = db.collection("users").document(uid).get().await()
                val progressRoot = doc.get("progress").asMap()
                val courseProgress = progressRoot?.get(courseType).asMap()

                val mIdx = courseProgress?.get("moduleIndex").asInt()
                val pIdx = courseProgress?.get("pageIndex").asInt()
                val safePosition = CourseProgressRules.sanitizePosition(mIdx, pIdx, _modules.value)

                _currentModuleIndex.value = safePosition.moduleIndex
                _currentPageIndex.value = safePosition.pageIndex

                val actualModuleIds = _modules.value.map { it.id }
                val completedIds = CourseProgressRules.mergeCompatibleCompletedIds(
                    courseCompletedIds = courseProgress?.get("completedModuleIds").asStringList(),
                    legacyCompletedIds = doc.get("completedModules").asStringList(),
                    actualModuleIds = actualModuleIds
                )
                val restoredRewardedIds = courseProgress
                    ?.get("rewardedQuizPageIds")
                    .asStringList()
                    .toSet()
                val storedCourseCompleted = courseProgress?.get("courseCompleted") as? Boolean
                val recomputedCourseCompleted = CourseProgressRules.isCourseCompleted(
                    actualModuleIds,
                    completedIds
                )

                _completedModuleIds.value = completedIds
                rewardedQuizPageIds = restoredRewardedIds
                _courseCompleted.value = recomputedCourseCompleted

                val needsProgressMigration =
                    courseProgress?.containsKey("completedModuleIds") != true ||
                        storedCourseCompleted != recomputedCourseCompleted

                if (needsProgressMigration) {
                    persistProgress(uid)
                }
                if (recomputedCourseCompleted) {
                    unlockCourseAchievementBestEffort(uid)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun Any?.asMap(): Map<*, *>? = this as? Map<*, *>

    private fun Any?.asInt(): Int = when (this) {
        is Number -> toInt()
        else -> 0
    }

    private fun Any?.asStringList(): List<String> =
        (this as? List<*>)?.filterIsInstance<String>() ?: emptyList()

    /* ---------------- Gamification helpers ---------------- */

    fun checkAnswer(
        page: Page.Test,
        userAnswerIndex: Int,
        rewardPageId: String
    ): QuizAttemptResult {
        val result = QuizProgressRules.answer(
            pageId = rewardPageId,
            selectedAnswerIndex = userAnswerIndex,
            correctAnswerIndex = page.correctAnswerIndex,
            rewardedPageIds = rewardedQuizPageIds
        )

        val uid = auth.currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                if (result.correct && CourseProgressRules.shouldAwardQuizReward(rewardPageId, rewardedQuizPageIds)) {
                    when (GameManager.awardQuizRewardIfNeeded(uid, courseType, rewardPageId)) {
                        QuizRewardResult.AWARDED,
                        QuizRewardResult.ALREADY_REWARDED -> {
                            rewardedQuizPageIds = CourseProgressRules.recordRewardedQuizPageId(
                                rewardPageId,
                                rewardedQuizPageIds
                            )
                        }
                        QuizRewardResult.FAILURE -> Unit
                    }
                }
                if (result.spendLife) {
                    val ok = GameManager.spendLife(uid)
                    if (!ok) _events.emit(UiEvent.NoLives)
                }
            }
        }
        return result
    }

    fun requestHint(page: Page.Test) = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        val ok  = GameManager.spendHint(uid)
        if (ok) _showHint.value = page.hint else _events.emit(UiEvent.NoHints)
    }

    fun clearHint() { _showHint.value = null }

    fun buyLife() = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        _events.emit(
            UiEvent.PurchaseFinished(
                item = PurchaseItem.LIFE,
                result = GameManager.buyLife(uid)
            )
        )
    }

    fun buyHint() = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        _events.emit(
            UiEvent.PurchaseFinished(
                item = PurchaseItem.HINT,
                result = GameManager.buyHint(uid)
            )
        )
    }

    /* ---------------- Mark module completed ---------------- */
    private fun markModuleCompleted(moduleId: String): CompletionUpdate {
        val progress = CourseProgressState(
            moduleIndex = _currentModuleIndex.value,
            pageIndex = _currentPageIndex.value,
            completedModuleIds = _completedModuleIds.value,
            rewardedQuizPageIds = rewardedQuizPageIds,
            courseCompleted = _courseCompleted.value
        )
        val update = CourseProgressRules.completeModuleAndEvaluate(
            progress = progress,
            moduleId = moduleId,
            actualModuleIds = _modules.value.map { it.id }
        )
        _completedModuleIds.value = update.progress.completedModuleIds
        return update
    }

    /* ---------------- Навігація ---------------- */
    fun next() {
        viewModelScope.launch {
            val module = currentModule.value ?: return@launch
            val lastModuleIndex = _modules.value.lastIndex
            val uid = auth.currentUser?.uid
            var shouldUnlockAchievement = false

            if (_currentPageIndex.value < module.pages.lastIndex) {
                _currentPageIndex.value += 1
            } else {
                val completionUpdate = markModuleCompleted(module.id)
                if (_currentModuleIndex.value < lastModuleIndex) {
                    _currentModuleIndex.value += 1
                    _currentPageIndex.value = 0
                } else {
                    _courseCompleted.value = completionUpdate.courseCompleted
                    shouldUnlockAchievement = completionUpdate.courseCompleted
                }
            }
            if (uid != null) {
                val saved = runCatching {
                    persistProgress(uid)
                }.onFailure {
                    _events.emit(UiEvent.SaveFailed)
                }.isSuccess

                if (!saved) return@launch

                if (shouldUnlockAchievement) {
                    unlockCourseAchievementBestEffort(uid)
                }
            }
        }
    }

    private suspend fun unlockCourseAchievementBestEffort(uid: String) {
        runCatching {
            val achId = if (courseType == "python") "PY_MASTER" else "JS_SAMURAI"
            AchievementManager.unlockAchievement(uid, achId)
        }.onFailure {
            _events.emit(UiEvent.AchievementUnlockFailed)
        }
    }

    override fun onCleared() {
        userListener?.remove()
        userListener = null
        super.onCleared()
    }
}
