package com.samyak.urltvalpha.models

data class Channel(
    val id: String? = null,
    var name: String = "",
    var link: String = "",
    var logo: String = "",
    var category: String = ""
) {
    // Empty constructor for Firebase
    constructor() : this(null, "", "", "", "")
}