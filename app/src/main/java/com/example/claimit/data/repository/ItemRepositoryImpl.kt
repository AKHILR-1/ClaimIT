package com.example.claimit.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.claimit.core.dispatchers.CoroutineDispatchers
import com.example.claimit.domain.model.AIVectorMetadata
import com.example.claimit.domain.model.AttributeType
import com.example.claimit.domain.model.Building
import com.example.claimit.domain.model.CampusLocation
import com.example.claimit.domain.model.DetectedAttribute
import com.example.claimit.domain.model.ItemCategory
import com.example.claimit.domain.model.ItemPost
import com.example.claimit.domain.model.ItemType
import com.example.claimit.domain.model.PhysicalAttributes
import com.example.claimit.domain.model.PostStatus
import com.example.claimit.domain.repository.ItemRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ItemRepositoryImpl(
    private val dispatchers: CoroutineDispatchers,
    private val firestore: FirebaseFirestore? = try { FirebaseFirestore.getInstance() } catch (_: Exception) { null },
    private val storage: FirebaseStorage? = try { FirebaseStorage.getInstance() } catch (_: Exception) { null }
) : ItemRepository {

    private val TAG = "ItemRepository"

    private val initialPosts = emptyList<ItemPost>()

    private val postsStateFlow = MutableStateFlow(initialPosts)
    private val savedPostsUserMap = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    init {
        listenToFirestoreItems()
    }

    private fun listenToFirestoreItems() {
        val db = firestore ?: return
        try {
            db.collection("organizations")
                .document("lpu_main_campus")
                .collection("items")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore item snapshot listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        val remotePosts = mutableListOf<ItemPost>()
                        for (doc in snapshot.documents) {
                            try {
                                val title = doc.getString("title") ?: continue
                                val typeStr = doc.getString("type") ?: ItemType.LOST.name
                                val categoryStr = doc.getString("category") ?: ItemCategory.OTHER.name
                                val statusStr = doc.getString("status") ?: PostStatus.ACTIVE.name

                                val type = try { ItemType.valueOf(typeStr) } catch (_: Exception) { ItemType.LOST }
                                val category = try { ItemCategory.valueOf(categoryStr) } catch (_: Exception) { ItemCategory.OTHER }
                                val status = try { PostStatus.valueOf(statusStr) } catch (_: Exception) { PostStatus.ACTIVE }

                                @Suppress("UNCHECKED_CAST")
                                val imageUrlsList = (doc.get("imageUrls") as? List<String>) ?: emptyList()

                                val post = ItemPost(
                                    id = doc.id,
                                    trackingCode = doc.getString("trackingCode") ?: "CLM-${doc.id.take(6).uppercase()}",
                                    organizationId = doc.getString("organizationId") ?: "lpu_main_campus",
                                    userId = doc.getString("userId") ?: "user_student_1",
                                    userDisplayName = doc.getString("userDisplayName") ?: "Campus Member",
                                    title = title,
                                    description = doc.getString("description") ?: "",
                                    type = type,
                                    category = category,
                                    location = CampusLocation(
                                        building = Building(
                                            id = doc.getString("buildingId") ?: "bld_15",
                                            code = doc.getString("buildingCode") ?: "15",
                                            name = doc.getString("buildingName") ?: "Campus Center",
                                            zoneName = doc.getString("zoneName") ?: "Central",
                                            landmarkName = doc.getString("buildingDesc") ?: ""
                                        ),
                                        floorWing = doc.getString("floorWing") ?: "",
                                        microLocationDetails = doc.getString("microLocation") ?: ""
                                    ),
                                    physicalAttributes = PhysicalAttributes(
                                        dominantColor = doc.getString("dominantColor") ?: "",
                                        material = doc.getString("material") ?: "",
                                        condition = doc.getString("condition") ?: "",
                                        distinguishingFeatures = doc.getString("distinguishingFeatures") ?: ""
                                    ),
                                    aiMetadata = null,
                                    acceptedTags = emptyList(),
                                    imageUrls = imageUrlsList,
                                    createdAtTimestamp = doc.getLong("createdAtTimestamp") ?: System.currentTimeMillis(),
                                    status = status,
                                    finderOrOwnerContact = doc.getString("contact") ?: "contact@campus.edu",
                                    actionTakenRemarks = doc.getString("remarks") ?: ""
                                )
                                remotePosts.add(post)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing Firestore item document: ${e.message}")
                            }
                        }
                        if (remotePosts.isNotEmpty()) {
                            val combined = (remotePosts + postsStateFlow.value).distinctBy { it.id }
                            postsStateFlow.value = combined
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Exception initializing Firestore item snapshot listener: ${e.message}")
        }
    }

    suspend fun uploadBitmapToStorage(
        bitmap: Bitmap,
        organizationId: String,
        itemId: String,
        index: Int
    ): Result<String> = withContext(dispatchers.io) {
        val st = storage ?: return@withContext Result.failure(Exception("Cloud Storage not initialized"))
        try {
            val ref = st.reference.child("organizations/$organizationId/items/$itemId/img_$index.jpg")
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val data = baos.toByteArray()

            ref.putBytes(data).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Cloud Storage photo upload failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun createItemPost(post: ItemPost): Result<String> = withContext(dispatchers.io) {
        try {
            // Write to Firestore
            firestore?.let { db ->
                try {
                    val itemMap = mapOf(
                        "id" to post.id,
                        "trackingCode" to post.trackingCode,
                        "organizationId" to post.organizationId,
                        "userId" to post.userId,
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
                        "status" to post.status.name,
                        "contact" to post.finderOrOwnerContact,
                        "remarks" to post.actionTakenRemarks,
                        "createdAtTimestamp" to post.createdAtTimestamp
                    )

                    db.collection("organizations")
                        .document(post.organizationId)
                        .collection("items")
                        .document(post.id)
                        .set(itemMap)
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore write for item ${post.id} failed: ${e.message}")
                }
            }

            val updated = postsStateFlow.value.toMutableList()
            updated.add(0, post)
            postsStateFlow.value = updated
            Result.success(post.id)
        } catch (e: Exception) {
            Log.e(TAG, "Create item post exception: ${e.message}", e)
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
                val statusMatch = status == null || post.status == status
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
                firestore?.let { db ->
                    try {
                        db.collection("organizations")
                            .document(it.organizationId)
                            .collection("items")
                            .document(id)
                            .update("status", status.name)
                    } catch (e: Exception) {
                        Log.w(TAG, "Firestore status update error: ${e.message}")
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
            firestore?.let { db ->
                try {
                    db.collection("organizations")
                        .document(target.organizationId)
                        .collection("items")
                        .document(id)
                        .delete()
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore delete post error: ${e.message}")
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
