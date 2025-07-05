package com.example.evenly

import android.content.Context
import android.content.SharedPreferences
import com.example.evenly.api.group.models.Group
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// DEPRECATED: This local storage is no longer used as groups are now managed by the backend API
// This file is kept for reference and cleanup purposes only
object GroupStorage {
    private const val PREFS_NAME = "group_storage"
    private const val GROUPS_KEY = "groups"
    private var prefs: SharedPreferences? = null
    private val gson = Gson()

    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun addGroup(group: Group) {
        val groups = getGroups().toMutableList()
        groups.add(group)
        saveGroups(groups)
    }

    fun getGroups(): List<Group> {
        val json = prefs?.getString(GROUPS_KEY, "[]") ?: "[]"
        val type = object : TypeToken<List<Group>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun updateGroup(updatedGroup: Group) {
        val groups = getGroups().toMutableList()
        val index = groups.indexOfFirst { it.id == updatedGroup.id }
        if (index != -1) {
            groups[index] = updatedGroup
            saveGroups(groups)
        }
    }

    fun clear() {
        prefs?.edit()?.remove(GROUPS_KEY)?.apply()
    }

    // Clear all local groups - call this to remove any existing local groups
    fun clearAllLocalGroups() {
        clear()
    }

    private fun saveGroups(groups: List<Group>) {
        val json = gson.toJson(groups)
        prefs?.edit()?.putString(GROUPS_KEY, json)?.apply()
    }
}
