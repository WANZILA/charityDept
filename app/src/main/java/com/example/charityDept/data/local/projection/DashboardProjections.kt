package com.example.charityDept.data.local.projection

data class NameCount(
    val name: String,
    val cnt: Int
)

data class EduCount(
    val pref: String, // or EducationPreference if you use a Room @TypeConverter
    val cnt: Int
)

