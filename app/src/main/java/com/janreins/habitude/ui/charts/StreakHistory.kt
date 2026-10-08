package com.janreins.habitude.ui.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.janreins.habitude.domain.Run
import java.time.format.DateTimeFormatter

/** The most recent runs, newest first, as horizontal bars with their length written beside them. */
@Composable
fun StreakHistory(
    runs: List<Run>,
    color: Color,
    modifier: Modifier = Modifier,
    limit: Int = 6,
) {
    val recent = runs.takeLast(limit).reversed()
    val longest = runs.maxOfOrNull { it.length } ?: 1
    val dateFormat = DateTimeFormatter.ofPattern("d MMM")

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        recent.forEach { run ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (run.ongoing) "Now" else run.end.format(dateFormat),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(56.dp),
                )
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.82f * run.length / longest)
                            .height(12.dp)
                            .background(
                                if (run.ongoing) color else color.copy(alpha = 0.55f),
                                RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                            ),
                    )
                    Text(
                        if (run.length == longest) "${run.length} ★" else "${run.length}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
