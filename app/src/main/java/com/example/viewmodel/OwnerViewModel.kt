package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BookingAndReviewRepository
import com.example.data.RestaurantRepository
import com.example.data.unwrapFirestoreErrorMessage
import com.example.model.Booking
import com.example.model.MenuCategory
import com.example.model.MenuItem
import com.example.model.Restaurant
import com.example.model.RestaurantRequest
import com.example.model.Review
import com.example.model.TableConfig
import com.example.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class OwnerAnalytics(
    val totalBookings: Int = 0,
    val todayBookings: Int = 0,
    val estimatedRevenue: Double = 0.0,
    val averageRating: Double = 0.0,
    val statusCounts: Map<String, Int> = emptyMap(),
    val dailyBookings: List<Pair<String, Int>> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerViewModel(
    private val restaurantRepository: RestaurantRepository,
    private val bookingRepository: BookingAndReviewRepository,
    private val currentUserId: String,
    private val isAdminPreview: Boolean
) : ViewModel() {

    private val _selectedRestaurantId = MutableStateFlow<String?>(null)
    val selectedRestaurantId: StateFlow<String?> = _selectedRestaurantId.asStateFlow()

    val ownerRequestsState: StateFlow<UiState<List<RestaurantRequest>>> =
        restaurantRepository.observeOwnerRequests(currentUserId)
            .map<List<RestaurantRequest>, UiState<List<RestaurantRequest>>> { UiState.Success(it) }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load partner requests")))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val ownerRestaurantsState: StateFlow<UiState<List<Restaurant>>> =
        restaurantRepository.observeOwnerRestaurants(currentUserId, isAdminPreview)
            .map<List<Restaurant>, UiState<List<Restaurant>>> { list ->
                if (_selectedRestaurantId.value == null && list.isNotEmpty()) {
                    _selectedRestaurantId.value = list.first().id
                }
                UiState.Success(list)
            }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load restaurants")))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val selectedRestaurant: StateFlow<Restaurant?> = combine(
        ownerRestaurantsState,
        _selectedRestaurantId
    ) { state, selectedId ->
        val list = (state as? UiState.Success)?.data ?: emptyList()
        list.firstOrNull { it.id == selectedId } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val ownerBookingsState: StateFlow<UiState<List<Booking>>> = _selectedRestaurantId
        .flatMapLatest { restId ->
            bookingRepository.observeOwnerBookings(currentUserId, isAdminPreview, restId)
                .map<List<Booking>, UiState<List<Booking>>> { UiState.Success(it) }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load bookings")))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val ownerReviewsState: StateFlow<UiState<List<Review>>> = _selectedRestaurantId
        .flatMapLatest { restId ->
            bookingRepository.observeOwnerReviews(currentUserId, isAdminPreview, restId)
                .map<List<Review>, UiState<List<Review>>> { UiState.Success(it) }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load reviews")))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val categories: StateFlow<List<MenuCategory>> = selectedRestaurant
        .flatMapLatest { r ->
            if (r == null) flowOf(emptyList())
            else restaurantRepository.observeRestaurantCategories(r.id)
                .catch { e ->
                    if (e is CancellationException && generateSequence<Throwable>(e) { it.cause }.none { it is FirebaseFirestoreException }) throw e
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val menuItems: StateFlow<List<MenuItem>> = selectedRestaurant
        .flatMapLatest { r ->
            if (r == null) flowOf(emptyList())
            else restaurantRepository.observeRestaurantMenuItems(r.id)
                .catch { e ->
                    if (e is CancellationException && generateSequence<Throwable>(e) { it.cause }.none { it is FirebaseFirestoreException }) throw e
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val tables: StateFlow<List<TableConfig>> = selectedRestaurant
        .flatMapLatest { r ->
            if (r == null) flowOf(emptyList())
            else restaurantRepository.observeRestaurantTables(r.id)
                .catch { e ->
                    if (e is CancellationException && generateSequence<Throwable>(e) { it.cause }.none { it is FirebaseFirestoreException }) throw e
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val analytics: StateFlow<OwnerAnalytics> = combine(
        ownerBookingsState,
        ownerReviewsState,
        selectedRestaurant
    ) { bState, rState, restaurant ->
        val bookings = (bState as? UiState.Success)?.data ?: emptyList()
        val reviews = (rState as? UiState.Success)?.data?.filter { it.status == "visible" } ?: emptyList()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val todayCount = bookings.count { it.date == today && it.status != "cancelled" }
        val activeRevenue = bookings
            .filter { it.status in listOf("confirmed", "completed", "pending") }
            .sumOf { it.estimatedSpend }
        val avgRating = if (reviews.isNotEmpty()) {
            Math.round(reviews.map { it.rating }.average() * 10.0) / 10.0
        } else restaurant?.ratingAvg ?: 0.0

        val statusCounts = mapOf(
            "Confirmed" to bookings.count { it.status == "confirmed" },
            "Pending" to bookings.count { it.status == "pending" },
            "Completed" to bookings.count { it.status == "completed" },
            "Cancelled" to bookings.count { it.status == "cancelled" || it.status == "no-show" }
        )

        val byDate = bookings.groupBy { it.date.takeLast(5) }
            .entries.sortedBy { it.key }
            .takeLast(6)
            .map { it.key to it.value.size }

        OwnerAnalytics(
            totalBookings = bookings.size,
            todayBookings = todayCount,
            estimatedRevenue = activeRevenue,
            averageRating = avgRating,
            statusCounts = statusCounts,
            dailyBookings = byDate
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), OwnerAnalytics())

    fun selectRestaurant(id: String) {
        _selectedRestaurantId.value = id
    }

    fun submitPartnerRequest(request: RestaurantRequest, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(restaurantRepository.submitPartnerRequest(request))
        }
    }

    fun updateRestaurantProfile(
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
        timeSlots: List<String>,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(
                restaurantRepository.updateRestaurantProfile(
                    restaurantId, name, description, cuisines, city, address,
                    phone, email, priceRange, openingHours, holidayClosures, timeSlots
                )
            )
        }
    }

    fun saveCategory(restaurantId: String, ownerId: String, categoryId: String?, name: String, sortOrder: Long, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(restaurantRepository.saveCategory(restaurantId, ownerId, categoryId, name, sortOrder))
        }
    }

    fun deleteCategory(categoryId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(restaurantRepository.deleteCategory(categoryId))
        }
    }

    fun saveMenuItem(item: MenuItem, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(restaurantRepository.saveMenuItem(item))
        }
    }

    fun toggleMenuItemAvailability(itemId: String, available: Boolean, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(restaurantRepository.toggleMenuItemAvailability(itemId, available))
        }
    }

    fun deleteMenuItem(itemId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(restaurantRepository.deleteMenuItem(itemId))
        }
    }

    fun saveTableConfig(table: TableConfig, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(restaurantRepository.saveTableConfig(table))
        }
    }

    fun deleteTableConfig(tableId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(restaurantRepository.deleteTableConfig(tableId))
        }
    }

    fun updateBookingStatus(booking: Booking, newStatus: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(bookingRepository.updateBookingStatusAsOwner(booking, newStatus))
        }
    }

    fun replyToReview(reviewId: String, reply: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(bookingRepository.replyToReview(reviewId, reply))
        }
    }
}
