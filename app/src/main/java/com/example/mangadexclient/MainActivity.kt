package com.example.mangadexclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.mangadexclient.ui.theme.MangadexClientTheme

class MainActivity : ComponentActivity() {
    private val mangaViewModel by viewModels<MangaViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mangaViewModel.getMangaList(this) // start async calls to APIs

        setContent {
            MangadexClientApp()
        }
    }

    @Composable
    fun MangadexClientApp() {
        val mangaList: List<Manga> by mangaViewModel.mangaList.observeAsState(listOf())
        MangadexClientTheme {
            Surface(color = MaterialTheme.colors.background) {
                if (mangaList.isEmpty()) {
                    LoadingScreen()
                } else {
                    MangaCardList(mangaList = mangaList)
                }
            }
        }
    }

}