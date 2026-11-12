package com.kangrio.byd.assistant.compose.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kangrio.byd.assistant.data.AssistantApp
import com.kangrio.byd.assistant.ui.composable.AppIcon

@Composable
fun AssistantSelectorDialog(
    assistantApps: List<AssistantApp>,
    onDismiss: () -> Unit,
    onSelected: (AssistantApp) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Assistant App")
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                assistantApps.forEach { assistant ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelected(assistant)
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(
                            packageName = assistant.packageName,
                            modifier = Modifier.size(40.dp)
                        )

                        Spacer(Modifier.width(12.dp))

                        Text(assistant.name)
                    }
                }
            }
        },
        confirmButton = {}
    )
}