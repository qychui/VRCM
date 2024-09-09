package io.github.vrcmteam.vrcm.network.api.groups

import io.github.vrcmteam.vrcm.network.api.attributes.GROUPS_API_PREFIX
import io.github.vrcmteam.vrcm.network.extensions.ifOKOrThrow
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import network.api.groups.data.GroupData

class GroupsApi(private val client: HttpClient) {

    suspend fun fetchGroup(groupId: String, includeRoles: Boolean = false, purpose: String = "other") =
        client.get("$GROUPS_API_PREFIX/$groupId?includeRoles=$includeRoles")
            .ifOKOrThrow { body<GroupData>() }

}