package app.possessory

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.speech.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource as s
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.possessory.data.*
import app.possessory.domain.*
import app.possessory.services.*
import coil.compose.AsyncImage
import java.io.*
import java.time.LocalDate
import java.util.*
import kotlinx.coroutines.*

@Composable
fun RoomDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
  var name by rememberSaveable { mutableStateOf("") }
  var kind by rememberSaveable { mutableStateOf("Living Room") }
  val defaultName = localizedLabel(kind)
  val kinds =
    listOf(
      "Living Room",
      "Kitchen",
      "Bedroom",
      "Bathroom",
      "Home Office",
      "Garage",
      "Garden",
      "Storage",
      "Other",
    )
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(s(R.string.add_room)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Field(s(R.string.room_name), name) { name = it }
        Choice(s(R.string.category), kind, kinds.map { it to localizedLabel(it) }, { kind = it })
      }
    },
    confirmButton = {
      TextButton(onClick = { onSave(name.trim().ifBlank { defaultName }, kind) }) {
        Text(s(R.string.save))
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(s(R.string.cancel)) } },
  )
}

@Composable
fun DetailScreen(
  item: InventoryItem,
  data: InventoryState,
  vm: InventoryViewModel,
  onBack: () -> Unit,
  onEdit: () -> Unit,
  onDeleted: () -> Unit,
) {
  val ctx = LocalContext.current
  var deleting by remember { mutableStateOf(false) }
  var ocr by remember { mutableStateOf<Attachment?>(null) }
  val attachments = data.attachments.filter { it.itemId == item.id }
  var kind by remember { mutableStateOf("Receipt") }
  var cameraFile by remember { mutableStateOf<File?>(null) }
  val picker =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null)
        vm.run {
          val a = withContext(Dispatchers.IO) { Files.importAttachment(ctx, uri, item.id, kind) }
          try {
            vm.dao.put(a)
          } catch (e: Exception) {
            File(ctx.filesDir, "attachments/${a.file}").delete()
            throw e
          }
        }
    }
  val camera =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
      val file = cameraFile
      if (success && file != null)
        vm.run {
          vm.dao.put(
            Attachment(
              itemId = item.id,
              name = ctx.getString(R.string.photo),
              mime = "image/jpeg",
              file = file.name,
              kind = "Photo",
            )
          )
        }
      else file?.delete()
    }
  LazyColumn(
    contentPadding = PaddingValues(24.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    item {
      Row {
        TextButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
          Text(s(R.string.back))
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onEdit) { Text(s(R.string.edit_item)) }
      }
    }
    item { Heading(item.name, data.rooms.find { it.id == item.roomId }?.name ?: "") }
    if (attachments.any { it.kind == "Photo" })
      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          items(attachments.filter { it.kind == "Photo" }) { a ->
            AsyncImage(
              File(ctx.filesDir, "attachments/${a.file}"),
              a.name,
              Modifier.height(180.dp).width(240.dp),
              contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            )
          }
        }
      }
    item {
      Panel(s(R.string.item_details)) {
        DetailLine(s(R.string.category), localizedLabel(item.category))
        DetailLine(s(R.string.brand), item.brand)
        DetailLine(s(R.string.model), item.model)
        DetailLine(s(R.string.serial), item.serial)
        DetailLine(
          s(R.string.purchase_price),
          item.priceMinor?.let { InventoryRules.money(it, item.currency) } ?: "—",
        )
        DetailLine(
          s(R.string.estimated_value),
          item.estimateMinor?.let { InventoryRules.money(it, item.currency) } ?: "",
        )
        DetailLine(s(R.string.purchase_date), date(item.purchaseDate))
        DetailLine(s(R.string.retailer), item.retailer)
        DetailLine(s(R.string.description), item.description)
        DetailLine(s(R.string.notes), item.notes)
        DetailLine(s(R.string.tags), item.tags)
        DetailLine(s(R.string.condition), localizedLabel(item.condition))
        DetailLine(s(R.string.status), localizedLabel(item.status))
      }
    }
    item {
      Panel(s(R.string.warranties)) {
        DetailLine(s(R.string.warranty_provider), item.warrantyProvider)
        DetailLine(s(R.string.warranty_start), date(item.warrantyStart))
        DetailLine(s(R.string.warranty_end), date(item.warrantyEnd))
        DetailLine(s(R.string.warranty_type), item.warrantyType)
        DetailLine(s(R.string.warranty_notes), item.warrantyNotes)
        Text(s(R.string.warranty_disclaimer), fontSize = 11.sp)
      }
    }
    item {
      Panel(s(R.string.receipts_documents)) {
        Row(
          Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = {
              kind = "Receipt"
              picker.launch(arrayOf("image/*", "application/pdf"))
            }
          ) {
            Icon(Icons.Outlined.AttachFile, null)
            Text(s(R.string.add_receipt))
          }
          OutlinedButton(
            onClick = {
              kind = "Photo"
              picker.launch(arrayOf("image/*"))
            }
          ) {
            Icon(Icons.Outlined.Image, null)
            Text(s(R.string.add_photo))
          }
          IconButton(
            onClick = {
              val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
              if (intent.resolveActivity(ctx.packageManager) != null) {
                val file =
                  File(ctx.filesDir, "attachments/${UUID.randomUUID()}").apply {
                    parentFile!!.mkdirs()
                  }
                cameraFile = file
                camera.launch(FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file))
              } else vm.error.value = "camera_unavailable"
            }
          ) {
            Icon(Icons.Outlined.PhotoCamera, s(R.string.take_photo))
          }
        }
        if (attachments.isEmpty())
          Text(s(R.string.no_attachments), Modifier.padding(top = 12.dp), fontSize = 13.sp)
        attachments.forEach { a ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
              onClick = {
                runCatching {
                    ctx.startActivity(
                      Intent(Intent.ACTION_VIEW)
                        .setDataAndType(
                          FileProvider.getUriForFile(
                            ctx,
                            "${ctx.packageName}.files",
                            File(ctx.filesDir, "attachments/${a.file}"),
                          ),
                          a.mime,
                        )
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    )
                  }
                  .onFailure { vm.error.value = "viewer_unavailable" }
              },
              modifier = Modifier.weight(1f),
            ) {
              Text(a.name)
            }
            if (a.kind == "Receipt")
              IconButton(onClick = { ocr = a }) {
                Icon(Icons.Outlined.DocumentScanner, s(R.string.scan_receipt))
              }
            IconButton(
              onClick = {
                vm.run {
                  vm.dao.delete(a)
                  File(ctx.filesDir, "attachments/${a.file}").delete()
                }
              }
            ) {
              Icon(Icons.Outlined.Close, s(R.string.remove_attachment))
            }
          }
        }
      }
    }
    item {
      Panel(s(R.string.maintenance)) {
        data.maintenance
          .filter { it.itemId == item.id }
          .forEach { m -> MaintenanceRow(m, item, vm) }
        var add by remember { mutableStateOf(false) }
        OutlinedButton(onClick = { add = true }) { Text(s(R.string.add_maintenance)) }
        if (add)
          MaintenanceDialog(
            listOf(item),
            onDismiss = { add = false },
            onSave = { m ->
              vm.run { vm.dao.put(m) }
              add = false
            },
          )
      }
    }
    item {
      Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedButton(
          onClick = {
            vm.save(
              item.copy(
                id = UUID.randomUUID().toString(),
                name = item.name + " (2)",
                createdAt = System.currentTimeMillis(),
              )
            )
          }
        ) {
          Text(s(R.string.duplicate))
        }
        OutlinedButton(
          onClick = {
            vm.save(item.copy(status = if (item.status == "Active") "Archived" else "Active"))
            onBack()
          }
        ) {
          Text(s(if (item.status == "Active") R.string.archive else R.string.unarchive))
        }
        TextButton(onClick = { deleting = true }) {
          Text(s(R.string.delete), color = MaterialTheme.colorScheme.error)
        }
      }
    }
  }
  if (deleting)
    AlertDialog(
      onDismissRequest = { deleting = false },
      title = { Text(s(R.string.delete_item)) },
      text = { Text(s(R.string.delete_item_body)) },
      confirmButton = {
        TextButton(
          onClick = {
            vm.delete(item)
            deleting = false
            onDeleted()
          }
        ) {
          Text(s(R.string.delete))
        }
      },
      dismissButton = { TextButton(onClick = { deleting = false }) { Text(s(R.string.cancel)) } },
    )
  ocr?.let { a ->
    ReceiptDialog(
      a,
      vm,
      onDismiss = { ocr = null },
      onSave = { retailer, price, date ->
        runCatching {
            item.copy(
              retailer = retailer.ifBlank { item.retailer },
              priceMinor =
                if (price.isBlank()) item.priceMinor
                else InventoryRules.amount(price, item.currency),
              purchaseDate = date.ifBlank { item.purchaseDate },
            )
          }
          .onSuccess {
            vm.save(it)
            ocr = null
          }
          .onFailure { vm.error.value = "invalid_receipt" }
      },
    )
  }
}

