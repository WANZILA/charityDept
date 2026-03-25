package com.example.charityDept.domain.repositories.online

import android.util.Log
import com.example.charityDept.core.di.Streets // Qualifier for the CollectionReference
import com.example.charityDept.core.Utils.picker.PickerOption
import com.example.charityDept.data.mappers.toFirestoreMap
import com.example.charityDept.data.mappers.toStreet
import com.example.charityDept.data.mappers.toStreets
import com.example.charityDept.data.model.Street
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

interface StreetsRepository {
    suspend fun getAll(activeOnly: Boolean = true): List<Street>
    fun watchAll(activeOnly: Boolean = true): Flow<List<Street>>
    fun streetsPickerWatchAll(activeOnly: Boolean = true): Flow<List<PickerOption>>

    suspend fun getById(streetId: String): Street?
    suspend fun upsert(street: Street, isNew: Boolean)
    suspend fun patch(streetId: String, patch: Map<String, Any?>)
    suspend fun delete(streetId: String)
    suspend fun searchByNamePrefix(prefix: String, activeOnly: Boolean = true, limit: Long = 20): List<Street>
}

private const val TAG = "Cliff here: StreetsRepo"

@Singleton
class StreetsRepositoryImpl @Inject constructor(
    @Streets private val ref: CollectionReference
) : StreetsRepository {

    private fun baseQuery(activeOnly: Boolean): Query {
        var q: Query = ref
        if (activeOnly) q = q.whereEqualTo("isActive", true)
        return q.orderBy("name")
    }

    override suspend fun getAll(activeOnly: Boolean): List<Street> {
        return baseQuery(activeOnly).get().await().toStreets()
    }

    override fun watchAll(activeOnly: Boolean): Flow<List<Street>> = callbackFlow {
        val registration: ListenerRegistration = baseQuery(activeOnly)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "listen failed", err)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snap?.toStreets().orEmpty()
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    override fun streetsPickerWatchAll(activeOnly: Boolean): Flow<List<PickerOption>> = callbackFlow {
        val registration = baseQuery(activeOnly).addSnapshotListener { snap, err ->
            if (err != null) {
                Log.w(TAG, "streetsPickerWatchAll failed -> ${err.message}", err)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val options = snap?.documents?.mapNotNull { doc ->
                val id = doc.getString("streetId") ?: doc.id
                val name = doc.getString("name") ?: return@mapNotNull null
                PickerOption(id = id, name = name)
            }.orEmpty()
            trySend(options)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun getById(streetId: String): Street? {
        val doc = ref.document(streetId).get().await()
        return doc.toStreet()
    }

    override suspend fun upsert(street: Street, isNew: Boolean) {
        val now = Timestamp.now()
        val data = if (isNew)
            street.copy(createdAt = now, updatedAt = now).toFirestoreMap()
        else
            street.copy(updatedAt = now).toFirestoreMap()

        // assumes street.streetId is set; ensure before calling
        ref.document(street.streetId).set(data, SetOptions.merge()).await()
    }

    override suspend fun patch(streetId: String, patch: Map<String, Any?>) {
        val withUpdate = HashMap(patch)
        withUpdate["updatedAt"] = Timestamp.now()
        ref.document(streetId).set(withUpdate, SetOptions.merge()).await()
    }

    override suspend fun delete(streetId: String) {
        ref.document(streetId).delete().await()
    }

    override suspend fun searchByNamePrefix(
        prefix: String,
        activeOnly: Boolean,
        limit: Long
    ): List<Street> {
        if (prefix.isBlank()) return emptyList()
        val end = prefix + "\uf8ff"
        var q: Query = ref
            .orderBy("name")
            .startAt(prefix)
            .endAt(end)
            .limit(limit)

        if (activeOnly) q = q.whereEqualTo("isActive", true)

        return q.get().await().toStreets()
    }
}

