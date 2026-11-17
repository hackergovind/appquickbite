package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BookingAndReviewRepository
import com.example.data.RestaurantRepository
import com.example.data.unwrapFirestoreErrorMessage
import com.example.model.Booking
import com.example.model.Favorite
import com.example.model.MenuCategory
import com.example.model.MenuItem
import com.example.model.PlatformSettings
import com.example.model.Restaurant
import com.example.model.RestaurantFilters
import com.example.model.Review
import com.example.model.SlotBookingCount
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

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerViewModel(
    private val restaurantRepository: RestaurantRepository,
    private val bookingRepository: BookingAndReviewRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _filters = MutableStateFlow(RestaurantFilters())
    val filters: StateFlow<RestaurantFilters> = _filters.asStateFlow()

    private val _selectedCity = MutableStateFlow("All Cities")
    val selectedCity: StateFlow<String> = _selectedCity.asStateFlow()

    private val _selectedRestaurantId = MutableStateFlow<String?>(null)

    val platformSettings: StateFlow<PlatformSettings> = restaurantRepository.observePlatformSettings()
        .catch { error ->
            val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fe == null && error is CancellationException) throw error
            emit(PlatformSettings())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PlatformSettings())

    val restaurantsState: StateFlow<UiState<List<Restaurant>>> = restaurantRepository.observeApprovedRestaurants()
        .map<List<Restaurant>, UiState<List<Restaurant>>> { UiState.Success(it) }
        .catch { error ->
            val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fe == null && error is CancellationException) throw error
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load restaurants")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val filteredRestaurants: StateFlow<List<Restaurant>> = combine(
        restaurantsState,
        filters,
        selectedCity
    ) { state, f, cityFilter ->
        val list = (state as? UiState.Success)?.data ?: emptyList()
        list.filter { r ->
            val matchesQuery = f.query.isBlank() ||
                r.name.contains(f.query, ignoreCase = true) ||
                r.cuisines.any { it.contains(f.query, ignoreCase = true) } ||
                r.city.contains(f.query, ignoreCase = true) ||
                r.description.contains(f.query, ignoreCase = true)
            val matchesCuisine = f.cuisine == null || f.cuisine == "Featured" ||
                r.cuisines.any { it.equals(f.cuisine, ignoreCase = true) }
            val activeCity = f.city ?: cityFilter.takeIf { it != "All Cities" }
            val matchesCity = activeCity == null || r.city.equals(activeCity, ignoreCase = true)
            val matchesPrice = f.priceRange == null || r.priceRange == f.priceRange
            val matchesRating = r.ratingAvg >= f.minRating
            val matchesDietary = f.dietaryTag == null ||
                r.dietaryOptions.any { it.equals(f.dietaryTag, ignoreCase = true) }
            matchesQuery && matchesCuisine && matchesCity && matchesPrice && matchesRating && matchesDietary
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val popularDishesState: StateFlow<UiState<List<MenuItem>>> = restaurantRepository.observePopularDishes()
        .map<List<MenuItem>, UiState<List<MenuItem>>> { UiState.Success(it) }
        .catch { error ->
            val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fe == null && error is CancellationException) throw error
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load popular dishes")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val favoritesState: StateFlow<UiState<List<Favorite>>> = restaurantRepository.observeFavorites(currentUserId)
        .map<List<Favorite>, UiState<List<Favorite>>> { UiState.Success(it) }
        .catch { error ->
            val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fe == null && error is CancellationException) throw error
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load favorites")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val myBookingsState: StateFlow<UiState<List<Booking>>> = bookingRepository.observeCustomerBookings(currentUserId)
        .map<List<Booking>, UiState<List<Booking>>> { UiState.Success(it) }
        .catch { error ->
            val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fe == null && error is CancellationException) throw error
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load your bookings")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    // Detail streams keyed by _selectedRestaurantId
    val detailCategories: StateFlow<List<MenuCategory>> = _selectedRestaurantId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList())
            else restaurantRepository.observeRestaurantCategories(id)
                .catch { e ->
                    if (e is CancellationException && generateSequence<Throwable>(e) { it.cause }.none { it is FirebaseFirestoreException }) throw e
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val detailMenuItems: StateFlow<List<MenuItem>> = _selectedRestaurantId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList())
            else restaurantRepository.observeRestaurantMenuItems(id)
                .catch { e ->
                    if (e is CancellationException && generateSequence<Throwable>(e) { it.cause }.none { it is FirebaseFirestoreException }) throw e
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val detailTables: StateFlow<List<TableConfig>> = _selectedRestaurantId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList())
            else restaurantRepository.observeRestaurantTables(id)
                .catch { e ->
                    if (e is CancellationException && generateSequence<Throwable>(e) { it.cause }.none { it is FirebaseFirestoreException }) throw e
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val detailSlotBookings: StateFlow<List<SlotBookingCount>> = _selectedRestaurantId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList())
            else restaurantRepository.observeSlotBookings(id)
                .catch { e ->
                    if (e is CancellationException && generateSequence<Throwable>(e) { it.cause }.none { it is FirebaseFirestoreException }) throw e
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val detailReviews: StateFlow<List<Review>> = _selectedRestaurantId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList())
            else bookingRepository.observeRestaurantReviews(id)
                .catch { e ->
                    if (e is CancellationException && generateSequence<Throwable>(e) { it.cause }.none { it is FirebaseFirestoreException }) throw e
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun selectRestaurant(restaurantId: String) {
        _selectedRestaurantId.value = restaurantId
    }

    fun updateFilters(transform: (RestaurantFilters) -> RestaurantFilters) {
        _filters.value = transform(_filters.value)
    }

    fun selectCity(city: String) {
        _selectedCity.value = city
    }

    fun clearFilters() {
        _filters.value = RestaurantFilters()
        _selectedCity.value = "All Cities"
    }

    fun toggleFavorite(restaurant: Restaurant, currentlyFavorite: Boolean, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(restaurantRepository.toggleFavorite(restaurant, currentlyFavorite))
        }
    }

    fun createBooking(
        restaurant: Restaurant,
        tables: List<TableConfig>,
        customerName: String,
        customerPhone: String,
        date: String,
        timeSlot: String,
        bookingEpochMillis: Long,
        partySize: Long,
        specialRequests: String,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(
                bookingRepository.createBooking(
                    restaurant = restaurant,
                    tables = tables,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    date = date,
                    timeSlot = timeSlot,
                    bookingEpochMillis = bookingEpochMillis,
                    partySize = partySize,
                    specialRequests = specialRequests
                )
            )
        }
    }

    fun modifyBooking(
        booking: Booking,
        newDate: String,
        newTimeSlot: String,
        newEpochMillis: Long,
        newPartySize: Long,
        newSpecialRequests: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(
                bookingRepository.modifyBooking(
                    booking, newDate, newTimeSlot, newEpochMillis, newPartySize, newSpecialRequests
                )
            )
        }
    }

    fun cancelBooking(booking: Booking, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(bookingRepository.cancelBookingAsCustomer(booking))
        }
    }

    fun submitReview(
        booking: Booking,
        customerName: String,
        rating: Long,
        comment: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(bookingRepository.submitReview(booking, customerName, rating, comment))
        }
    }
}
