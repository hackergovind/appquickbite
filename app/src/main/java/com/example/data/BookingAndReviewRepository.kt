package com.example.data

import com.example.model.Booking
import com.example.model.Restaurant
import com.example.model.Review
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
import java.util.UUID

class BookingAndReviewRepository(
    private val db: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in.")

    private fun slotDocId(restaurantId: String, date: String, timeSlot: String): String {
        val cleanSlot = timeSlot.replace(":", "").replace(" ", "_")
        return "${restaurantId}_${date}_${cleanSlot}"
    }

    fun observeCustomerBookings(customerId: String): Flow<List<Booking>> = flow {
        emitAll(
            db.collection("bookings")
                .whereEqualTo("customerId", customerId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.bookingEpochMillis }
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == customerId) handleFirestoreError(fe ?: error, OperationType.LIST, "bookings")
                    throw error
                }
        )
    }

    fun observeOwnerBookings(ownerId: String, isAdminPreview: Boolean, restaurantIdFilter: String? = null): Flow<List<Booking>> = flow {
        val uid = requireUserId()
        val query = if (isAdminPreview) {
            db.collection("bookings")
        } else {
            db.collection("bookings").whereEqualTo("ownerId", ownerId)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(Booking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { restaurantIdFilter == null || it.restaurantId == restaurantIdFilter }
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

    fun observeRestaurantReviews(restaurantId: String): Flow<List<Review>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("reviews")
                .whereEqualTo("restaurantId", restaurantId)
                .whereEqualTo("status", "visible")
                .snapshots()
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

    fun observeOwnerReviews(ownerId: String, isAdminPreview: Boolean, restaurantIdFilter: String? = null): Flow<List<Review>> = flow {
        val uid = requireUserId()
        val query = if (isAdminPreview) {
            db.collection("reviews")
        } else {
            db.collection("reviews").whereEqualTo("ownerId", ownerId)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(Review::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { restaurantIdFilter == null || it.restaurantId == restaurantIdFilter }
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

    /**
     * Real-time atomic table booking transaction to prevent double-booking.
     */
    suspend fun createBooking(
        restaurant: Restaurant,
        tables: List<TableConfig>,
        customerName: String,
        customerPhone: String,
        date: String,
        timeSlot: String,
        bookingEpochMillis: Long,
        partySize: Long,
        specialRequests: String
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val bookingId = "bk_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "bookings/$bookingId"
        return try {
            require(partySize in 1L..30L) { "Party size must be between 1 and 30." }
            require(!restaurant.holidayClosures.contains(date)) { "Restaurant is closed for a holiday on $date." }
            require(bookingEpochMillis > System.currentTimeMillis() - 60_000L) { "Please select a future date & time slot." }

            val matchingTables = tables.filter { it.active && it.capacity >= partySize }.sortedBy { it.capacity }
            val chosenTable = matchingTables.firstOrNull()
                ?: throw IllegalStateException("No single table accommodates $partySize guests at ${restaurant.name}.")
            val totalTablesForSlot = tables.filter { it.active }.sumOf { it.count }.coerceAtLeast(1L)
            val totalSeatsForSlot = tables.filter { it.active }.sumOf { it.capacity * it.count }.coerceAtLeast(4L)

            val slotId = slotDocId(restaurant.id, date, timeSlot)
            val slotRef = db.collection("slotBookings").document(slotId)
            val bookingRef = db.collection("bookings").document(bookingId)

            val avgCheckPerGuest = when (restaurant.priceRange) {
                "$" -> 22.0
                "$$" -> 38.0
                "$$$" -> 62.0
                else -> 95.0
            }

            db.runTransaction { tx ->
                val slotSnap = tx.get(slotRef)
                val currentTables = if (slotSnap.exists()) (slotSnap.getLong("bookedTables") ?: 0L) else 0L
                val currentSeats = if (slotSnap.exists()) (slotSnap.getLong("bookedSeats") ?: 0L) else 0L

                if (currentTables + 1L > totalTablesForSlot || currentSeats + partySize > totalSeatsForSlot) {
                    throw IllegalStateException("All tables for $timeSlot on $date are fully booked. Try another slot!")
                }

                if (slotSnap.exists()) {
                    tx.update(
                        slotRef,
                        mapOf(
                            "bookedTables" to (currentTables + 1L),
                            "bookedSeats" to (currentSeats + partySize),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.set(
                        slotRef,
                        mapOf(
                            "restaurantId" to restaurant.id,
                            "date" to date,
                            "timeSlot" to timeSlot,
                            "bookedTables" to 1L,
                            "bookedSeats" to partySize,
                            "published" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                tx.set(
                    bookingRef,
                    mapOf(
                        "restaurantId" to restaurant.id,
                        "restaurantName" to restaurant.name.take(100),
                        "restaurantImageUrl" to restaurant.coverPhotoUrl.take(500),
                        "ownerId" to restaurant.ownerId,
                        "customerId" to uid,
                        "customerName" to customerName.trim().ifBlank { "Guest" }.take(80),
                        "customerPhone" to customerPhone.trim().take(30),
                        "date" to date,
                        "timeSlot" to timeSlot,
                        "bookingEpochMillis" to bookingEpochMillis,
                        "partySize" to partySize,
                        "tableLabel" to chosenTable.label.take(60),
                        "specialRequests" to specialRequests.trim().take(300),
                        "estimatedSpend" to (partySize * avgCheckPerGuest),
                        "status" to "pending",
                        "reviewed" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            authRepository.sendNotification(
                recipientUserId = restaurant.ownerId,
                title = "New Table Request",
                message = "$customerName requested a table for $partySize on $date at $timeSlot.",
                type = "booking_pending",
                relatedId = bookingId
            )

            Result.success(bookingId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun modifyBooking(
        booking: Booking,
        newDate: String,
        newTimeSlot: String,
        newEpochMillis: Long,
        newPartySize: Long,
        newSpecialRequests: String
    ): Result<Unit> {
        val path = "bookings/${booking.id}"
        return try {
            requireUserId()
            require(booking.status == "pending" || booking.status == "confirmed") {
                "Only upcoming pending or confirmed bookings can be modified."
            }
            val twoHoursMs = 2L * 3600L * 1000L
            require(booking.bookingEpochMillis - System.currentTimeMillis() >= twoHoursMs) {
                "Bookings can only be modified at least 2 hours before the reservation time."
            }
            require(newPartySize in 1L..30L) { "Party size must be between 1 and 30." }

            db.collection("bookings").document(booking.id).update(
                mapOf(
                    "date" to newDate,
                    "timeSlot" to newTimeSlot,
                    "bookingEpochMillis" to newEpochMillis,
                    "partySize" to newPartySize,
                    "specialRequests" to newSpecialRequests.trim().take(300),
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

    /**
     * Customers can cancel ONLY up to 2 hours before the booking time.
     */
    suspend fun cancelBookingAsCustomer(booking: Booking): Result<Unit> {
        val path = "bookings/${booking.id}"
        return try {
            requireUserId()
            val twoHoursMs = 2L * 3600L * 1000L
            val diff = booking.bookingEpochMillis - System.currentTimeMillis()
            if (diff < twoHoursMs) {
                return Result.failure(
                    IllegalStateException("Cancellations are only permitted up to 2 hours before your reservation.")
                )
            }
            val slotRef = db.collection("slotBookings").document(slotDocId(booking.restaurantId, booking.date, booking.timeSlot))
            val bookingRef = db.collection("bookings").document(booking.id)

            db.runTransaction { tx ->
                val slotSnap = tx.get(slotRef)
                if (slotSnap.exists()) {
                    val curTables = (slotSnap.getLong("bookedTables") ?: 1L)
                    val curSeats = (slotSnap.getLong("bookedSeats") ?: booking.partySize)
                    tx.update(
                        slotRef,
                        mapOf(
                            "bookedTables" to (curTables - 1L).coerceAtLeast(0L),
                            "bookedSeats" to (curSeats - booking.partySize).coerceAtLeast(0L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                tx.update(
                    bookingRef,
                    mapOf("status" to "cancelled", "updatedAt" to FieldValue.serverTimestamp())
                )
            }.await()

            authRepository.sendNotification(
                recipientUserId = booking.ownerId,
                title = "Booking Cancelled",
                message = "${booking.customerName} cancelled their ${booking.date} ${booking.timeSlot} booking.",
                type = "booking_cancelled",
                relatedId = booking.id
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateBookingStatusAsOwner(booking: Booking, newStatus: String): Result<Unit> {
        val path = "bookings/${booking.id}"
        return try {
            requireUserId()
            require(newStatus in listOf("confirmed", "completed", "cancelled", "no-show")) {
                "Invalid booking status."
            }
            val bookingRef = db.collection("bookings").document(booking.id)
            if (newStatus == "cancelled") {
                val slotRef = db.collection("slotBookings").document(slotDocId(booking.restaurantId, booking.date, booking.timeSlot))
                db.runTransaction { tx ->
                    val slotSnap = tx.get(slotRef)
                    if (slotSnap.exists()) {
                        val curTables = (slotSnap.getLong("bookedTables") ?: 1L)
                        val curSeats = (slotSnap.getLong("bookedSeats") ?: booking.partySize)
                        tx.update(
                            slotRef,
                            mapOf(
                                "bookedTables" to (curTables - 1L).coerceAtLeast(0L),
                                "bookedSeats" to (curSeats - booking.partySize).coerceAtLeast(0L),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    tx.update(bookingRef, mapOf("status" to newStatus, "updatedAt" to FieldValue.serverTimestamp()))
                }.await()
            } else {
                bookingRef.update(mapOf("status" to newStatus, "updatedAt" to FieldValue.serverTimestamp())).await()
            }

            if (newStatus == "confirmed" || newStatus == "cancelled") {
                val title = if (newStatus == "confirmed") "Table Confirmed!" else "Booking Declined"
                val msg = if (newStatus == "confirmed") {
                    "Your table at ${booking.restaurantName} on ${booking.date} at ${booking.timeSlot} is confirmed."
                } else {
                    "Your request at ${booking.restaurantName} on ${booking.date} at ${booking.timeSlot} was declined."
                }
                authRepository.sendNotification(
                    recipientUserId = booking.customerId,
                    title = title,
                    message = msg,
                    type = if (newStatus == "confirmed") "booking_confirmed" else "booking_cancelled",
                    relatedId = booking.id
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitReview(
        booking: Booking,
        customerName: String,
        rating: Long,
        comment: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "reviews"
        return try {
            require(rating in 1L..5L) { "Rating must be 1 to 5 stars." }
            require(comment.trim().length in 2..1000) { "Review must be 2–1000 characters." }

            val revRef = db.collection("reviews").document()
            revRef.set(
                mapOf(
                    "restaurantId" to booking.restaurantId,
                    "ownerId" to booking.ownerId,
                    "bookingId" to booking.id,
                    "customerId" to uid,
                    "customerName" to customerName.ifBlank { "TableTrail Diner" }.take(80),
                    "rating" to rating,
                    "comment" to comment.trim().take(1000),
                    "ownerReply" to "",
                    "status" to "visible",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("bookings").document(booking.id).update(
                mapOf("reviewed" to true, "updatedAt" to FieldValue.serverTimestamp())
            ).await()

            // Recompute restaurant aggregate rating
            val allVisible = db.collection("reviews")
                .whereEqualTo("restaurantId", booking.restaurantId)
                .whereEqualTo("status", "visible")
                .get().await()
            val ratings = allVisible.documents.mapNotNull { it.getLong("rating") }
            if (ratings.isNotEmpty()) {
                val avg = (ratings.sum().toDouble() / ratings.size.toDouble())
                val rounded = Math.round(avg * 10.0) / 10.0
                runCatching {
                    db.collection("restaurants").document(booking.restaurantId).update(
                        mapOf(
                            "ratingAvg" to rounded,
                            "reviewCount" to ratings.size.toLong(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun replyToReview(reviewId: String, replyText: String): Result<Unit> {
        val path = "reviews/$reviewId"
        return try {
            requireUserId()
            require(replyText.trim().length in 1..1000) { "Reply cannot be empty." }
            db.collection("reviews").document(reviewId).update(
                mapOf(
                    "ownerReply" to replyText.trim().take(1000),
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
}
