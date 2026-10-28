package app.possessory

import android.os.Bundle
import android.speech.*
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource as s
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.possessory.data.*
import app.possessory.domain.*
import app.possessory.services.*
import coil.compose.AsyncImage
import java.io.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

val Navy = Color(0xFF24344A)
val Ivory = Color(0xFFF8F6F1)
val Sage = Color(0xFF6D8B78)
val Gold = Color(0xFFC5A46D)
val Ink = Color(0xFF292D33)

@Composable
fun localizedLabel(value: String): String =
    when (value) {
        "Living Room" -> s(R.string.living_room)
        "Kitchen" -> s(R.string.kitchen)
        "Bedroom" -> s(R.string.bedroom)
        "Bathroom" -> s(R.string.bathroom)
        "Home Office" -> s(R.string.home_office)
        "Garage" -> s(R.string.garage)
        "Garden" -> s(R.string.garden)
        "Storage" -> s(R.string.storage)
        "Other" -> s(R.string.other)
        "Electronics" -> s(R.string.electronics)
        "Appliances" -> s(R.string.appliances)
        "Furniture" -> s(R.string.furniture)
        "Jewellery" -> s(R.string.jewellery)
        "Clothing" -> s(R.string.clothing)
        "Tools" -> s(R.string.tools)
        "Art" -> s(R.string.art)
        "New" -> s(R.string.condition_new)
        "Good" -> s(R.string.condition_good)
        "Fair" -> s(R.string.condition_fair)
        "Needs repair" -> s(R.string.condition_repair)
        "Active" -> s(R.string.active)
        "Archived" -> s(R.string.archived)
        "Sold" -> s(R.string.status_sold)
        "Donated" -> s(R.string.status_donated)
        "Lost" -> s(R.string.status_lost)
        "Disposed" -> s(R.string.status_disposed)
        "Manufacturer" -> s(R.string.warranty_manufacturer)
        "Extended warranty" -> s(R.string.warranty_extended)
        "Retailer" -> s(R.string.warranty_retailer)
        "Other warranty" -> s(R.string.warranty_other)
        else -> value
    }

class MainActivity : ComponentActivity() {
    private var unlocked by mutableStateOf(false)
    private var lockEnabled by mutableStateOf(false)
    private val unlockLauncher =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) {
            unlocked = it.resultCode == android.app.Activity.RESULT_OK
        }

    override fun onStart() {
        super.onStart()
        lockEnabled = getSharedPreferences("app-lock", MODE_PRIVATE).getBoolean("enabled", false)
        // The OS removes its credential when the owner disables device security.
        // Avoid trapping that owner behind a confirmation screen Android cannot show.
        if (
            lockEnabled && !getSystemService(android.app.KeyguardManager::class.java).isDeviceSecure
        ) {
            getSharedPreferences("app-lock", MODE_PRIVATE)
                .edit()
                .putBoolean("enabled", false)
                .apply()
            lockEnabled = false
        }
        if (lockEnabled) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onStop() {
        if (lockEnabled) unlocked = false
        super.onStop()
    }

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(AppLanguage.apply(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockEnabled = getSharedPreferences("app-lock", MODE_PRIVATE).getBoolean("enabled", false)
        if (lockEnabled) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        (application as PossessoryApp).billing.start()
        setContent {
            if (!lockEnabled || unlocked) Possessory()
            else
                MaterialTheme {
                    Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.Center) {
                            Text(
                                s(R.string.app_locked),
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            Text(s(R.string.unlock_notice))
                            Button(
                                onClick = {
                                    val manager =
                                        getSystemService(android.app.KeyguardManager::class.java)
                                    manager
                                        .createConfirmDeviceCredentialIntent(
                                            getString(R.string.app_name),
                                            getString(R.string.unlock_notice),
                                        )
                                        ?.let { unlockLauncher.launch(it) }
                                }
                            ) {
                                Text(s(R.string.unlock))
                            }
                        }
                    }
                }
        }
    }
}

