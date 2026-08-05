package com.panjirai0110.shared.validation

object InputValidator {
    private val emailPattern =
        Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun credentialError(email: String, password: String): String? = when {
        email.isBlank() -> "Email wajib diisi."
        !emailPattern.matches(email.trim()) -> "Format email tidak valid."
        password.length < 6 -> "Kata sandi minimal 6 karakter."
        else -> null
    }

    fun emailError(email: String): String? = when {
        email.isBlank() -> "Email wajib diisi."
        !emailPattern.matches(email.trim()) -> "Format email tidak valid."
        else -> null
    }

    fun scheduleError(
        from: String,
        to: String,
        time: String,
        price: String
    ): String? = when {
        from.isBlank() -> "Kota asal wajib diisi."
        to.isBlank() -> "Kota tujuan wajib diisi."
        from.trim().length > 60 || to.trim().length > 60 ->
            "Nama kota maksimal 60 karakter."
        from.trim().equals(to.trim(), ignoreCase = true) ->
            "Kota asal dan tujuan harus berbeda."
        time.isBlank() -> "Jam keberangkatan wajib diisi."
        !time.trim().matches(Regex("^(?:[01][0-9]|2[0-3]):[0-5][0-9]$")) ->
            "Jam harus menggunakan format 24 jam HH:mm."
        price.toIntOrNull() == null || price.toInt() <= 0 ->
            "Harga harus berupa angka lebih dari nol."
        else -> null
    }
}
