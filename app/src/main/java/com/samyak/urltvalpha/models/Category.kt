package com.samyak.urltvalpha.models

data class Category(
    val id: String? = null,
    var name: String = "",
    var type: String = "",
    var icon: String = ""
) {
    // Empty constructor for Firebase
    constructor() : this(null, "", "", "")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Category
        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
} 