@Composable
fun Possessory(vm: InventoryViewModel = viewModel()) {
    val data by vm.state.collectAsStateWithLifecycle()
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val dark = prefs[stringPreferencesKey("theme")] == "dark"
    val colours =
        if (dark)
            darkColorScheme(
                primary = Color(0xFFB8CEBD),
                secondary = Gold,
                background = Color(0xFF161F2B),
                surface = Color(0xFF202D3D),
                onSurface = Ivory,
            )
        else
            lightColorScheme(
                primary = Navy,
                secondary = Color(0xFF546559),
                tertiary = Color(0xFF875C19),
                background = Ivory,
                surface = Color.White,
                onSurface = Ink,
                secondaryContainer = Color(0xFFE3EBDD),
                onSecondaryContainer = Navy,
                outline = Color(0xFFCDD3CD),
                surfaceVariant = Color(0xFFEDEFE9),
            )
    MaterialTheme(
        colorScheme = colours,
        typography =
            Typography(
                bodyLarge =
                    MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.SansSerif)
            ),
    ) {
        Surface(
            Modifier.fillMaxSize().systemBarsPadding(),
            color = MaterialTheme.colorScheme.background,
        ) {
            var page by rememberSaveable { mutableStateOf("overview") }
            var query by rememberSaveable { mutableStateOf("") }
            var roomFilter by rememberSaveable { mutableStateOf("") }
            var warrantyMonthOnly by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(page) { if (page != "warranties") warrantyMonthOnly = false }
            var selectedHome by rememberSaveable { mutableStateOf("") }
            val home =
                data.households.find { it.id == selectedHome } ?: data.households.firstOrNull()
            val rooms = data.rooms.filter { it.householdId == home?.id }
            val items = data.items.filter { i -> rooms.any { it.id == i.roomId } }
            var editingId by rememberSaveable { mutableStateOf<String?>(null) }
            val editing = data.items.find { it.id == editingId }
            var detail by rememberSaveable { mutableStateOf<String?>(null) }
            var adding by rememberSaveable { mutableStateOf(false) }
            var voiceDraft by remember { mutableStateOf<InventoryItem?>(null) }
            var voiceMove by remember { mutableStateOf<Pair<InventoryItem, RoomRecord>?>(null) }
            var voiceMessage by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(adding) { if (!adding) voiceDraft = null }
            BackHandler(enabled = detail != null || page != "overview") {
                if (detail != null) detail = null else page = "overview"
            }
            var onboardingDismissed by rememberSaveable { mutableStateOf(false) }
            var roomDialog by remember { mutableStateOf(false) }
            var voice by remember { mutableStateOf(false) }
            var householdDialog by remember { mutableStateOf(false) }
            var editHousehold by remember { mutableStateOf(false) }
            val ctx = LocalContext.current
            val tts = remember(ctx) { TextToSpeech(ctx) {} }
            DisposableEffect(tts) { onDispose { tts.shutdown() } }
            val navigation =
                listOf(
                    "overview" to Icons.Outlined.SpaceDashboard,
                    "inventory" to Icons.Outlined.Inventory2,
                    "rooms" to Icons.Outlined.MeetingRoom,
                    "warranties" to Icons.Outlined.VerifiedUser,
                    "maintenance" to Icons.Outlined.Build,
                    "reports" to Icons.Outlined.Description,
                    "settings" to Icons.Outlined.Settings,
                )
            BoxWithConstraints {
                val wide = maxWidth >= 840.dp
                Column {
                    Row(Modifier.weight(1f)) {
                        if (wide)
                            Column(
                                Modifier.width(220.dp)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState())
                                    .background(
                                        if (dark) MaterialTheme.colorScheme.surface
                                        else Color(0xFFEDEFE9)
                                    )
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    Modifier.padding(vertical = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    BrandMark(36.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "possessory",
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 22.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Text(
                                    s(R.string.your_home).uppercase(),
                                    fontSize = 10.sp,
                                    letterSpacing = 2.sp,
                                    modifier = Modifier.padding(vertical = 16.dp),
                                )
                                navigation.forEach { (key, icon) ->
                                    val active = page == key
                                    Surface(
                                        onClick = {
                                            page = key
                                            detail = null
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color =
                                            if (active) MaterialTheme.colorScheme.primary
                                            else Color.Transparent,
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                icon,
                                                null,
                                                Modifier.size(20.dp),
                                                tint =
                                                    if (active) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.onSurface,
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                                label(key),
                                                fontSize = 14.sp,
                                                color =
                                                    if (active) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Card(
                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.background
                                        )
                                ) {
                                    Column(Modifier.padding(16.dp)) {
                                        Icon(Icons.Outlined.Lock, null, tint = Sage)
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            s(R.string.private_by_design),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                        )
                                        Text(
                                            s(R.string.local_promise),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 6.dp),
                                        )
                                    }
                                }
                                Text(
                                    "POSSESSORY  /  ${BuildConfig.VERSION_NAME}",
                                    fontSize = 9.sp,
                                    letterSpacing = 2.sp,
                                    modifier = Modifier.padding(top = 14.dp),
                                )
                            }
                        Column(Modifier.weight(1f)) {
                            Row(
                                Modifier.fillMaxWidth()
                                    .padding(
                                        horizontal = if (wide) 32.dp else 16.dp,
                                        vertical = 12.dp,
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (!wide) {
                                    BrandMark(32.dp)
                                    Spacer(Modifier.width(8.dp))
                                }
                                var homeMenu by remember { mutableStateOf(false) }
                                Box {
                                    TextButton(onClick = { homeMenu = true }) {
                                        Icon(Icons.Outlined.Home, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(home?.name ?: s(R.string.your_home))
                                        Icon(Icons.Outlined.ExpandMore, null)
                                    }
                                    DropdownMenu(
                                        expanded = homeMenu,
                                        onDismissRequest = { homeMenu = false },
                                    ) {
                                        data.households.forEach { h ->
                                            DropdownMenuItem(
                                                text = { Text(h.name) },
                                                onClick = {
                                                    selectedHome = h.id
                                                    homeMenu = false
                                                    roomFilter = ""
                                                },
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text(s(R.string.add_household)) },
                                            onClick = {
                                                homeMenu = false
                                                editHousehold = false
                                                householdDialog = true
                                            },
                                        )
                                        if (home != null)
                                            DropdownMenuItem(
                                                text = { Text(s(R.string.edit_household)) },
                                                onClick = {
                                                    homeMenu = false
                                                    editHousehold = true
                                                    householdDialog = true
                                                },
                                            )
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { voice = true }) {
                                    Icon(Icons.Outlined.Mic, s(R.string.voice))
                                }
                                FilledTonalButton(
                                    onClick = {
                                        if (home == null) onboardingDismissed = false
                                        else adding = true
                                    }
                                ) {
                                    Icon(Icons.Outlined.Add, if (wide) null else s(R.string.add_item))
                                    if (wide)
                                        Text(s(R.string.add_item), Modifier.padding(start = 6.dp))
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = .3f)
                            )
                            if (BuildConfig.REVIEW_MODE)
                                Text(
                                    s(R.string.review_mode_notice),
                                    Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            val open: (InventoryItem) -> Unit = { detail = it.id }
                            Box(Modifier.weight(1f)) {
                                if (detail != null) {
                                    val item = data.items.find { it.id == detail }
                                    if (item != null)
                                        DetailScreen(
                                            item,
                                            data,
                                            vm,
                                            onBack = { detail = null },
                                            onEdit = { editingId = item.id },
                                            onDeleted = { detail = null },
                                        )
                                } else
                                    when (page) {
                                        "overview" ->
                                            Dashboard(
                                                home,
                                                rooms,
                                                items,
                                                data,
                                                wide,
                                                onAdd = {
                                                    if (home == null) onboardingDismissed = false
                                                    else adding = true
                                                },
                                                onOpen = open,
                                                onRoom = {
                                                    roomFilter = it
                                                    page = "inventory"
                                                },
                                                onWarranties = { page = "warranties" },
                                                onReport = { page = "reports" },
                                            )
                                        "inventory" ->
                                            InventoryScreen(
                                                items,
                                                rooms,
                                                data.attachments,
                                                data.maintenance,
                                                query,
                                                { query = it },
                                                roomFilter,
                                                { roomFilter = it },
                                                open,
                                                { adding = true },
                                                vm = vm,
                                            )
                                        "rooms" ->
                                            RoomScreen(
                                                rooms,
                                                items,
                                                onAdd = {
                                                    if (home == null) onboardingDismissed = false
                                                    else roomDialog = true
                                                },
                                                onOpen = {
                                                    roomFilter = it
                                                    page = "inventory"
                                                },
                                                vm = vm,
                                            )
                                        "warranties" ->
                                            WarrantyScreen(
                                                items,
                                                onOpen = open,
                                                monthOnly = warrantyMonthOnly,
                                                onReset = { warrantyMonthOnly = false },
                                            )
                                        "maintenance" ->
                                            MaintenanceScreen(items, data.maintenance, vm, open)
                                        "reports" ->
                                            ReportsScreen(
                                                data.copy(
                                                    households = listOfNotNull(home),
                                                    rooms = rooms,
                                                    items = items,
                                                ),
                                                vm,
                                            )
                                        "settings" -> SettingsScreen(vm, data, dark)
                                    }
                            }
                        }
                    }
                    if (!wide)
                        ScrollableTabRow(
                            selectedTabIndex =
                                navigation.indexOfFirst { it.first == page }.coerceAtLeast(0),
                            edgePadding = 0.dp,
                        ) {
                            navigation.forEach { (key, icon) ->
                                Tab(
                                    selected = page == key,
                                    onClick = {
                                        page = key
                                        detail = null
                                    },
                                    icon = { Icon(icon, null) },
                                    text = { Text(label(key), fontSize = 11.sp) },
                                )
                            }
                        }
                }
            }
            if (home == null && !onboardingDismissed)
                Onboarding(vm, onSkip = { onboardingDismissed = true })
            if (householdDialog)
                HouseholdDialog(if (editHousehold) home else null, vm) { householdDialog = false }
            if (roomDialog && home != null)
                RoomDialog(onDismiss = { roomDialog = false }) { name, kind ->
                    vm.room(home.id, name, kind)
                    roomDialog = false
                }
            if (adding || editing != null) {
                if (rooms.isEmpty())
                    AlertDialog(
                        onDismissRequest = {
                            adding = false
                            editingId = null
                        },
                        title = { Text(s(R.string.add_room)) },
                        text = { Text(s(R.string.room_required)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    adding = false
                                    editingId = null
                                    roomDialog = true
                                }
                            ) {
                                Text(s(R.string.add_room))
                            }
                        },
                    )
                else
                    ItemForm(
                        editing,
                        rooms,
                        home?.currency ?: "GBP",
                        onDismiss = {
                            adding = false
                            editingId = null
                            voiceDraft = null
                        },
                        onSave = {
                            vm.save(it)
                            adding = false
                            editingId = null
                            voiceDraft = null
                        },
                        draft = voiceDraft,
                    )
            }
            if (voice)
                VoiceDialog(
                    onDismiss = { voice = false },
                    onIntent = { intent ->
                        tts.language = Locale.getDefault()
                        if (
                            intent !is VoiceIntent.Count &&
                                intent != VoiceIntent.ReadReminders &&
                                intent !is VoiceIntent.Move &&
                                intent !is VoiceIntent.AddNamed
                        )
                            tts.speak(
                                ctx.getString(R.string.voice_confirm),
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "result",
                            )
                        when (intent) {
                            VoiceIntent.Add -> adding = true
                            is VoiceIntent.Find -> {
                                query = intent.query
                                roomFilter = ""
                                page = "inventory"
                            }
                            is VoiceIntent.Room -> {
                                roomFilter =
                                    rooms.find { it.name.contains(intent.name, true) }?.id ?: ""
                                query = if (roomFilter.isEmpty()) intent.name else ""
                                page = "inventory"
                            }
                            VoiceIntent.Warranties -> {
                                warrantyMonthOnly = false
                                page = "warranties"
                            }
                            VoiceIntent.WarrantiesThisMonth -> {
                                warrantyMonthOnly = true
                                page = "warranties"
                            }
                            VoiceIntent.Summary -> page = "overview"
                            VoiceIntent.Receipt -> page = "inventory"
                            is VoiceIntent.AddNamed ->
                                vm.premium {
                                    val target =
                                        rooms
                                            .filter {
                                                it.name.equals(intent.room, true) ||
                                                    it.name.contains(intent.room, true) ||
                                                    it.kind.contains(intent.room, true)
                                            }
                                            .singleOrNull()
                                    if (target == null || intent.name.isBlank())
                                        voiceMessage = ctx.getString(R.string.voice_room_missing)
                                    else {
                                        voiceDraft =
                                            InventoryItem(
                                                name = intent.name,
                                                roomId = target.id,
                                                currency = home?.currency ?: "GBP",
                                            )
                                        adding = true
                                    }
                                }
                            is VoiceIntent.Move ->
                                vm.premium {
                                    val target =
                                        rooms
                                            .filter {
                                                it.name.equals(intent.room, true) ||
                                                    it.name.contains(intent.room, true) ||
                                                    it.kind.contains(intent.room, true)
                                            }
                                            .singleOrNull()
                                    val found =
                                        items
                                            .filter { it.name.contains(intent.name, true) }
                                            .singleOrNull()
                                    if (target == null)
                                        voiceMessage = ctx.getString(R.string.voice_room_missing)
                                    else if (found == null || intent.name.isBlank())
                                        voiceMessage = ctx.getString(R.string.voice_item_missing)
                                    else voiceMove = found to target
                                }
                            is VoiceIntent.Count ->
                                vm.premium {
                                    val targets =
                                        rooms.filter {
                                            it.name.contains(intent.room, true) ||
                                                it.kind.contains(intent.room, true)
                                        }
                                    voiceMessage =
                                        if (targets.isEmpty())
                                            ctx.getString(R.string.voice_room_missing)
                                        else
                                            ctx.getString(
                                                R.string.voice_count_result,
                                                items.count { item ->
                                                    item.status == "Active" &&
                                                        targets.any { it.id == item.roomId }
                                                },
                                            )
                                    tts.speak(voiceMessage, TextToSpeech.QUEUE_FLUSH, null, "count")
                                }
                            VoiceIntent.ReadReminders ->
                                vm.premium {
                                    val pending =
                                        items.filter {
                                            it.status == "Active" &&
                                                InventoryRules.warranty(it) == "soon"
                                        }
                                    voiceMessage =
                                        pending
                                            .joinToString(". ") {
                                                it.name + ": " + date(it.warrantyEnd)
                                            }
                                            .ifBlank { ctx.getString(R.string.nothing_here) }
                                    tts.speak(
                                        voiceMessage,
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "reminders",
                                    )
                                }
                            else -> {}
                        }
                        voice = false
                    },
                )
            voiceMove?.let { (item, target) ->
                AlertDialog(
                    onDismissRequest = { voiceMove = null },
                    title = { Text(s(R.string.voice_move_title)) },
                    text = { Text(s(R.string.voice_move_body, item.name, target.name)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                vm.save(item.copy(roomId = target.id))
                                voiceMove = null
                            }
                        ) {
                            Text(s(R.string.save))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { voiceMove = null }) { Text(s(R.string.cancel)) }
                    },
                )
            }
            voiceMessage?.let { message ->
                AlertDialog(
                    onDismissRequest = { voiceMessage = null },
                    title = { Text(s(R.string.voice_title)) },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(onClick = { voiceMessage = null }) { Text(s(R.string.close)) }
                    },
                )
            }
            error?.let { message ->
                AlertDialog(
                    onDismissRequest = { vm.error.value = null },
                    title = { Text(s(R.string.operation_failed)) },
                    text = {
                        Text(
                            if (message == "premium_unavailable")
                                ctx.getString(R.string.premium_unavailable)
                            else if (message == "device_lock_required")
                                ctx.getString(R.string.device_lock_required)
                            else ctx.getString(R.string.operation_error)
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { vm.error.value = null }) { Text(s(R.string.close)) }
                    },
                )
            }
        }
    }
}

@Composable
fun label(key: String): String =
    s(
        when (key) {
            "overview" -> R.string.overview
            "inventory" -> R.string.inventory
            "rooms" -> R.string.rooms
            "warranties" -> R.string.warranties
            "maintenance" -> R.string.maintenance
            "reports" -> R.string.reports
            else -> R.string.settings
        }
    )

@Composable
fun BrandMark(size: Dp) {
    Icon(
        Icons.Outlined.Cottage,
        null,
        Modifier.size(size),
        tint = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun Heading(title: String, subtitle: String = "", trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontFamily = FontFamily.Serif,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            if (subtitle.isNotEmpty())
                Text(
                    subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
        }
        trailing()
    }
}

@Composable
fun Panel(
    title: String,
    action: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .25f)),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), fontFamily = FontFamily.Serif, fontSize = 21.sp)
                action()
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    icon: ImageVector = Icons.Outlined.Inventory2,
    action: @Composable () -> Unit = {},
) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, Modifier.size(48.dp), tint = Sage)
        Text(title, fontFamily = FontFamily.Serif, fontSize = 22.sp)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        action()
    }
}

