package com.example.charityDept.core.Utils

import androidx.room.TypeConverter
import com.example.charityDept.data.model.*
import com.google.firebase.Timestamp

class Converters {

    @TypeConverter
    fun fromTimestamp(ts: Timestamp?): Long? = ts?.seconds

    @TypeConverter
    fun toTimestamp(seconds: Long?): Timestamp? = seconds?.let { Timestamp(it, 0) }

    @TypeConverter fun fromIndividual(v: Individual?): String? = v?.name
    @TypeConverter fun toIndividual(s: String?): Individual =
        s?.let { runCatching { Individual.valueOf(it) }.getOrNull() } ?: Individual.UNCLE

    @TypeConverter fun fromEducationPreference(v: EducationPreference?): String? = v?.name
    @TypeConverter fun toEducationPreference(s: String?): EducationPreference =
        s?.let { runCatching { EducationPreference.valueOf(it) }.getOrNull() } ?: EducationPreference.NONE

    @TypeConverter fun fromReply(v: Reply?): String? = v?.name
    @TypeConverter fun toReply(s: String?): Reply =
        s?.let { runCatching { Reply.valueOf(it) }.getOrNull() } ?: Reply.NO

    // ✅ renamed enum
    @TypeConverter fun fromRelationship(v: Relationship?): String? = v?.name
    @TypeConverter fun toRelationship(s: String?): Relationship =
        s?.let { runCatching { Relationship.valueOf(it) }.getOrNull() } ?: Relationship.NONE

    @TypeConverter fun fromRegistrationStatus(v: RegistrationStatus?): String? = v?.name
    @TypeConverter fun toRegistrationStatus(s: String?): RegistrationStatus =
        s?.let { runCatching { RegistrationStatus.valueOf(it) }.getOrNull() } ?: RegistrationStatus.BASICINFOR
}

