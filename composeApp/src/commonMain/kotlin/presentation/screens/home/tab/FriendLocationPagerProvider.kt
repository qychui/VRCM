package io.github.vrcmteam.vrcm.presentation.screens.home.tab

import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.vrcmteam.vrcm.network.api.attributes.IUser
import io.github.vrcmteam.vrcm.network.api.attributes.LocationType
import io.github.vrcmteam.vrcm.network.api.attributes.UserStatus
import io.github.vrcmteam.vrcm.network.api.friends.date.FriendData
import io.github.vrcmteam.vrcm.presentation.compoments.AImage
import io.github.vrcmteam.vrcm.presentation.compoments.RefreshBox
import io.github.vrcmteam.vrcm.presentation.compoments.UserStateIcon
import io.github.vrcmteam.vrcm.presentation.extensions.createFailureCallbackDoNavigation
import io.github.vrcmteam.vrcm.presentation.extensions.currentNavigator
import io.github.vrcmteam.vrcm.presentation.extensions.getInsetPadding
import io.github.vrcmteam.vrcm.presentation.screens.auth.AuthAnimeScreen
import io.github.vrcmteam.vrcm.presentation.screens.home.data.FriendLocation
import io.github.vrcmteam.vrcm.presentation.screens.profile.UserProfileScreen
import io.github.vrcmteam.vrcm.presentation.screens.profile.data.UserProfileVO
import io.github.vrcmteam.vrcm.presentation.screens.world.WorldScreen
import io.github.vrcmteam.vrcm.presentation.supports.ListPagerProvider
import org.koin.compose.koinInject
import org.koin.core.parameter.parameterSetOf

object FriendLocationPagerProvider : ListPagerProvider {

    override val index: Int
        get() = 0
    override val title: String
        get() = "Location"

    override val icon: Painter
        @Composable  get() = rememberVectorPainter(image = Icons.Rounded.Explore)

    @Composable
    override fun createPager(lazyListState: LazyListState):@Composable () -> Unit {
        val isRefreshing = rememberSaveable { mutableStateOf(true) }
        val callbackDoNavigation = createFailureCallbackDoNavigation {
            // 如果报错跳转登录，并重制刷新标记
            isRefreshing.value = true
            AuthAnimeScreen(false)
        }
        val friendLocationPagerModel: FriendLocationPagerModel = koinInject {
            parameterSetOf(callbackDoNavigation)
        }


        return remember {
            {
                FriendLocationPager(
                    friendLocationPagerModel = friendLocationPagerModel,
                    isRefreshing = isRefreshing.value,
                    lazyListState = lazyListState
                ) {
                    isRefreshing.value = false
                    friendLocationPagerModel.refreshFriendLocation()
                }
            }
        }
    }

}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun  FriendLocationPager(
    friendLocationPagerModel: FriendLocationPagerModel,
    isRefreshing:Boolean,
    lazyListState: LazyListState = rememberLazyListState(),
    doRefresh: suspend () -> Unit
) {
    RefreshBox(
        isStartRefresh = isRefreshing,
        doRefresh = doRefresh
    ) {
        val currentNavigator = currentNavigator

        val friendLocationMap = friendLocationPagerModel.friendLocationMap
        val offlineFriendLocation = friendLocationMap[LocationType.Offline]?.get(0)
        val privateFriendLocation = friendLocationMap[LocationType.Private]?.get(0)
        val travelingFriendLocation = friendLocationMap[LocationType.Traveling]?.get(0)
        val instanceFriendLocations = friendLocationMap[LocationType.Instance]
        val onClickUserIcon = { user: IUser ->
            if (currentNavigator.size <= 1) currentNavigator push UserProfileScreen(UserProfileVO(user))
        }
        // 如果没有底部系统手势条，默认12dp
        val bottomPadding = getInsetPadding(12, WindowInsets::getBottom) + 86.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(
                start = 6.dp,
                top = 6.dp,
                end = 6.dp,
                bottom = bottomPadding
            )
        ) {

            item(key = LocationType.Offline) {
                SingleLocationCard(
                    offlineFriendLocation,
                    "Active on the Website",
                    onClickUserIcon
                )
            }

            item(key = LocationType.Private) {
                SingleLocationCard(
                    privateFriendLocation,
                    "Friends in Private Worlds",
                    onClickUserIcon
                )
            }

            item(key = LocationType.Traveling) {
                SingleLocationCard(
                    travelingFriendLocation,
                    "Friends is Traveling",
                    onClickUserIcon
                )
            }

            if (!instanceFriendLocations.isNullOrEmpty()) {
                item(key = LocationType.Instance) {
                    Text(
                        text = "by Location",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                items(instanceFriendLocations, key = { it.location }) { locations ->
                    LocationCard(locations) {
                        UserIconsRow(locations.friends, onClickUserIcon)
                    }
                }
            }

        }
    }
}
@Composable
private fun SingleLocationCard(
    friendLocations: FriendLocation?,
    text: String,
    onClickUserIcon: (IUser) -> Unit
) {
    if (friendLocations == null) return
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
    )
    Spacer(modifier = Modifier.height(6.dp))
    UserIconsRow(friendLocations.friends, onClickUserIcon)
}

@Composable
private fun UserIconsRow(
    friends: MutableList<MutableState<FriendData>>,
    onClickUserIcon: (IUser) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(friends, key = { it.value.id }) {
            LocationFriend(
                it.value.iconUrl,
                it.value.displayName,
                it.value.status
            ) { onClickUserIcon(it.value) }
        }
    }
}

@Composable
private fun LocationFriend(
    iconUrl: String,
    name: String,
    userStatus: UserStatus,
    onClickUserIcon: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(60.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClickUserIcon),
        verticalArrangement = Arrangement.Center
    ) {
        UserStateIcon(
            modifier = Modifier.fillMaxSize(),
            iconUrl = iconUrl,
            userStatus = userStatus
        )
        Text(
            modifier = Modifier.fillMaxSize(),
            text = name,
            maxLines = 1,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun LocationCard(location: FriendLocation, content: @Composable () -> Unit) {
    val instants by location.instants
    val currentNavigator = currentNavigator

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier
                    .height(80.dp)
                    .fillMaxWidth()
            ) {
                AImage(
                    modifier = Modifier
                        .width(120.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { currentNavigator.push(WorldScreen()) },
                    imageData = instants.worldImageUrl,
                    contentDescription = "WorldImage"
                )
                Column(
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        text = instants.worldName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                    Row(
                        modifier = Modifier
                            .height(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AImage(
                            modifier = Modifier
                                .size(15.dp)
                                .align(Alignment.CenterVertically)
                                .clip(CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                            imageData = instants.regionIconUrl
                        )
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 6.dp),
                            text = instants.accessType,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 6.dp),
                            text = instants.userCount,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Icon(
                            modifier = Modifier
                                .size(15.dp),
                            imageVector = Icons.Rounded.Person,
                            contentDescription = "PersonCount"
                        )
                    }
                }
            }
            content()
        }
    }
}