package com.example.charityDept.migration

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.charityDept.presentation.screens.migrationToolKit.BackFillPassScreen
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ----------------------------- Data / Helpers ---------------------------------

data class TargetProjectCreds(
    val applicationId: String, // e.g. "1:1234567890:android:abcdef123456"
    val apiKey: String,        // e.g. "AIzaSy...."
    val projectId: String      // e.g. "uplift-finance-target"
)

fun targetFirestore(context: Context, creds: TargetProjectCreds): FirebaseFirestore {
    val existing = FirebaseApp.getApps(context).firstOrNull { it.name == "target" }
    val app = existing ?: run {
        val opts = FirebaseOptions.Builder()
            .setApplicationId(creds.applicationId)
            .setApiKey(creds.apiKey)
            .setProjectId(creds.projectId)
            .build()
        FirebaseApp.initializeApp(context, opts, "target")
    }
    return FirebaseFirestore.getInstance(app)
}

// ----------------------------- ViewModel --------------------------------------

class MigrationJsonlVM : ViewModel() {
    var log by mutableStateOf("")
        private set
    var isBusy by mutableStateOf(false)
        private set

    fun clearLog() { log = "" }
    private fun post(msg: String) { log += if (log.isEmpty()) msg else "\n$msg" }

    fun exportCollectionsToDocuments(
        context: Context,
        db: FirebaseFirestore,
        collections: List<String>,
        pageSize: Int = 2000
    ) = launchOp {
        FireJsonlCore.exportCollections(
            context = context,
            db = db,
            collections = collections,
            makeOutputForCollection = { col ->
                val filename = "${col}-${timeStampForFile()}.jsonl"
                createDocumentsFileOutputStream(context, filename)
            },
            pageSize = pageSize,
            onProgress = ::post
        )
        post("✅ Export complete.")
    }

    fun importIntoDbFromJsonl(
        context: Context,
        db: FirebaseFirestore,
        inputUri: Uri,
        batchSize: Int = 450,
        merge: Boolean = true
    ) = launchOp {
        FireJsonlCore.importJsonl(
            context = context,
            db = db,
            inputUri = inputUri,
            batchSize = batchSize,
            merge = merge,
            onProgress = ::post
        )
        post("✅ Import complete.")
    }

    private fun launchOp(block: suspend () -> Unit) = viewModelScope.launch {
        isBusy = true
        clearLog()
        try {
            block()
        } catch (t: Throwable) {
            post("❌ Failed: ${t.message}")
        } finally {
            isBusy = false
        }
    }

    private fun timeStampForFile(): String {
        val sdf = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    private fun createDocumentsFileOutputStream(context: Context, filename: String): OutputStream {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/jsonl")
//            put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOCUMENTS)
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                android.os.Environment.DIRECTORY_DOCUMENTS + "/ZionExports"
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val collection = MediaStore.Files.getContentUri("external")
        val uri = resolver.insert(collection, values) ?: error("Failed to create file in Documents")
        val out = resolver.openOutputStream(uri) ?: error("Failed to open output stream: $filename")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return out
    }
}

// -------------------------- FireJsonl (core logic) ----------------------------

/**
 * KISS Firestore → JSONL exporter/importer (Spark-plan friendly, no subcollections).
 * One line per doc:
 *   {"path":"<collection>/<docId>","data":{...}}  // with type tags for TS/Geo/Ref
 *
 * Type tags:
 *   Timestamp         -> {"__ts__":"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"}
 *   GeoPoint          -> {"__geo__":{"lat":..., "lng":...}}
 *   DocumentReference -> {"__ref__":"collection/docId"}  (kept as String path on import)
 */
object FireJsonlCore {

