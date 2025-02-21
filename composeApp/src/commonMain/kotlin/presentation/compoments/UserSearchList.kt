package io.github.vrcmteam.vrcm.presentation.compoments

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.vrcmteam.vrcm.core.shared.SharedFlowCentre
import io.github.vrcmteam.vrcm.network.api.attributes.IUser
import io.github.vrcmteam.vrcm.network.api.attributes.UserStatus
import io.github.vrcmteam.vrcm.presentation.extensions.animateScrollToFirst
import io.github.vrcmteam.vrcm.presentation.extensions.currentNavigator
import io.github.vrcmteam.vrcm.presentation.extensions.getInsetPadding
import io.github.vrcmteam.vrcm.presentation.extensions.ignoredFormat
import io.github.vrcmteam.vrcm.presentation.screens.user.UserProfileScreen
import io.github.vrcmteam.vrcm.presentation.screens.user.data.UserProfileVo
import io.github.vrcmteam.vrcm.presentation.settings.locale.strings
import io.github.vrcmteam.vrcm.presentation.supports.AppIcons
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


@Composable
fun UserSearchList(
    key: String,
    isRefreshing: Boolean? = null,
    userListInit: (String) -> List<IUser> = { emptyList() },
    doRefresh: (suspend () -> Unit)? = null,
    onUpdateSearch: suspend (searchText: String, userList: MutableList<IUser>) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    var searchText by rememberSaveable(key) { mutableStateOf("") }
    val userList: MutableList<IUser> by remember { derivedStateOf { userListInit(searchText).toMutableStateList() } }
    LaunchedEffect(searchText) {
        onUpdateSearch(searchText, userList)
    }
    LaunchedEffect(key) {
        SharedFlowCentre.toPagerTop.collect {
            // 防止滑动时手动阻止滑动动画导致任务取消,监听失效的bug
            runCatching {
                lazyListState.animateScrollToFirst()
            }
        }
    }
    UserList(
        lazyListState,
        userList,
        isRefreshing,
        doRefresh
    ) {
        val focusManager = LocalFocusManager.current
        ITextField(
            modifier = Modifier.padding(horizontal = 16.dp),
            imageVector = AppIcons.Search,
            hintText = strings.fiendListPagerSearch,
            textValue = searchText,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            onValueChange = { searchText = it })
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserList(
    state: LazyListState,
    friendList: List<IUser>,
    isRefreshing: Boolean? = null,
    doRefresh: (suspend () -> Unit)? = null,
    searchBar: @Composable () -> Unit,
) {
    val topPadding = getInsetPadding(WindowInsets::getTop) + 80.dp
    val currentNavigator = currentNavigator
    val sharedSuffixKey = LocalSharedSuffixKey.current
    val toProfile = { user: IUser ->
        if (currentNavigator.size <= 1) {
            currentNavigator push UserProfileScreen(sharedSuffixKey, UserProfileVo(user))
        }
    }
    // 如果没有底部系统手势条，默认12dp
    val bottomPadding = getInsetPadding(12, WindowInsets::getBottom) + 80.dp

    val userListLazyColumn = @Composable {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state,
            contentPadding = PaddingValues(
                top = topPadding, bottom = bottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                searchBar()
            }
            items(friendList, key = { it.id }) {
                UserListItem(it, toProfile)
            }
        }
    }
    if (isRefreshing != null && doRefresh != null) {
        RefreshBox(
            refreshContainerOffsetY = topPadding, isRefreshing = isRefreshing, doRefresh = doRefresh
        ) {
            userListLazyColumn()
        }
    } else {
        userListLazyColumn()
    }


}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LazyItemScope.UserListItem(friend: IUser, toProfile: (IUser) -> Unit) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .padding(horizontal = 6.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable { toProfile(friend) }
            .animateItem(),
        leadingContent = {
            UserStateIcon(
                modifier = Modifier.sharedBoundsBy("${friend.id}UserIcon"),
                iconUrl = friend.iconUrl,
            )
        },
        headlineContent = {
            UserInfoRow(
                iconSize = 16.dp,
                style = MaterialTheme.typography.titleMedium,
                user = friend
            )
        },
        supportingContent = {
            UserStatusRow(
                iconSize = 8.dp,
                style = MaterialTheme.typography.bodyMedium,
                user = friend
            )
        },

        trailingContent = {
            // 离线好友最后登录时间
            // 例如: lastLogin = 2023-04-01T09:03:04.000Z
            val lastLoginStr = friend.lastLogin
            if (friend.status != UserStatus.Offline || lastLoginStr.isNullOrEmpty()) return@ListItem
            val lastLogin = remember { Instant.parse(lastLoginStr).toLocalDateTime(TimeZone.currentSystemDefault()) }
            Text(
                text = lastLogin.ignoredFormat, style = MaterialTheme.typography.labelSmall, maxLines = 1
            )
        },
    )
}


