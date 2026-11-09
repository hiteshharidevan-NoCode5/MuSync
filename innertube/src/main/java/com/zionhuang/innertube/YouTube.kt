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
import com.zionhuang.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
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
import com.zionhuang.innertube.pages.SearchContinuationPage
import com.zionhuang.innertube.pages.SearchResult
import com.zionhuang.innertube.pages.SearchSummary
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
        SearchSummaryPage(
            summaries = response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
                ?.mapNotNull { it.musicCardShelfRenderer ?: it.musicShelfRenderer }
                ?.mapNotNull(SearchSummary.Companion::fromMusicShelfRenderer)
                .orEmpty()
        )
    }

    suspend fun search(query: String, filter: SearchFilter): Result<SearchResult> = runCatching {
        val response = innerTube.search(WEB_REMIX, query, filter.value).body<SearchResponse>()
        SearchResult(
            items = response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer?.contents
                ?.mapNotNull { it.musicResponsiveListItemRenderer }
                ?.mapNotNull(SearchResult.Companion::fromMusicResponsiveListItemRenderer)
                .orEmpty(),
            continuation = response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer?.continuations
                ?.getContinuation()
        )
    }

    suspend fun searchContinuation(continuation: String): Result<SearchContinuationPage> = runCatching {
        val response = innerTube.search(WEB_REMIX, continuation = continuation).body<SearchResponse>()
        SearchContinuationPage(
            items = response.continuationContents?.musicShelfContinuation?.contents
                ?.mapNotNull { it.musicResponsiveListItemRenderer }
                ?.mapNotNull(SearchResult.Companion::fromMusicResponsiveListItemRenderer)
                .orEmpty(),
            continuation = response.continuationContents?.musicShelfContinuation?.continuations
                ?.getContinuation()
        )
    }

    suspend fun searchSuggestions(query: String): Result<SearchSuggestions> = runCatching {
        val response = innerTube.getSearchSuggestions(WEB_REMIX, query).body<GetSearchSuggestionsResponse>()
        SearchSuggestions(
            queries = response.contents?.firstOrNull()?.searchSuggestionsSectionRenderer?.contents
                ?.mapNotNull { it.searchSuggestionRenderer?.suggestion?.runs?.firstOrNull()?.text }
                .orEmpty(),
            recommendedItems = response.contents?.getOrNull(1)?.searchSuggestionsSectionRenderer?.contents
                ?.mapNotNull { it.musicResponsiveListItemRenderer }
                ?.mapNotNull(SearchSuggestions.Companion::fromMusicResponsiveListItemRenderer)
                .orEmpty()
        )
    }

    suspend fun artist(browseId: String): Result<ArtistPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId).body<BrowseResponse>()
        val header = response.header?.musicImmersiveHeaderRenderer
            ?: response.header?.musicVisualHeaderRenderer
        ArtistPage(
            artist = ArtistItem(
                id = browseId,
                name = header?.title?.runs?.firstOrNull()?.text.orEmpty(),
                thumbnail = header?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
            ),
            sections = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
                ?.mapNotNull { it.musicCarouselShelfRenderer }
                ?.mapNotNull(ArtistPage.Companion::fromMusicCarouselShelfRenderer)
                .orEmpty(),
            description = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
                ?.find { it.musicDescriptionShelfRenderer != null }
                ?.musicDescriptionShelfRenderer?.description?.runs?.firstOrNull()?.text
        )
    }

    suspend fun artistItems(endpoint: BrowseEndpoint): Result<ArtistItemsPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<BrowseResponse>()
        ArtistItemsPage(
            title = response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text.orEmpty(),
            items = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer?.contents
                ?.mapNotNull { it.musicResponsiveListItemRenderer }
                ?.mapNotNull(ArtistItemsPage.Companion::fromMusicResponsiveListItemRenderer)
                ?: response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.gridRenderer?.items
                    ?.mapNotNull { it.musicTwoRowItemRenderer }
                    ?.mapNotNull(ArtistItemsPage.Companion::fromMusicTwoRowItemRenderer)
                    .orEmpty(),
            continuation = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer?.continuations
                ?.getContinuation()
                ?: response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.gridRenderer?.continuations
                    ?.getContinuation()
        )
    }

    suspend fun artistItemsContinuation(continuation: String): Result<ArtistItemsContinuationPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, continuation = continuation).body<BrowseResponse>()
        ArtistItemsContinuationPage(
            items = response.continuationContents?.musicShelfContinuation?.contents
                ?.mapNotNull { it.musicResponsiveListItemRenderer }
                ?.mapNotNull(ArtistItemsPage.Companion::fromMusicResponsiveListItemRenderer)
                ?: response.continuationContents?.gridContinuation?.items
                    ?.mapNotNull { it.musicTwoRowItemRenderer }
                    ?.mapNotNull(ArtistItemsPage.Companion::fromMusicTwoRowItemRenderer)
                    .orEmpty(),
            continuation = response.continuationContents?.musicShelfContinuation?.continuations
                ?.getContinuation()
                ?: response.continuationContents?.gridContinuation?.continuations
                    ?.getContinuation()
        )
    }

    suspend fun album(browseId: String): Result<AlbumPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId).body<BrowseResponse>()
        AlbumPage(
            album = AlbumItem(
                browseId = browseId,
                playlistId = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer?.contents?.firstOrNull()?.musicResponsiveListItemRenderer?.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.playlistId
                    ?: response.header?.musicDetailHeaderRenderer?.menu?.menuRenderer?.topLevelButtons?.firstOrNull()?.likeButtonRenderer?.target?.playlistId.orEmpty(),
                title = response.header?.musicDetailHeaderRenderer?.title?.runs?.firstOrNull()?.text.orEmpty(),
                artists = response.header?.musicDetailHeaderRenderer?.subtitle?.runs?.splitBySeparator()?.firstOrNull()?.map {
                    Artist(
                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                        name = it.text
                    )
                }.orEmpty(),
                year = response.header?.musicDetailHeaderRenderer?.subtitle?.runs?.splitBySeparator()?.getOrNull(1)?.firstOrNull()?.text?.toIntOrNull(),
                thumbnail = response.header?.musicDetailHeaderRenderer?.thumbnail?.croppedSquareThumbnailRenderer?.getThumbnailUrl().orEmpty()
            ),
            songs = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer?.contents
                ?.mapNotNull { it.musicResponsiveListItemRenderer }
                ?.mapNotNull(AlbumPage.Companion::fromMusicResponsiveListItemRenderer)
                .orEmpty()
        )
    }

    suspend fun playlist(playlistId: String): Result<PlaylistPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, "VL$playlistId").body<BrowseResponse>()
        PlaylistPage(
            playlist = PlaylistItem(
                id = playlistId,
                title = response.header?.musicDetailHeaderRenderer?.title?.runs?.firstOrNull()?.text
                    ?: response.header?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicDetailHeaderRenderer?.title?.runs?.firstOrNull()?.text.orEmpty(),
                author = response.header?.musicDetailHeaderRenderer?.subtitle?.runs?.splitBySeparator()?.firstOrNull()?.firstOrNull()?.let {
                    Artist(
                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                        name = it.text
                    )
                } ?: response.header?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicDetailHeaderRenderer?.subtitle?.runs?.splitBySeparator()?.firstOrNull()?.firstOrNull()?.let {
                    Artist(
                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                        name = it.text
                    )
                },
                songCountText = response.header?.musicDetailHeaderRenderer?.secondSubtitle?.runs?.firstOrNull()?.text
                    ?: response.header?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicDetailHeaderRenderer?.secondSubtitle?.runs?.firstOrNull()?.text,
                thumbnail = response.header?.musicDetailHeaderRenderer?.thumbnail?.croppedSquareThumbnailRenderer?.getThumbnailUrl()
                    ?: response.header?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicDetailHeaderRenderer?.thumbnail?.croppedSquareThumbnailRenderer?.getThumbnailUrl().orEmpty(),
                playEndpoint = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicPlaylistShelfRenderer?.contents?.firstOrNull()?.musicResponsiveListItemRenderer?.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint
            ),
            songs = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicPlaylistShelfRenderer?.contents
                ?.mapNotNull { it.musicResponsiveListItemRenderer }
                ?.mapNotNull(PlaylistPage.Companion::fromMusicResponsiveListItemRenderer)
                .orEmpty(),
            continuation = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicPlaylistShelfRenderer?.continuations
                ?.getContinuation()
        )
    }

    suspend fun playlistContinuation(continuation: String): Result<PlaylistContinuationPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, continuation = continuation).body<BrowseResponse>()
        PlaylistContinuationPage(
            songs = response.continuationContents?.musicPlaylistShelfContinuation?.contents
                ?.mapNotNull { it.musicResponsiveListItemRenderer }
                ?.mapNotNull(PlaylistPage.Companion::fromMusicResponsiveListItemRenderer)
                .orEmpty(),
            continuation = response.continuationContents?.musicPlaylistShelfContinuation?.continuations
                ?.getContinuation()
        )
    }

    suspend fun explore(): Result<ExplorePage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, "FEmusic_explore").body<BrowseResponse>()
        ExplorePage(
            newReleaseAlbums = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
                ?.find { it.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.moreContentButton?.navigationEndpoint?.browseEndpoint?.browseId == "FEmusic_new_releases_albums" }
                ?.musicCarouselShelfRenderer?.contents
                ?.mapNotNull { it.musicTwoRowItemRenderer }
                ?.mapNotNull(NewReleaseAlbumPage.Companion::fromMusicTwoRowItemRenderer)
                .orEmpty(),
            moodAndGenres = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
                ?.find { it.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.moreContentButton?.navigationEndpoint?.browseEndpoint?.browseId == "FEmusic_moods_and_genres" }
                ?.musicCarouselShelfRenderer?.contents
                ?.mapNotNull { it.musicNavigationButtonRenderer }
                ?.mapNotNull(MoodAndGenres.Companion::fromMusicNavigationButtonRenderer)
                .orEmpty()
        )
    }

    suspend fun newReleaseAlbums(): Result<List<AlbumItem>> = runCatching {
        val response = innerTube.browse(WEB_REMIX, "FEmusic_new_releases_albums").body<BrowseResponse>()
        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.gridRenderer?.items
            ?.mapNotNull { it.musicTwoRowItemRenderer }
            ?.mapNotNull(NewReleaseAlbumPage.Companion::fromMusicTwoRowItemRenderer)
            .orEmpty()
    }

    suspend fun moodAndGenres(): Result<List<MoodAndGenres>> = runCatching {
        val response = innerTube.browse(WEB_REMIX, "FEmusic_moods_and_genres").body<BrowseResponse>()
        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
            ?.mapNotNull { it.gridRenderer }
            ?.flatMap { it.items }
            ?.mapNotNull { it.musicNavigationButtonRenderer }
            ?.mapNotNull(MoodAndGenres.Companion::fromMusicNavigationButtonRenderer)
            .orEmpty()
    }

    suspend fun browseResult(endpoint: BrowseEndpoint): Result<BrowseResult> = runCatching {
        val response = innerTube.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<BrowseResponse>()
        BrowseResult(
            title = response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text.orEmpty(),
            items = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.gridRenderer?.items
                ?.mapNotNull { it.musicTwoRowItemRenderer }
                ?.mapNotNull(BrowseResult.Companion::fromMusicTwoRowItemRenderer)
                .orEmpty()
        )
    }

    suspend fun related(endpoint: BrowseEndpoint): Result<RelatedPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, endpoint.browseId).body<BrowseResponse>()
        val songs = mutableListOf<SongItem>()
        val albums = mutableListOf<AlbumItem>()
        val artists = mutableListOf<ArtistItem>()
        val playlists = mutableListOf<PlaylistItem>()
        response.contents?.sectionListRenderer?.contents?.forEach { section ->
            section.musicCarouselShelfRenderer?.contents?.forEach { item ->
                when {
                    item.musicResponsiveListItemRenderer != null -> {
                        item.musicResponsiveListItemRenderer.let {
                            if (it.navigationEndpoint?.watchEndpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType == MUSIC_VIDEO_TYPE_ATV) {
                                songs.add(
                                    SongItem(
                                        id = it.navigationEndpoint.watchEndpoint.videoId,
                                        title = it.flexColumns.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text.orEmpty(),
                                        artists = it.flexColumns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.splitBySeparator()?.firstOrNull()?.map { run ->
                                            Artist(
                                                id = run.navigationEndpoint?.browseEndpoint?.browseId,
                                                name = run.text
                                            )
                                        }.orEmpty(),
                                        album = it.flexColumns.getOrNull(2)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.let { run ->
                                            AlbumItem(
                                                browseId = run.navigationEndpoint?.browseEndpoint?.browseId.orEmpty(),
                                                playlistId = "",
                                                title = run.text
                                            )
                                        },
                                        duration = null,
                                        thumbnail = it.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl().orEmpty(),
                                        endpoint = it.navigationEndpoint.watchEndpoint
                                    )
                                )
                            }
                        }
                    }
                    item.musicTwoRowItemRenderer != null -> {
                        item.musicTwoRowItemRenderer.let {
                            when (it.navigationEndpoint.browseEndpoint?.browseId?.substring(0, 2)) {
                                "MP" -> albums.add(
                                    AlbumItem(
                                        browseId = it.navigationEndpoint.browseEndpoint.browseId,
                                        playlistId = "",
                                        title = it.title.runs.firstOrNull()?.text.orEmpty(),
                                        artists = it.subtitle?.runs?.splitBySeparator()?.getOrNull(1)?.map { run ->
                                            Artist(
                                                id = run.navigationEndpoint?.browseEndpoint?.browseId,
                                                name = run.text
                                            )
                                        }.orEmpty(),
                                        year = it.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                                        thumbnail = it.thumbnailRenderer.musicThumbnailRenderer.getThumbnailUrl()
                                    )
                                )
                                "UC" -> artists.add(
                                    ArtistItem(
                                        id = it.navigationEndpoint.browseEndpoint.browseId,
                                        name = it.title.runs.firstOrNull()?.text.orEmpty(),
                                        thumbnail = it.thumbnailRenderer.musicThumbnailRenderer.getThumbnailUrl()
                                    )
                                )
                                "VL" -> playlists.add(
                                    PlaylistItem(
                                        id = it.navigationEndpoint.browseEndpoint.browseId.removePrefix("VL"),
                                        title = it.title.runs.firstOrNull()?.text.orEmpty(),
                                        author = it.subtitle?.runs?.splitBySeparator()?.getOrNull(1)?.firstOrNull()?.let { run ->
                                            Artist(
                                                id = run.navigationEndpoint?.browseEndpoint?.browseId,
                                                name = run.text
                                            )
                                        },
                                        songCountText = it.subtitle?.runs?.splitBySeparator()?.getOrNull(2)?.firstOrNull()?.text,
                                        thumbnail = it.thumbnailRenderer.musicThumbnailRenderer.getThumbnailUrl(),
                                        playEndpoint = null
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        RelatedPage(
            songs = songs,
            albums = albums,
            artists = artists,
            playlists = playlists
        )
    }

    suspend fun queue(videoIds: List<String>?, playlistId: String? = null): Result<List<SongItem>> = runCatching {
        val response = innerTube.getQueue(WEB_REMIX, videoIds, playlistId).body<GetQueueResponse>()
        response.queueDatas
            ?.mapNotNull { it.content.playlistPanelVideoRenderer }
            ?.map(NextPage.Companion::fromPlaylistPanelVideoRenderer)
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
                ?.let(NextPage.Companion::fromPlaylistPanelVideoRenderer)
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
            continuation = response.continuationContents?.playlistPanelContinuation?.continuations?.getContinuation(),
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
