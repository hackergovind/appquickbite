package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminRepository
import com.example.data.RestaurantRepository
import com.example.data.unwrapFirestoreErrorMessage
import com.example.model.ActivityLog
import com.example.model.Booking
import com.example.model.PlatformSettings
import com.example.model.Restaurant
import com.example.model.RestaurantRequest
import com.example.model.Review
import com.example.model.UiState
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val adminRepository: AdminRepository,
    private val restaurantRepository: RestaurantRepository
) : ViewModel() {

    val usersState: StateFlow<UiState<List<UserProfile>>> = adminRepository.observeAllUsers()
        .map<List<UserProfile>, UiState<List<UserProfile>>> { UiState.Success(it) }
        .catch { error ->
            val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fe == null && error is CancellationException) throw error
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Admin access required to load users")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val restaurantsState: StateFlow<UiState<List<Restaurant>>> = adminRepository.observeAllRestaurants()
        .map<List<Restaurant>, UiState<List<Restaurant>>> { UiState.Success(it) }
        .catch { error ->
            val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fe == null && error is CancellationException) throw error
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load restaurants")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val requestsState: StateFlow<UiState<List<RestaurantRequest>>> = adminRepository.observeAllRequests()
        .map<List<RestaurantRequest>, UiState<List<RestaurantRequest>>> { UiState.Success(it) }
        .catch { error ->
            val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fe == null && error is CancellationException) throw error
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load requests")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val bookingsState: StateFlow<UiState<List<Booking>>> = adminRepository.observeAllBookings()
        .map<List<Booking>, UiState<List<Booking>>> { UiState.Success(it) }
        .catch { error ->
            val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fe == null && error is CancellationException) throw error
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load bookings")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val reviewsState: StateFlow<UiState<List<Review>>> = adminRepository.observeAllReviews()
        .map<List<Review>, UiState<List<Review>>> { UiState.Success(it) }
        .catch { error ->
            val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fe == null && error is CancellationException) throw error
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load reviews")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val activityLogsState: StateFlow<UiState<List<ActivityLog>>> = adminRepository.observeActivityLogs()
        .map<List<ActivityLog>, UiState<List<ActivityLog>>> { UiState.Success(it) }
        .catch { error ->
            val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fe == null && error is CancellationException) throw error
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load activity logs")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val platformSettings: StateFlow<PlatformSettings> = restaurantRepository.observePlatformSettings()
        .catch { error ->
            val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fe == null && error is CancellationException) throw error
            emit(PlatformSettings())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PlatformSettings())

    fun approveRequest(req: RestaurantRequest, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onResult(adminRepository.approveRestaurantRequest(req)) }
    }

    fun reviewRequest(req: RestaurantRequest, status: String, reason: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onResult(adminRepository.reviewRestaurantRequest(req, status, reason)) }
    }

    fun toggleRestaurantSuspended(r: Restaurant, onResult: (Result<Unit>) -> Unit) {
        val nextStatus = if (r.status == "active") "suspended" else "active"
        viewModelScope.launch {
            onResult(adminRepository.updateRestaurantAdminFlags(r, status = nextStatus))
        }
    }

    fun toggleRestaurantFeatured(r: Restaurant, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(adminRepository.updateRestaurantAdminFlags(r, featured = !r.featured))
        }
    }

    fun deleteRestaurant(r: Restaurant, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onResult(adminRepository.deleteRestaurant(r)) }
    }

    fun updateUserStatusOrRole(user: UserProfile, status: String, role: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onResult(adminRepository.updateUserStatusOrRole(user, status, role)) }
    }

    fun removeReview(review: Review, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onResult(adminRepository.removeReview(review)) }
    }

    fun saveSettings(settings: PlatformSettings, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch { onResult(adminRepository.savePlatformSettings(settings)) }
    }
}
