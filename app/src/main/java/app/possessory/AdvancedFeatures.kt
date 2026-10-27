package app.possessory

import android.app.Activity
import android.app.KeyguardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource as s
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.possessory.data.*
import app.possessory.domain.*
import app.possessory.services.Files
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SearchDialog(initial: SearchCriteria, onDismiss: () -> Unit, onSave: (SearchCriteria) -> Unit) {
    var condition by rememberSaveable { mutableStateOf(initial.condition) }
    var currency by rememberSaveable { mutableStateOf(initial.currency) }
    var min by rememberSaveable {
        mutableStateOf(
            if (initial.minimum.isBlank()) ""
            else InventoryRules.editableAmount(initial.amount(initial.minimum), initial.currency)
        )
    }
    var max by rememberSaveable {
        mutableStateOf(
            if (initial.maximum.isBlank()) ""
            else InventoryRules.editableAmount(initial.amount(initial.maximum), initial.currency)
        )
    }
    var after by rememberSaveable { mutableStateOf(initial.purchasedAfter) }
    var before by rememberSaveable { mutableStateOf(initial.purchasedBefore) }
    var invalid by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s(R.string.advanced_search)) },
        text = {
            Column(
                Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Choice(
                    s(R.string.condition),
                    condition,
                    listOf("" to s(R.string.all_conditions)) +
                        listOf("New", "Good", "Fair", "Needs repair").map {
                            it to localizedLabel(it)
                        },
                    { condition = it },
                )
                Field(s(R.string.currency), currency) { currency = it.uppercase().trim() }
                Field(s(R.string.minimum_price), min) { min = it }
                Field(s(R.string.maximum_price), max) { max = it }
                Text(s(R.string.range_currency_notice))
                Field(s(R.string.purchased_after) + " · YYYY-MM-DD", after) { after = it }
                Field(s(R.string.purchased_before) + " · YYYY-MM-DD", before) { before = it }
                if (invalid) Text(s(R.string.form_error), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = SearchCriteria(condition, currency, min, max, after, before)
                    runCatching { value.validate() }
                        .onSuccess { onSave(value) }
                        .onFailure { invalid = true }
                }
            ) {
                Text(s(R.string.apply_filters))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s(R.string.cancel)) } },
    )
}

@Composable
fun SavedSearchMenu(vm: InventoryViewModel?, current: SavedSearch, onLoad: (SavedSearch) -> Unit) {
    if (vm == null) return
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val key = "saved_searches"
    val saved =
        remember(prefs) {
            runCatching {
                    Files.gson
                        .fromJson(
                            prefs[stringPreferencesKey(key)] ?: "[]",
                            Array<SavedSearch>::class.java,
                        )
                        .toList()
                }
                .getOrDefault(emptyList())
        }
    var naming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { vm.premium { naming = true } }) { Text(s(R.string.save_search)) }
        if (saved.isNotEmpty()) {
            Choice(
                s(R.string.saved_searches),
                "",
                listOf("" to s(R.string.saved_searches)) + saved.map { it.name to it.name },
                { name -> saved.find { it.name == name }?.let(onLoad) },
            )
            TextButton(onClick = { deleting = true }) { Text(s(R.string.manage_searches)) }
        }
    }
    if (naming)
        TextEntryDialog(s(R.string.save_search), s(R.string.name), { naming = false }) { name ->
            vm.setting(
                key,
                Files.gson.toJson(
                    (saved.filterNot { it.name == name } + current.copy(name = name)).takeLast(20)
                ),
            )
            naming = false
        }
    if (deleting)
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text(s(R.string.saved_searches)) },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    saved.forEach { search ->
                        TextButton(
                            onClick = {
                                vm.setting(
                                    key,
                                    Files.gson.toJson(saved.filterNot { it.name == search.name }),
                                )
                            }
                        ) {
                            Text(s(R.string.delete) + " · " + search.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { deleting = false }) { Text(s(R.string.close)) }
            },
        )
}

