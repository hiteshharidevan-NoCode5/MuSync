package com.zionhuang.innertube

import com.zionhuang.innertube.models.AccountInfo
import com.zionhuang.innertube.models.AlbumItem
import com.zionhuang.innertube.models.Artist
import com.zionhuang.innertube.models.ArtistItem
import com.zionhuang.innertube.models.BrowseEndpoint
import com.zionhuang.innertube.models.GridRenderer
import com.zionhuang.innertube.models.PlaylistItem
import com.zionhuang.innertube.models.SearchSuggestions
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.models.WatchEndpoint
import com.zionhuang.innertube.models.YouTubeClient.Companion.ANDROID_MUSIC
import com.zionhuang.innertube.models.YouTubeClient.Companion.ANDROID
import com.zionhuang.innertube.models.YouTubeClient.Companion.IOS
import com.zionhuang.innertube.models.YouTubeClient.Companion.TVHTML5
import com.zionhuang.innertube.models.YouTubeClient.Companion.WEB
import com.zionhuang.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.zionhuang.innertube.models.YouTubeLocale
import com.zionhuang.innertube.models.response.AccountMenuResponse
import com.zionhuang.innertube.models.response.BrowseResponse
import com.zionhuang.innertube.models.response.GetQueueResponse
import com.zionhuang.innertube.models.response.GetSearchSuggestionsResponse
import com.zionhuang.innertube.models.response.NextResponse
import com.zionhuang.innertube.models.response.PlayerResponse
import com.zionhuang.innertube.models.response.SearchResponse
import com.zionhuang.innertube.pages.AlbumPage
import com.zionhuang.innertube.pages.ArtistItemsContinuationPage
import com.zionhuang.innertube.pages.ArtistItemsPage
import com.zionhuang.innertube.pages.ArtistPage
import com.zionhuang.innertube.pages.BrowseResult
import com.zionhuang.innertube.pages.ExplorePage
import com.zionhuang.innertube.pages.MoodAndGenres
import com.zionhuang.innertube.pages.NewReleaseAlbumPage
import com.zionhuang.innertube.pages.NextPage
import com.zionhuang.innertube.pages.NextResult
import com.zionhuang.innertube.pages.PlaylistContinuationPage
import com.zionhuang.innertube.pages.PlaylistPage
import com.zionhuang.innertube.pages.RelatedPage
import com.zionhuang.innertube.pages.SearchPage
import com.zionhuang.innertube.pages.SearchSummaryPage
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import java.net.Proxy

object YouTube {
    private val innerTube = InnerTube()

    var locale: YouTubeLocale
        get() = innerTube.locale
        set(value) {
            innerTube.locale = value
        }

    var cookie: String?
        get() = innerTube.cookie
        set(value) {
            innerTube.cookie = value
        }

    var proxy: Proxy?
        get() = innerTube.proxy
        set(value) {
            innerTube.proxy = value
        }

    suspend fun searchSummary(query: String): Result<SearchSummaryPage> = runCatching {
        val response = innerTube.search(WEB_REMIX, query).body<SearchResponse>()
        SearchSummaryPage.fromSearchResponse(response)
    }

    suspend fun search(query: String, filter: SearchFilter): Result<SearchPage> = runCatching {
        val response = innerTube.search(WEB_REMIX, query, filter.value).body<SearchResponse>()
        SearchPage.fromSearchResponse(response)
    }

    suspend fun searchContinuation(continuation: String): Result<SearchPage> = runCatching {
        val response = innerTube.search(WEB_REMIX, continuation = continuation).body<SearchResponse>()
        SearchPage.fromSearchResponse(response)
    }

    suspend fun searchSuggestions(query: String): Result<SearchSuggestions> = runCatching {
        val response = innerTube.getSearchSuggestions(WEB_REMIX, query).body<GetSearchSuggestionsResponse>()
        SearchSuggestions(
            queries = response.contents?.firstOrNull()?.searchSuggestionsSectionRenderer?.contents
                ?.mapNotNull { it.searchSuggestionRenderer?.suggestion?.runs?.firstOrNull()?.text }
                .orEmpty(),
            recommendedItems = response.contents?.getOrNull(1)?.searchSuggestionsSectionRenderer?.contents
                ?.mapNotNull { it.musicResponsiveListItemRenderer }
                ?.mapNotNull(SearchSuggestions::fromMusicResponsiveListItemRenderer)
                .orEmpty()
        )
    }

