// File: app/src/main/java/com/bambiloff/kvantor/CourseCompletionChecker.kt
package com.bambiloff.kvantor

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object CourseCompletionChecker {

    /**
     * Перевіряє завершення курсу за реальними id модулів і підтримує старе поле completedModules.
     */
    suspend fun checkCourseCompleted(uid: String, courseType: String): Boolean {
        val db = FirebaseFirestore.getInstance()

        // обираємо вашу колекцію з модулями
        val modulesCol = if (courseType == "javascript") "modules_js" else "modules"

        val moduleIds = db.collection(modulesCol)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val dtoId = doc.toObject(ModuleDto::class.java)?.id.orEmpty()
                dtoId.ifBlank { doc.id }.takeIf { it.isNotBlank() }
            }

        val userDoc = db.collection("users").document(uid).get().await()
        val progress = userDoc.get("progress") as? Map<*, *>
        val courseProgress = progress?.get(courseType) as? Map<*, *>
        val courseCompletedIds =
            (courseProgress?.get("completedModuleIds") as? List<*>)?.filterIsInstance<String>()
                ?: emptyList()
        val legacyCompletedIds =
            (userDoc.get("completedModules") as? List<*>)?.filterIsInstance<String>()
                ?: emptyList()
        val completedIds = CourseProgressRules.mergeCompatibleCompletedIds(
            courseCompletedIds = courseCompletedIds,
            legacyCompletedIds = legacyCompletedIds,
            actualModuleIds = moduleIds
        )

        val completed = CourseProgressRules.isCourseCompleted(moduleIds, completedIds)
        if (completed) {
            val achId = if (courseType == "python") "PY_MASTER" else "JS_SAMURAI"
            AchievementManager.unlockAchievement(uid, achId)
            println("🏆 [$courseType] achievement unlocked via CourseCompletionChecker")
        }
        return completed
    }
}