@Composable
fun Dashboard(
    home: Household?,
    rooms: List<RoomRecord>,
    items: List<InventoryItem>,
    data: InventoryState,
    wide: Boolean,
    onAdd: () -> Unit,
    onOpen: (InventoryItem) -> Unit,
    onRoom: (String) -> Unit,
    onWarranties: () -> Unit,
    onReport: () -> Unit,
) {
    val active = items.filter { it.status == "Active" }
    val soon = active.filter { InventoryRules.warranty(it) == "soon" }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(if (wide) 32.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { Heading(s(R.string.everything_in_place), s(R.string.dashboard_subtitle)) }
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Navy),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            s(R.string.a_little_order).uppercase(),
                            fontSize = 10.sp,
                            color = Gold,
                            letterSpacing = 2.sp,
                        )
                        Text(
                            s(R.string.hero_title),
                            fontFamily = FontFamily.Serif,
                            fontSize = 30.sp,
                            lineHeight = 36.sp,
                            color = Ivory,
                        )
                        Text(s(R.string.hero_body), fontSize = 13.sp, color = Color(0xFFCDD6DC))
                        Button(
                            onClick = onAdd,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = Ivory,
                                    contentColor = Navy,
                                ),
                        ) {
                            Icon(Icons.Outlined.Add, null)
                            Text(s(R.string.add_item), Modifier.padding(start = 6.dp))
                        }
                    }
                    if (wide) HomeIllustration(Modifier.width(210.dp).height(155.dp))
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat(
                    s(R.string.total_items),
                    "${active.size}",
                    Icons.Outlined.Inventory2,
                    Modifier.weight(1f),
                )
                val totals =
                    active
                        .filter { it.priceMinor != null }
                        .groupBy { it.currency }
                        .map { (currency, list) ->
                            InventoryRules.money(
                                list.fold(java.math.BigInteger.ZERO) { acc, i ->
                                    acc + java.math.BigInteger.valueOf(i.priceMinor ?: 0)
                                },
                                currency,
                            )
                        }
                        .joinToString("\n")
                Stat(
                    s(R.string.recorded_value),
                    totals.ifEmpty { InventoryRules.money(0, home?.currency ?: "GBP") },
                    Icons.Outlined.AccountBalanceWallet,
                    Modifier.weight(1.4f),
                )
                if (wide)
                    Stat(
                        s(R.string.missing_receipts),
                        "${active.count { i -> data.attachments.none { it.itemId==i.id && it.kind=="Receipt" } }}",
                        Icons.Outlined.ReceiptLong,
                        Modifier.weight(1f),
                    )
            }
        }
        item {
            Panel(
                s(R.string.rooms),
                action = { Text("${rooms.size}", color = MaterialTheme.colorScheme.secondary) },
            ) {
                if (rooms.isEmpty()) Text(s(R.string.room_required))
                else
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(rooms) { room ->
                            RoomCard(
                                room,
                                active.filter { it.roomId == room.id },
                                Modifier.width(190.dp),
                                onClick = { onRoom(room.id) },
                            )
                        }
                    }
            }
        }
        item {
            Panel(
                s(R.string.recently_added),
                action = { Icon(Icons.Outlined.Schedule, null, tint = Sage) },
            ) {
                if (active.isEmpty())
                    EmptyState(
                        s(R.string.start_collection),
                        s(R.string.empty_inventory_body),
                        action = {
                            TextButton(onClick = onAdd) { Text(s(R.string.add_first_item)) }
                        },
                    )
                else
                    active.take(4).forEach {
                        ItemRow(it, rooms, data.attachments, onClick = { onOpen(it) })
                    }
            }
        }
        item {
            Panel(
                s(R.string.needs_attention),
                action = { TextButton(onClick = onWarranties) { Text(s(R.string.view_all)) } },
            ) {
                if (soon.isEmpty())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = Sage)
                        Text(s(R.string.no_warranties_due), Modifier.padding(start = 12.dp))
                    }
                else
                    soon.take(3).forEach {
                        ItemRow(it, rooms, data.attachments, onClick = { onOpen(it) })
                    }
            }
        }
        item {
            OutlinedButton(onClick = onReport, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.FileDownload, null)
                Text(s(R.string.create_report), Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
fun Stat(title: String, value: String, icon: ImageVector, modifier: Modifier) {
    Card(
        modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = Sage, modifier = Modifier.size(22.dp))
            Text(
                value,
                fontSize = 27.sp,
                fontFamily = FontFamily.Serif,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun HomeIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawCircle(
            Color(0xFF33465B),
            h * .46f,
            androidx.compose.ui.geometry.Offset(w * .54f, h * .52f),
        )
        val path =
            androidx.compose.ui.graphics.Path().apply {
                moveTo(w * .2f, h * .43f)
                lineTo(w * .52f, h * .08f)
                lineTo(w * .84f, h * .43f)
                lineTo(w * .84f, h * .9f)
                lineTo(w * .2f, h * .9f)
                close()
            }
        drawPath(path, Color(0xFFE9E5D9))
        drawRect(
            Color(0xFF6D8B78),
            androidx.compose.ui.geometry.Offset(w * .34f, h * .45f),
            androidx.compose.ui.geometry.Size(w * .18f, h * .22f),
        )
        drawRect(
            Color(0xFFC5A46D),
            androidx.compose.ui.geometry.Offset(w * .57f, h * .45f),
            androidx.compose.ui.geometry.Size(w * .14f, h * .22f),
        )
        drawRect(
            Color(0xFFC5A46D),
            androidx.compose.ui.geometry.Offset(w * .34f, h * .72f),
            androidx.compose.ui.geometry.Size(w * .37f, h * .12f),
        )
        drawLine(
            Navy,
            androidx.compose.ui.geometry.Offset(w * .4f, h * .73f),
            androidx.compose.ui.geometry.Offset(w * .64f, h * .73f),
            3f,
        )
    }
}

@Composable
fun RoomCard(
    room: RoomRecord,
    items: List<InventoryItem>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().height(100.dp).background(Color(0xFFE7EBE3)),
                contentAlignment = Alignment.Center,
            ) {
                if (room.photoFile.isNotBlank())
                    AsyncImage(
                        File(LocalContext.current.filesDir, "attachments/${room.photoFile}"),
                        room.name,
                        Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                else Icon(roomIcon(room.kind), null, Modifier.size(52.dp), tint = Navy)
            }
            Column(Modifier.padding(14.dp)) {
                Text(room.name, fontWeight = FontWeight.Medium)
                Text(
                    pluralStringResource(R.plurals.inventory_count, items.size, items.size),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                items
                    .filter { it.priceMinor != null }
                    .groupBy { it.currency }
                    .forEach { (currency, group) ->
                        Text(
                            InventoryRules.money(
                                group.fold(java.math.BigInteger.ZERO) { sum, item ->
                                    sum + java.math.BigInteger.valueOf(item.priceMinor!!)
                                },
                                currency,
                            ),
                            fontSize = 12.sp,
                        )
                    }
            }
        }
    }
}

fun roomIcon(kind: String): ImageVector =
    when (kind) {
        "Living Room" -> Icons.Outlined.Weekend
        "Kitchen" -> Icons.Outlined.Kitchen
        "Bedroom" -> Icons.Outlined.Bed
        "Home Office" -> Icons.Outlined.Desk
        "Garden" -> Icons.Outlined.Yard
        "Garage" -> Icons.Outlined.Garage
        else -> Icons.Outlined.MeetingRoom
    }

@Composable
fun ItemRow(
    item: InventoryItem,
    rooms: List<RoomRecord>,
    attachments: List<Attachment>,
    onClick: () -> Unit,
) {
    val photo =
        attachments.firstOrNull {
            it.itemId == item.id && it.kind == "Photo" && it.mime.startsWith("image/")
        }
    val ctx = LocalContext.current
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null)
                AsyncImage(
                    File(ctx.filesDir, "attachments/${photo.file}"),
                    item.name,
                    Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            else Icon(Icons.Outlined.Inventory2, null, tint = Sage)
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(item.name, fontWeight = FontWeight.Medium)
            Text(
                rooms.find { it.id == item.roomId }?.name ?: localizedLabel(item.category),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item.warrantyEnd.isNotBlank())
                Text(
                    s(R.string.warranty_end) + " · " + date(item.warrantyEnd),
                    fontSize = 11.sp,
                    color =
                        if (InventoryRules.warranty(item) == "soon")
                            MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.secondary,
                )
        }
        Text(
            item.priceMinor?.let { InventoryRules.money(it, item.currency) } ?: "—",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Icon(
            Icons.Outlined.ChevronRight,
            null,
            Modifier.padding(start = 8.dp).size(18.dp),
            tint = Sage,
        )
    }
}