@Composable
fun DetailLine(label: String, value: String) {
  if (value.isNotBlank())
    Row(Modifier.padding(vertical = 7.dp)) {
      Text(
        label,
        Modifier.weight(1f),
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(value, Modifier.weight(1.5f), fontSize = 14.sp)
    }
}

@Composable
fun ReceiptDialog(
  attachment: Attachment,
  vm: InventoryViewModel,
  onDismiss: () -> Unit,
  onSave: (String, String, String) -> Unit,
) {
  val ctx = LocalContext.current
  val scope = rememberCoroutineScope()
  var bitmap by remember { mutableStateOf<Bitmap?>(null) }
  var raw by rememberSaveable { mutableStateOf("") }
  var retailer by rememberSaveable { mutableStateOf("") }
  var total by rememberSaveable { mutableStateOf("") }
  var date by rememberSaveable { mutableStateOf("") }
  var busy by remember { mutableStateOf(false) }
  var failure by remember { mutableStateOf(false) }
  LaunchedEffect(attachment.id) {
    runCatching {
        withContext(Dispatchers.IO) {
          ReceiptOcr.bitmap(File(ctx.filesDir, "attachments/${attachment.file}"), attachment.mime)
        }
      }
      .onSuccess { bitmap = it }
      .onFailure { failure = true }
  }
  AlertDialog(
    onDismissRequest = { if (!busy) onDismiss() },
    title = { Text(s(R.string.review_receipt)) },
    text = {
      Column(
        Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(s(R.string.ocr_notice), fontSize = 12.sp)
        bitmap?.let { b ->
          Image(
            b.asImageBitmap(),
            s(R.string.receipt_preview),
            Modifier.fillMaxWidth().height(180.dp),
          )
          Row {
            TextButton(
              onClick = {
                if (!busy)
                  bitmap =
                    Bitmap.createBitmap(
                      b,
                      0,
                      0,
                      b.width,
                      b.height,
                      Matrix().apply { postRotate(90f) },
                      true,
                    )
              },
              enabled = !busy,
            ) {
              Text(s(R.string.rotate))
            }
            TextButton(
              onClick = {
                if (!busy && b.width > 100 && b.height > 100)
                  bitmap =
                    Bitmap.createBitmap(
                      b,
                      b.width / 10,
                      b.height / 10,
                      b.width * 8 / 10,
                      b.height * 8 / 10,
                    )
              },
              enabled = !busy,
            ) {
              Text(s(R.string.crop_edges))
            }
          }
          Button(
            onClick = {
              scope.launch {
                busy = true
                failure = false
                runCatching { withContext(Dispatchers.IO) { ReceiptOcr.recognise(ctx, b) } }
                  .onSuccess { text ->
                    raw = text
                    val f = ReceiptOcr.extract(text)
                    retailer = f.retailer
                    total = f.total
                    date = f.date
                  }
                  .onFailure { failure = true }
                busy = false
              }
            },
            enabled = !busy,
          ) {
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text(s(R.string.extract_text))
          }
        }
        if (failure) Text(s(R.string.ocr_unavailable), color = MaterialTheme.colorScheme.error)
        Field(s(R.string.extracted_text), raw) { raw = it }
        Field(s(R.string.retailer), retailer) { retailer = it }
        Field(s(R.string.purchase_price), total) { total = it }
        Field(s(R.string.purchase_date) + " · YYYY-MM-DD", date) { date = it }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (InventoryRules.validDate(date)) onSave(retailer, total, date) else failure = true
        },
        enabled = !busy,
      ) {
        Text(s(R.string.apply_details))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !busy) { Text(s(R.string.cancel)) }
    },
  )
}

