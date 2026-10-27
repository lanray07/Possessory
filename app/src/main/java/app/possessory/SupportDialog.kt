package app.possessory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource as s
import androidx.compose.ui.unit.dp
import com.google.gson.JsonParser
import java.util.Locale

@Composable
fun SupportDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val values =
        remember(context) {
            val tag = AppLanguage.selected(context).ifBlank { Locale.getDefault().toLanguageTag() }
            val locale =
                AppLanguage.targets.firstOrNull { it.first == tag }?.first
                    ?: AppLanguage.targets
                        .firstOrNull { it.first.substringBefore('-') == tag.substringBefore('-') }
                        ?.first
                    ?: "en-GB"
            runCatching { context.assets.open("support/$locale.json") }
                .getOrElse { context.assets.open("support/en-GB.json") }
                .bufferedReader()
                .use { JsonParser.parseReader(it).asJsonObject }
        }
    val sections =
        linkedMapOf(
            "getting_started" to R.string.welcome,
            "where_is_my_data" to R.string.privacy,
            "receipts" to R.string.receipts_documents,
            "backup" to R.string.backup_restore,
            "restore" to R.string.restore,
            "reminders" to R.string.reminders,
            "voice" to R.string.voice,
            "reports" to R.string.reports,
            "premium" to R.string.premium,
            "warranties" to R.string.warranties,
            "delete" to R.string.delete_all,
            "app_lock" to R.string.app_lock,
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(values.get("title")?.asString ?: "Possessory") },
        text = {
            Column(
                Modifier.heightIn(max = 550.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                sections.forEach { (key, label) ->
                    values.get(key)?.asString?.let {
                        Text(s(label), style = MaterialTheme.typography.titleSmall)
                        Text(it)
                    }
                }
                Text("O. Bankole · banksmi@mail.com")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(s(R.string.close)) } },
    )
}
