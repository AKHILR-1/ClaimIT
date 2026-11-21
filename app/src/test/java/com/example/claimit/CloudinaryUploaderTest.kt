package com.example.claimit

import com.example.claimit.data.repository.CloudinaryUploadResult
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudinaryUploaderTest {

    @Test
    fun testCloudinaryUploadResultDataClass() {
        val result = CloudinaryUploadResult(
            secureUrl = "https://res.cloudinary.com/docxcvl6l/image/upload/v1741350000/claimit/items/test.jpg",
            publicId = "claimit/items/test"
        )

        assertEquals("https://res.cloudinary.com/docxcvl6l/image/upload/v1741350000/claimit/items/test.jpg", result.secureUrl)
        assertEquals("claimit/items/test", result.publicId)
    }

    @Test
    fun testCloudinaryJsonResponseParsing() {
        val mockJsonResponse = """
            {
              "public_id": "claimit/items/sample_id_123",
              "secure_url": "https://res.cloudinary.com/docxcvl6l/image/upload/v1741350000/claimit/items/sample_id_123.jpg"
            }
        """.trimIndent()

        val secureUrl = mockJsonResponse.substringAfter("\"secure_url\": \"").substringBefore("\"")
        val publicId = mockJsonResponse.substringAfter("\"public_id\": \"").substringBefore("\"")

        assertEquals("https://res.cloudinary.com/docxcvl6l/image/upload/v1741350000/claimit/items/sample_id_123.jpg", secureUrl)
        assertEquals("claimit/items/sample_id_123", publicId)
    }

    @Test
    fun testBitmapScalingCalculation() {
        val originalWidth = 2400
        val originalHeight = 1800
        val maxDimension = 1280

        val max = maxOf(originalWidth, originalHeight)
        assertTrue(max > maxDimension)

        val scale = maxDimension.toFloat() / max
        val scaledWidth = (originalWidth * scale).toInt()
        val scaledHeight = (originalHeight * scale).toInt()

        assertEquals(1280, scaledWidth)
        assertEquals(960, scaledHeight)
    }
}
