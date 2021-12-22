package com.example.mangadexclient

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import com.example.mangadexclient.ui.theme.MangadexClientTheme

val mockedManga = Manga(
    "fakeId",
    "The Way of the House Husband",
    "Oono Kousuke",
    "Immortal Tatsu is an ex-yakuza who’ s given up violence for making an honest " +
            "man of himself - but is it still possible for a devoted stay-at-home husband to" +
            " get into a few scrapes?"
)

@Composable
fun MangaCardList(mangaList: List<Manga>, onLoadMore : () -> Unit) {
    val listState = rememberLazyListState()

    LazyColumn(state = listState) {
        items(mangaList) { manga ->
            MangaCard(manga)
            Divider()
        }
    }

    // This will start loading more when reaches at total - 5 items
    // Sweet spot should be list api limit / 2
    listState.OnBottomReached(buffer = 10) {
        onLoadMore()
    }
}

@Composable
fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Loading mangaList...")
        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "loading")
    }
}

@Composable
fun MangaCardList2(mangaList: State<List<Manga>>) {
    LazyColumn {
        items(mangaList.value) { manga ->
            MangaCard(manga)
            Divider()
        }
    }
}

@Composable
fun MangaCard(manga: Manga) {
    Row(modifier = Modifier.padding(all = 8.dp)) {
        MangaCardThumbnail(manga.coverUrl)
        MangaCardText(manga)
    }
}

@Composable
fun MangaCardText(manga: Manga) {
    Column {
        Text(
            text = manga.title,
            style = MaterialTheme.typography.h1,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = manga.author,
            fontStyle = FontStyle.Italic,
            fontSize = 14.sp,
            style = MaterialTheme.typography.h2
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = manga.description,
            fontSize = 12.sp,
            style = MaterialTheme.typography.body2,
            maxLines = 7,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MangaCardThumbnail(coverUrl: String) {
    Image(
        painter = rememberImagePainter(coverUrl),
        contentDescription = "Manga cover",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(180.dp)
            .padding(end = 8.dp)
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun DefaultPreview() {
    val mockedList = listOf(mockedManga, mockedManga)
    MangadexClientTheme {
        Surface(color = MaterialTheme.colors.background) {
            MangaCardList(mockedList) { }
        }
    }
}