fun date(value: String): String =
    runCatching {
            val date = LocalDate.parse(value)
            if (RegionalPreferences.dateStyle == "iso") date.toString()
            else
                date.format(
                    DateTimeFormatter.ofLocalizedDate(
                        if (RegionalPreferences.dateStyle == "short") FormatStyle.SHORT
                        else FormatStyle.MEDIUM
                    )
                )
        }
        .getOrDefault(value)

@Composable
fun InventoryScreen(
    items: List<InventoryItem>,
    rooms: List<RoomRecord>,
    attachments: List<Attachment>,
    maintenance: List<Maintenance>,
    query: String,
    onQuery: (String) -> Unit,
    room: String,
    onRoom: (String) -> Unit,
    onOpen: (InventoryItem) -> Unit,
    onAdd: () -> Unit,
    vm: InventoryViewModel? = null,
) {
    var category by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("active") }
    var sort by rememberSaveable { mutableStateOf("newest") }
    var advanced by remember { mutableStateOf(SearchCriteria()) }
    var advancedDialog by remember { mutableStateOf(false) }
    val list =
        items
            .filter { i ->
                advanced.matches(i) &&
                    InventoryRules.matches(
                        i,
                        query,
                        rooms.find { it.id == i.roomId }?.name ?: "",
                    ) &&
                    (room.isBlank() || i.roomId == room) &&
                    (category.isBlank() || i.category == category) &&
                    when (filter) {
                        "missing" -> attachments.none { it.itemId == i.id && it.kind == "Receipt" }
                        "soon" -> InventoryRules.warranty(i) == "soon"
                        "expired" -> InventoryRules.warranty(i) == "expired"
                        "covered" -> InventoryRules.warranty(i) == "covered"
                        "archived" -> i.status != "Active"
                        "due" ->
                            maintenance.any {
                                it.itemId == i.id &&
                                    it.nextDate.isNotBlank() &&
                                    it.nextDate <= LocalDate.now().toString()
                            }
                        else -> i.status == "Active"
                    }
            }
            .let { list ->
                when (sort) {
                    "name" -> list.sortedBy { it.name.lowercase() }
                    "value" ->
                        list.sortedWith(
                            compareBy<InventoryItem> { it.currency }
                                .thenByDescending { it.priceMinor ?: 0 }
                        )
                    "room" -> list.sortedBy { i -> rooms.find { it.id == i.roomId }?.name }
                    "purchase_date" -> list.sortedByDescending { it.purchaseDate }
                    else -> list.sortedByDescending { it.createdAt }
                }
            }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Heading(s(R.string.your_belongings), s(R.string.inventory_subtitle)) }
        item {
            OutlinedTextField(
                query,
                onQuery,
                Modifier.fillMaxWidth(),
                placeholder = { Text(s(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
        }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Choice(
                    s(R.string.all_rooms),
                    room,
                    listOf("" to s(R.string.all_rooms)) + rooms.map { it.id to it.name },
                    onRoom,
                )
                Choice(
                    s(R.string.category),
                    category,
                    listOf("" to s(R.string.all_categories)) +
                        items.map { it.category }.distinct().map { it to localizedLabel(it) },
                    { category = it },
                )
                Choice(
                    s(R.string.filter),
                    filter,
                    listOf(
                        "active" to s(R.string.active),
                        "missing" to s(R.string.missing_receipts),
                        "soon" to s(R.string.expiring_soon),
                        "expired" to s(R.string.expired),
                        "covered" to s(R.string.covered),
                        "due" to s(R.string.maintenance_due),
                        "archived" to s(R.string.archived),
                    ),
                    { filter = it },
                )
                Choice(
                    s(R.string.sort),
                    sort,
                    listOf(
                        "newest" to s(R.string.newest),
                        "name" to s(R.string.name),
                        "value" to s(R.string.purchase_price),
                        "room" to s(R.string.room),
                        "purchase_date" to s(R.string.purchase_date),
                    ),
                    { sort = it },
                )
            }
        }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        if (vm == null) advancedDialog = true
                        else vm.premium { advancedDialog = true }
                    }
                ) {
                    Text(s(R.string.advanced_search))
                }
                TextButton(
                    onClick = {
                        advanced = SearchCriteria()
                        category = ""
                        filter = "active"
                        onQuery("")
                        onRoom("")
                    }
                ) {
                    Text(s(R.string.clear_filters))
                }
            }
            SavedSearchMenu(vm, SavedSearch("", query, room, category, filter, sort, advanced)) {
                saved ->
                onQuery(saved.query)
                onRoom(saved.room)
                category = saved.category
                filter = saved.filter
                sort = saved.sort
                advanced = saved.criteria
            }
        }
        item {
            Text(
                pluralStringResource(R.plurals.inventory_count, list.size, list.size),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (list.isEmpty())
            item {
                EmptyState(
                    s(R.string.no_items),
                    s(R.string.no_items_body),
                    action = { Button(onClick = onAdd) { Text(s(R.string.add_item)) } },
                )
            }
        items(list, key = { it.id }) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    ItemRow(item, rooms, attachments, { onOpen(item) })
                }
            }
        }
    }
    if (advancedDialog)
        SearchDialog(advanced, { advancedDialog = false }) {
            advanced = it
            advancedDialog = false
        }
}

