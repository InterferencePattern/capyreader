package com.capyreader.app.ui.articles.audio.speech

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import com.capyreader.app.common.SPOKEN_ARTICLE_SCHEME
import java.io.IOException

/**
 * Rewrites a playlist item's synthetic `capytts://` URI into the POST that actually fetches the
 * Passage's audio, per ADR-0001. Anything else (an Audio Enclosure's real URL) passes through
 * unchanged.
 */
@UnstableApi
class SpeechDataSourceResolver : ResolvingDataSource.Resolver {
    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        if (dataSpec.uri.scheme != SPOKEN_ARTICLE_SCHEME) {
            return dataSpec
        }

        val request = SpeechPassageRegistry.resolve(dataSpec.uri.toString())
            ?: throw IOException("No pending speech request for ${dataSpec.uri}")

        return dataSpec.buildUpon()
            .setUri(Uri.parse(request.url))
            .setHttpMethod(DataSpec.HTTP_METHOD_POST)
            .setHttpBody(request.body)
            .setHttpRequestHeaders(request.headers)
            .build()
    }
}

/**
 * Lets a Passage be cached even though its length is unknown until it finishes downloading.
 *
 * Progressive playback asks for `FLAG_DONT_CACHE_IF_LENGTH_UNKNOWN`, and `CacheDataSink` honours
 * it by silently discarding every write -- so a Speech Provider streaming a Passage chunked would
 * be paid for again on every replay. The flag exists to avoid caching a stream that never ends;
 * a Passage does end, and its synthetic URI already hashes everything that determines the audio.
 *
 * Must sit above [androidx.media3.datasource.cache.CacheDataSource]: the sink reads these flags
 * when it opens, which is before [SpeechDataSourceResolver] rewrites anything.
 */
@UnstableApi
class SpeechCacheFlagResolver : ResolvingDataSource.Resolver {
    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        if (dataSpec.uri.scheme != SPOKEN_ARTICLE_SCHEME) {
            return dataSpec
        }

        return dataSpec.buildUpon()
            .setFlags(dataSpec.flags and DataSpec.FLAG_DONT_CACHE_IF_LENGTH_UNKNOWN.inv())
            .build()
    }
}
