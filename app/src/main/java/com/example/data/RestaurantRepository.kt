package com.example.data

import com.example.model.Favorite
import com.example.model.MenuCategory
import com.example.model.MenuItem
import com.example.model.PlatformSettings
import com.example.model.Restaurant
import com.example.model.RestaurantRequest
import com.example.model.SlotBookingCount
import com.example.model.TableConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class RestaurantRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in.")

    fun observeApprovedRestaurants(): Flow<List<Restaurant>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("restaurants")
                .whereEqualTo("approved", true)
                .whereEqualTo("status", "active")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Restaurant::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.featured }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, "restaurants")
                    throw error
                }
        )
    }

    fun observePopularDishes(): Flow<List<MenuItem>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("menuItems")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MenuItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.popular && it.available }
                        .sortedByDescending { it.rating }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, "menuItems")
                    throw error
                }
        )
    }

    fun observePlatformSettings(): Flow<PlatformSettings> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("platformSettings").document("config")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(PlatformSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: PlatformSettings()
                    } else PlatformSettings()
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.GET, "platformSettings/config")
                    // Fallback to defaults if rules deny missing doc on non-seeded cold start
                    emit(PlatformSettings())
                }
        )
    }

    fun observeRestaurantCategories(restaurantId: String): Flow<List<MenuCategory>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("categories")
                .whereEqualTo("restaurantId", restaurantId)
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MenuCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, "categories")
                    throw error
                }
        )
    }

    fun observeRestaurantMenuItems(restaurantId: String): Flow<List<MenuItem>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("menuItems")
                .whereEqualTo("restaurantId", restaurantId)
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MenuItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.popular }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, "menuItems")
                    throw error
                }
        )
    }

    fun observeRestaurantTables(restaurantId: String): Flow<List<TableConfig>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("tables")
                .whereEqualTo("restaurantId", restaurantId)
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TableConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.capacity }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, "tables")
                    throw error
                }
        )
    }

    fun observeSlotBookings(restaurantId: String): Flow<List<SlotBookingCount>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("slotBookings")
                .whereEqualTo("restaurantId", restaurantId)
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SlotBookingCount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, "slotBookings")
                    throw error
                }
        )
    }

    fun observeFavorites(userId: String): Flow<List<Favorite>> = flow {
        emitAll(
            db.collection("favorites")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Favorite::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) handleFirestoreError(fe ?: error, OperationType.LIST, "favorites")
                    throw error
                }
        )
    }

    suspend fun toggleFavorite(restaurant: Restaurant, isCurrentlyFavorite: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val favId = "${uid}_${restaurant.id}"
        val path = "favorites/$favId"
        return try {
            val docRef = db.collection("favorites").document(favId)
            if (isCurrentlyFavorite) {
                docRef.delete().await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "restaurantId" to restaurant.id,
                        "restaurantName" to restaurant.name.take(100),
                        "restaurantCity" to restaurant.city.take(80),
                        "restaurantCoverUrl" to restaurant.coverPhotoUrl.take(500),
                        "restaurantRating" to restaurant.ratingAvg.coerceIn(0.0, 5.0),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- OWNER OPERATIONS ---

    fun observeOwnerRestaurants(ownerId: String, isAdminPreview: Boolean): Flow<List<Restaurant>> = flow {
        val uid = requireUserId()
        val query = if (isAdminPreview) {
            db.collection("restaurants")
        } else {
            db.collection("restaurants").whereEqualTo("ownerId", ownerId)
        }
        emitAll(
            query.snapshots()
                .map { snap -> snap.toObjects(Restaurant::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, "restaurants")
                    throw error
                }
        )
    }

    fun observeOwnerRequests(ownerId: String): Flow<List<RestaurantRequest>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("restaurantRequests")
                .whereEqualTo("ownerId", ownerId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(RestaurantRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, "restaurantRequests")
                    throw error
                }
        )
    }

    suspend fun submitPartnerRequest(request: RestaurantRequest): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "restaurantRequests"
        return try {
            require(request.restaurantName.trim().length in 2..100) { "Restaurant name must be 2–100 characters." }
            require(request.cuisines.isNotEmpty()) { "Select at least one cuisine." }
            require(request.city.isNotBlank()) { "City is required." }
            require(request.address.trim().length >= 2) { "Address is required." }
            require(request.phone.trim().length >= 3) { "Phone is required." }
            require(request.email.trim().length >= 3) { "Email is required." }
            require(request.licenseNumber.trim().length >= 2) { "License number is required." }

            val docRef = if (request.id.isNotBlank()) {
                db.collection("restaurantRequests").document(request.id)
            } else {
                db.collection("restaurantRequests").document()
            }

            if (request.id.isNotBlank()) {
                docRef.update(
                    mapOf(
                        "restaurantName" to request.restaurantName.trim().take(100),
                        "cuisines" to request.cuisines.take(10),
                        "address" to request.address.trim().take(200),
                        "city" to request.city.trim().take(80),
                        "phone" to request.phone.trim().take(30),
                        "email" to request.email.trim().take(120),
                        "licenseNumber" to request.licenseNumber.trim().take(80),
                        "description" to request.description.trim().take(1000),
                        "logoUrl" to request.logoUrl.trim().take(500),
                        "coverPhotoUrl" to request.coverPhotoUrl.trim().take(500),
                        "galleryUrls" to request.galleryUrls.take(10),
                        "status" to "pending",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "ownerId" to uid,
                        "ownerName" to request.ownerName.ifBlank { "Partner Chef" }.take(80),
                        "restaurantName" to request.restaurantName.trim().take(100),
                        "cuisines" to request.cuisines.take(10),
                        "address" to request.address.trim().take(200),
                        "city" to request.city.trim().take(80),
                        "phone" to request.phone.trim().take(30),
                        "email" to request.email.trim().take(120),
                        "licenseNumber" to request.licenseNumber.trim().take(80),
                        "description" to request.description.trim().take(1000),
                        "logoUrl" to request.logoUrl.trim().take(500),
                        "coverPhotoUrl" to request.coverPhotoUrl.trim().take(500),
                        "galleryUrls" to request.galleryUrls.take(10),
                        "status" to "pending",
                        "adminNote" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateRestaurantProfile(
        restaurantId: String,
        name: String,
        description: String,
        cuisines: List<String>,
        city: String,
        address: String,
        phone: String,
        email: String,
        priceRange: String,
        openingHours: String,
        holidayClosures: List<String>,
        timeSlots: List<String>
    ): Result<Unit> {
        val path = "restaurants/$restaurantId"
        return try {
            requireUserId()
            require(name.trim().length in 2..100) { "Name must be 2–100 characters." }
            require(cuisines.isNotEmpty()) { "At least one cuisine is required." }
            require(timeSlots.isNotEmpty()) { "At least one time slot is required." }
            db.collection("restaurants").document(restaurantId).update(
                mapOf(
                    "name" to name.trim().take(100),
                    "description" to description.trim().take(1000),
                    "cuisines" to cuisines.take(10),
                    "city" to city.trim().take(80),
                    "address" to address.trim().take(200),
                    "phone" to phone.trim().take(30),
                    "email" to email.trim().take(120),
                    "priceRange" to priceRange,
                    "openingHours" to openingHours.trim().take(100),
                    "holidayClosures" to holidayClosures.take(30),
                    "timeSlots" to timeSlots.take(24),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveCategory(restaurantId: String, ownerId: String, categoryId: String?, name: String, sortOrder: Long): Result<Unit> {
        val path = "categories"
        return try {
            requireUserId()
            require(name.trim().length in 1..60) { "Category name must be 1–60 characters." }
            if (categoryId.isNullOrBlank()) {
                db.collection("categories").document().set(
                    mapOf(
                        "restaurantId" to restaurantId,
                        "ownerId" to ownerId,
                        "name" to name.trim().take(60),
                        "sortOrder" to sortOrder.coerceIn(0L, 1000L),
                        "published" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                db.collection("categories").document(categoryId).update(
                    mapOf(
                        "name" to name.trim().take(60),
                        "sortOrder" to sortOrder.coerceIn(0L, 1000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        val path = "categories/$categoryId"
        return try {
            requireUserId()
            db.collection("categories").document(categoryId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveMenuItem(item: MenuItem): Result<Unit> {
        val path = "menuItems"
        return try {
            requireUserId()
            require(item.name.trim().length in 1..100) { "Dish name is required." }
            require(item.price >= 0.0) { "Price must be non-negative." }
            if (item.id.isBlank()) {
                db.collection("menuItems").document().set(
                    mapOf(
                        "restaurantId" to item.restaurantId,
                        "ownerId" to item.ownerId,
                        "categoryId" to item.categoryId.ifBlank { "${item.restaurantId}_cat_2" },
                        "categoryName" to item.categoryName.ifBlank { "Mains" },
                        "name" to item.name.trim().take(100),
                        "description" to item.description.trim().take(500),
                        "price" to item.price.coerceIn(0.0, 100000.0),
                        "imageUrl" to item.imageUrl.trim().take(500),
                        "veg" to item.veg,
                        "popular" to item.popular,
                        "dietaryTags" to item.dietaryTags.take(8),
                        "available" to item.available,
                        "published" to true,
                        "rating" to item.rating.coerceIn(0.0, 5.0),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                db.collection("menuItems").document(item.id).update(
                    mapOf(
                        "categoryId" to item.categoryId,
                        "categoryName" to item.categoryName,
                        "name" to item.name.trim().take(100),
                        "description" to item.description.trim().take(500),
                        "price" to item.price.coerceIn(0.0, 100000.0),
                        "imageUrl" to item.imageUrl.trim().take(500),
                        "veg" to item.veg,
                        "popular" to item.popular,
                        "dietaryTags" to item.dietaryTags.take(8),
                        "available" to item.available,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleMenuItemAvailability(itemId: String, available: Boolean): Result<Unit> {
        val path = "menuItems/$itemId"
        return try {
            requireUserId()
            db.collection("menuItems").document(itemId).update(
                mapOf("available" to available, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMenuItem(itemId: String): Result<Unit> {
        val path = "menuItems/$itemId"
        return try {
            requireUserId()
            db.collection("menuItems").document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveTableConfig(table: TableConfig): Result<Unit> {
        val path = "tables"
        return try {
            requireUserId()
            require(table.label.trim().isNotBlank()) { "Table label is required." }
            require(table.capacity in 1L..30L) { "Capacity must be 1–30." }
            require(table.count in 1L..200L) { "Count must be 1–200." }
            if (table.id.isBlank()) {
                db.collection("tables").document().set(
                    mapOf(
                        "restaurantId" to table.restaurantId,
                        "ownerId" to table.ownerId,
                        "label" to table.label.trim().take(60),
                        "capacity" to table.capacity,
                        "count" to table.count,
                        "active" to table.active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                db.collection("tables").document(table.id).update(
                    mapOf(
                        "label" to table.label.trim().take(60),
                        "capacity" to table.capacity,
                        "count" to table.count,
                        "active" to table.active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTableConfig(tableId: String): Result<Unit> {
        val path = "tables/$tableId"
        return try {
            requireUserId()
            db.collection("tables").document(tableId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