    suspend fun exportCollections(
        context: Context,
        db: FirebaseFirestore,
        collections: List<String>,
        makeOutputForCollection: (String) -> OutputStream,
        pageSize: Int = 2000,
        onProgress: (String) -> Unit = {}
    ) {
        require(pageSize in 100..10000) { "pageSize must be 100..10000" }
        for (col in collections) {
            onProgress("📤 Exporting [$col] ...")
            val out = makeOutputForCollection(col)
            OutputStreamWriter(out, Charsets.UTF_8).use { w ->
                var lastId: String? = null
                var total = 0
                while (true) {
                    var q: Query = db.collection(col)
                        .orderBy(FieldPath.documentId())
                        .limit(pageSize.toLong())
                    if (lastId != null) q = q.startAfter(lastId)

                    val snap = q.get(Source.SERVER).await()
                    if (snap.isEmpty) break

                    for (doc in snap.documents) {
                        val jsonLine = JSONObject().apply {
                            put("path", doc.reference.path)
                            put("data", encodeValue(doc.data ?: emptyMap<String, Any?>()))
                        }
                        w.write(jsonLine.toString())
                        w.write("\n")
                        lastId = doc.id
                        total++
                    }
                    onProgress("[$col] … up to id=$lastId (+${snap.size()} docs, total=$total)")
                    if (snap.size() < pageSize) break
                }
            }
            onProgress("✅ Done [$col].")
        }
    }

    suspend fun importJsonl(
        context: Context,
        db: FirebaseFirestore,
        inputUri: Uri,
        batchSize: Int = 450,
        merge: Boolean = true,
        onProgress: (String) -> Unit = {}
    ) {
        require(batchSize in 1..500) { "batchSize 1..500" }
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(inputUri).use { stream ->
                requireNotNull(stream) { "Could not open input stream: $inputUri" }
                BufferedReader(InputStreamReader(stream)).useLines { seq ->
                    var count = 0
                    var batch = db.batch()
                    for (line in seq) {
                        if (line.isBlank()) continue
                        val json = JSONObject(line)
                        val path = json.getString("path")
                        val dataObj = json.get("data")
                        val map = decodeValue(dataObj) as Map<*, *>

                        val ref = db.document(path)
                        if (merge) batch.set(ref, toStringAnyMap(map), SetOptions.merge())
                        else batch.set(ref, toStringAnyMap(map))

                        count++
                        if (count % batchSize == 0) {
                            batch.commit().await()
                            onProgress("Imported $count docs…")
                            batch = db.batch()
                        }
                    }
                    batch.commit().await()
                    onProgress("Imported $count docs total.")
                }
            }
        }
    }

    // -------- Encoding/Decoding helpers --------

    private val iso: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    private fun encodeValue(v: Any?): Any? = when (v) {
        null -> null
        is String, is Number, is Boolean -> v
        is Map<*, *> -> JSONObject().also { o ->
            v.forEach { (k, vv) -> if (k is String) o.put(k, encodeValue(vv)) }
        }
        is List<*> -> JSONArray().also { a -> v.forEach { a.put(encodeValue(it)) } }
        is Timestamp -> JSONObject().apply { put("__ts__", iso.get().format(v.toDate())) }
        is GeoPoint -> JSONObject().apply {
            put("__geo__", JSONObject().apply {
                put("lat", v.latitude); put("lng", v.longitude)
            })
        }
        is DocumentReference -> JSONObject().apply { put("__ref__", v.path) }
        else -> v.toString()
    }

    private fun decodeValue(v: Any?): Any? = when (v) {
        null -> null
        is JSONObject -> when {
            v.has("__ts__") -> Timestamp(iso.get().parse(v.getString("__ts__"))!!)
            v.has("__geo__") -> v.getJSONObject("__geo__").let {
                GeoPoint(it.getDouble("lat"), it.getDouble("lng"))
            }
            v.has("__ref__") -> v.getString("__ref__") // keep as String path (KISS)
            else -> mutableMapOf<String, Any?>().also { m ->
                for (key in v.keys()) m[key] = decodeValue(v.get(key))
            }
        }
        is JSONArray -> ArrayList<Any?>().also { list ->
            for (i in 0 until v.length()) list.add(decodeValue(v.get(i)))
        }
        is String, is Number, is Boolean -> v
        else -> v.toString()
    }

    private fun toStringAnyMap(m: Map<*, *>): Map<String, Any?> {
        val out = LinkedHashMap<String, Any?>()
        for ((k, v) in m) {
            val key = k as? String ?: continue
            out[key] = upgradeRefs(v)
        }
        return out
    }

    private fun upgradeRefs(v: Any?): Any? = when (v) {
        null -> null
        is Map<*, *> -> toStringAnyMap(v)
        is List<*> -> v.map { upgradeRefs(it) }
        else -> v
    }
}

