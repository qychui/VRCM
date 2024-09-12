package io.github.vrcmteam.vrcm.presentation.screens.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.vrcmteam.vrcm.core.extensions.pretty
import io.github.vrcmteam.vrcm.core.shared.SharedFlowCentre
import io.github.vrcmteam.vrcm.network.api.friends.FriendsApi
import io.github.vrcmteam.vrcm.network.api.notification.NotificationApi
import io.github.vrcmteam.vrcm.network.api.users.UsersApi
import io.github.vrcmteam.vrcm.network.api.users.data.UserData
import io.github.vrcmteam.vrcm.presentation.compoments.ToastText
import io.github.vrcmteam.vrcm.presentation.screens.profile.data.UserProfileVo
import io.github.vrcmteam.vrcm.service.AuthService
import io.ktor.client.call.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.koin.core.logger.Logger

class UserProfileScreenModel(
    private val authService: AuthService,
    private val usersApi: UsersApi,
    private val friendsApi: FriendsApi,
    private val notificationApi: NotificationApi,
    private val logger: Logger,
) : ScreenModel {

    private val _userState = mutableStateOf<UserProfileVo?>(null)
    val userState by _userState

    private val _userJson = mutableStateOf("")
    val userJson by _userJson

    fun initUserState(userProfileVO: UserProfileVo) {
        if (_userState.value == null) _userState.value = userProfileVO
    }

    fun refreshUser(userId: String) =
        screenModelScope.launch(Dispatchers.IO) {
            authService.reTryAuthCatching {
                usersApi.fetchUserResponse(userId)
            }.onFailure {
                logger.error(it.message.toString())
                SharedFlowCentre.toastText.emit(ToastText.Error(it.message.toString()))
            }.onSuccess {
                _userState.value = UserProfileVo(it.body<UserData>())
                _userJson.value = it.bodyAsText().pretty()
            }
        }

    suspend fun sendFriendRequest(userId: String, message: String): Boolean =
        friendAction(message) {
            friendsApi.sendFriendRequest(userId)
        }

    suspend fun deleteFriendRequest(userId: String, message: String): Boolean = friendAction(message) {
        friendsApi.deleteFriendRequest(userId)
    }

    suspend fun unfriend(userId: String, message: String): Boolean = friendAction(message) {
        friendsApi.unfriend(userId)
    }

    private suspend fun friendAction(message: String, action: suspend () -> Any): Boolean =
        screenModelScope.async(Dispatchers.IO) {
            authService.reTryAuthCatching {
                action()
            }.onFailure {
                logger.error(it.message.toString())
                SharedFlowCentre.toastText.emit(ToastText.Error(it.message.toString()))
            }.onSuccess {
                SharedFlowCentre.toastText.emit(ToastText.Success(message))
                _userState.value?.id?.also { refreshUser(it) }
            }.isSuccess
        }.await()

    suspend fun acceptFriendRequest(userId: String, message: String) = friendAction(message) {
        // 看看要不要加载大于 100 条的通知
        return@friendAction notificationApi.fetchNotificationsV2(
            hidden = true
        ).firstOrNull { it.senderUserId == userId }?.let {
            notificationApi.acceptFriendRequest(it.id).isSuccess
        } ?: error("Not found notification")

    }

}
