package com.mkmemories.copilot.feature.carnet

import android.content.Context
import androidx.core.content.edit

/**
 * Progression du carnet : quelles entrées (trajets, ferries, nuits…) sont
 * franchies. 100 % local, survit au redémarrage. La barre de progression et la
 * mise en avant de « la prochaine étape » s'appuient dessus.
 */
class CarnetProgress(context: Context) {

    private val prefs = context.getSharedPreferences("carnet_progress", Context.MODE_PRIVATE)

    fun isDone(id: String): Boolean = prefs.getBoolean(id, false)

    fun setDone(id: String, done: Boolean) {
        prefs.edit { putBoolean(id, done) }
    }

    fun toggle(id: String): Boolean {
        val next = !isDone(id)
        setDone(id, next)
        return next
    }

    /** Nombre d'entrées franchies parmi une liste d'identifiants. */
    fun doneCount(ids: List<String>): Int = ids.count { isDone(it) }

    fun clear() {
        prefs.edit { clear() }
    }
}
