package com.example.mangadexclient

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject
import java.lang.Exception
import java.util.concurrent.ConcurrentHashMap


data class Manga(
    val id: String,
    val title: String,
    val description: String,
    val author: String = "",
    val coverUrl: String = "")

data class MangaRelationshipIds(
    val authorId: String,
    val coverArtId: String
)

const val coverUrlApiUrl = "https://uploads.mangadex.org/covers/"
const val coverApiUrl = "https://api.mangadex.org/cover/"
const val authorApiUrl = "https://api.mangadex.org/author"
const val mangaApiUrl = "https://api.mangadex.org/manga"

class MangaListViewModel: ViewModel() {

    // missing either author, coverUrl or both on initial manga obj from manga list API
    private var _incompleteMangas = ConcurrentHashMap<String, Pair<Manga, MangaRelationshipIds>>(10)
    private var _seenMangas = mutableSetOf<String>()
    private var _mangaList = MutableLiveData(listOf<Manga>())
    val mangaList: LiveData<List<Manga>> = _mangaList

    private fun onMangaListResponse(response: JSONObject, context: Context) {
        val dataArray = response.getJSONArray("data")
        for (i in 0 until dataArray.length()) {
            val currentObject = dataArray.getJSONObject(i)
            val parsedManga = parseManga(currentObject)
            if (parsedManga != null) {
                val mangaRelationships = parseMangaRelationships(currentObject)
                _incompleteMangas[parsedManga.id] = Pair(parsedManga, mangaRelationships)
                getCoverArtUrl(parsedManga.id, context)
                getAuthor(parsedManga.id, context)
            }
        }
    }

    fun getMangaList(context: Context,
                     language: String = "en",
                     limit: Int = 20,
    ) {
        val offset = _mangaList.value!!.size
        val url = "$mangaApiUrl?availableTranslatedLanguage[]=$language&offset=$offset&limit=$limit"
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response -> onMangaListResponse(response, context)},
            { error ->
                run {
                    println("Error while retrieving manga list with url:$url")
                    print(error)
                }
            }
        )
        VolleyHttpClientSingleton.getInstance(context).addToRequestQueue(jsonObjectRequest)
    }

    private fun parseManga(rawData: JSONObject): Manga? {
        return try {
            val id = rawData.getString("id")
            val attributes = rawData.getJSONObject("attributes")
            val title = attributes.getJSONObject("title").getString("en")
            val description = attributes.getJSONObject("description").getString("en")
            Manga(id = id, title = title, description = description)
        } catch (exception: Exception) {
            // TODO do something smarter than this
            println(exception)
            null
        }
    }

    private fun parseMangaRelationships(rawData: JSONObject): MangaRelationshipIds {
        val relationships = rawData.getJSONArray("relationships")
        var authorId = ""
        var coverArtId = ""
        for (i in 0 until relationships.length()) {
            val relationship = relationships.getJSONObject(i)
            if (relationship.getString("type").equals("author")) {
                authorId = relationship.getString("id")
            } else if (relationship.getString("type").equals("cover_art")) {
                coverArtId = relationship.getString("id")
            }
        }

        if (authorId.isEmpty() || coverArtId.isEmpty()) {
            throw Exception("Author id or cover art id is missing")
        }

        return MangaRelationshipIds(authorId = authorId, coverArtId = coverArtId)
    }

    private fun mangaObjectComplete(manga: Manga): Boolean {
        return manga.coverUrl.isNotEmpty() && manga.author.isNotEmpty()
    }

    private fun getCoverUrl(mangaId: String, fileName: String): String {
        val smallThumbnailExtension = ".256.jpg" // 256px wide thumbnail

        return "$coverUrlApiUrl$mangaId/$fileName$smallThumbnailExtension"
    }

    private fun onCoverArtUrlApiResponse(response: JSONObject, mangaId: String) {
        val fileName = response.getJSONObject("data")
            .getJSONObject("attributes")
            .getString("fileName")

        val incompleteManga = _incompleteMangas[mangaId]!!.first

        _incompleteMangas[mangaId] = Pair(
            first = incompleteManga.copy(coverUrl = getCoverUrl(incompleteManga.id, fileName)),
            second = _incompleteMangas[mangaId]!!.second
        )

        // race condition between cover API and author API
        if (mangaObjectComplete(_incompleteMangas[mangaId]!!.first)) {
            updateCompleteMangaObjectList(mangaId)
        }
    }

    private fun getCoverArtUrl(mangaId: String, context: Context) {
        val coverArtId = _incompleteMangas[mangaId]!!.second.coverArtId
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, coverApiUrl + coverArtId, null,
            { response -> onCoverArtUrlApiResponse(response, mangaId) },
            { error ->
                run {
                    println("Error while retrieving covert art with url:$coverApiUrl")
                    print(error)
                }
            }
        )
        VolleyHttpClientSingleton.getInstance(context).addToRequestQueue(jsonObjectRequest)
    }

    private fun onAuthorApiResponse(response: JSONObject, mangaId: String) {
        val authorName = (response.getJSONArray("data")[0] as JSONObject)
            .getJSONObject("attributes")
            .getString("name")

        val incompleteManga = _incompleteMangas[mangaId]!!.first

        _incompleteMangas[mangaId] = Pair(
            first = incompleteManga.copy(author = authorName),
            second = _incompleteMangas[mangaId]!!.second
        )

        // race condition between cover API and author API
        if (mangaObjectComplete(_incompleteMangas[mangaId]!!.first)) {
            updateCompleteMangaObjectList(mangaId)
        }
    }

    private fun updateCompleteMangaObjectList(mangaId: String) {
        val completedMangaObject = _incompleteMangas[mangaId]!!.first
        _incompleteMangas.remove(mangaId)
        if (!_seenMangas.contains(completedMangaObject.id)) {
            _mangaList.value = _mangaList.value!! + listOf(completedMangaObject)
            _seenMangas.add(completedMangaObject.id)
        }
        /*
            Else if the API returns a new item at the top, because we can't know about it, we will
            have duplicates in the next list query with the offset.
            We don't want duplicate, do nothing in that case
         */
    }

    private fun getAuthor(mangaId: String, context: Context) {
        val authorId = _incompleteMangas[mangaId]!!.second.authorId
        val url = "$authorApiUrl?ids[]=$authorId"
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response -> onAuthorApiResponse(response, mangaId) },
            { error ->
                run {
                    println("Error while retrieving author with url:$url")
                    print(error)
                }
            }
        )
        VolleyHttpClientSingleton.getInstance(context).addToRequestQueue(jsonObjectRequest)
    }

}