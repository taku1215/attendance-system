package com.kojinguide.app.data

import android.content.Context
import android.content.SharedPreferences

/** チェックリストのチェック状態を SharedPreferences に保存する */
class ChecklistRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("checklist", Context.MODE_PRIVATE)

    fun isChecked(itemId: String): Boolean = prefs.getBoolean(itemId, false)

    fun setChecked(itemId: String, checked: Boolean) {
        prefs.edit().putBoolean(itemId, checked).apply()
    }

    fun loadAll(): Map<String, Boolean> =
        GuideRepository.checklistItems.associate { it.id to isChecked(it.id) }
}
