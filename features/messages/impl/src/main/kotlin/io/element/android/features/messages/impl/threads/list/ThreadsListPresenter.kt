/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.threads.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.room.JoinedRoom
import io.element.android.libraries.matrix.api.room.threads.ThreadListItem
import io.element.android.libraries.matrix.api.room.threads.ThreadsListService
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import timber.log.Timber

@Inject
class ThreadsListPresenter(
    private val room: JoinedRoom,
) : Presenter<ThreadsListState> {
    @Composable
    override fun present(): ThreadsListState {
        val coroutineScope = rememberCoroutineScope()
        var threadsListService by remember { mutableStateOf<ThreadsListService?>(null) }
        val threads by produceState(initialValue = persistentListOf(), key1 = threadsListService) {
            threadsListService?.subscribeToItemUpdates()
                ?.onStart { threadsListService?.paginate() }
                ?.collect {
                    Timber.d("Received thread list update with ${it.size} items")
                    value = it.toImmutableList()
                }
        }

        val roomInfo by room.roomInfoFlow.collectAsState()

        DisposableEffect(Unit) {
            coroutineScope.launch {
                threadsListService = room.threadListService()
                    .onSuccess { Timber.d("Loaded thread list service") }
                    .onFailure { Timber.e(it, "Boom") }
                    .getOrNull()
            }
            onDispose {
                threadsListService?.destroy()
            }
        }

        fun handleEvent(event: ThreadsListEvents) {}

        return ThreadsListState(
            threads = threads,
            roomId = room.roomId,
            roomName = roomInfo.name ?: room.roomId.value,
            roomAvatarUrl = roomInfo.avatarUrl,
            eventSink = ::handleEvent,
        )
    }
}

data class ThreadsListState(
    val roomId: RoomId,
    val roomName: String,
    val roomAvatarUrl: String?,
    val threads: ImmutableList<ThreadListItem>,
    val eventSink: (ThreadsListEvents) -> Unit,
)

sealed interface ThreadsListEvents
