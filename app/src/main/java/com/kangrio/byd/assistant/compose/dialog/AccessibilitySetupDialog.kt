package com.kangrio.byd.assistant.compose.dialog

import android.view.KeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kangrio.byd.assistant.util.Preferences

@Composable
fun AccessibilitySetupDialog(
    onDismiss: () -> Unit,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    triggerKeyCode: Int,
    listening: Boolean,
    onListen: () -> Unit,
    onCancelListen: () -> Unit,
    blockOriginal: Boolean,
    onBlockOriginalChange: (Boolean) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Button Trigger") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable")
                    Switch(checked = enabled, onCheckedChange = onEnabledChange)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Trigger Button")
                        Text(
                            text = when {
                                listening -> "Press a button..."
                                triggerKeyCode == Preferences.KEYCODE_NONE -> "Not set"
                                else -> KeyEvent.keyCodeToString(triggerKeyCode)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = if (listening) onCancelListen else onListen,
                        enabled = enabled // capture needs the service running
                    ) {
                        Text(if (listening) "Cancel" else "Listen")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Block original action")
                    Switch(checked = blockOriginal, onCheckedChange = onBlockOriginalChange)
                }

                if (!enabled) {
                    Text(
                        "Enable the accessibility service first, otherwise buttons can't be captured or blocked.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}