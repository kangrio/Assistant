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

@Composable
fun AccessibilitySetupDialog(
    onDismiss: () -> Unit,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    triggerKeyCodes: List<Int>,
    listening: Boolean,
    onListen: () -> Unit,
    onDone: () -> Unit,
    blockOriginal: Boolean,
    onBlockOriginalChange: (Boolean) -> Unit,
    keyEventAction: Int,
    onKeyEventActionChange: (Int) -> Unit
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
                                listening && triggerKeyCodes.isEmpty() -> "Press a button..."
                                triggerKeyCodes.isEmpty() -> "Not set"
                                else -> KeyEvent.keyCodeToString(triggerKeyCodes.first())
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = if (listening) onDone else onListen,
                        enabled = enabled // capture needs the service running
                    ) {
                        Text(if (listening) "Done" else "Listen")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Key event action")
                    FilledTonalButton(
                        onClick = {
                            onKeyEventActionChange(if (keyEventAction == KeyEvent.ACTION_UP) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP)
                        },
                        enabled = enabled
                    ) {
                        Text(if (keyEventAction == KeyEvent.ACTION_UP) "Action Up" else "Action Down")
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