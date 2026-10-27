package app.possessory

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource as s

object RegionalPreferences {
    var dateStyle = "regional"
}

@Composable
fun RegionalSettings() {
    val context = LocalContext.current
    val store = context.getSharedPreferences("regional", android.content.Context.MODE_PRIVATE)
    var style by remember { mutableStateOf(store.getString("dateStyle", "regional") ?: "regional") }
    Choice(
        s(R.string.date_format),
        style,
        listOf(
            "regional" to s(R.string.regional_date),
            "iso" to "YYYY-MM-DD",
            "short" to s(R.string.short_date),
        ),
        {
            style = it
            RegionalPreferences.dateStyle = it
            store.edit().putString("dateStyle", it).apply()
            (context as? android.app.Activity)?.recreate()
        },
    )
}
