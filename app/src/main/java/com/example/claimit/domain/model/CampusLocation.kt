package com.example.claimit.domain.model

data class Building(
    val id: String,
    val code: String,
    val name: String,
    val zoneName: String,
    val landmarkName: String,
    val availableFloors: List<String> = emptyList(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) {
    val displayTitle: String get() = "$code - $name"
}

data class CampusLocation(
    val building: Building,
    val floorWing: String,
    val microLocationDetails: String
) {
    val fullFormattedAddress: String
        get() = buildString {
            append("${building.code} (${building.name})")
            if (floorWing.isNotBlank()) append(" • $floorWing")
            if (microLocationDetails.isNotBlank()) append(" • $microLocationDetails")
        }
}
