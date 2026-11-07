package com.example.claimit.domain.repository

import com.example.claimit.domain.model.ItemCategory
import com.example.claimit.domain.model.ItemPost
import com.example.claimit.domain.model.ItemType
import com.example.claimit.domain.model.PostStatus
import kotlinx.coroutines.flow.Flow

interface ItemRepository {
    suspend fun createItemPost(post: ItemPost): Result<String>
    suspend fun updateItemPost(post: ItemPost): Result<Unit>
    suspend fun getItemPostById(id: String): ItemPost?
    fun getRecentItemPosts(
        type: ItemType? = null,
        category: ItemCategory? = null,
        searchQuery: String = "",
        status: PostStatus? = PostStatus.ACTIVE,
        organizationId: String = "lpu_main_campus"
    ): Flow<List<ItemPost>>
    fun getPostsByUser(userId: String): Flow<List<ItemPost>>
    suspend fun updatePostStatus(id: String, status: PostStatus): Boolean
    suspend fun deletePost(id: String): Boolean
    suspend fun toggleSavePost(userId: String, postId: String): Boolean
    fun getSavedPosts(userId: String): Flow<List<ItemPost>>
}
