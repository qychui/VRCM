package io.github.vrcmteam.vrcm.presentation.screens.home.tab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import io.github.vrcmteam.vrcm.network.api.attributes.IUser
import io.github.vrcmteam.vrcm.network.api.friends.date.FriendData
import io.github.vrcmteam.vrcm.presentation.compoments.RefreshBox
import io.github.vrcmteam.vrcm.presentation.compoments.UserStateIcon
import io.github.vrcmteam.vrcm.presentation.extensions.createFailureCallbackDoNavigation
import io.github.vrcmteam.vrcm.presentation.extensions.currentNavigator
import io.github.vrcmteam.vrcm.presentation.extensions.getInsetPadding
import io.github.vrcmteam.vrcm.presentation.screens.auth.AuthAnimeScreen
import io.github.vrcmteam.vrcm.presentation.screens.profile.UserProfileScreen
import io.github.vrcmteam.vrcm.presentation.screens.profile.data.UserProfileVO
import io.github.vrcmteam.vrcm.presentation.supports.ListPagerProvider
import io.github.vrcmteam.vrcm.presentation.theme.GameColor
import org.koin.compose.koinInject
import org.koin.core.parameter.parameterSetOf

object FriendListPagerProvider : ListPagerProvider {

    override val index: Int
        get() = 1
    override val title: String
        get() = "Friend"

    override val icon: Painter
        @Composable  get() = rememberVectorPainter(image = Icons.Rounded.Person)

    @Composable
    override fun createPager(lazyListState: LazyListState):@Composable ()->Unit {
        val isRefreshing = rememberSaveable { mutableStateOf(true) }
        val callbackDoNavigation = createFailureCallbackDoNavigation {
            // 如果报错跳转登录，并重制刷新标记
            isRefreshing.value = true
            AuthAnimeScreen(false)
        }
        val friendListPagerModel: FriendListPagerModel = koinInject {
            parameterSetOf(callbackDoNavigation)
        }

        return remember {
            {
                FriendListPager(
                    friendListPagerModel = friendListPagerModel,
                    isRefreshing = isRefreshing.value,
                    state = lazyListState,
                ) {
                    isRefreshing.value = false
                    friendListPagerModel.refreshFriendList()
                }
            }
        }
    }

}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendListPager(
    friendListPagerModel: FriendListPagerModel,
    isRefreshing: Boolean,
    state: LazyListState = rememberLazyListState(),
    doRefresh: suspend () -> Unit
) {
    RefreshBox(
        isStartRefresh = isRefreshing,
        doRefresh = doRefresh

    ) {
        val currentNavigator = currentNavigator
        val toProfile = { user: IUser ->
            if (currentNavigator.size <= 1) currentNavigator push UserProfileScreen(UserProfileVO(user))
        }
        val friendList = friendListPagerModel.friendList
        // 如果没有底部系统手势条，默认12dp
        val bottomPadding = getInsetPadding(12, WindowInsets::getBottom) + 86.dp
        LazyColumn(
            modifier =  Modifier.fillMaxSize(),
            state = state,
            contentPadding = PaddingValues(start = 6.dp, top = 6.dp, end = 6.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(friendList, key = { it.id }) {
                FriendListItem(it, toProfile)
            }
        }
    }
}

@Composable
fun FriendListItem(friend: FriendData, toProfile: (FriendData) -> Unit) {
    Card (
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable { toProfile(friend) }
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            UserStateIcon(
                modifier = Modifier
                    .height(56.dp)
                    .clip(CircleShape),
                iconUrl = friend.iconUrl,
                userStatus = friend.status
            )
            Column{
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier
                            .size(16.dp),
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = "TrustRankIcon",
                        tint = GameColor.Rank.fromValue(friend.trustRank)
                    )
                    Text(
                        text = friend.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                }

                Text(
                    modifier = Modifier.alpha(0.6f),
                    text = friend.statusDescription.ifBlank { friend.status.value },
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
            Spacer(Modifier.weight(1f))
        }
    }
}