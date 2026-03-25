package com.example.charityDept.data.model


data class AppUpdateConfig(
    val minVersionCode: String,
    val latestVersionCode: String?,
    val downloadUrl: String?,
    val forceMessage: String?,
    val softMessage: String?,
    val force: Boolean
)
