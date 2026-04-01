package eu.kanade.tachiyomi.extension.en.nhentai

import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.await
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class Nhentai : HttpSource() {

    override val name = "Nhentai"
    override val baseUrl = "https://nhentai.net"
    override val lang = "en"
    override val supportsLatest = true

    override val client: OkHttpClient = network.client.newBuilder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Referer", baseUrl)
                .build()

            val response = chain.proceed(request)

            if (response.code == 429) {
                Thread.sleep(1500)
                chain.proceed(request)
            } else response
        }
        .build()

    override fun headersBuilder(): Headers.Builder {
        return super.headersBuilder()
            .add("User-Agent", "Mozilla/5.0")
            .add("Referer", baseUrl)
    }

    override fun popularMangaRequest(page: Int) =
        GET("$baseUrl/api/galleries/all?page=$page", headers)

    override fun popularMangaParse(response: okhttp3.Response) =
        parseGalleryList(response)

    override fun latestUpdatesRequest(page: Int) =
        GET("$baseUrl/api/galleries/all?page=$page", headers)

    override fun latestUpdatesParse(response: okhttp3.Response) =
        parseGalleryList(response)

    private fun parseGalleryList(response: okhttp3.Response): List<SManga> {
        val json = JSONObject(response.body!!.string())
        val result = mutableListOf<SManga>()

        val array = json.getJSONArray("result")
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val manga = SManga.create()
            manga.title = obj.getString("title")
            manga.url = "/g/${obj.getInt("id")}"
            manga.thumbnail_url = "https://t.nhentai.net/galleries/${obj.getInt("media_id")}/cover.jpg"
            result.add(manga)
        }
        return result
    }

    override fun mangaDetailsRequest(manga: SManga) =
        GET("$baseUrl/api/gallery/${manga.url.substringAfterLast("/")}", headers)

    override fun mangaDetailsParse(response: okhttp3.Response): SManga {
        val json = JSONObject(response.body!!.string())
        val manga = SManga.create()
        manga.title = json.getJSONObject("title").getString("pretty")
        return manga
    }

    override fun chapterListParse(response: okhttp3.Response): List<SChapter> {
        val json = JSONObject(response.body!!.string())
        val chapter = SChapter.create()
        chapter.name = "Chapter"
        chapter.url = "/g/${json.getInt("id")}"
        return listOf(chapter)
    }

    override fun pageListParse(response: okhttp3.Response): List<Page> {
        val json = JSONObject(response.body!!.string())
        val pages = mutableListOf<Page>()
        val mediaId = json.getString("media_id")
        val images = json.getJSONObject("images").getJSONArray("pages")

        for (i in 0 until images.length()) {
            val ext = images.getJSONObject(i).getString("t")
            val url = "https://i.nhentai.net/galleries/$mediaId/${i + 1}.$ext"
            pages.add(Page(i, "", url))
        }
        return pages
    }

    override fun imageUrlParse(response: okhttp3.Response) = ""
}
