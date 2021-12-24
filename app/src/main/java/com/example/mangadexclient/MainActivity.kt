package com.example.mangadexclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.mangadexclient.ui.theme.MangadexClientTheme

class MainActivity : ComponentActivity() {
    private val mangaListViewModel by viewModels<MangaListViewModel>()

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mangaListViewModel.getMangaList(this) // start async calls to APIs

        setContent {
            MangadexClientApp()
        }
    }

    @ExperimentalAnimationApi
    @Composable
    fun MangadexClientApp() {
        val mangaList: List<Manga> by mangaListViewModel.mangaList.observeAsState(listOf())
        val applicationContext = this.applicationContext
        MangadexClientTheme {
            Scaffold(
                topBar = { TopAppBar(title = { Text("MangaDex") }) },
                content = {
                    Surface(color = MaterialTheme.colors.background) {
                        AnimatedVisibility(visible = mangaList.isEmpty()) {
                            LoadingScreen()
                        }

                        AnimatedVisibility(
                            visible = mangaList.isNotEmpty(),
                            enter = fadeIn() + slideInVertically()
                        ) {
                            MangaCardList(mangaList = mangaList, onLoadMore = {
                                mangaListViewModel.getMangaList(applicationContext)
                            })
                        }
                    }
                },
            )
        }
    }

}