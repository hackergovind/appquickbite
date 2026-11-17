package com.example

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AdminRepository
import com.example.data.AuthRepository
import com.example.data.BookingAndReviewRepository
import com.example.data.RestaurantRepository
import com.example.model.Restaurant
import com.example.model.UiState
import com.example.model.UserRole
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.components.NotificationsBottomSheet
import com.example.ui.customer.CustomerHomeScreen
import com.example.ui.customer.FavoritesScreen
import com.example.ui.customer.MyBookingsScreen
import com.example.ui.customer.ProfileAndDevScreen
import com.example.ui.customer.RestaurantDetailScreen
import com.example.ui.owner.OwnerDashboardScreen
import com.example.ui.owner.PartnerOnboardingScreen
import com.example.ui.theme.TableTrailTheme
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.CustomerViewModel
import com.example.viewmodel.OwnerViewModel
import com.example.viewmodel.SessionViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class AppTab(
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    EXPLORE("Explore", Icons.Filled.Restaurant, "nav_explore"),
    BOOKINGS("Bookings", Icons.Filled.CalendarMonth, "nav_bookings"),
    FAVORITES("Favorites", Icons.Filled.Favorite, "nav_favorites"),
    OWNER_STUDIO("Owner", Icons.Filled.Storefront, "nav_owner"),
    ADMIN_CONTROL("Admin", Icons.Filled.AdminPanelSettings, "nav_admin"),
    PROFILE("Profile", Icons.Filled.Person, "nav_profile")
}

