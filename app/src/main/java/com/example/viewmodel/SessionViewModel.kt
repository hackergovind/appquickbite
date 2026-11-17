package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.SeedData
import com.example.data.unwrapFirestoreErrorMessage
import com.example.model.AppNotification
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.UserRole
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModel(
    private val db: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val currentUserId: String,
    private val firebaseUser: FirebaseUser
) : ViewModel() {

    private val _activeWorkspaceRole = MutableStateFlow<UserRole?>(null)
    val activeWorkspaceRole: StateFlow<UserRole?> = _activeWorkspaceRole.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isSeeding = MutableStateFlow(false)
    val isSeeding: StateFlow<Boolean> = _isSeeding.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    val userProfileState: StateFlow<UiState<UserProfile?>> = authRepository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fe == null && error is CancellationException) throw error
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load profile")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val notificationsState: StateFlow<UiState<List<AppNotification>>> = authRepository.observeNotifications(currentUserId)
        .map<List<AppNotification>, UiState<List<AppNotification>>> { UiState.Success(it) }
        .catch { error ->
            val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (fe == null && error is CancellationException) throw error
            emit(UiState.Error(unwrapFirestoreErrorMessage(error, "Failed to load notifications")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    init {
        viewModelScope.launch {
            val profileRes = authRepository.ensureUserProfileExists(firebaseUser)
            profileRes.onSuccess { profile ->
                if (_activeWorkspaceRole.value == null) {
                    _activeWorkspaceRole.value = UserRole.fromWire(profile.role)
                }
                val isAdminUser = profile.role == "admin" ||
                    (firebaseUser.email.equals(AuthRepository.BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) && firebaseUser.isEmailVerified)
                if (isAdminUser) {
                    // Auto-seed sample restaurants on first launch if empty
                    SeedData.seedAllIfNeeded(
                        db = db,
                        currentAdminUid = currentUserId,
                        currentAdminEmail = firebaseUser.email ?: "admin@tabletrail.app",
                        forceReseed = false
                    )
                }
            }
        }
    }

    fun switchWorkspaceRole(role: UserRole) {
        _activeWorkspaceRole.value = role
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun seedSampleData(forceReseed: Boolean = true) {
        viewModelScope.launch {
            _isSeeding.value = true
            val res = SeedData.seedAllIfNeeded(
                db = db,
                currentAdminUid = currentUserId,
                currentAdminEmail = firebaseUser.email ?: "admin@tabletrail.app",
                forceReseed = forceReseed
            )
            _isSeeding.value = false
            res.onSuccess { seeded ->
                _bannerMessage.value = if (seeded) {
                    "Seeded 6 restaurants, menus, 2 owners, 3 customers & bookings!"
                } else {
                    "Sample data already present."
                }
            }.onFailure { err ->
                _bannerMessage.value = err.localizedMessage ?: "Seeding requires Admin permissions."
            }
        }
    }

    fun updateProfile(name: String, phone: String, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = authRepository.updateProfile(name, phone)
            onDone(res)
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            authRepository.markNotificationRead(notificationId)
        }
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }
}