@Composable
fun MaintenanceScreen(
  items: List<InventoryItem>,
  records: List<Maintenance>,
  vm: InventoryViewModel,
  onOpen: (InventoryItem) -> Unit,
) {
  var add by remember { mutableStateOf(false) }
  LazyColumn(
    contentPadding = PaddingValues(24.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    item { Heading(s(R.string.maintenance_title), s(R.string.maintenance_subtitle)) }
    item {
      Button(onClick = { add = true }, enabled = items.isNotEmpty()) {
        Icon(Icons.Outlined.Add, null)
        Text(s(R.string.add_maintenance))
      }
    }
    if (records.none { m -> items.any { it.id == m.itemId } })
      item {
        EmptyState(s(R.string.no_maintenance), s(R.string.maintenance_empty), Icons.Outlined.Build)
      }
    items(records.filter { m -> items.any { it.id == m.itemId } }, key = { it.id }) { m ->
      Panel(m.type) { MaintenanceRow(m, items.first { it.id == m.itemId }, vm) }
    }
    item { Text(s(R.string.maintenance_disclaimer), fontSize = 12.sp) }
  }
  if (add)
    MaintenanceDialog(
      items,
      { add = false },
      { m ->
        vm.run { vm.dao.put(m) }
        add = false
      },
    )
}

@Composable
fun MaintenanceRow(record: Maintenance, item: InventoryItem, vm: InventoryViewModel) {
  Column(Modifier.padding(vertical = 8.dp)) {
    Text(item.name)
    Text(record.type, fontSize = 14.sp)
    if (record.nextDate.isNotBlank())
      Text(s(R.string.next_due) + " · " + date(record.nextDate), color = Sage, fontSize = 13.sp)
    if (record.notes.isNotBlank()) Text(record.notes, fontSize = 12.sp)
    Row {
      TextButton(
        onClick = {
          vm.run {
            val next =
              if (record.repeatDays > 0)
                LocalDate.now().plusDays(record.repeatDays.toLong()).toString()
              else ""
            vm.dao.put(record.copy(date = LocalDate.now().toString(), nextDate = next))
          }
        }
      ) {
        Text(s(R.string.mark_complete))
      }
      TextButton(onClick = { vm.run { vm.dao.delete(record) } }) { Text(s(R.string.remove)) }
    }
  }
}

@Composable
fun MaintenanceDialog(
  items: List<InventoryItem>,
  onDismiss: () -> Unit,
  onSave: (Maintenance) -> Unit,
) {
  var item by remember { mutableStateOf(items.firstOrNull()?.id ?: "") }
  var type by rememberSaveable { mutableStateOf("") }
  var next by rememberSaveable { mutableStateOf("") }
  var notes by rememberSaveable { mutableStateOf("") }
  var repeat by remember { mutableStateOf("0") }
  var cost by rememberSaveable { mutableStateOf("") }
  var error by remember { mutableStateOf(false) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(s(R.string.add_maintenance)) },
    text = {
      Column(
        Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Choice(s(R.string.item_name), item, items.map { it.id to it.name }, { item = it })
        Field(s(R.string.maintenance_type), type) { type = it }
        Field(s(R.string.next_due) + " · YYYY-MM-DD", next) { next = it }
        Field(s(R.string.notes), notes) { notes = it }
        Field(s(R.string.cost), cost) { cost = it }
        Choice(
          s(R.string.repeat_days),
          repeat,
          listOf(0, 7, 30, 90, 180, 365).map { it.toString() to it.toString() },
          { repeat = it },
        )
        if (error) Text(s(R.string.form_error), color = MaterialTheme.colorScheme.error)
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          runCatching {
              require(type.isNotBlank() && InventoryRules.validDate(next))
              Maintenance(
                itemId = item,
                type = type,
                date = LocalDate.now().toString(),
                nextDate = next,
                repeatDays = repeat.toInt(),
                notes = notes,
                costMinor = InventoryRules.amount(cost, items.first { it.id == item }.currency),
              )
            }
            .onSuccess(onSave)
            .onFailure { error = true }
        },
        enabled = items.isNotEmpty(),
      ) {
        Text(s(R.string.save))
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(s(R.string.cancel)) } },
  )
}