@Composable
fun Choice(
    title: String,
    value: String,
    options: List<Pair<String, String>>,
    onChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(options.find { it.first == value }?.second ?: title)
            Icon(Icons.Outlined.ExpandMore, null)
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onChange(key)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun RoomScreen(
    rooms: List<RoomRecord>,
    items: List<InventoryItem>,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    vm: InventoryViewModel? = null,
) {
    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Heading(
                s(R.string.room_by_room),
                s(R.string.rooms_subtitle),
                trailing = {
                    IconButton(onClick = onAdd) { Icon(Icons.Outlined.Add, s(R.string.add_room)) }
                },
            )
        }
        if (rooms.isEmpty())
            item {
                EmptyState(
                    s(R.string.add_room),
                    s(R.string.room_required),
                    Icons.Outlined.MeetingRoom,
                )
            }
        items(rooms.chunked(2)) { group ->
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    group.forEach { room ->
                        RoomCard(
                            room,
                            items.filter { it.roomId == room.id },
                            Modifier.weight(1f),
                            { onOpen(room.id) },
                        )
                    }
                    if (group.size == 1) Spacer(Modifier.weight(1f))
                }
                if (vm != null)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        group.forEach { room ->
                            Box(Modifier.weight(1f)) { RoomPhotoAction(room, vm) }
                        }
                        if (group.size == 1) Spacer(Modifier.weight(1f))
                    }
            }
        }
        item {
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Add, null)
                Text(s(R.string.add_room))
            }
        }
    }
}

