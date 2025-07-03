package com.example.evenly.api.group.models

import com.google.gson.annotations.SerializedName

data class CreateGroupRequest(
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("creator_id")
    val creatorId: Int
)

data class CreateGroupResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("group")
    val group: Group
)

data class Group(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("creator_id")
    val creatorId: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("members")
    val members: List<GroupMember> = emptyList()
)

data class GroupMember(
    @SerializedName("id")
    val id: Int,
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("group_id")
    val groupId: Int,
    @SerializedName("joined_at")
    val joinedAt: String,
    @SerializedName("user")
    val user: User? = null
)

data class User(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String
) 