@Composable
fun ReportsScreen(data: InventoryState, vm: InventoryViewModel) {
  val ctx = LocalContext.current
  var room by rememberSaveable { mutableStateOf("") }
  var serials by rememberSaveable { mutableStateOf(false) }
  var prices by rememberSaveable { mutableStateOf(true) }
  var format by rememberSaveable { mutableStateOf("pdf") }
  var result by remember { mutableStateOf(false) }
  var filter by remember { mutableStateOf("all") }
  val list =
    data.items.filter { i ->
      i.status == "Active" &&
        (room.isBlank() || i.roomId == room) &&
        when (filter) {
          "receipts" -> data.attachments.any { it.itemId == i.id && it.kind == "Receipt" }
          "warranties" -> i.warrantyEnd.isNotBlank()
          else -> true
        }
    }
  val export =
    rememberLauncherForActivityResult(
      ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
      if (uri != null)
        vm.run {
          withContext(Dispatchers.IO) {
            ctx.contentResolver.openOutputStream(uri)!!.use { output ->
              when (format) {
                "csv" ->
                  output.write(
                    Files.csv(list, data.rooms, serials, prices).toByteArray(Charsets.UTF_8)
                  )
                "json" ->
                  output.write(
                    Files.gson
                      .toJson(
                        list.map {
                          it.copy(
                            serial = if (serials) it.serial else "",
                            priceMinor = if (prices) it.priceMinor else null,
                            estimateMinor = if (prices) it.estimateMinor else null,
                            notes = "",
                            description = "",
                            tags = "",
                            warrantyNotes = "",
                          )
                        }
                      )
                      .toByteArray(Charsets.UTF_8)
                  )
                else -> Files.pdf(ctx, list, data.rooms, serials, prices, output)
              }
            }
          }
          result = true
        }
    }
  LazyColumn(
    contentPadding = PaddingValues(24.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    item { Heading(s(R.string.reports_title), s(R.string.reports_subtitle)) }
    item {
      Panel(s(R.string.build_report)) {
        Choice(
          s(R.string.room),
          room,
          listOf("" to s(R.string.all_rooms)) + data.rooms.map { it.id to it.name },
          { room = it },
        )
        Choice(
          s(R.string.filter),
          filter,
          listOf(
            "all" to s(R.string.all_items),
            "receipts" to s(R.string.with_receipts),
            "warranties" to s(R.string.warranties),
          ),
          { filter = it },
        )
        Toggle(s(R.string.include_serials), serials) { serials = it }
        Toggle(s(R.string.include_prices), prices) { prices = it }
        Choice(
          s(R.string.format),
          format,
          listOf("pdf" to "PDF", "csv" to "CSV", "json" to "JSON"),
          { format = it },
        )
        Text(
          pluralStringResource(R.plurals.inventory_count, list.size, list.size),
          Modifier.padding(vertical = 16.dp),
        )
        Button(onClick = { export.launch("possessory-${LocalDate.now()}.$format") }) {
          Icon(Icons.Outlined.FileDownload, null)
          Text(s(R.string.export_report), Modifier.padding(start = 8.dp))
        }
        if (result)
          Text(s(R.string.export_complete), color = Sage, modifier = Modifier.padding(top = 12.dp))
      }
    }
    item {
      Panel(s(R.string.report_preview)) {
        Text(
          "POSSESSORY",
          fontFamily = FontFamily.Serif,
          fontSize = 26.sp,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(s(R.string.inventory_report), Modifier.padding(vertical = 8.dp))
        HorizontalDivider()
        list.take(6).forEach { i ->
          Column(Modifier.padding(vertical = 12.dp)) {
            Text(i.name)
            Text(data.rooms.find { it.id == i.roomId }?.name ?: "", fontSize = 12.sp)
            if (prices)
              Text(
                i.priceMinor?.let { InventoryRules.money(it, i.currency) } ?: "—",
                fontSize = 12.sp,
              )
            if (serials) Text(i.serial, fontSize = 12.sp)
          }
        }
        Text(s(R.string.report_disclaimer), fontSize = 11.sp)
      }
    }
    item { Text(s(R.string.export_privacy), fontSize = 12.sp) }
  }
}

@Composable
fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
  Row(
    Modifier.fillMaxWidth().heightIn(min = 52.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, Modifier.weight(1f))
    Switch(value, onChange)
  }
}

@Composable
fun SettingsScreen(vm: InventoryViewModel, data: InventoryState, dark: Boolean) {
  val ctx = LocalContext.current
  val prefs by vm.preferences.collectAsStateWithLifecycle()
  var preview by remember { mutableStateOf<Files.Preview?>(null) }
  var erase by remember { mutableStateOf(false) }
  var complete by remember { mutableStateOf(false) }
  val billing by vm.app.billing.state.collectAsStateWithLifecycle()
  var encrypted by rememberSaveable { mutableStateOf(false) }
  var password by rememberSaveable { mutableStateOf("") }
  var restoring by remember { mutableStateOf(false) }
  val export =
    rememberLauncherForActivityResult(
      ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
      if (uri != null)
        vm.run {
          withContext(Dispatchers.IO) {
            ctx.contentResolver.openOutputStream(uri)!!.use { out ->
              if (encrypted) {
                val buffer = ByteArrayOutputStream()
                Files.backup(ctx, data, buffer)
                out.write(BackupCrypto.encrypt(buffer.toByteArray(), password.toCharArray()))
              } else Files.backup(ctx, data, out)
            }
          }
          password = ""
          complete = true
        }
    }
  val import =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null)
        vm.run {
          preview =
            withContext(Dispatchers.IO) {
              ctx.contentResolver.openInputStream(uri)!!.use { input ->
                val bytes = Files.readBounded(input)
                Files.preview(
                  ByteArrayInputStream(
                    if (encrypted) BackupCrypto.decrypt(bytes, password.toCharArray()) else bytes
                  )
                )
              }
            }
          password = ""
        }
    }
  val notification =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
      vm.setting("reminders", if (allowed) "on" else "off")
    }
  LazyColumn(
    contentPadding = PaddingValues(24.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    item { Heading(s(R.string.settings), s(R.string.settings_subtitle)) }
    item {
      Panel(s(R.string.preferences)) {
        Toggle(s(R.string.dark_theme), dark) { vm.setting("theme", if (it) "dark" else "light") }
        Toggle(s(R.string.reminders), prefs[stringPreferencesKey("reminders")] == "on") { enabled ->
          if (enabled && Build.VERSION.SDK_INT >= 33)
            notification.launch(Manifest.permission.POST_NOTIFICATIONS)
          else vm.setting("reminders", if (enabled) "on" else "off")
        }
        Text(s(R.string.reminder_timing), fontSize = 12.sp)
        LanguagePicker()
        Text(s(R.string.language_system), Modifier.padding(top = 14.dp), fontSize = 13.sp)
      }
    }
    item {
      Panel(s(R.string.backup_restore)) {
        Text(s(R.string.backup_body), fontSize = 13.sp)
        Toggle(s(R.string.encrypted_backup), encrypted) { encrypted = it }
        if (encrypted)
          OutlinedTextField(
            password,
            { password = it },
            Modifier.fillMaxWidth(),
            label = { Text(s(R.string.backup_password)) },
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            singleLine = true,
          )
        Row(
          Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = {
              export.launch("possessory-${LocalDate.now()}." + if (encrypted) "poss" else "zip")
            },
            enabled = !encrypted || password.length >= 12,
          ) {
            Text(s(R.string.create_backup))
          }
          OutlinedButton(
            onClick = { import.launch(arrayOf("*/*")) },
            enabled = !encrypted || password.length >= 12,
          ) {
            Text(s(R.string.restore_backup))
          }
        }
        if (complete) Text(s(R.string.backup_complete), color = Sage)
      }
    }
    item {
      Panel(s(R.string.premium)) {
        Text(s(R.string.premium_body))
        if (!vm.app.billing.enabled)
          Text(
            s(R.string.premium_unavailable),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 12.dp),
          )
        else {
          Text(
            s(
              if (billing.premium) R.string.premium_active
              else R.string.purchase_verification_notice
            ),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 12.dp),
          )
          if (!billing.premium)
            Button(
              onClick = { vm.app.billing.purchase() },
              enabled = billing.price != null && !billing.busy,
            ) {
              Text(s(R.string.upgrade) + (billing.price?.let { " · $it" } ?: ""))
            }
          if (billing.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
          if (billing.status == "retry") Text(s(R.string.purchase_retry), fontSize = 12.sp)
        }
        OutlinedButton(
          onClick = {
            if (vm.app.billing.enabled) vm.app.billing.restore()
            else vm.error.value = "premium_unavailable"
          },
          enabled = !billing.busy,
        ) {
          Text(s(R.string.restore_purchases))
        }
        if (billing.premium) {
          var homeDialog by remember { mutableStateOf(false) }
          OutlinedButton(onClick = { homeDialog = true }) { Text(s(R.string.add_household)) }
          if (homeDialog)
            TextEntryDialog(
              s(R.string.add_household),
              s(R.string.household_name),
              { homeDialog = false },
            ) {
              vm.household(it, data.households.firstOrNull()?.currency ?: "GBP")
              homeDialog = false
            }
        }
      }
    }
    item {
      Panel(s(R.string.privacy)) {
        Text(s(R.string.privacy_body), fontSize = 13.sp)
        TextButton(onClick = { erase = true }) {
          Text(s(R.string.delete_all), color = MaterialTheme.colorScheme.error)
        }
      }
    }
    item {
      Text(
        s(R.string.preview_build),
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
  preview?.let { p ->
    AlertDialog(
      onDismissRequest = { if (!restoring) preview = null },
      title = { Text(s(R.string.restore_preview)) },
      text = {
        Text(
          s(
            R.string.restore_preview_body,
            p.snapshot.households.size,
            p.snapshot.items.size,
            p.snapshot.attachments.size,
          )
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            restoring = true
            vm.run {
              try {
                withContext(Dispatchers.IO) { Files.restore(vm.app, p) }
                preview = null
                complete = true
              } finally {
                restoring = false
              }
            }
          },
          enabled = !restoring,
        ) {
          Text(s(R.string.restore))
        }
      },
      dismissButton = {
        TextButton(onClick = { preview = null }, enabled = !restoring) { Text(s(R.string.cancel)) }
      },
    )
  }
  if (erase)
    AlertDialog(
      onDismissRequest = { erase = false },
      title = { Text(s(R.string.delete_all)) },
      text = { Text(s(R.string.delete_all_body)) },
      confirmButton = {
        TextButton(
          onClick = {
            vm.erase { (ctx as? Activity)?.recreate() }
            erase = false
          }
        ) {
          Text(s(R.string.delete))
        }
      },
      dismissButton = { TextButton(onClick = { erase = false }) { Text(s(R.string.cancel)) } },
    )
}