@Composable
fun WarrantyScreen(
    items: List<InventoryItem>,
    onOpen: (InventoryItem) -> Unit,
    monthOnly: Boolean = false,
    onReset: () -> Unit = {},
) {
    val groups =
        listOf(
            "soon" to R.string.expiring_soon,
            "covered" to R.string.covered,
            "expired" to R.string.expired,
        )
    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { Heading(s(R.string.warranty_title), s(R.string.warranty_subtitle)) }
        if (monthOnly)
            item {
                Text(
                    date(LocalDate.now().withDayOfMonth(1).toString()) +
                        " – " +
                        date(
                            LocalDate.now()
                                .withDayOfMonth(LocalDate.now().lengthOfMonth())
                                .toString()
                        )
                )
                TextButton(onClick = onReset) { Text(s(R.string.clear_filters)) }
            }
        groups.forEach { (status, title) ->
            item {
                Panel(s(title)) {
                    val list =
                        items
                            .filter {
                                it.status == "Active" &&
                                    InventoryRules.warranty(it) == status &&
                                    (!monthOnly ||
                                        it.warrantyEnd.startsWith(
                                            LocalDate.now().toString().take(7)
                                        ))
                            }
                            .sortedBy { it.warrantyEnd }
                    if (list.isEmpty())
                        Text(
                            s(R.string.nothing_here),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    else
                        list.forEach { item ->
                            ListItem(
                                headlineContent = { Text(item.name) },
                                supportingContent = { Text(date(item.warrantyEnd)) },
                                leadingContent = {
                                    Icon(
                                        Icons.Outlined.VerifiedUser,
                                        null,
                                        tint = if (status == "soon") Gold else Sage,
                                    )
                                },
                                modifier = Modifier.clickable { onOpen(item) },
                            )
                        }
                }
            }
        }
        item {
            Text(
                s(R.string.warranty_disclaimer),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun TextEntryDialog(title: String, label: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value, { value = it }, label = { Text(label) }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onSave(value.trim()) }, enabled = value.isNotBlank()) {
                Text(s(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s(R.string.cancel)) } },
    )
}

@Composable
fun LanguagePicker() {
    val context = LocalContext.current
    Choice(
        s(R.string.app_language),
        AppLanguage.selected(context),
        listOf("" to s(R.string.use_device_language)) +
            (if (BuildConfig.DRAFT_TRANSLATIONS) AppLanguage.targets
            else AppLanguage.targets.take(1)),
        { tag ->
            AppLanguage.select(context, tag)
            (context as? android.app.Activity)?.recreate()
        },
    )
}

@Composable
fun Onboarding(vm: InventoryViewModel, onSkip: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf("GBP") }
    var roomName by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onSkip,
        icon = { BrandMark(48.dp) },
        title = { Text(s(R.string.welcome), fontFamily = FontFamily.Serif) },
        text = {
            Column(
                Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                LanguagePicker()
                HomeIllustration(Modifier.fillMaxWidth().height(100.dp))
                Text(s(R.string.tagline))
                Text(s(R.string.local_promise))
                OutlinedTextField(
                    name,
                    { name = it },
                    label = { Text(s(R.string.household_name)) },
                    singleLine = true,
                )
                Choice(
                    s(R.string.currency),
                    currency,
                    listOf(
                            "GBP",
                            "USD",
                            "EUR",
                            "AUD",
                            "CAD",
                            "JPY",
                            "INR",
                            "BRL",
                            "AED",
                            "TRY",
                            "IDR",
                            "PLN",
                            "SEK",
                        )
                        .map { it to it },
                    { currency = it },
                )
                Text(s(R.string.onboarding_privacy), fontSize = 12.sp)
                Field(s(R.string.room_name), roomName) { roomName = it }
                RegionalSettings()
            }
        },
        confirmButton = {
            Button(
                onClick = { vm.household(name, currency, roomName) },
                enabled = name.isNotBlank(),
            ) {
                Text(s(R.string.create_home))
            }
        },
        dismissButton = { TextButton(onClick = onSkip) { Text(s(R.string.skip)) } },
    )
}

