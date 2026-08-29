package com.ravk24.ravmusic.ui.folders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ravk24.ravmusic.ui.components.PlaceholderList
import com.ravk24.ravmusic.ui.theme.RavMusicTheme

/** Folders tab (artboard 1c). Placeholder body until the library phase. */
@Composable
fun FoldersScreen(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("screen_folders"),
    ) {
        Text(
            text = "Folders",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 12.dp),
        )
        PlaceholderList(
            message = "Folder browsing arrives in the library phase. Songs will be grouped the way they are stored on your phone.",
            listState = listState,
            modifier = Modifier.testTag("folders_list"),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FoldersScreenPreview() {
    RavMusicTheme {
        FoldersScreen(listState = rememberLazyListState())
    }
}