@Composable
fun VoiceDialog(onDismiss: () -> Unit, onIntent: (VoiceIntent) -> Unit) {
  val ctx = LocalContext.current
  var text by rememberSaveable { mutableStateOf("") }
  var listening by remember { mutableStateOf(false) }
  var failure by remember { mutableStateOf(false) }
  val recognizer = remember {
    if (SpeechRecognizer.isRecognitionAvailable(ctx)) SpeechRecognizer.createSpeechRecognizer(ctx)
    else null
  }
  DisposableEffect(Unit) { onDispose { recognizer?.destroy() } }
  fun listen() {
    if (recognizer == null) {
      failure = true
      return
    }
    recognizer.setRecognitionListener(
      object : RecognitionListener {
        override fun onReadyForSpeech(params: android.os.Bundle?) {
          listening = true
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
          listening = false
        }

        override fun onError(error: Int) {
          listening = false
          failure = true
        }

        override fun onResults(results: android.os.Bundle?) {
          text =
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
          listening = false
        }

        override fun onPartialResults(partialResults: android.os.Bundle?) {}

        override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
      }
    )
    failure = false
    recognizer.startListening(
      Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
    )
  }
  val permission =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
      if (it) listen() else failure = true
    }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(s(R.string.voice_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(s(R.string.voice_notice), fontSize = 12.sp)
        Button(
          onClick = {
            if (listening) {
              recognizer?.cancel()
              listening = false
            } else if (
              ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            )
              listen()
            else permission.launch(Manifest.permission.RECORD_AUDIO)
          },
          enabled = recognizer != null,
        ) {
          Icon(Icons.Outlined.Mic, null)
          Text(s(if (listening) R.string.listening else R.string.tap_to_speak))
        }
        if (recognizer == null || failure) Text(s(R.string.voice_unavailable), fontSize = 12.sp)
        Field(s(R.string.voice_command), text) { text = it }
        Text(s(R.string.voice_examples), fontSize = 12.sp)
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          val intent = VoiceParser.parse(text)
          if (intent == VoiceIntent.Unknown) failure = true else onIntent(intent)
        },
        enabled = text.isNotBlank(),
      ) {
        Text(s(R.string.review_command))
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(s(R.string.close)) } },
  )
}
