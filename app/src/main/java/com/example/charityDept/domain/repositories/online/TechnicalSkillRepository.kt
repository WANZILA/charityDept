package com.example.charityDept.domain.repositories.online

import android.util.Log
import com.example.charityDept.core.di.TechnicalSkills
import com.example.charityDept.core.utils.picker.PickerOption
import com.example.charityDept.data.mappers.toFirestoreMap
import com.example.charityDept.data.mappers.toTechnicalSkill
import com.example.charityDept.data.mappers.toTechnicalSkills
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

import com.example.charityDept.data.model.TechnicalSkill

interface TechnicalSkillsRepository {

    /** One-shot fetch (optionally active only) */
    suspend fun getAll(activeOnly: Boolean = true): List<TechnicalSkill>

    /** Live updates (optionally active only) */
    fun watchAll(activeOnly: Boolean = true): Flow<List<TechnicalSkill>>
    fun techSkillsPickerWatchAll(activeOnly: Boolean = true): Flow<List<PickerOption>>


    suspend fun getById(skillId: String): TechnicalSkill?

    /** Create or update (isNew controls createdAt handling) */
    suspend fun upsert(skill: TechnicalSkill, isNew: Boolean)

    /** Partial update for small edits */
    suspend fun patch(skillId: String, patch: Map<String, Any?>)

    suspend fun delete(skillId: String)

    /** Case-insensitive name prefix search */
    suspend fun searchByNamePrefix(prefix: String, activeOnly: Boolean = true, limit: Long = 20): List<TechnicalSkill>
}


private const val TAG = "Cliff here: SkillsRepo"

@Singleton
class TechnicalSkillsRepositoryImpl @Inject constructor(
    @TechnicalSkills private val ref: CollectionReference
) : TechnicalSkillsRepository {

    private fun baseQuery(activeOnly: Boolean): Query {
        var q: Query = ref
        if (activeOnly) q = q.whereEqualTo("isActive", true)
        return q.orderBy("name")
    }

    override suspend fun getAll(activeOnly: Boolean): List<TechnicalSkill> {
        return baseQuery(activeOnly).get().await().toTechnicalSkills()
    }

    override fun watchAll(activeOnly: Boolean): Flow<List<TechnicalSkill>> = callbackFlow {
        val registration: ListenerRegistration = baseQuery(activeOnly)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "listen failed", err)
                    trySend(emptyList()).isSuccess
                    return@addSnapshotListener
                }
                val items = snap?.toTechnicalSkills().orEmpty()
                trySend(items).isSuccess
            }

        awaitClose { registration.remove() }
    }

    override fun techSkillsPickerWatchAll(activeOnly: Boolean): Flow<List<PickerOption>> = callbackFlow {
        val registration = baseQuery(activeOnly).addSnapshotListener { snap, err ->
            if (err != null) {
                Log.w(TAG, "techSkillsPickerWatchAll: listen failed -> ${err.message}", err)
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snap == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            // Map docs -> PickerOption
            val options = snap.documents.mapNotNull { doc ->
                val id = doc.getString("skillId") ?: doc.id
                val name = doc.getString("name") ?: return@mapNotNull null
                PickerOption(id = id, name = name)
            }

            trySend(options)
        }

        awaitClose { registration.remove() }
    }



    override suspend fun getById(skillId: String): TechnicalSkill? {
        val doc = ref.document(skillId).get().await()
        return doc.toTechnicalSkill()
    }

    override suspend fun upsert(skill: TechnicalSkill, isNew: Boolean) {
        val now = Timestamp.now()
        val data = if (isNew)
            skill.copy(createdAt = now, updatedAt = now).toFirestoreMap()
        else
            skill.copy(updatedAt = now).toFirestoreMap()

        ref.document(skill.skillId).set(data, SetOptions.merge()).await()
    }

    override suspend fun patch(skillId: String, patch: Map<String, Any?>) {
        val withUpdate = HashMap(patch)
        withUpdate["updatedAt"] = Timestamp.now()
        ref.document(skillId).set(withUpdate, SetOptions.merge()).await()
    }

    override suspend fun delete(skillId: String) {
        ref.document(skillId).delete().await()
    }

    override suspend fun searchByNamePrefix(
        prefix: String,
        activeOnly: Boolean,
        limit: Long
    ): List<TechnicalSkill> {
        if (prefix.isBlank()) return emptyList()
        val end = prefix + "\uf8ff"
        var q: Query = ref
            .orderBy("name")
            .startAt(prefix)
            .endAt(end)
            .limit(limit)

        if (activeOnly) q = q.whereEqualTo("isActive", true)

        // When combining range + equality, Firestore may require a composite index (console link will show)
        return q.get().await().toTechnicalSkills()
    }
}

