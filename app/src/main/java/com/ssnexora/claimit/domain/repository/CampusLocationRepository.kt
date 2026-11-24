package com.ssnexora.claimit.domain.repository

import com.ssnexora.claimit.domain.model.Building

interface CampusLocationRepository {
    suspend fun getBuildings(): List<Building>
    suspend fun searchBuildingsFuzzy(query: String): List<Building>
    suspend fun getBuildingById(id: String): Building?
}

