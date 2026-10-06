package com.example.claimit.domain.repository

import com.example.claimit.domain.model.ItemPost
import com.example.claimit.domain.model.ItemType
import kotlinx.coroutines.flow.Flow

interface ItemRepository {
    suspend fun createItemPost(post: ItemPost): Result<String>
    suspend fun getItemPostById(id: String): ItemPost?
    fun getRecentItemPosts(type: ItemType? = null): Flow<List<ItemPost>>
    suspend fun updatePostStatus(id: String, status: com.example.claimit.domain.model.PostStatus): Boolean
}
