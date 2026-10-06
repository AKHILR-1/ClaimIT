# ClaimIT 🔍📱

**ClaimIT** is a native Android campus Lost & Found application written 100% in Kotlin with Jetpack Compose.

## Description
ClaimIT is a native Android campus Lost & Found app built with Kotlin and Jetpack Compose. Features AI item recognition, vector proximity duplicate matching, LPU campus geospatial location indexing, multi-step report creation, and a clean editorial fintech interface.

---

## Key Features

- **Native Multimodal AI Pipeline**: On-device/hybrid item classification, structural tag generation, and confidence score extraction.
- **Vector Proximity & Duplicate Cluster Engine**: Anti-spam compound scoring heuristic ($Score = W_v \cdot Sim_v + W_l \cdot Score_l + W_t \cdot Score_t$) with embedded inline match banners and side-by-side verification.
- **LPU Geospatial Campus Indexing**: Multi-tier indexing across all 50+ Lovely Professional University (LPU) campus blocks and facilities with fast offline fuzzy search.
- **Multi-Step Report Creation**: Structured 3-step wizard (`Overview` $\rightarrow$ `Tags` $\rightarrow$ `Location`) with custom floor inputs and action taken remarks.
- **Multi-Page Navigation Menu**: Bottom navigation menu system (`Report`, `Campus Feed`, `AI Matches`, `Campus Zones`).

---

## Tech Stack

- **Language**: 100% Modern Kotlin
- **UI Framework**: Jetpack Compose & Material 3
- **Architecture**: Clean Architecture + MVI / UDF (`ViewModel`, `StateFlow`, `SharedFlow`, `Channel`)
- **Concurrency**: Kotlin Coroutines & Injected Dispatchers (`CoroutineDispatchers`)
- **Design Tokens**: Editorial Industrial Fintech Slate Palette

---

## Authors & Maintainers
- **Akhil / Sachin**
