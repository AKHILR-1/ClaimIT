package com.example.claimit.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.claimit.core.dispatchers.CoroutineDispatchers
import com.example.claimit.domain.model.Building
import com.example.claimit.domain.model.CampusLocation
import com.example.claimit.domain.model.ItemCategory
import com.example.claimit.domain.model.ItemPost
import com.example.claimit.domain.model.ItemType
import com.example.claimit.domain.model.PhysicalAttributes
import com.example.claimit.domain.model.PostStatus
import com.example.claimit.domain.repository.ImageStorageRepository
import com.example.claimit.domain.repository.ImageUploadResult
import com.example.claimit.domain.repository.ItemRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ItemRepositoryImpl(
    private val dispatchers: CoroutineDispatchers,
    private val imageStorageRepository: ImageStorageRepository = ImageStorageRepositoryImpl(),
    private val realtimeDatabase: FirebaseDatabase? = try {
        FirebaseDatabase.getInstance("https://claimit-322b5-default-rtdb.asia-southeast1.firebasedatabase.app")
    } catch (_: Exception) {
        try { FirebaseDatabase.getInstance() } catch (_: Exception) { null }
    }
) : ItemRepository {

    private val TAG = "ItemRepository"
    private val postsStateFlow = MutableStateFlow<List<ItemPost>>(emptyList())
    private val savedPostsUserMap = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    init {
        listenToRealtimeDatabaseItems()
    }

    private fun listenToRealtimeDatabaseItems() {
        val rtdb = realtimeDatabase ?: return
        try {
            rtdb.reference.child("organizations").child("lpu_main_campus").child("items")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val rtdbPosts = mutableListOf<ItemPost>()
                            for (child in snapshot.children) {
                                try {
                                    val post = parseItemSnapshot(child) ?: continue
                                    rtdbPosts.add(post)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing RTDB item child: ${e.message}")
                                }
                            }
                            if (rtdbPosts.isNotEmpty()) {
                                updatePostsList(rtdbPosts)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "RTDB org items listener cancelled: ${error.message}")
                    }
                })

            rtdb.reference.child("items")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val rtdbPosts = mutableListOf<ItemPost>()
                            for (child in snapshot.children) {
                                try {
                                    val post = parseItemSnapshot(child) ?: continue
                                    rtdbPosts.add(post)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing RTDB global item child: ${e.message}")
                                }
                            }
                            if (rtdbPosts.isNotEmpty()) {
                                updatePostsList(rtdbPosts)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "RTDB global items listener cancelled: ${error.message}")
                    }
                })
        } catch (e: Exception) {
            Log.w(TAG, "Exception initializing RTDB item listener: ${e.message}")
        }
    }

    private fun parseItemSnapshot(snapshot: DataSnapshot): ItemPost? {
        val id = snapshot.key ?: snapshot.child("id").getValue(String::class.java) ?: snapshot.child("itemId").getValue(String::class.java) ?: return null
        val title = snapshot.child("title").getValue(String::class.java) ?: return null
        val typeStr = snapshot.child("type").getValue(String::class.java) ?: ItemType.LOST.name
        val categoryStr = snapshot.child("category").getValue(String::class.java) ?: ItemCategory.OTHER.name
        val statusStr = snapshot.child("status").getValue(String::class.java) ?: PostStatus.ACTIVE.name

        val type = try { ItemType.valueOf(typeStr) } catch (_: Exception) { ItemType.LOST }
        val category = try { ItemCategory.valueOf(categoryStr) } catch (_: Exception) { ItemCategory.OTHER }
        val status = try { PostStatus.valueOf(statusStr) } catch (_: Exception) { PostStatus.ACTIVE }

        val imageUrlsList = mutableListOf<String>()
        val imgArr = snapshot.child("imageUrls")
        if (imgArr.exists()) {
            for (img in imgArr.children) {
                img.getValue(String::class.java)?.let { imageUrlsList.add(it) }
            }
        }

        val imageFileIdsList = mutableListOf<String>()
        val fileIdArr = snapshot.child("imageFileIds")
        if (fileIdArr.exists()) {
            for (fid in fileIdArr.children) {
                fid.getValue(String::class.java)?.let { imageFileIdsList.add(it) }
            }
        }

        return ItemPost(
            id = id,
            trackingCode = snapshot.child("trackingCode").getValue(String::class.java) ?: "CLM-${id.take(6).uppercase()}",
            organizationId = snapshot.child("organizationId").getValue(String::class.java) ?: snapshot.child("collegeId").getValue(String::class.java) ?: "lpu_main_campus",
            userId = snapshot.child("userId").getValue(String::class.java) ?: snapshot.child("ownerUid").getValue(String::class.java) ?: "user_student_1",
            userDisplayName = snapshot.child("userDisplayName").getValue(String::class.java) ?: "Campus Member",
            title = title,
            description = snapshot.child("description").getValue(String::class.java) ?: "",
            type = type,
            category = category,
            location = CampusLocation(
                building = Building(
                    id = snapshot.child("buildingId").getValue(String::class.java) ?: "bld_15",
                    code = snapshot.child("buildingCode").getValue(String::class.java) ?: "15",
                    name = snapshot.child("buildingName").getValue(String::class.java) ?: "Campus Center",
                    zoneName = snapshot.child("zoneName").getValue(String::class.java) ?: "Central",
                    landmarkName = snapshot.child("buildingDesc").getValue(String::class.java) ?: ""
                ),
                floorWing = snapshot.child("floorWing").getValue(String::class.java) ?: "",
                microLocationDetails = snapshot.child("microLocation").getValue(String::class.java) ?: ""
            ),
            physicalAttributes = PhysicalAttributes(
                dominantColor = snapshot.child("dominantColor").getValue(String::class.java) ?: "",
                material = snapshot.child("material").getValue(String::class.java) ?: "",
                condition = snapshot.child("condition").getValue(String::class.java) ?: "",
                distinguishingFeatures = snapshot.child("distinguishingFeatures").getValue(String::class.java) ?: ""
            ),
            aiMetadata = null,
            acceptedTags = emptyList(),
            imageUrls = imageUrlsList,
            imageFileIds = imageFileIdsList,
            createdAtTimestamp = snapshot.child("createdAtTimestamp").getValue(Long::class.java)
                ?: snapshot.child("createdAt").getValue(Long::class.java)
                ?: System.currentTimeMillis(),
            status = status,
            finderOrOwnerContact = snapshot.child("contact").getValue(String::class.java) ?: "contact@campus.edu",
            actionTakenRemarks = snapshot.child("remarks").getValue(String::class.java) ?: ""
        )
    }

    private fun updatePostsList(newPosts: List<ItemPost>) {
        val current = postsStateFlow.value
        val combined = (newPosts + current).distinctBy { it.id }
        // Sort newest first
        val sorted = combined.sortedByDescending { it.createdAtTimestamp }
        postsStateFlow.value = sorted
    }

    override suspend fun uploadImage(bitmap: Bitmap): Result<ImageUploadResult> {
        return imageStorageRepository.uploadImage(bitmap)
    }

    override suspend fun createItemPost(post: ItemPost): Result<String> = withContext(dispatchers.io) {
        try {
            val itemMap = mapOf(
                "id" to post.id,
                "itemId" to post.id,
                "trackingCode" to post.trackingCode,
                "organizationId" to post.organizationId,
                "collegeId" to post.organizationId,
                "userId" to post.userId,
                "ownerUid" to post.userId,
                "userDisplayName" to post.userDisplayName,
                "title" to post.title,
                "description" to post.description,
                "type" to post.type.name,
                "category" to post.category.name,
                "buildingId" to post.location.building.id,
                "buildingCode" to post.location.building.code,
                "buildingName" to post.location.building.name,
                "zoneName" to post.location.building.zoneName,
                "buildingDesc" to post.location.building.landmarkName,
                "floorWing" to post.location.floorWing,
                "microLocation" to post.location.microLocationDetails,
                "dominantColor" to post.physicalAttributes.dominantColor,
                "material" to post.physicalAttributes.material,
                "condition" to post.physicalAttributes.condition,
                "distinguishingFeatures" to post.physicalAttributes.distinguishingFeatures,
                "acceptedTags" to post.acceptedTags,
                "imageUrls" to post.imageUrls,
                "imageFileIds" to post.imageFileIds,
                "status" to post.status.name,
                "contact" to post.finderOrOwnerContact,
                "remarks" to post.actionTakenRemarks,
                "createdAtTimestamp" to post.createdAtTimestamp,
                "createdAt" to ServerValue.TIMESTAMP,
                "updatedAt" to ServerValue.TIMESTAMP
            )

            val primaryPath = "organizations/${post.organizationId}/items/${post.id}"
            Log.i(TAG, "DIAGNOSTIC - RTDB Report Write Started: op=CREATE_REPORT, path=$primaryPath, uid=${post.userId}, collegeId=${post.organizationId}")

            val rtdb = realtimeDatabase
            if (rtdb != null) {
                try {
                    // Primary write: /organizations/{organizationId}/items/{itemId}
                    rtdb.reference.child("organizations")
                        .child(post.organizationId)
                        .child("items")
                        .child(post.id)
                        .setValue(itemMap)
                        .await()

                    Log.i(TAG, "DIAGNOSTIC - RTDB Primary Write Success: path=$primaryPath, uid=${post.userId}")
                } catch (e: Exception) {
                    Log.e(TAG, "DIAGNOSTIC - RTDB Primary Write Failed: path=$primaryPath, uid=${post.userId}, error=${e.message}", e)
                    throw e
                }

                // Secondary write: /items/{itemId}
                try {
                    rtdb.reference.child("items")
                        .child(post.id)
                        .setValue(itemMap)
                        .await()
                    Log.i(TAG, "DIAGNOSTIC - RTDB Secondary Write Success: path=items/${post.id}")
                } catch (e: Exception) {
                    Log.w(TAG, "DIAGNOSTIC - RTDB Secondary Write Warning: path=items/${post.id}, warning=${e.message}")
                }
            }

            val updated = postsStateFlow.value.toMutableList()
            updated.add(0, post)
            postsStateFlow.value = updated.sortedByDescending { it.createdAtTimestamp }
            Result.success(post.id)
        } catch (e: Exception) {
            Log.e(TAG, "Create item post failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateItemPost(post: ItemPost): Result<Unit> = withContext(dispatchers.io) {
        try {
            val itemMap = mapOf<String, Any>(
                "title" to post.title,
                "description" to post.description,
                "microLocation" to post.location.microLocationDetails,
                "updatedAt" to ServerValue.TIMESTAMP
            )

            realtimeDatabase?.let { rtdb ->
                try {
                    rtdb.reference.child("organizations")
                        .child(post.organizationId)
                        .child("items")
                        .child(post.id)
                        .updateChildren(itemMap)
                        .await()

                    rtdb.reference.child("items")
                        .child(post.id)
                        .updateChildren(itemMap)
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "RTDB update item ${post.id} error: ${e.message}")
                }
            }

            val updated = postsStateFlow.value.map {
                if (it.id == post.id) post else it
            }
            postsStateFlow.value = updated
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Update item post exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getItemPostById(id: String): ItemPost? = withContext(dispatchers.io) {
        postsStateFlow.value.firstOrNull { it.id == id }
    }

    override fun getRecentItemPosts(
        type: ItemType?,
        category: ItemCategory?,
        searchQuery: String,
        status: PostStatus?,
        organizationId: String
    ): Flow<List<ItemPost>> {
        return postsStateFlow.map { list ->
            list.filter { post ->
                val typeMatch = type == null || post.type == type
                val categoryMatch = category == null || post.category == category
                // Feed displays active reports (ACTIVE or CLAIM_PENDING or CLAIM_APPROVED), excludes REMOVED or ARCHIVED or REJECTED
                val statusMatch = if (status != null) {
                    post.status == status
                } else {
                    post.status == PostStatus.ACTIVE || post.status == PostStatus.CLAIM_PENDING || post.status == PostStatus.CLAIM_APPROVED
                }
                val orgMatch = post.organizationId == organizationId
                val queryMatch = searchQuery.isBlank() ||
                        post.title.contains(searchQuery, ignoreCase = true) ||
                        post.description.contains(searchQuery, ignoreCase = true) ||
                        post.location.building.name.contains(searchQuery, ignoreCase = true) ||
                        post.acceptedTags.any { it.contains(searchQuery, ignoreCase = true) }

                typeMatch && categoryMatch && statusMatch && orgMatch && queryMatch
            }
        }
    }

    override fun getPostsByUser(userId: String): Flow<List<ItemPost>> {
        return postsStateFlow.map { list ->
            list.filter { it.userId == userId }
        }
    }

    override suspend fun updatePostStatus(id: String, status: PostStatus): Boolean = withContext(dispatchers.io) {
        val updated = postsStateFlow.value.map {
            if (it.id == id) {
                val newPost = it.copy(status = status)
                realtimeDatabase?.let { rtdb ->
                    try {
                        rtdb.reference.child("organizations")
                            .child(it.organizationId)
                            .child("items")
                            .child(id)
                            .child("status")
                            .setValue(status.name)

                        rtdb.reference.child("items")
                            .child(id)
                            .child("status")
                            .setValue(status.name)
                    } catch (e: Exception) {
                        Log.w(TAG, "RTDB status update error: ${e.message}")
                    }
                }
                newPost
            } else it
        }
        postsStateFlow.value = updated
        true
    }

    override suspend fun deletePost(id: String): Boolean = withContext(dispatchers.io) {
        val target = postsStateFlow.value.firstOrNull { it.id == id }
        if (target != null) {
            realtimeDatabase?.let { rtdb ->
                try {
                    rtdb.reference.child("organizations")
                        .child(target.organizationId)
                        .child("items")
                        .child(id)
                        .removeValue()

                    rtdb.reference.child("items")
                        .child(id)
                        .removeValue()
                } catch (e: Exception) {
                    Log.w(TAG, "RTDB delete post error: ${e.message}")
                }
            }
        }
        val updated = postsStateFlow.value.filterNot { it.id == id }
        val changed = updated.size != postsStateFlow.value.size
        postsStateFlow.value = updated
        changed
    }

    override suspend fun toggleSavePost(userId: String, postId: String): Boolean = withContext(dispatchers.io) {
        val currentMap = savedPostsUserMap.value.toMutableMap()
        val userSet = currentMap[userId]?.toMutableSet() ?: mutableSetOf()
        val isSavedNow: Boolean
        if (userSet.contains(postId)) {
            userSet.remove(postId)
            isSavedNow = false
        } else {
            userSet.add(postId)
            isSavedNow = true
        }
        currentMap[userId] = userSet
        savedPostsUserMap.value = currentMap
        isSavedNow
    }

    override fun getSavedPosts(userId: String): Flow<List<ItemPost>> {
        return savedPostsUserMap.map { map ->
            val savedIds = map[userId] ?: emptySet()
            postsStateFlow.value.filter { savedIds.contains(it.id) }
        }
    }
}
