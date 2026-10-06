package com.example.claimit.data.repository

import com.example.claimit.core.dispatchers.CoroutineDispatchers
import com.example.claimit.core.util.FuzzySearch
import com.example.claimit.domain.model.Building
import com.example.claimit.domain.repository.CampusLocationRepository
import kotlinx.coroutines.withContext

class CampusLocationRepositoryImpl(
    private val dispatchers: CoroutineDispatchers
) : CampusLocationRepository {

    private val campusBuildings = listOf(
        Building("bld_others", "OTHERS", "Others (Specify Custom Building / Landmark)", "Custom Zone", "Custom Unlisted Location"),
        Building("bld_00", "00", "Main Gate 1", "Campus Entry", "Main University Gate 1 Entrance"),
        Building("bld_01a", "01A", "School of Design (Fashion)", "Design & Arts", "School of Fashion Design Studios"),
        Building("bld_01b", "01B", "School of Design (Fashion)", "Design & Arts", "School of Fashion Design Labs"),
        Building("bld_02a", "02A", "Uni Auditorium", "Events & Culture", "University Indoor Auditorium 02A"),
        Building("bld_03", "03", "Uni Health Center", "Medical & Health", "University Health Center & Medical Clinic"),
        Building("bld_04", "04", "Lovely Institute of Technology - Pharmacy", "Health Sciences", "LIT Pharmacy Research Block"),
        Building("bld_05", "05", "Animal House", "Bio Sciences", "Veterinary & Animal Science Lab"),
        Building("bld_06", "06", "School of Architecture & Designing", "Design & Arts", "Architecture & Interior Design Studios"),
        Building("bld_07", "07", "School of Pharmaceutical Science", "Health Sciences", "Pharmaceutical Sciences Research Labs"),
        Building("bld_08", "08", "School of Journalism, Film Production and Creative Arts", "Media & Arts", "Journalism, Film & Creative Arts Studios"),
        Building("bld_13", "13", "Student Welfare Wing", "Student Services", "Division of Student Welfare (DSW)"),
        Building("bld_14", "14", "School of Business", "Business & Finance", "Mittal School of Business (MSB)"),
        Building("bld_15", "15", "Uni Mall", "Commercial Hub", "LPU UniMall Shopping, Banks & Food Court"),
        Building("bld_15a", "15A", "Student Resource Centre", "Student Services", "Central Student Resource & Study Center"),
        Building("bld_15b", "15B", "School of Hotel Management and Tourism", "Hospitality", "School of Hotel Management & Training Kitchens"),
        Building("bld_16", "16", "Baldev Raj Mittal UniPolis", "Events & Culture", "UniPolis Open Air Grand Arena & Events Dome"),
        Building("bld_18", "18", "Lovely School of Education", "Humanities", "Lovely School of Education & Humanities"),
        Building("bld_19", "19", "School of Journalism, Films And Creative Arts - I", "Media & Arts", "Film Production & Broadcasting Block I"),
        Building("bld_20", "20", "School of Law", "Legal Studies", "Lovely School of Law - Moot Court"),
        Building("bld_21", "21", "School of Law", "Legal Studies", "Lovely School of Law - Academic Wing"),
        Building("bld_25", "25", "School of AI and Emerging Technology", "Engineering & Tech", "AI & Emerging Technology Labs"),
        Building("bld_26", "26", "School of AI and Emerging Technology", "Engineering & Tech", "Robotics & Machine Learning Research Center"),
        Building("bld_27", "27", "School of Computing and Artificial Intelligence", "Engineering & Tech", "Computing & AI Complex"),
        Building("bld_28", "28", "School of Computing and Artificial Intelligence", "Engineering & Tech", "Software Engineering & Cloud Computing Labs"),
        Building("bld_29", "29", "Administration-I", "Administrative Core", "Central Administrative Block I"),
        Building("bld_30", "30", "Central Administrative Office", "Administrative Core", "Central Administrative Office & Executive Wing"),
        Building("bld_31", "31", "Admission Block", "Administrative Core", "Central Admissions Office & Helpdesk"),
        Building("bld_32", "32", "Administration-II", "Administrative Core", "Administrative Block II & Accounts"),
        Building("bld_33", "33", "School of Computer Science & Engineering", "CSE Complex", "School of CSE - Software Labs"),
        Building("bld_34", "34", "School of Computer Science & Engineering", "CSE Complex", "School of CSE - Cyber Security & Data Science Labs"),
        Building("bld_35", "35", "Shanti Devi Mittal Auditorium", "Events & Culture", "Shanti Devi Mittal Main Indoor Auditorium"),
        Building("bld_36", "36", "School of Electronics and Electrical Engineering", "Engineering & Tech", "Electrical & Electronics Engineering Labs"),
        Building("bld_37", "37", "Centre Library", "Central Academic", "Central University Library & Digital Archives"),
        Building("bld_38", "38", "School of Computer Application / Division of Research and Development", "Research & Tech", "School of Computer Application & R&D Division"),
        Building("bld_39", "39", "Innovation Studio", "Research & Tech", "LPU Innovation Studio & Startup Incubator"),
        Building("bld_47", "47", "Shanti Devi Mittal Indoor Sports Complex", "Sports & Fitness", "Indoor Sports Complex, Swimming Pool & Gym"),
        Building("bld_55", "55", "School of Mechanical Engineering / School of Polytechnic / School of Chemical Engineering & Physical Sciences", "Polytechnic & Sciences", "Mechanical, Polytechnic & Chemical Sciences Block"),
        Building("bld_55a", "55A", "School of Mechanical Engineering / School of Chemical Engineering & Physical Sciences", "Engineering & Sciences", "Mechanical & Physical Sciences Labs"),
        Building("bld_56", "56", "School of Civil Engineering / School of Bio Engineering and Bio Sciences", "Engineering & Bio", "Civil Engineering & Bio Sciences Complex"),
        Building("bld_57", "57", "School of Agriculture", "Agriculture", "School of Agriculture & Research Fields"),
        Building("bld_57a", "57A", "School of Bio Engineering and Bio Sciences", "Bio Sciences", "Bio Engineering & Biotechnology Labs"),
        Building("bld_58", "58", "Mechanical Engineering Workshops", "Engineering Workshops", "Mechanical Machine & Foundry Workshops"),
        Building("bld_59", "59", "New Polytechnic Block", "Polytechnic Wing", "Polytechnic Academic Block"),
        Building("bld_60", "60", "Workshop", "Facilities", "Central Campus Engineering Workshop"),
        Building("bld_61", "61", "Student Learning Center", "Student Learning", "Student Learning & Tutoring Center"),
        Building("bld_41_ab", "41-AB", "Boys Apartment", "Residential (Boys)", "Boys Residential Apartment Block 41-AB"),
        Building("bld_41_cd", "41-CD", "Staff Residence", "Residential Staff", "Staff & Faculty Apartments Block 41-CD"),
        Building("bld_bh1", "BH-1", "Boys Hostel-01", "Residential Quad (Boys)", "BH-01 Reception, Mess & Common Room"),
        Building("bld_bh2", "BH-2", "Boys Hostel-02", "Residential Quad (Boys)", "BH-02 Reception & Mess"),
        Building("bld_bh3", "BH-3", "Boys Hostel-03", "Residential Quad (Boys)", "BH-03 Reception & Sports Lawn"),
        Building("bld_bh4", "BH-4", "Boys Hostel-04", "Residential Quad (Boys)", "BH-04 Reception"),
        Building("bld_bh5", "BH-5", "Boys Hostel-05", "Residential Quad (Boys)", "BH-05 Mess & Gym"),
        Building("bld_bh6", "BH-6", "Boys Hostel-06", "Residential Quad (Boys)", "BH-06 Reception"),
        Building("bld_bh7", "BH-7", "Boys Hostel-07", "Residential Quad (Boys)", "BH-07 Reception"),
        Building("bld_bs11", "BS-11", "Boys Studios 11", "Residential Quad (Boys)", "Boys Studio Apartments BS-11"),
        Building("bld_bs12", "BS-12", "Boys Studios 12", "Residential Quad (Boys)", "Boys Studio Apartments BS-12"),
        Building("bld_bs13", "BS-13", "Boys Studios 13", "Residential Quad (Boys)", "Boys Studio Apartments BS-13"),
        Building("bld_gh1", "GH-1", "Girls Hostel-01", "Residential Quad (Girls)", "GH-01 Reception, Mess & Study Room"),
        Building("bld_gh2", "GH-2", "Girls Hostel-02", "Residential Quad (Girls)", "GH-02 Reception & Mess"),
        Building("bld_gh3", "GH-3", "Girls Hostel-03", "Residential Quad (Girls)", "GH-03 Reception"),
        Building("bld_gh4", "GH-4", "Girls Hostel-04", "Residential Quad (Girls)", "GH-04 Reception"),
        Building("bld_gh5", "GH-5", "Girls Hostel-05", "Residential Quad (Girls)", "GH-05 Mess & Lounge"),
        Building("bld_gh6", "GH-6", "Girls Hostel-06", "Residential Quad (Girls)", "GH-06 Reception"),
        Building("bld_gh7", "GH-7", "Girls Hostel-07", "Residential Quad (Girls)", "GH-07 Reception"),
        Building("bld_gs11", "GS-11", "Girls Studios 11", "Residential Quad (Girls)", "Girls Studio Apartments GS-11"),
        Building("bld_gs12", "GS-12", "Girls Studios 12", "Residential Quad (Girls)", "Girls Studio Apartments GS-12"),
        Building("bld_vet", "VET", "Vet Clinic", "Medical & Health", "Veterinary Clinic & Animal Care")
    )

    override suspend fun getBuildings(): List<Building> = withContext(dispatchers.io) {
        campusBuildings
    }

    override suspend fun searchBuildingsFuzzy(query: String): List<Building> = withContext(dispatchers.io) {
        if (query.isBlank()) return@withContext campusBuildings

        val fuzzyMatches = campusBuildings
            .map { building ->
                val codeScore = FuzzySearch.calculateSimilarity(query, building.code)
                val nameScore = FuzzySearch.calculateSimilarity(query, building.name)
                val landmarkScore = FuzzySearch.calculateSimilarity(query, building.landmarkName)
                val maxScore = maxOf(codeScore, nameScore, landmarkScore)
                building to maxScore
            }
            .filter { it.second > 0.30f }
            .sortedByDescending { it.second }
            .map { it.first }

        // Always keep OTHERS as an option
        val othersBuilding = campusBuildings.first()
        if (!fuzzyMatches.contains(othersBuilding)) {
            listOf(othersBuilding) + fuzzyMatches
        } else fuzzyMatches
    }

    override suspend fun getBuildingById(id: String): Building? = withContext(dispatchers.io) {
        campusBuildings.firstOrNull { it.id == id }
    }
}
