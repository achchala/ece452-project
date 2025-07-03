package com.example.evenly

import com.example.evenly.api.group.models.Group

// Simple in-memory storage for groups (temporary until backend is ready)
object GroupStorage {
    private val groups = mutableListOf<Group>()
    
    fun addGroup(group: Group) {
        groups.add(group)
    }
    
    fun getGroups(): List<Group> = groups.toList()
    
    fun updateGroup(updatedGroup: Group) {
        val index = groups.indexOfFirst { it.id == updatedGroup.id }
        if (index != -1) {
            groups[index] = updatedGroup
        }
    }
    
    fun clear() {
        groups.clear()
    }
} 