@Composable
fun ItemForm(
    original: InventoryItem?,
    rooms: List<RoomRecord>,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (InventoryItem) -> Unit,
    draft: InventoryItem? = null,
) {
    var item by
        rememberSaveable(
            stateSaver =
                Saver<InventoryItem, String>(
                    save = { Files.gson.toJson(it) },
                    restore = { Files.gson.fromJson(it, InventoryItem::class.java) },
                )
        ) {
            mutableStateOf(
                original
                    ?: draft
                    ?: InventoryItem(name = "", roomId = rooms.first().id, currency = currency)
            )
        }
    var price by rememberSaveable {
        mutableStateOf(InventoryRules.editableAmount(item.priceMinor, item.currency))
    }
    var estimate by rememberSaveable {
        mutableStateOf(InventoryRules.editableAmount(item.estimateMinor, item.currency))
    }
    var error by remember { mutableStateOf(false) }
    var more by rememberSaveable { mutableStateOf(false) }
    val categories =
        listOf(
            "Electronics",
            "Appliances",
            "Furniture",
            "Jewellery",
            "Clothing",
            "Tools",
            "Art",
            "Other",
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                s(if (original == null) R.string.add_item else R.string.edit_item),
                fontFamily = FontFamily.Serif,
            )
        },
        text = {
            Column(
                Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Field(s(R.string.item_name), item.name) { item = item.copy(name = it) }
                Choice(
                    s(R.string.room),
                    item.roomId,
                    rooms.map { it.id to it.name },
                    { item = item.copy(roomId = it) },
                )
                Choice(
                    s(R.string.category),
                    item.category,
                    categories.map { it to localizedLabel(it) },
                    { item = item.copy(category = it) },
                )
                Choice(
                    s(R.string.currency),
                    item.currency,
                    listOf(
                            "GBP",
                            "USD",
                            "EUR",
                            "AUD",
                            "CAD",
                            "JPY",
                            "INR",
                            "BRL",
                            "AED",
                            "TRY",
                            "IDR",
                            "PLN",
                            "SEK",
                            "KWD",
                        )
                        .map { it to it },
                    {
                        item = item.copy(currency = it)
                        price = ""
                        estimate = ""
                    },
                )
                Field(s(R.string.purchase_price) + " (${item.currency})", price) { price = it }
                Text(s(R.string.decimal_hint), fontSize = 11.sp)
                Field(s(R.string.purchase_date) + " · YYYY-MM-DD", item.purchaseDate) {
                    item = item.copy(purchaseDate = it)
                }
                Field(s(R.string.warranty_end) + " · YYYY-MM-DD", item.warrantyEnd) {
                    item = item.copy(warrantyEnd = it)
                }
                TextButton(onClick = { more = !more }) {
                    Text(s(if (more) R.string.fewer_details else R.string.more_details))
                }
                if (more) {
                    Field(s(R.string.description), item.description) {
                        item = item.copy(description = it)
                    }
                    Field(s(R.string.brand), item.brand) { item = item.copy(brand = it) }
                    Field(s(R.string.model), item.model) { item = item.copy(model = it) }
                    Field(s(R.string.serial), item.serial) { item = item.copy(serial = it) }
                    Field(s(R.string.retailer), item.retailer) { item = item.copy(retailer = it) }
                    Field(s(R.string.estimated_value) + " (${item.currency})", estimate) {
                        estimate = it
                    }
                    Text(s(R.string.estimate_disclaimer), fontSize = 11.sp)
                    Field(s(R.string.warranty_start) + " · YYYY-MM-DD", item.warrantyStart) {
                        item = item.copy(warrantyStart = it)
                    }
                    Field(s(R.string.warranty_provider), item.warrantyProvider) {
                        item = item.copy(warrantyProvider = it)
                    }
                    Choice(
                        s(R.string.warranty_type),
                        item.warrantyType,
                        (listOf("Manufacturer", "Extended warranty", "Retailer", "Other warranty") +
                                listOf(item.warrantyType).filter { it.isNotBlank() })
                            .distinct()
                            .map { it to localizedLabel(it) },
                        { item = item.copy(warrantyType = it) },
                    )
                    Field(s(R.string.warranty_notes), item.warrantyNotes) {
                        item = item.copy(warrantyNotes = it)
                    }
                    Choice(
                        s(R.string.reminder_days),
                        item.reminderDays.toString(),
                        listOf(0, 7, 14, 30, 60, 90).map { it.toString() to it.toString() },
                        { item = item.copy(reminderDays = it.toInt()) },
                    )
                    Field(s(R.string.reminder_offsets), item.reminderOffsets) {
                        item = item.copy(reminderOffsets = it)
                    }
                    Field(s(R.string.custom_reminder_dates), item.reminderDates) {
                        item = item.copy(reminderDates = it)
                    }
                    Text(s(R.string.custom_reminder_hint), fontSize = 12.sp)
                    Field(s(R.string.tags), item.tags) { item = item.copy(tags = it) }
                    Field(s(R.string.notes), item.notes) { item = item.copy(notes = it) }
                    Choice(
                        s(R.string.condition),
                        item.condition,
                        listOf("New", "Good", "Fair", "Needs repair").map {
                            it to localizedLabel(it)
                        },
                        { item = item.copy(condition = it) },
                    )
                    Choice(
                        s(R.string.status),
                        item.status,
                        listOf("Active", "Archived", "Sold", "Donated", "Lost", "Disposed").map {
                            it to localizedLabel(it)
                        },
                        { item = item.copy(status = it) },
                    )
                }
                if (error) Text(s(R.string.form_error), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    runCatching {
                            require(item.name.isNotBlank())
                            ReminderRules.offsets(item.reminderOffsets)
                            ReminderRules.dates(item.reminderDates)
                            require(
                                listOf(item.purchaseDate, item.warrantyStart, item.warrantyEnd)
                                    .all { InventoryRules.validDate(it) }
                            )
                            require(
                                item.warrantyStart.isBlank() ||
                                    item.warrantyEnd.isBlank() ||
                                    item.warrantyStart <= item.warrantyEnd
                            )
                            item.copy(
                                priceMinor = InventoryRules.amount(price, item.currency),
                                estimateMinor = InventoryRules.amount(estimate, item.currency),
                            )
                        }
                        .onSuccess(onSave)
                        .onFailure { error = true }
                }
            ) {
                Text(s(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s(R.string.cancel)) } },
    )
}

@Composable
fun Field(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value,
        onValue,
        Modifier.fillMaxWidth(),
        label = { Text(label) },
        shape = RoundedCornerShape(12.dp),
        maxLines = 4,
    )
}
