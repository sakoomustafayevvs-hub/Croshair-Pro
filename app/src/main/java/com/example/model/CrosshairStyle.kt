package com.example.model

enum class CrosshairStyle(val displayName: String, val description: String) {
    CLASSIC_CROSS("Klasik Xaç", "Ənənəvi dörd xəttli nişangah"),
    DOT("Nöqtə", "Sürətli hədəf üçün minimalist mərkəz nöqtəsi"),
    CIRCLE_CROSS("Dairəvi Xaç", "Mərkəzi xaç ilə əhatə olunmuş dairə"),
    SQUARE_CROSS("Kvadrat Xaç", "Kvadrat tipli taktiki nişangah"),
    T_SHAPE("T-Formalı", "Üst xətti olmayan klassik T-xaç"),
    SNIPER("Snayper Uzaqgörən", "Uzaq məsafə üçün dörddəbir xəttli halqa"),
    X_CROSS("X-Forma", "Diaqonal xaç nişangah"),
    TRIANGLE("Chevron / Üçbucaq", "Yuxarı yönəlmiş taktiki üçbucaq"),
    DIAMOND("Romb / Diamond", "Həndəsi romb formalı nişangah"),
    HALO_RING("Halo Halqası", "Mərkəzi nöqtəsi olan halo halqa"),
    DYNAMIC_TARGET("Taktiki Hədəf", "Hədəf alma dairəsi və nişan nöqtələri"),
    STAR_CROSS("Ulduz Nişangah", "Səkkiz güşəli dəqiq vuruş ulduzu")
}
