package com.janreins.habitude.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProgressScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Text("Progress", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(32.dp))
        EmptyStateCard(
            title = "Nothing to chart yet",
            body = "Heatmaps, streak history and weekly trends will show up once you've logged a few days.",
        )
    }
}
