package com.cineby

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class CinebyProvider : MainAPI() {
    override var name = "Cineby"
    override var mainUrl = "https://www.cineby.sc"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override var lang = "en"

    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "$mainUrl/home" to "Home",
        "$mainUrl/movie" to "Movies",
        "$mainUrl/tv" to "TV Series",
        "$mainUrl/trending" to "Trending",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + if (page > 1) "?page=$page" else "").document
        val home = document.select(".flw-item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".film-name")?.text()?.trim() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href") ?: return null)
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src"))

        return if (href.contains("/tv/")) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search/$query"
        val document = app.get(url).document
        return document.select(".flw-item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst(".film-name")?.text()?.trim() ?: document.selectFirst("h2.heading-name")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst(".film-poster img")?.attr("src"))
        val plot = document.selectFirst(".description, .detail-resume")?.text()?.trim()
        val year = document.selectFirst(".fdi-item")?.text()?.trim()?.toIntOrNull()
        val type = if (url.contains("/tv/")) TvType.TvSeries else TvType.Movie

        return if (type == TvType.TvSeries) {
            val episodes = document.select(".nav-item .episode-item").map {
                val href = fixUrl(it.attr("href"))
                val name = it.text().trim()
                val season = it.attr("data-season").toIntOrNull()
                val episode = it.attr("data-number").toIntOrNull()
                newEpisode(href) {
                    this.name = name
                    this.season = season
                    this.episode = episode
                }
            }
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = plot
                this.year = year
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = plot
                this.year = year
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val iframe = document.selectFirst("iframe")?.attr("src") ?: return false
        
        return loadExtractor(iframe, subtitleCallback, callback)
    }
}
