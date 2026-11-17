package com.example.data

import com.example.model.ActivityLog
import com.example.model.Booking
import com.example.model.PlatformSettings
import com.example.model.Restaurant
import com.example.model.RestaurantRequest
import com.example.model.Review
import com.example.model.UserProfile
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
import java.util.UUID

class AdminRepository(
    private val db: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireAdmin(): Pair<String, String> {
        val user = auth.currentUser ?: throw IllegalStateException("Admin must be signed in.")
        return user.uid to (user.email ?: "admin@tabletrail.app")
    }

    fun observeAllUsers(): Flow<List<UserProfile>> = flow {
        val (uid, _) = requireAdmin()
        emitAll(
            db.collection("users").snapshots()
                .map { it.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, "users")
                    throw error
                }
        )
    }

    fun observeAllRestaurants(): Flow<List<Restaurant>> = flow {
        val (uid, _) = requireAdmin()
        emitAll(
            db.collection("restaurants").snapshots()
                .map { it.toObjects(Restaurant::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, "restaurants")
                    throw error
                }
        )
    }

    fun observeAllRequests(): Flow<List<RestaurantRequest>> = flow {
        val (uid, _) = requireAdmin()
        emitAll(
            db.collection("restaurantRequests").snapshots()
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

    fun observeAllBookings(): Flow<List<Booking>> = flow {
        val (uid, _) = requireAdmin()
        emitAll(
            db.collection("bookings").snapshots()
                .map { snap ->
                    snap.toObjects(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.bookingEpochMillis }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, "bookings")
                    throw error
                }
        )
    }

    fun observeAllReviews(): Flow<List<Review>> = flow {
        val (uid, _) = requireAdmin()
        emitAll(
            db.collection("reviews").snapshots()
                .map { snap ->
                    snap.toObjects(Review::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, "reviews")
                    throw error
                }
        )
    }

    fun observeActivityLogs(): Flow<List<ActivityLog>> = flow {
        val (uid, _) = requireAdmin()
        emitAll(
            db.collection("activityLogs").snapshots()
                .map { snap ->
                    snap.toObjects(ActivityLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) handleFirestoreError(fe ?: error, OperationType.LIST, "activityLogs")
                    throw error
                }
        )
    }

    private suspend fun logAdminAction(action: String, targetType: String, targetId: String, details: String) {
        val (uid, email) = requireAdmin()
        runCatching {
            db.collection("activityLogs").document().set(
                mapOf(
                    "adminId" to uid,
                    "adminEmail" to email.take(120),
                    "action" to action.take(80),
                    "targetType" to targetType.take(40),
                    "targetId" to targetId.take(128),
                    "details" to details.take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun approveRestaurantRequest(req: RestaurantRequest, note: String = "Approved! Welcome to TableTrail."): Result<Unit> {
        val path = "restaurantRequests/${req.id}"
        return try {
            requireAdmin()
            val restId = "rest_${UUID.randomUUID().toString().replace("-", "").take(10)}"
            val now = FieldValue.serverTimestamp()
            val batch = db.batch()

            batch.update(
                db.collection("restaurantRequests").document(req.id),
                mapOf("status" to "approved", "adminNote" to note.take(500), "updatedAt" to now)
            )
            batch.set(
                db.collection("restaurants").document(restId),
                mapOf(
                    "ownerId" to req.ownerId,
                    "name" to req.restaurantName.take(100),
                    "description" to req.description.take(1000),
                    "cuisines" to req.cuisines.ifEmpty { listOf("Italian") }.take(10),
                    "city" to req.city.take(80),
                    "address" to req.address.take(200),
                    "phone" to req.phone.take(30),
                    "email" to req.email.take(120),
                    "priceRange" to "$$",
                    "dietaryOptions" to listOf("veg"),
                    "openingHours" to "11:30 AM - 10:30 PM",
                    "holidayClosures" to emptyList<String>(),
                    "timeSlots" to listOf("12:00", "13:30", "18:00", "19:30", "21:00"),
                    "logoUrl" to req.logoUrl.ifBlank { req.coverPhotoUrl }.take(500),
                    "coverPhotoUrl" to req.coverPhotoUrl.ifBlank {
                        "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=1000&q=80"
                    }.take(500),
                    "galleryUrls" to req.galleryUrls.take(10),
                    "ratingAvg" to 5.0,
                    "reviewCount" to 0L,
                    "status" to "active",
                    "featured" to false,
                    "approved" to true,
                    "createdAt" to now,
                    "updatedAt" to now
                )
            )
            // Default starter categories and tables for newly approved partner
            listOf("Starters", "Mains", "Desserts", "Drinks").forEachIndexed { idx, name ->
                batch.set(
                    db.collection("categories").document("${restId}_cat_${idx + 1}"),
                    mapOf(
                        "restaurantId" to restId,
                        "ownerId" to req.ownerId,
                        "name" to name,
                        "sortOrder" to (idx + 1L),
                        "published" to true,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
            }
            batch.set(
                db.collection("tables").document("${restId}_tbl_4top"),
                mapOf(
                    "restaurantId" to restId,
                    "ownerId" to req.ownerId,
                    "label" to "Main Dining 4-Top",
                    "capacity" to 4L,
                    "count" to 6L,
                    "active" to true,
                    "createdAt" to now,
                    "updatedAt" to now
                )
            )
            batch.commit().await()

            // Promote user to owner if user doc exists and isn't already admin
            val userDoc = db.collection("users").document(req.ownerId).get().await()
            if (userDoc.exists() && userDoc.getString("role") != "admin") {
                db.collection("users").document(req.ownerId).update(
                    mapOf("role" to "owner", "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            }

            authRepository.sendNotification(
                recipientUserId = req.ownerId,
                title = "Partner Request Approved!",
                message = "${req.restaurantName} is now live on TableTrail.",
                type = "request_approved",
                relatedId = restId
            )
            logAdminAction("APPROVE_REQUEST", "request", req.id, "Approved ${req.restaurantName} ($restId)")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun reviewRestaurantRequest(req: RestaurantRequest, newStatus: String, reason: String): Result<Unit> {
        val path = "restaurantRequests/${req.id}"
        return try {
            requireAdmin()
            require(newStatus in listOf("rejected", "info_requested"))
            db.collection("restaurantRequests").document(req.id).update(
                mapOf(
                    "status" to newStatus,
                    "adminNote" to reason.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            authRepository.sendNotification(
                recipientUserId = req.ownerId,
                title = if (newStatus == "rejected") "Partner Request Rejected" else "More Info Needed",
                message = "${req.restaurantName}: $reason",
                type = "request_rejected",
                relatedId = req.id
            )
            logAdminAction(
                action = if (newStatus == "rejected") "REJECT_REQUEST" else "REQUEST_INFO",
                targetType = "request",
                targetId = req.id,
                details = "${req.restaurantName}: $reason"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateRestaurantAdminFlags(
        restaurant: Restaurant,
        status: String = restaurant.status,
        featured: Boolean = restaurant.featured,
        approved: Boolean = restaurant.approved
    ): Result<Unit> {
        val path = "restaurants/${restaurant.id}"
        return try {
            requireAdmin()
            db.collection("restaurants").document(restaurant.id).update(
                mapOf(
                    "status" to status,
                    "featured" to featured,
                    "approved" to approved,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logAdminAction(
                action = "UPDATE_RESTAURANT",
                targetType = "restaurant",
                targetId = restaurant.id,
                details = "${restaurant.name}: status=$status, featured=$featured, approved=$approved"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteRestaurant(restaurant: Restaurant): Result<Unit> {
        val path = "restaurants/${restaurant.id}"
        return try {
            requireAdmin()
            db.collection("restaurants").document(restaurant.id).delete().await()
            logAdminAction("DELETE_RESTAURANT", "restaurant", restaurant.id, "Deleted ${restaurant.name}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserStatusOrRole(user: UserProfile, newStatus: String, newRole: String): Result<Unit> {
        val path = "users/${user.uid}"
        return try {
            requireAdmin()
            require(newStatus in listOf("active", "blocked"))
            require(newRole in listOf("customer", "owner", "admin"))
            db.collection("users").document(user.uid).update(
                mapOf(
                    "status" to newStatus,
                    "role" to newRole,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (newRole == "admin") {
                runCatching {
                    db.collection("admins").document(user.uid).set(
                        mapOf("email" to user.email, "createdAt" to FieldValue.serverTimestamp())
                    ).await()
                }
            }
            logAdminAction(
                action = "UPDATE_USER",
                targetType = "user",
                targetId = user.uid,
                details = "${user.email}: role=$newRole, status=$newStatus"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeReview(review: Review): Result<Unit> {
        val path = "reviews/${review.id}"
        return try {
            requireAdmin()
            db.collection("reviews").document(review.id).update(
                mapOf("status" to "removed", "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            logAdminAction("REMOVE_REVIEW", "review", review.id, "Removed review by ${review.customerName}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun savePlatformSettings(settings: PlatformSettings): Result<Unit> {
        val path = "platformSettings/config"
        return try {
            requireAdmin()
            db.collection("platformSettings").document("config").set(
                mapOf(
                    "cuisines" to settings.cuisines.take(30),
                    "cities" to settings.cities.take(30),
                    "featuredRestaurantIds" to settings.featuredRestaurantIds.take(20),
                    "heroBannerTitle" to settings.heroBannerTitle.trim().take(120),
                    "heroBannerSubtitle" to settings.heroBannerSubtitle.trim().take(240),
                    "heroBannerCta" to settings.heroBannerCta.trim().take(40),
                    "published" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logAdminAction("UPDATE_SETTINGS", "settings", "config", "Updated platform cuisines, cities, and hero banner")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