@Composable
fun HouseholdDialog(original: Household?, vm: InventoryViewModel, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(original?.name ?: "") }
    var currency by rememberSaveable { mutableStateOf(original?.currency ?: "GBP") }
    var invalid by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(s(if (original == null) R.string.add_household else R.string.edit_household))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(s(R.string.household_name), name) { name = it }
                Field(s(R.string.currency), currency) { currency = it.uppercase().trim() }
                Text(s(R.string.household_currency_notice))
                if (invalid) Text(s(R.string.form_error))
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    if (runCatching { java.util.Currency.getInstance(currency) }.isFailure)
                        invalid = true
                    else {
                        if (original == null) vm.household(name, currency)
                        else
                            vm.run {
                                vm.dao.put(original.copy(name = name.trim(), currency = currency))
                            }
                        onDismiss()
                    }
                },
            ) {
                Text(s(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s(R.string.cancel)) } },
    )
}

@Composable
fun AppLockSetting(vm: InventoryViewModel) {
    val ctx = LocalContext.current
    val keyguard = ctx.getSystemService(KeyguardManager::class.java)
    val store = ctx.getSharedPreferences("app-lock", android.content.Context.MODE_PRIVATE)
    var enabled by remember { mutableStateOf(store.getBoolean("enabled", false)) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == Activity.RESULT_OK) {
                store.edit().putBoolean("enabled", true).apply()
                enabled = true
                (ctx as? Activity)?.recreate()
            }
        }
    Toggle(s(R.string.app_lock), enabled) { requested ->
        if (!requested) {
            store.edit().putBoolean("enabled", false).apply()
            enabled = false
            (ctx as? Activity)?.recreate()
        } else if (keyguard.isDeviceSecure)
            launcher.launch(
                keyguard.createConfirmDeviceCredentialIntent(
                    ctx.getString(R.string.app_name),
                    ctx.getString(R.string.unlock_notice),
                )
            )
        else vm.error.value = "device_lock_required"
    }
    Text(s(R.string.app_lock_notice))
}

@Composable
fun RoomPhotoAction(room: RoomRecord, vm: InventoryViewModel) {
    val context = LocalContext.current
    var renaming by remember { mutableStateOf(false) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null)
                vm.run {
                    val file = File(context.filesDir, "attachments/${UUID.randomUUID()}")
                    try {
                        withContext(Dispatchers.IO) {
                            val bytes =
                                context.contentResolver.openInputStream(uri)!!.use {
                                    Files.readBounded(it, 25L * 1024 * 1024)
                                }
                            val bounds =
                                android.graphics.BitmapFactory.Options().apply {
                                    inJustDecodeBounds = true
                                }
                            android.graphics.BitmapFactory.decodeByteArray(
                                bytes,
                                0,
                                bytes.size,
                                bounds,
                            )
                            require(bounds.outWidth > 0 && bounds.outHeight > 0)
                            file.parentFile!!.mkdirs()
                            file.writeBytes(bytes)
                        }
                        vm.dao.put(room.copy(photoFile = file.name))
                        if (room.photoFile.isNotBlank())
                            File(context.filesDir, "attachments/${room.photoFile}").delete()
                    } catch (error: Exception) {
                        file.delete()
                        throw error
                    }
                }
        }
    Row {
        TextButton(onClick = { picker.launch(arrayOf("image/*")) }) { Text(s(R.string.room_photo)) }
        TextButton(onClick = { renaming = true }) { Text(s(R.string.rename)) }
        if (room.photoFile.isNotBlank())
            TextButton(
                onClick = {
                    vm.run {
                        vm.dao.put(room.copy(photoFile = ""))
                        File(context.filesDir, "attachments/${room.photoFile}").delete()
                    }
                }
            ) {
                Text(s(R.string.remove))
            }
    }
    if (renaming)
        TextEntryDialog(s(R.string.rename), s(R.string.room_name), { renaming = false }) { name ->
            vm.run { vm.dao.put(room.copy(name = name)) }
            renaming = false
        }
}
