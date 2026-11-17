package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class UserRole(val wire: String, val label: String) {
    CUSTOMER("customer", "Customer"),
    OWNER("owner", "Restaurant Owner"),
    ADMIN("admin", "Admin");

    companion object {
        fun fromWire(value: String?): UserRole =
            entries.firstOrNull { it.wire == value?.lowercase() } ?: CUSTOMER
    }
}

enum class BookingStatus(val wire: String, val label: String) {
    PENDING("pending", "Pending"),
    CONFIRMED("confirmed", "Confirmed"),
    COMPLETED("completed", "Completed"),
    CANCELLED("cancelled", "Cancelled"),
    NO_SHOW("no-show", "No-Show");

    companion object {
        fun fromWire(value: String?): BookingStatus =
            entries.firstOrNull { it.wire == value?.lowercase() } ?: PENDING
    }
}

enum class RequestStatus(val wire: String, val label: String) {
    PENDING("pending", "Pending Review"),
    APPROVED("approved", "Approved"),
    REJECTED("rejected", "Rejected"),
    INFO_REQUESTED("info_requested", "More Info Requested");

    companion object {
        fun fromWire(value: String?): RequestStatus =
            entries.firstOrNull { it.wire == value?.lowercase() } ?: PENDING
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val role: String = "customer",
    val status: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Restaurant(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val description: String = "",
    val cuisines: List<String> = emptyList(),
    val city: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val priceRange: String = "$$",
    val dietaryOptions: List<String> = emptyList(),
    val openingHours: String = "11:00 AM - 10:30 PM",
    val holidayClosures: List<String> = emptyList(),
    val timeSlots: List<String> = listOf("12:00", "13:30", "18:00", "19:30", "21:00"),
    val logoUrl: String = "",
    val coverPhotoUrl: String = "",
    val galleryUrls: List<String> = emptyList(),
    val ratingAvg: Double = 4.5,
    val reviewCount: Long = 0L,
    val status: String = "active",
    val featured: Boolean = false,
    val approved: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RestaurantRequest(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val restaurantName: String = "",
    val cuisines: List<String> = emptyList(),
    val address: String = "",
    val city: String = "",
    val phone: String = "",
    val email: String = "",
    val licenseNumber: String = "",
    val description: String = "",
    val logoUrl: String = "",
    val coverPhotoUrl: String = "",
    val galleryUrls: List<String> = emptyList(),
    val status: String = "pending",
    val adminNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MenuCategory(
    @DocumentId val id: String = "",
    val restaurantId: String = "",
    val ownerId: String = "",
    val name: String = "",
    val sortOrder: Long = 0L,
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MenuItem(
    @DocumentId val id: String = "",
    val restaurantId: String = "",
    val ownerId: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val veg: Boolean = true,
    val popular: Boolean = false,
    val dietaryTags: List<String> = emptyList(),
    val available: Boolean = true,
    val published: Boolean = true,
    val rating: Double = 4.7,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TableConfig(
    @DocumentId val id: String = "",
    val restaurantId: String = "",
    val ownerId: String = "",
    val label: String = "",
    val capacity: Long = 4L,
    val count: Long = 2L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SlotBookingCount(
    @DocumentId val id: String = "",
    val restaurantId: String = "",
    val date: String = "",
    val timeSlot: String = "",
    val bookedTables: Long = 0L,
    val bookedSeats: Long = 0L,
    val published: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Booking(
    @DocumentId val id: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val restaurantImageUrl: String = "",
    val ownerId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val date: String = "",
    val timeSlot: String = "",
    val bookingEpochMillis: Long = 0L,
    val partySize: Long = 2L,
    val tableLabel: String = "",
    val specialRequests: String = "",
    val estimatedSpend: Double = 0.0,
    val status: String = "pending",
    val reviewed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Review(
    @DocumentId val id: String = "",
    val restaurantId: String = "",
    val ownerId: String = "",
    val bookingId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val ownerReply: String = "",
    val status: String = "visible",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Favorite(
    @DocumentId val id: String = "",
    val userId: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val restaurantCity: String = "",
    val restaurantCoverUrl: String = "",
    val restaurantRating: Double = 4.5,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ActivityLog(
    @DocumentId val id: String = "",
    val adminId: String = "",
    val adminEmail: String = "",
    val action: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "info",
    val read: Boolean = false,
    val relatedId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PlatformSettings(
    @DocumentId val id: String = "",
    val cuisines: List<String> = listOf("Italian", "Indian", "Japanese", "Grill", "Mediterranean", "Mexican", "Desserts"),
    val cities: List<String> = listOf("San Francisco", "New York", "London", "Austin"),
    val featuredRestaurantIds: List<String> = emptyList(),
    val heroBannerTitle: String = "CRAFTED TABLES.\nDINING PERFECTED.",
    val heroBannerSubtitle: String = "Discover neighborhood kitchens and reserve the best seats in seconds.",
    val heroBannerCta: String = "Explore Tables",
    val published: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class RestaurantFilters(
    val query: String = "",
    val cuisine: String? = null,
    val city: String? = null,
    val priceRange: String? = null,
    val minRating: Double = 0.0,
    val dietaryTag: String? = null
)
