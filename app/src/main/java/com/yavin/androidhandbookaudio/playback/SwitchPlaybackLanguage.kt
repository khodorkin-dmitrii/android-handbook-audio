package com.yavin.androidhandbookaudio.playback

import com.yavin.androidhandbookaudio.domain.model.TimedTranscript
import com.yavin.androidhandbookaudio.domain.repository.TranscriptRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class SwitchPlaybackLanguage @Inject constructor(
    private val playbackController: PlaybackController,
    private val transcriptRepository: TranscriptRepository,
) {
    suspend operator fun invoke(
        language: String,
        loadedCurrentTranscript: TimedTranscript? = null,
    ) {
        val initialState = playbackController.state.value
        val trackId = initialState.currentTrackId ?: return
        val currentLanguage = initialState.currentLanguage ?: return
        val normalizedLanguage = language.trim().lowercase()
        if (normalizedLanguage == currentLanguage) return
        val targetRendition = playbackController.getCurrentTrackRendition(normalizedLanguage)
            ?: return

        val targetPositionMs = try {
            resolveTargetPosition(
                playbackState = initialState,
                targetRendition = targetRendition,
                loadedCurrentTranscript = loadedCurrentTranscript,
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            0L
        }

        val latestState = playbackController.state.value
        if (latestState.currentTrackId != trackId || latestState.currentLanguage != currentLanguage) {
            return
        }
        playbackController.switchLanguage(normalizedLanguage, targetPositionMs)
    }

    private suspend fun resolveTargetPosition(
        playbackState: PlaybackState,
        targetRendition: PlaybackRenditionMetadata,
        loadedCurrentTranscript: TimedTranscript?,
    ): Long {
        val currentUrl = playbackState.timedTranscriptUrl.validValue() ?: return 0
        val currentFormat = playbackState.timedTranscriptFormat.supportedSrtFormat() ?: return 0
        val targetUrl = targetRendition.timedTranscriptUrl.validValue() ?: return 0
        val targetFormat = targetRendition.timedTranscriptFormat.supportedSrtFormat() ?: return 0

        val currentTranscript = loadedCurrentTranscript
            ?: transcriptRepository.getTimedTranscript(currentUrl, currentFormat)
        val targetTranscript = transcriptRepository.getTimedTranscript(targetUrl, targetFormat)
        return findEquivalentCueStartMs(
            currentTranscript = currentTranscript,
            targetTranscript = targetTranscript,
            currentPositionMs = playbackState.positionMs,
        ) ?: 0
    }
}

internal fun findEquivalentCueStartMs(
    currentTranscript: TimedTranscript,
    targetTranscript: TimedTranscript,
    currentPositionMs: Long,
): Long? {
    if (currentTranscript.segments.isEmpty() ||
        currentTranscript.segments.size != targetTranscript.segments.size
    ) {
        return null
    }
    val cueIndex = currentTranscript.segments
        .indexOfLast { segment -> segment.startMs <= currentPositionMs }
        .takeIf { it >= 0 }
        ?: return null
    return targetTranscript.segments[cueIndex].startMs
}

private fun String?.validValue(): String? = this?.trim()?.takeIf(String::isNotEmpty)

private fun String?.supportedSrtFormat(): String? =
    validValue()?.lowercase()?.takeIf { it == "srt" }
