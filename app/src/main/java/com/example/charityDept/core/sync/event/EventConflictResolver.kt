//// <app/src/main/java/com/example/zionkids/core/sync/event/EventConflict.kt>
//// /// CHANGED: Implement nanos-accurate conflict resolver for Event using Firestore Timestamps.
//// /// CHANGED: Rule = prefer higher version; else newer updatedAt; tie → keep local if dirty (else remote).
//// /// CHANGED: Normalize fields on merge: createdAt=min(local,remote), updatedAt=max(local,remote), eventId prefer local if set.
//// /// CHANGED: Keep API shape: top-level resolveEvent(local, remote) + OfflineEventsRepository extension.
//
//package com.example.charityDept.core.sync.event
//
//import com.example.charityDept.data.model.Event
//import com.example.charityDept.domain.repositories.offline.OfflineEventsRepository
//import com.google.firebase.Timestamp
//
//// Top-level helper so you can call resolveEvent(local, remote) directly
//fun resolveEvent(local: Event?, remote: Event?): Event {
//    // If there's no remote, keep local as-is (stay dirty so it will push).
//    // /// CHANGED: Defensive require to avoid both null.
//    if (remote == null) return requireNotNull(local) { "resolveEvent: both local and remote are null" }
//
//    // If no local, accept remote (coming from server) and mark clean.
//    if (local == null) return remote.copy(isDirty = false)
//
//    // /// CHANGED: Compare by version first; then by nanos-accurate updatedAt (no Date allocations).
//    val cmpVersion = local.version.compareTo(remote.version)
//    val tsCompare = compareTs(local.updatedAt, remote.updatedAt)
//    val winnerIsRemote = when {
//        cmpVersion < 0 -> true
//        cmpVersion > 0 -> false
//        else -> tsCompare < 0
//    }
//
//    // /// CHANGED: Normalize stable fields regardless of winner.
//    val createdMin = minTs(local.createdAt, remote.createdAt)
//    val updatedMax = maxTs(local.updatedAt, remote.updatedAt)
//    val resolvedId = if (local.eventId.isNotBlank()) local.eventId else remote.eventId
//
//    return when {
//        // Remote strictly wins by rule → adopt remote, clean it
//        winnerIsRemote && (cmpVersion != 0 || tsCompare != 0) ->
//            remote.copy(eventId = resolvedId, createdAt = createdMin, updatedAt = updatedMax, isDirty = false)
//
//        // Local strictly wins → keep local, preserve dirty flag
//        !winnerIsRemote && (cmpVersion != 0 || tsCompare != 0) ->
//            local.copy(eventId = resolvedId, createdAt = createdMin, updatedAt = updatedMax)
//
//        // Tie (same version & updatedAt): keep local if dirty, else remote (clean)
//        else ->
//            if (local.isDirty)
//                local.copy(eventId = resolvedId, createdAt = createdMin, updatedAt = updatedMax)
//            else
//                remote.copy(eventId = resolvedId, createdAt = createdMin, updatedAt = updatedMax, isDirty = false)
//    }
//}
//
//// /// CHANGED: Timestamp helpers (seconds + nanoseconds) to avoid allocations/GCs in hot loops.
//private fun compareTs(a: Timestamp, b: Timestamp): Int {
//    val sec = a.seconds.compareTo(b.seconds)
//    return if (sec != 0) sec else a.nanoseconds.compareTo(b.nanoseconds)
//}
//private fun maxTs(a: Timestamp, b: Timestamp): Timestamp =
//    if (compareTs(a, b) >= 0) a else b
//private fun minTs(a: Timestamp, b: Timestamp): Timestamp =
//    if (compareTs(a, b) <= 0) a else b
//
//// Extension so existing calls like `offlineRepo.resolveEvent(local, remote)` still compile.
//fun OfflineEventsRepository.resolveEvent(local: Event?, remote: Event?): Event =
//    resolveEvent(local, remote)
//
////package com.example.charityDept.core.sync.event
////
////import com.example.charityDept.data.model.Event
////import com.example.charityDept.domain.repositories.offline.OfflineEventsRepository
////
//////class EventConflictResolver {
//////}
//////package com.example.charityDept.core.sync
////
////// <app/src/main/java/com/example/zionkids/core/sync/ConflictResolver.kt>
////// /// CHANGED: new tiny, shared conflict resolver used by worker/listener
//////package com.example.charityDept.core.sync
////
//////import com.example.charityDept.data.model.Event
//////import com.example.charityDept.domain.repositories.offline.OfflineEventrenRepository
////
////
////// Top-level helper so you can call resolveEvent(local, remote) directly
////fun resolveEvent(local: Event?, remote: Event?): Event {
////    // If there's no remote, keep local as-is (stay dirty so it will push).
////    if (remote == null) return requireNotNull(local) { "resolveEvent: both local and remote are null" }
////
////    // If no local, accept remote (coming from server) and mark clean.
////    if (local == null) return remote.copy(isDirty = false)
////
////    val lv = local.version
////    val rv = remote.version
////    return when {
////        // 1) Higher version wins
////        rv > lv -> remote.copy(isDirty = false)
////        rv < lv -> local
////
////        // 2) Same version: compare updatedAt
////        remote.updatedAt.toDate().time > local.updatedAt.toDate().time -> remote.copy(isDirty = false)
////        remote.updatedAt.toDate().time < local.updatedAt.toDate().time -> local
////
////        // 3) Still tied: keep local if it’s dirty (let it push), else remote (clean)
////        else -> if (local.isDirty) local else remote.copy(isDirty = false)
////    }
////}
////
////// Extension so existing calls like `offlineRepo.resolveEvent(local, remote)` still compile.
////fun OfflineEventsRepository.resolveEvent(local: Event?, remote: Event?): Event =
////    resolveEvent(local, remote)
