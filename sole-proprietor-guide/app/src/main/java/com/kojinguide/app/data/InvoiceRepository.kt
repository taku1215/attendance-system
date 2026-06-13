package com.kojinguide.app.data

import android.content.Context

/** 請求書の発行者情報(自分の情報)。SharedPreferencesに保存して再利用する */
data class InvoiceProfile(
    val name: String = "",
    val address: String = "",
    val bank: String = "",
    val registrationNumber: String = ""
)

class InvoiceRepository(context: Context) {

    private val prefs = context.getSharedPreferences("invoice", Context.MODE_PRIVATE)

    fun loadProfile(): InvoiceProfile = InvoiceProfile(
        name = prefs.getString("name", "") ?: "",
        address = prefs.getString("address", "") ?: "",
        bank = prefs.getString("bank", "") ?: "",
        registrationNumber = prefs.getString("registrationNumber", "") ?: ""
    )

    fun saveProfile(profile: InvoiceProfile) {
        prefs.edit()
            .putString("name", profile.name)
            .putString("address", profile.address)
            .putString("bank", profile.bank)
            .putString("registrationNumber", profile.registrationNumber)
            .apply()
    }
}