    suspend fun artist(browseId: String): Result<ArtistPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId).body<BrowseResponse>()
        ArtistPage.fromBrowseResponse(response, browseId)
    }

    suspend fun artistItems(endpoint: BrowseEndpoint): Result<ArtistItemsPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<BrowseResponse>()
        ArtistItemsPage(
            title = response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text.orEmpty(),
            items = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer?.contents
                ?.mapNotNull { it.musicResponsiveListItemRenderer }
                ?.mapNotNull(ArtistItemsPage::fromMusicResponsiveListItemRenderer)
                ?: response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.gridRenderer?.items
                    ?.mapNotNull { it.musicTwoRowItemRenderer }
                    ?.mapNotNull(ArtistItemsPage::fromMusicTwoRowItemRenderer)
                    .orEmpty(),
            continuation = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer?.continuations
                ?.firstOrNull()?.nextContinuationData?.continuation
                ?: response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.gridRenderer?.continuations
                    ?.firstOrNull()?.nextContinuationData?.continuation
        )
    }

    suspend fun artistItemsContinuation(continuation: String): Result<ArtistItemsContinuationPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, continuation = continuation).body<BrowseResponse>()
        ArtistItemsContinuationPage(
            items = response.continuationContents?.musicShelfContinuation?.contents
                ?.mapNotNull { it.musicResponsiveListItemRenderer }
                ?.mapNotNull(ArtistItemsPage::fromMusicResponsiveListItemRenderer)
                ?: response.continuationContents?.gridContinuation?.items
                    ?.mapNotNull { it.musicTwoRowItemRenderer }
                    ?.mapNotNull(ArtistItemsPage::fromMusicTwoRowItemRenderer)
                    .orEmpty(),
            continuation = response.continuationContents?.musicShelfContinuation?.continuations
                ?.firstOrNull()?.nextContinuationData?.continuation
                ?: response.continuationContents?.gridContinuation?.continuations
                    ?.firstOrNull()?.nextContinuationData?.continuation
        )
    }

    suspend fun album(browseId: String): Result<AlbumPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId).body<BrowseResponse>()
        AlbumPage.fromBrowseResponse(response, browseId)
    }

    suspend fun playlist(playlistId: String): Result<PlaylistPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, "VL$playlistId").body<BrowseResponse>()
        PlaylistPage.fromBrowseResponse(response, playlistId)
    }

    suspend fun playlistContinuation(continuation: String): Result<PlaylistContinuationPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, continuation = continuation).body<BrowseResponse>()
        PlaylistContinuationPage(
            songs = response.continuationContents?.musicPlaylistShelfContinuation?.contents
                ?.mapNotNull { it.musicResponsiveListItemRenderer }
                ?.mapNotNull(PlaylistPage::fromMusicResponsiveListItemRenderer)
                .orEmpty(),
            continuation = response.continuationContents?.musicPlaylistShelfContinuation?.continuations
                ?.firstOrNull()?.nextContinuationData?.continuation
        )
    }

    suspend fun explore(): Result<ExplorePage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, "FEmusic_explore").body<BrowseResponse>()
        ExplorePage.fromBrowseResponse(response)
    }

    suspend fun newReleaseAlbums(): Result<List<AlbumItem>> = runCatching {
        val response = innerTube.browse(WEB_REMIX, "FEmusic_new_releases_albums").body<BrowseResponse>()
        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.gridRenderer?.items
            ?.mapNotNull { it.musicTwoRowItemRenderer }
            ?.mapNotNull(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer)
            .orEmpty()
    }

    suspend fun moodAndGenres(): Result<List<MoodAndGenres>> = runCatching {
        val response = innerTube.browse(WEB_REMIX, "FEmusic_moods_and_genres").body<BrowseResponse>()
        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
            ?.mapNotNull { it.gridRenderer }
            ?.flatMap { it.items }
            ?.mapNotNull { it.musicNavigationButtonRenderer }
            ?.mapNotNull(MoodAndGenres::fromMusicNavigationButtonRenderer)
            .orEmpty()
    }

    suspend fun browseResult(endpoint: BrowseEndpoint): Result<BrowseResult> = runCatching {
        val response = innerTube.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<BrowseResponse>()
        BrowseResult(
            title = response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text.orEmpty(),
            items = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.gridRenderer?.items
                ?.mapNotNull { it.musicTwoRowItemRenderer }
                ?.mapNotNull(BrowseResult::fromMusicTwoRowItemRenderer)
                .orEmpty()
        )
    }

    suspend fun related(endpoint: BrowseEndpoint): Result<RelatedPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, endpoint.browseId).body<BrowseResponse>()
        RelatedPage.fromBrowseResponse(response)
    }

    suspend fun queue(videoIds: List<String>?, playlistId: String? = null): Result<List<SongItem>> = runCatching {
        val response = innerTube.getQueue(WEB_REMIX, videoIds, playlistId).body<GetQueueResponse>()
        response.queueDatas
            ?.mapNotNull { it.content.playlistPanelVideoRenderer }
            ?.map(NextPage::fromPlaylistPanelVideoRenderer)
            .orEmpty()
    }

    suspend fun likedPlaylists(): Result<List<PlaylistItem>> = runCatching {
        val response = innerTube.browse(
            client = WEB_REMIX,
            browseId = "FEmusic_liked_playlists",
            setLogin = true
        ).body<BrowseResponse>()
        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.gridRenderer?.items!!
            .drop(1)
            .mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
            .mapNotNull {
                ArtistItemsPage.fromMusicTwoRowItemRenderer(it) as? PlaylistItem
            }
    }

    suspend fun player(videoId: String, playlistId: String? = null): Result<PlayerResponse> = runCatching {
        var playerResponse: PlayerResponse
        if (this.cookie != null) {
            playerResponse = innerTube.player(ANDROID_MUSIC, videoId, playlistId).body<PlayerResponse>()
            if (playerResponse.playabilityStatus.status == "OK") {
                return@runCatching playerResponse
            }
        }

        playerResponse = innerTube.player(WEB_REMIX, videoId, playlistId).body<PlayerResponse>()
        if (playerResponse.playabilityStatus.status == "OK") {
            return@runCatching playerResponse
        }

        playerResponse = innerTube.player(IOS, videoId, playlistId).body<PlayerResponse>()
        if (playerResponse.playabilityStatus.status == "OK") {
            return@runCatching playerResponse
        }

        playerResponse = innerTube.player(TVHTML5, videoId, playlistId).body<PlayerResponse>()
        return@runCatching playerResponse
    }

    suspend fun next(endpoint: WatchEndpoint, continuation: String? = null): Result<NextResult> = runCatching {
        val response = innerTube.next(WEB_REMIX, endpoint.videoId, endpoint.playlistId, endpoint.playlistSetVideoId, endpoint.index, endpoint.params, continuation).body<NextResponse>()
        val title = response.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs[0].tabRenderer.content?.musicQueueRenderer?.header?.musicQueueHeaderRenderer?.subtitle?.runs?.firstOrNull()?.text
        val playlistPanelRenderer = response.continuationContents?.playlistPanelContinuation
            ?: response.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs[0].tabRenderer.content?.musicQueueRenderer?.content?.playlistPanelRenderer!!

        val items = playlistPanelRenderer.contents.mapNotNull { content ->
            content.playlistPanelVideoRenderer
                ?.let(NextPage::fromPlaylistPanelVideoRenderer)
                ?.let { it to content.playlistPanelVideoRenderer.selected }
        }
        val songs = items.map { it.first }
        val currentIndex = items.indexOfFirst { it.second }.takeIf { it != -1 }

        playlistPanelRenderer.contents.lastOrNull()?.automixPreviewVideoRenderer?.content?.automixPlaylistVideoRenderer?.navigationEndpoint?.watchPlaylistEndpoint?.let { watchPlaylistEndpoint ->
            return@runCatching next(watchPlaylistEndpoint).getOrThrow().let { result ->
                result.copy(
                    title = title,
                    items = songs + result.items,
                    lyricsEndpoint = response.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.getOrNull(1)?.tabRenderer?.endpoint?.browseEndpoint,
                    relatedEndpoint = response.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.getOrNull(2)?.tabRenderer?.endpoint?.browseEndpoint,
                    currentIndex = currentIndex,
                    endpoint = watchPlaylistEndpoint
                )
            }
        }

        NextResult(
            title = title,
            items = songs,
            currentIndex = currentIndex,
            lyricsEndpoint = response.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.getOrNull(1)?.tabRenderer?.endpoint?.browseEndpoint,
            relatedEndpoint = response.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.getOrNull(2)?.tabRenderer?.endpoint?.browseEndpoint,
            continuation = response.continuationContents?.playlistPanelContinuation?.continuations?.firstOrNull()?.nextContinuationData?.continuation,
            endpoint = endpoint
        )
    }

    suspend fun lyrics(endpoint: BrowseEndpoint): Result<String?> = runCatching {
        val response = innerTube.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<BrowseResponse>()
        response.contents?.sectionListRenderer?.contents?.firstOrNull()?.musicDescriptionShelfRenderer?.description?.runs?.firstOrNull()?.text
    }

    suspend fun transcript(videoId: String): Result<String?> = runCatching {
        val response = innerTube.getTranscript(WEB, videoId).bodyAsText()
        Regex("<text start=\"(.*?)\" dur=\"(.*?)\">(.*?)</text>").findAll(response)
            .joinToString(separator = "\n") {
                it.groupValues[3]
            }
    }

    suspend fun accountInfo(): Result<AccountInfo> = runCatching {
        innerTube.accountMenu(WEB_REMIX).body<AccountMenuResponse>()
            .actions[0].openPopupAction.popup.multiPageMenuRenderer
            .header?.activeAccountHeaderRenderer
            ?.toAccountInfo()!!
    }

    @JvmInline
    value class SearchFilter(val value: String) {
        companion object {
            val FILTER_SONG = SearchFilter("EgWKAQIIAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D")
            val FILTER_VIDEO = SearchFilter("EgWKAQIQAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D")
            val FILTER_ALBUM = SearchFilter("EgWKAQIYAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D")
            val FILTER_ARTIST = SearchFilter("EgWKAQIgAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D")
            val FILTER_FEATURED_PLAYLIST = SearchFilter("EgeKAQQoADgBahAQAxAEEAkQChAFEBEQEBAV")
            val FILTER_COMMUNITY_PLAYLIST = SearchFilter("EgeKAQQoAEABahAQAxAEEAkQChAFEBEQEBAV")
        }
    }

    const val MAX_GET_QUEUE_SIZE = 1000

    private const val VISITOR_DATA_PREFIX = "Cgt"

    const val DEFAULT_VISITOR_DATA = "CgtsZG1ySnZiQWtSbyiMjuGSBg%3D%3D"
}
