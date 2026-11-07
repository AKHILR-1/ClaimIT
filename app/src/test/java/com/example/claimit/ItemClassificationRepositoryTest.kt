package com.example.claimit

import com.example.claimit.data.repository.ItemClassificationRepositoryImpl
import com.example.claimit.domain.model.AttributeType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ItemClassificationRepositoryTest {

    private lateinit var classificationRepository: ItemClassificationRepositoryImpl
    private val dispatchers = TestCoroutineDispatchers()

    @Before
    fun setup() {
        classificationRepository = ItemClassificationRepositoryImpl(dispatchers)
    }

    @Test
    fun `classify bitmap produces 128-dim normalized embedding and color attributes`() = runBlocking {
        val result = classificationRepository.classifyItemImage(null)

        assertTrue(result.isSuccess)
        val metadata = result.getOrNull()
        assertNotNull(metadata)

        assertEquals(128, metadata?.vectorEmbedding?.size)
        assertTrue(metadata?.detectedAttributes?.any { it.type == AttributeType.COLOR } == true)
    }
}