@Composable
fun TableTrailApp() {
    val context = LocalContext.current
    val db = remember(context) {
        FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
    }
    val authRepository = remember(db) { AuthRepository(db) }
    val currentUser by authRepository.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val systemDark = isSystemInDarkTheme()
    var darkModeOverride by remember { mutableStateOf<Boolean?>(null) }

    TableTrailTheme(darkTheme = darkModeOverride ?: systemDark) {
        val user = currentUser
        if (user == null) {
            AuthScreen(
                onAuthSuccess = {
                    // authStateFlow updates automatically upon Firebase Auth state change
                }
            )
        } else {
            key(user.uid) {
                AuthenticatedTableTrailShell(
                    db = db,
                    authRepository = authRepository,
                    firebaseUser = user,
                    onDarkModeSync = { darkModeOverride = it }
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedTableTrailShell(
    db: FirebaseFirestore,
    authRepository: AuthRepository,
    firebaseUser: FirebaseUser,
    onDarkModeSync: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val restaurantRepository = remember(db) { RestaurantRepository(db) }
    val bookingRepository = remember(db, authRepository) { BookingAndReviewRepository(db, authRepository) }
    val adminRepository = remember(db, authRepository) { AdminRepository(db, authRepository) }

    val sessionViewModel: SessionViewModel = viewModel(
        key = "session_${firebaseUser.uid}",
        factory = viewModelFactory {
            initializer {
                SessionViewModel(
                    db = db,
                    authRepository = authRepository,
                    currentUserId = firebaseUser.uid,
                    firebaseUser = firebaseUser
                )
            }
        }
    )

    val userProfileState by sessionViewModel.userProfileState.collectAsStateWithLifecycle()
    val activeWorkspaceRoleState by sessionViewModel.activeWorkspaceRole.collectAsStateWithLifecycle()
    val isDarkMode by sessionViewModel.isDarkMode.collectAsStateWithLifecycle()
    val isSeeding by sessionViewModel.isSeeding.collectAsStateWithLifecycle()
    val bannerMessage by sessionViewModel.bannerMessage.collectAsStateWithLifecycle()
    val notificationsState by sessionViewModel.notificationsState.collectAsStateWithLifecycle()

    LaunchedEffect(isDarkMode) {
        onDarkModeSync(isDarkMode)
    }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            sessionViewModel.clearBanner()
        }
    }

    val userProfile = (userProfileState as? UiState.Success)?.data
    val activeRole = activeWorkspaceRoleState
        ?: UserRole.fromWire(userProfile?.role ?: "customer")

    val isRealAdmin = (userProfile?.role == "admin") ||
        (firebaseUser.email.equals(AuthRepository.BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) &&
            firebaseUser.isEmailVerified)

    val customerViewModel: CustomerViewModel = viewModel(
        key = "customer_${firebaseUser.uid}",
        factory = viewModelFactory {
            initializer {
                CustomerViewModel(
                    restaurantRepository = restaurantRepository,
                    bookingRepository = bookingRepository,
                    currentUserId = firebaseUser.uid
                )
            }
        }
    )

    val ownerViewModel: OwnerViewModel = viewModel(
        key = "owner_${firebaseUser.uid}_$isRealAdmin",
        factory = viewModelFactory {
            initializer {
                OwnerViewModel(
                    restaurantRepository = restaurantRepository,
                    bookingRepository = bookingRepository,
                    currentUserId = firebaseUser.uid,
                    isAdminPreview = isRealAdmin
                )
            }
        }
    )

    val performSignOut: () -> Unit = {
        scope.launch {
            withContext(NonCancellable) {
                runCatching {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                }
                Firebase.auth.signOut()
            }
        }
    }

    if (userProfile?.status == "blocked") {
        BlockedAccountScreen(onSignOut = performSignOut)
        return
    }

    val restaurantsState by customerViewModel.restaurantsState.collectAsStateWithLifecycle()
    val filteredRestaurants by customerViewModel.filteredRestaurants.collectAsStateWithLifecycle()
    val popularDishesState by customerViewModel.popularDishesState.collectAsStateWithLifecycle()
    val favoritesState by customerViewModel.favoritesState.collectAsStateWithLifecycle()
    val myBookingsState by customerViewModel.myBookingsState.collectAsStateWithLifecycle()
    val platformSettings by customerViewModel.platformSettings.collectAsStateWithLifecycle()
    val filters by customerViewModel.filters.collectAsStateWithLifecycle()
    val selectedCity by customerViewModel.selectedCity.collectAsStateWithLifecycle()
    val detailCategories by customerViewModel.detailCategories.collectAsStateWithLifecycle()
    val detailMenuItems by customerViewModel.detailMenuItems.collectAsStateWithLifecycle()
    val detailTables by customerViewModel.detailTables.collectAsStateWithLifecycle()
    val detailSlotBookings by customerViewModel.detailSlotBookings.collectAsStateWithLifecycle()
    val detailReviews by customerViewModel.detailReviews.collectAsStateWithLifecycle()

    val ownerRestaurantsState by ownerViewModel.ownerRestaurantsState.collectAsStateWithLifecycle()
    val ownerSelectedRestaurant by ownerViewModel.selectedRestaurant.collectAsStateWithLifecycle()
    val ownerRequestsState by ownerViewModel.ownerRequestsState.collectAsStateWithLifecycle()
    val ownerBookingsState by ownerViewModel.ownerBookingsState.collectAsStateWithLifecycle()
    val ownerReviewsState by ownerViewModel.ownerReviewsState.collectAsStateWithLifecycle()
    val ownerCategories by ownerViewModel.categories.collectAsStateWithLifecycle()
    val ownerMenuItems by ownerViewModel.menuItems.collectAsStateWithLifecycle()
    val ownerTables by ownerViewModel.tables.collectAsStateWithLifecycle()
    val ownerAnalytics by ownerViewModel.analytics.collectAsStateWithLifecycle()

    val notifications = (notificationsState as? UiState.Success)?.data ?: emptyList()
    val unreadCount = notifications.count { !it.read }
    val favorites = (favoritesState as? UiState.Success)?.data ?: emptyList()
    val allApprovedRestaurants = (restaurantsState as? UiState.Success)?.data ?: emptyList()

    var currentTab by remember(activeRole) {
        mutableStateOf(
            when (activeRole) {
                UserRole.CUSTOMER -> AppTab.EXPLORE
                UserRole.OWNER -> AppTab.OWNER_STUDIO
                UserRole.ADMIN -> AppTab.ADMIN_CONTROL
            }
        )
    }
    var detailRestaurant by remember { mutableStateOf<Restaurant?>(null) }
    var showPartnerApply by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = detailRestaurant != null || showPartnerApply) {
        if (detailRestaurant != null) {
            detailRestaurant = null
        } else if (showPartnerApply) {
            showPartnerApply = false
        }
    }

    val navTabs = when (activeRole) {
        UserRole.CUSTOMER -> listOf(AppTab.EXPLORE, AppTab.BOOKINGS, AppTab.FAVORITES, AppTab.PROFILE)
        UserRole.OWNER -> listOf(AppTab.OWNER_STUDIO, AppTab.PROFILE)
        UserRole.ADMIN -> listOf(AppTab.ADMIN_CONTROL, AppTab.PROFILE)
    }

    val showChrome = detailRestaurant == null && !showPartnerApply
    val showSnackbar: (String) -> Unit = { msg ->
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showChrome && !useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        navTabs.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showChrome && useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        navTabs.forEach { tab ->
                            NavigationRailItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val activeDetail = detailRestaurant
                    if (activeDetail != null) {
                        val liveRestaurant = allApprovedRestaurants.firstOrNull { it.id == activeDetail.id } ?: activeDetail
                        val isFav = favorites.any { it.restaurantId == liveRestaurant.id }
                        RestaurantDetailScreen(
                            restaurant = liveRestaurant,
                            categories = detailCategories,
                            menuItems = detailMenuItems,
                            tables = detailTables,
                            slotBookings = detailSlotBookings,
                            reviews = detailReviews,
                            isFavorite = isFav,
                            defaultGuestName = userProfile?.name ?: firebaseUser.displayName ?: "",
                            defaultGuestPhone = userProfile?.phone ?: "",
                            onBack = { detailRestaurant = null },
                            onToggleFavorite = {
                                customerViewModel.toggleFavorite(liveRestaurant, isFav) { res ->
                                    res.onSuccess {
                                        showSnackbar(if (isFav) "Removed from favorites" else "Saved to favorites")
                                    }.onFailure { err ->
                                        showSnackbar(err.localizedMessage ?: "Couldn't update favorite")
                                    }
                                }
                            },
                            onBookTable = { date, slot, epochMs, partySize, guestName, guestPhone, specialRequests, done ->
                                customerViewModel.createBooking(
                                    restaurant = liveRestaurant,
                                    tables = detailTables,
                                    customerName = guestName,
                                    customerPhone = guestPhone,
                                    date = date,
                                    timeSlot = slot,
                                    bookingEpochMillis = epochMs,
                                    partySize = partySize,
                                    specialRequests = specialRequests
                                ) { res ->
                                    done(res)
                                    res.onSuccess {
                                        showSnackbar("Table reserved for $date at $slot!")
                                    }.onFailure { err ->
                                        showSnackbar(err.localizedMessage ?: "Reservation failed")
                                    }
                                }
                            }
                        )
                    } else if (showPartnerApply) {
                        PartnerOnboardingScreen(
                            requestsState = ownerRequestsState,
                            ownerName = userProfile?.name ?: firebaseUser.displayName ?: "Partner Owner",
                            ownerEmail = userProfile?.email ?: firebaseUser.email ?: "",
                            onBack = { showPartnerApply = false },
                            onSubmitRequest = { req, done ->
                                ownerViewModel.submitPartnerRequest(req) { res ->
                                    done(res)
                                    res.onSuccess {
                                        showSnackbar("Partner application submitted!")
                                    }.onFailure { err ->
                                        showSnackbar(err.localizedMessage ?: "Submission failed")
                                    }
                                }
                            },
                            modifier = Modifier.widthIn(max = 840.dp)
                        )
                    } else {
                        val contentModifier = Modifier.widthIn(max = 860.dp)
                        when (currentTab) {
                            AppTab.EXPLORE -> CustomerHomeScreen(
                                restaurantsState = restaurantsState,
                                filteredRestaurants = filteredRestaurants,
                                popularDishesState = popularDishesState,
                                favorites = favorites,
                                platformSettings = platformSettings,
                                filters = filters,
                                selectedCity = selectedCity,
                                unreadNotificationCount = unreadCount,
                                onSelectCity = { customerViewModel.selectCity(it) },
                                onUpdateFilters = { customerViewModel.updateFilters(it) },
                                onClearFilters = { customerViewModel.clearFilters() },
                                onOpenRestaurant = { r ->
                                    customerViewModel.selectRestaurant(r.id)
                                    detailRestaurant = r
                                },
                                onToggleFavorite = { r, currentlyFav ->
                                    customerViewModel.toggleFavorite(r, currentlyFav) { res ->
                                        res.onSuccess {
                                            showSnackbar(if (currentlyFav) "Removed from favorites" else "Saved to favorites")
                                        }.onFailure { err ->
                                            showSnackbar(err.localizedMessage ?: "Failed to update favorite")
                                        }
                                    }
                                },
                                onOpenNotifications = { showNotificationsSheet = true },
                                onOpenProfile = { currentTab = AppTab.PROFILE },
                                onOpenPartnerApply = { showPartnerApply = true },
                                modifier = contentModifier
                            )

                            AppTab.BOOKINGS -> MyBookingsScreen(
                                bookingsState = myBookingsState,
                                customerName = userProfile?.name ?: firebaseUser.displayName ?: "Guest",
                                onExploreClick = { currentTab = AppTab.EXPLORE },
                                onCancelBooking = { booking ->
                                    customerViewModel.cancelBooking(booking) { res ->
                                        res.onSuccess { showSnackbar("Booking cancelled.") }
                                            .onFailure { showSnackbar(it.localizedMessage ?: "Couldn't cancel booking") }
                                    }
                                },
                                onModifyBooking = { booking, date, slot, epochMs, partySize, requests ->
                                    customerViewModel.modifyBooking(
                                        booking, date, slot, epochMs, partySize, requests
                                    ) { res ->
                                        res.onSuccess { showSnackbar("Booking updated!") }
                                            .onFailure { showSnackbar(it.localizedMessage ?: "Couldn't modify booking") }
                                    }
                                },
                                onSubmitReview = { booking, customerName, rating, comment ->
                                    customerViewModel.submitReview(booking, customerName, rating, comment) { res ->
                                        res.onSuccess { showSnackbar("Thank you for your review!") }
                                            .onFailure { showSnackbar(it.localizedMessage ?: "Couldn't submit review") }
                                    }
                                },
                                modifier = contentModifier
                            )

                            AppTab.FAVORITES -> FavoritesScreen(
                                favoritesState = favoritesState,
                                allRestaurants = allApprovedRestaurants,
                                onOpenRestaurant = { r ->
                                    customerViewModel.selectRestaurant(r.id)
                                    detailRestaurant = r
                                },
                                onRemoveFavorite = { r ->
                                    customerViewModel.toggleFavorite(r, true) { res ->
                                        res.onSuccess { showSnackbar("Removed from favorites") }
                                    }
                                },
                                onExploreClick = { currentTab = AppTab.EXPLORE },
                                modifier = contentModifier
                            )

                            AppTab.OWNER_STUDIO -> OwnerDashboardScreen(
                                restaurantsState = ownerRestaurantsState,
                                selectedRestaurant = ownerSelectedRestaurant,
                                requestsState = ownerRequestsState,
                                bookingsState = ownerBookingsState,
                                reviewsState = ownerReviewsState,
                                categories = ownerCategories,
                                menuItems = ownerMenuItems,
                                tables = ownerTables,
                                analytics = ownerAnalytics,
                                ownerName = userProfile?.name ?: firebaseUser.displayName ?: "Owner",
                                ownerEmail = userProfile?.email ?: firebaseUser.email ?: "",
                                onSelectRestaurant = { ownerViewModel.selectRestaurant(it) },
                                onSubmitPartnerRequest = { req, done ->
                                    ownerViewModel.submitPartnerRequest(req) { res ->
                                        done(res)
                                        res.onSuccess { showSnackbar("Partner application submitted!") }
                                            .onFailure { showSnackbar(it.localizedMessage ?: "Submission failed") }
                                    }
                                },
                                onUpdateBookingStatus = { booking, status ->
                                    ownerViewModel.updateBookingStatus(booking, status) { res ->
                                        res.onSuccess { showSnackbar("Booking marked $status") }
                                            .onFailure { showSnackbar(it.localizedMessage ?: "Update failed") }
                                    }
                                },
                                onToggleDishAvailable = { itemId, available ->
                                    ownerViewModel.toggleMenuItemAvailability(itemId, available) { res ->
                                        res.onFailure { showSnackbar(it.localizedMessage ?: "Update failed") }
                                    }
                                },
                                onSaveDish = { item ->
                                    ownerViewModel.saveMenuItem(item) { res ->
                                        res.onSuccess { showSnackbar("Menu item saved!") }
                                            .onFailure { showSnackbar(it.localizedMessage ?: "Save failed") }
                                    }
                                },
                                onDeleteDish = { itemId ->
                                    ownerViewModel.deleteMenuItem(itemId) { res ->
                                        res.onSuccess { showSnackbar("Menu item deleted") }
                                            .onFailure { showSnackbar(it.localizedMessage ?: "Delete failed") }
                                    }
                                },
                                onSaveCategory = { restId, ownerId, catId, name, order ->
                                    ownerViewModel.saveCategory(restId, ownerId, catId, name, order) { res ->
                                        res.onSuccess { showSnackbar("Category saved") }
                                            .onFailure { showSnackbar(it.localizedMessage ?: "Save failed") }
                                    }
                                },
                                onDeleteCategory = { catId ->
                                    ownerViewModel.deleteCategory(catId) { res ->
                                        res.onSuccess { showSnackbar("Category deleted") }
                                    }
                                },
                                onSaveTable = { table ->
                                    ownerViewModel.saveTableConfig(table) { res ->
                                        res.onSuccess { showSnackbar("Table configuration saved") }
                                            .onFailure { showSnackbar(it.localizedMessage ?: "Save failed") }
                                    }
                                },
                                onDeleteTable = { tableId ->
                                    ownerViewModel.deleteTableConfig(tableId) { res ->
                                        res.onSuccess { showSnackbar("Table removed") }
                                    }
                                },
                                onUpdateRestaurantProfile = { id, name, desc, cuisines, city, address, phone, email, price, hours, holidays, slots ->
                                    ownerViewModel.updateRestaurantProfile(
                                        id, name, desc, cuisines, city, address, phone, email, price, hours, holidays, slots
                                    ) { res ->
                                        res.onSuccess { showSnackbar("Restaurant profile updated!") }
                                            .onFailure { showSnackbar(it.localizedMessage ?: "Update failed") }
                                    }
                                },
                                onReplyReview = { reviewId, reply ->
                                    ownerViewModel.replyToReview(reviewId, reply) { res ->
                                        res.onSuccess { showSnackbar("Reply posted!") }
                                            .onFailure { showSnackbar(it.localizedMessage ?: "Reply failed") }
                                    }
                                },
                                modifier = contentModifier
                            )

                            AppTab.ADMIN_CONTROL -> {
                                if (!isRealAdmin) {
                                    AdminAccessRequiredCard(
                                        onReturnToCustomer = {
                                            sessionViewModel.switchWorkspaceRole(UserRole.CUSTOMER)
                                        },
                                        modifier = contentModifier
                                    )
                                } else {
                                    val adminViewModel: AdminViewModel = viewModel(
                                        key = "admin_${firebaseUser.uid}",
                                        factory = viewModelFactory {
                                            initializer {
                                                AdminViewModel(
                                                    adminRepository = adminRepository,
                                                    restaurantRepository = restaurantRepository
                                                )
                                            }
                                        }
                                    )
                                    val adminUsers by adminViewModel.usersState.collectAsStateWithLifecycle()
                                    val adminRestaurants by adminViewModel.restaurantsState.collectAsStateWithLifecycle()
                                    val adminRequests by adminViewModel.requestsState.collectAsStateWithLifecycle()
                                    val adminBookings by adminViewModel.bookingsState.collectAsStateWithLifecycle()
                                    val adminReviews by adminViewModel.reviewsState.collectAsStateWithLifecycle()
                                    val adminLogs by adminViewModel.activityLogsState.collectAsStateWithLifecycle()
                                    val adminSettings by adminViewModel.platformSettings.collectAsStateWithLifecycle()

                                    AdminDashboardScreen(
                                        usersState = adminUsers,
                                        restaurantsState = adminRestaurants,
                                        requestsState = adminRequests,
                                        bookingsState = adminBookings,
                                        reviewsState = adminReviews,
                                        logsState = adminLogs,
                                        platformSettings = adminSettings,
                                        onApproveRequest = { req ->
                                            adminViewModel.approveRequest(req) { res ->
                                                res.onSuccess { showSnackbar("Approved ${req.restaurantName}!") }
                                                    .onFailure { showSnackbar(it.localizedMessage ?: "Approve failed") }
                                            }
                                        },
                                        onReviewRequest = { req, status, reason ->
                                            adminViewModel.reviewRequest(req, status, reason) { res ->
                                                res.onSuccess { showSnackbar("Request updated: $status") }
                                                    .onFailure { showSnackbar(it.localizedMessage ?: "Action failed") }
                                            }
                                        },
                                        onToggleSuspendRestaurant = { r ->
                                            adminViewModel.toggleRestaurantSuspended(r) { res ->
                                                res.onSuccess { showSnackbar("Updated ${r.name} status") }
                                                    .onFailure { showSnackbar(it.localizedMessage ?: "Update failed") }
                                            }
                                        },
                                        onToggleFeatureRestaurant = { r ->
                                            adminViewModel.toggleRestaurantFeatured(r) { res ->
                                                res.onSuccess { showSnackbar("Updated featured flag for ${r.name}") }
                                                    .onFailure { showSnackbar(it.localizedMessage ?: "Update failed") }
                                            }
                                        },
                                        onDeleteRestaurant = { r ->
                                            adminViewModel.deleteRestaurant(r) { res ->
                                                res.onSuccess { showSnackbar("Deleted ${r.name}") }
                                                    .onFailure { showSnackbar(it.localizedMessage ?: "Delete failed") }
                                            }
                                        },
                                        onUpdateUser = { u, status, role ->
                                            adminViewModel.updateUserStatusOrRole(u, status, role) { res ->
                                                res.onSuccess { showSnackbar("Updated ${u.name}") }
                                                    .onFailure { showSnackbar(it.localizedMessage ?: "Update failed") }
                                            }
                                        },
                                        onRemoveReview = { rev ->
                                            adminViewModel.removeReview(rev) { res ->
                                                res.onSuccess { showSnackbar("Review removed") }
                                                    .onFailure { showSnackbar(it.localizedMessage ?: "Remove failed") }
                                            }
                                        },
                                        onSaveSettings = { settings ->
                                            adminViewModel.saveSettings(settings) { res ->
                                                res.onSuccess { showSnackbar("Platform settings saved!") }
                                                    .onFailure { showSnackbar(it.localizedMessage ?: "Save failed") }
                                            }
                                        },
                                        modifier = contentModifier
                                    )
                                }
                            }

                            AppTab.PROFILE -> ProfileAndDevScreen(
                                userProfile = userProfile,
                                activeWorkspaceRole = activeRole,
                                isDarkMode = isDarkMode,
                                isSeeding = isSeeding,
                                onToggleDarkMode = { sessionViewModel.toggleDarkMode() },
                                onSaveProfile = { name, phone, done ->
                                    sessionViewModel.updateProfile(name, phone) { res ->
                                        done(res)
                                        res.onSuccess { showSnackbar("Profile saved!") }
                                            .onFailure { showSnackbar(it.localizedMessage ?: "Save failed") }
                                    }
                                },
                                onSwitchWorkspaceRole = { role ->
                                    sessionViewModel.switchWorkspaceRole(role)
                                    showSnackbar("Switched workspace to ${role.name.lowercase().replaceFirstChar { it.uppercase() }}")
                                },
                                onSeedSampleData = { sessionViewModel.seedSampleData(forceReseed = true) },
                                onOpenPartnerApply = { showPartnerApply = true },
                                onSignOut = performSignOut,
                                modifier = contentModifier
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNotificationsSheet) {
        NotificationsBottomSheet(
            notifications = notifications,
            onMarkRead = { sessionViewModel.markNotificationRead(it) },
            onDismiss = { showNotificationsSheet = false }
        )
    }
}

@Composable
private fun AdminAccessRequiredCard(
    onReturnToCustomer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AdminPanelSettings,
                    contentDescription = "Admin Required",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = "Admin Super-Control Locked",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Firestore security rules enforce Admin permissions strictly on the server. Sign in with govindsinghpratap123@gmail.com (or an account promoted to Admin) to unlock platform moderation, request approvals, and activity logs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onReturnToCustomer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Return to Customer View")
                }
            }
        }
    }
}

@Composable
private fun BlockedAccountScreen(onSignOut: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Account Suspended",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "This account has been blocked by a TableTrail administrator. Please contact support or sign out.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }
        }
    }
}
