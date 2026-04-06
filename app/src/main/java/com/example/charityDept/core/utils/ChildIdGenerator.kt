package com.example.charityDept.core.utils

import java.util.UUID


object ChildIdGenerator {
    fun generateKidId(gender: String): String {
        val prefix = if (gender.equals("female", ignoreCase = true)) "ZF" else "ZM"
        val timePart = System.currentTimeMillis().toString().takeLast(6)
        val randomPart = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        return "$prefix$timePart$randomPart"
    }
}

/**
 * Usage in ViewModel (UI calls Domain):

 * val kidId = KidIdGenerator.generateKidId(gender)
 */
