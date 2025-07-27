package com.example.evenly.api.group.models

import com.google.gson.annotations.SerializedName

data class CreateGroupRequest(
        @SerializedName("name") val name: String,
        @SerializedName("description") val description: String? = null,
        @SerializedName("firebaseId") val firebaseId: String
)

data class GetUserGroupsRequest(@SerializedName("firebaseId") val firebaseId: String)

data class CreateGroupResponse(
        @SerializedName("success") val success: Boolean,
        @SerializedName("message") val message: String,
        @SerializedName("group") val group: Group
)

data class Group(
        @SerializedName("id") val id: String,
        @SerializedName("name") val name: String,
        @SerializedName("description") val description: String?,
        @SerializedName("creator_id") val creatorId: String,
        @SerializedName("created_at") val createdAt: String,
        @SerializedName("members") val members: List<GroupMember> = emptyList()
)

data class GroupMember(
        @SerializedName("id") val id: String,
        @SerializedName("user_id") val userId: String,
        @SerializedName("group_id") val groupId: String,
        @SerializedName("joined_at") val joinedAt: String,
        @SerializedName("user") val user: User? = null
)

data class User(
        @SerializedName("id") val id: String,
        @SerializedName("name") val name: String,
        @SerializedName("email") val email: String
)

data class AddMemberRequest(
        @SerializedName("firebaseId") val firebaseId: String,
        @SerializedName("memberEmail") val memberEmail: String,
)

data class GroupNotificationRequest(
        @SerializedName("email") val email: String,
        @SerializedName("groupId") val groupId: String
)