// ----------------------------- Compose UI -------------------------------------

@Composable
fun MigrationJsonlAllInOneScreen(
    navigateUp:() -> Unit
) {
    val vm: MigrationJsonlVM = viewModel()
    val context = LocalContext.current
    val currentDb = remember { FirebaseFirestore.getInstance() }

    var collectionsText by remember {
        mutableStateOf("appConfig, attendances, authAttempts, deletedUsers, children, users,events,streets,technicalSkills")
    }

    var targetAppId by remember { mutableStateOf("") }
    var targetApiKey by remember { mutableStateOf("") }
    var targetProjectId by remember { mutableStateOf("") }

    val openDocForCurrent = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            vm.importIntoDbFromJsonl(
                context = context,
                db = currentDb,
                inputUri = uri,
                batchSize = 450,
                merge = true
            )
        }
    }

    val openDocForTarget = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
//            val creds = TargetProjectCreds(
//                applicationId = targetAppId.trim(),
//                apiKey = targetApiKey.trim(),
//                projectId = targetProjectId.trim()
//            )
            val creds = TargetProjectCreds(
                applicationId = "1:184074794191:android:be158f9c9fbafd13f8b3ba",
                apiKey = "AIzaSyD8sWZuydYoUWybRo8jxEAB-uZ8R3Ubanc",
                projectId = "children-of-zion-2ab1c"
            )
            val targetDb = try { targetFirestore(context, creds) }
            catch (_: Throwable) { null }

            if (targetDb != null) {
                vm.importIntoDbFromJsonl(
                    context = context,
                    db = targetDb,
                    inputUri = uri,
                    batchSize = 450,
                    merge = true
                )
            } else {
                // surface error to log
                vm.clearLog()
                vm.exportCollectionsToDocuments( // harmless no-op export triggers log header if you want; remove if noisy
                    context = context,
                    db = currentDb,
                    collections = emptyList()
                )
            }
        }
    }

    val isBusy = vm.isBusy
    val log = vm.log

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Text("Firestore Migration (JSONL • Spark • KISS)", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = collectionsText,
            onValueChange = { collectionsText = it },
            label = { Text("Collections (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        Button(
            enabled = !isBusy,
            onClick = {
                val cols = collectionsText.split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                vm.exportCollectionsToDocuments(
                    context = context,
                    db = currentDb,
                    collections = cols,
                    pageSize = 2000
                )
            }
        ) {
            Text(if (isBusy) "Working…" else "Export → Documents (.jsonl per collection)")
        }

        Spacer(Modifier.height(16.dp))

        Text("Import into CURRENT project", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Button(
            enabled = false,
//            enabled = !isBusy,
            onClick = { openDocForCurrent.launch(arrayOf("*/*")) }
        ) {
            Text("Pick .jsonl → Import (current)")
        }

        Spacer(Modifier.height(16.dp))

//        Text("Import into TARGET project", style = MaterialTheme.typography.titleMedium)
//        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = targetAppId, onValueChange = { targetAppId = it },
            label = { Text("Target Application ID") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(6.dp))
//        OutlinedTextField(
//            value = targetApiKey, onValueChange = { targetApiKey = it },
//            label = { Text("Target API Key") },
//            modifier = Modifier.fillMaxWidth(), singleLine = true
//        )
//        Spacer(Modifier.height(6.dp))
//        OutlinedTextField(
//            value = targetProjectId, onValueChange = { targetProjectId = it },
//            label = { Text("Target Project ID") },
//            modifier = Modifier.fillMaxWidth(), singleLine = true
//        )
//        Spacer(Modifier.height(8.dp))

//        && targetAppId.isNotBlank() && targetApiKey.isNotBlank() && targetProjectId.isNotBlank()
        Button(
            enabled = false,
//            enabled = !isBusy ,
            onClick = { openDocForTarget.launch(arrayOf("*/*")) }
        ) {
            Text("Pick .jsonl → Import (target)")
        }

        Spacer(Modifier.height(16.dp))

        Text("Log", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Surface(
            tonalElevation = 2.dp,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (log.isBlank()) "No activity yet." else log,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Visible
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        BackFillPassScreen()
    }
}

