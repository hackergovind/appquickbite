package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ActivityLog
import com.example.model.Booking
import com.example.model.PlatformSettings
import com.example.model.Restaurant
import com.example.model.RestaurantRequest
import com.example.model.Review
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadgeChip

@Composable
fun AdminDashboardScreen(
    usersState: UiState<List<UserProfile>>,
    restaurantsState: UiState<List<Restaurant>>,
    requestsState: UiState<List<RestaurantRequest>>,
    bookingsState: UiState<List<Booking>>,
    reviewsState: UiState<List<Review>>,
    logsState: UiState<List<ActivityLog>>,
    platformSettings: PlatformSettings,
    onApproveRequest: (RestaurantRequest) -> Unit,
    onReviewRequest: (RestaurantRequest, String, String) -> Unit,
    onToggleSuspendRestaurant: (Restaurant) -> Unit,
    onToggleFeatureRestaurant: (Restaurant) -> Unit,
    onDeleteRestaurant: (Restaurant) -> Unit,
    onUpdateUser: (UserProfile, String, String) -> Unit,
    onRemoveReview: (Review) -> Unit,
    onSaveSettings: (PlatformSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val users = (usersState as? UiState.Success)?.data ?: emptyList()
    val restaurants = (restaurantsState as? UiState.Success)?.data ?: emptyList()
    val requests = (requestsState as? UiState.Success)?.data ?: emptyList()
    val bookings = (bookingsState as? UiState.Success)?.data ?: emptyList()
    val reviews = (reviewsState as? UiState.Success)?.data ?: emptyList()
    val logs = (logsState as? UiState.Success)?.data ?: emptyList()

    val pendingCount = requests.count { it.status == "pending" || it.status == "info_requested" }

    var selectedTab by remember { mutableIntStateOf(0) }
    var rejectOrInfoTarget by remember { mutableStateOf<Pair<RestaurantRequest, String>?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("Admin Control Center", style = MaterialTheme.typography.headlineMedium)
            if (usersState is UiState.Error) {
                Text(
                    text = "Admin privileges required for super-control operations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Stats Summary Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatBadge("Users", users.size.toString(), Modifier.weight(1f))
            StatBadge("Kitchens", restaurants.size.toString(), Modifier.weight(1f))
            StatBadge("Bookings", bookings.size.toString(), Modifier.weight(1f))
            StatBadge("Pending", pendingCount.toString(), Modifier.weight(1f))
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 12.dp
        ) {
            listOf("Requests ($pendingCount)", "Restaurants", "Users", "Moderation", "Settings & Logs").forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(label) }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                if (requests.isEmpty()) {
                    EmptyStateCard(
                        title = "No partner requests",
                        subtitle = "New restaurant onboarding applications will show up here.",
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(requests, key = { it.id }) { req ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(req.restaurantName, style = MaterialTheme.typography.titleMedium)
                                        StatusBadgeChip(status = req.status)
                                    }
                                    Text("Owner: ${req.ownerName} • ${req.email}", style = MaterialTheme.typography.bodySmall)
                                    Text("${req.address}, ${req.city} • License: ${req.licenseNumber}", style = MaterialTheme.typography.bodySmall)
                                    Text(req.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (req.status == "pending" || req.status == "info_requested") {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = { onApproveRequest(req) },
                                                shape = RoundedCornerShape(50),
                                                modifier = Modifier.weight(1f)
                                            ) { Text("Approve") }
                                            OutlinedButton(
                                                onClick = { rejectOrInfoTarget = req to "info_requested" },
                                                shape = RoundedCornerShape(50),
                                                modifier = Modifier.weight(1f)
                                            ) { Text("More Info") }
                                            OutlinedButton(
                                                onClick = { rejectOrInfoTarget = req to "rejected" },
                                                shape = RoundedCornerShape(50),
                                                modifier = Modifier.weight(1f)
                                            ) { Text("Reject", color = MaterialTheme.colorScheme.error) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(restaurants, key = { it.id }) { r ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(r.name, style = MaterialTheme.typography.titleMedium)
                                        Text("${r.city} • ${r.cuisines.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    StatusBadgeChip(status = r.status)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { onToggleFeatureRestaurant(r) },
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (r.featured) "Unfeature" else "Feature")
                                    }
                                    OutlinedButton(
                                        onClick = { onToggleSuspendRestaurant(r) },
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (r.status == "active") "Suspend" else "Reactivate")
                                    }
                                    OutlinedButton(
                                        onClick = { onDeleteRestaurant(r) },
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Delete", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(users, key = { it.uid.ifBlank { it.id } }) { u ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(u.name, style = MaterialTheme.typography.titleSmall)
                                        Text(u.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusBadgeChip(status = u.status)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("customer", "owner", "admin").forEach { roleOpt ->
                                        FilterChip(
                                            selected = u.role == roleOpt,
                                            onClick = { onUpdateUser(u, u.status, roleOpt) },
                                            label = { Text(roleOpt.replaceFirstChar { it.uppercase() }) }
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    TextButton(onClick = {
                                        val nextStatus = if (u.status == "active") "blocked" else "active"
                                        onUpdateUser(u, nextStatus, u.role)
                                    }) {
                                        Text(
                                            text = if (u.status == "active") "Block" else "Unblock",
                                            color = if (u.status == "active") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text("Recent Reviews Moderation", style = MaterialTheme.typography.titleLarge)
                    }
                    items(reviews, key = { it.id }) { rev ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${rev.customerName} • ${rev.rating}★ (${rev.status})", style = MaterialTheme.typography.titleSmall)
                                    Text(rev.comment, style = MaterialTheme.typography.bodySmall)
                                }
                                if (rev.status == "visible") {
                                    TextButton(onClick = { onRemoveReview(rev) }) {
                                        Text("Remove", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("All Platform Bookings (${bookings.size})", style = MaterialTheme.typography.titleLarge)
                    }
                    items(bookings.take(15), key = { it.id }) { b ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${b.customerName} @ ${b.restaurantName}", style = MaterialTheme.typography.titleSmall)
                                    Text("${b.date} ${b.timeSlot} • ${b.partySize} guests", style = MaterialTheme.typography.bodySmall)
                                }
                                StatusBadgeChip(status = b.status)
                            }
                        }
                    }
                }
            }

            4 -> {
                var cuisinesText by remember(platformSettings) { mutableStateOf(platformSettings.cuisines.joinToString(", ")) }
                var citiesText by remember(platformSettings) { mutableStateOf(platformSettings.cities.joinToString(", ")) }
                var bannerTitle by remember(platformSettings) { mutableStateOf(platformSettings.heroBannerTitle) }
                var bannerSub by remember(platformSettings) { mutableStateOf(platformSettings.heroBannerSubtitle) }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Platform Settings", style = MaterialTheme.typography.titleLarge)
                                OutlinedTextField(value = cuisinesText, onValueChange = { cuisinesText = it }, label = { Text("Cuisine Categories") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = citiesText, onValueChange = { citiesText = it }, label = { Text("Supported Cities") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = bannerTitle, onValueChange = { bannerTitle = it }, label = { Text("Homepage Banner Title") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = bannerSub, onValueChange = { bannerSub = it }, label = { Text("Homepage Banner Subtitle") }, modifier = Modifier.fillMaxWidth())
                                Button(
                                    onClick = {
                                        onSaveSettings(
                                            platformSettings.copy(
                                                cuisines = cuisinesText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                                cities = citiesText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                                heroBannerTitle = bannerTitle,
                                                heroBannerSubtitle = bannerSub
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Save Platform Settings")
                                }
                            }
                        }
                    }

                    item {
                        Text("Admin Activity Log (${logs.size})", style = MaterialTheme.typography.titleLarge)
                    }
                    items(logs, key = { it.id }) { log ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("${log.action} • ${log.targetType}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Text(log.details, style = MaterialTheme.typography.bodySmall)
                                Text("By ${log.adminEmail}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    rejectOrInfoTarget?.let { (req, targetStatus) ->
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { rejectOrInfoTarget = null },
            title = { Text(if (targetStatus == "rejected") "Reject ${req.restaurantName}" else "Request Info from ${req.restaurantName}") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason / Instructions for Owner") },
                    minLines = 2
                )
            },
            confirmButton = {
                Button(
                    colors = if (targetStatus == "rejected") ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
                    onClick = {
                        if (reason.isNotBlank()) {
                            onReviewRequest(req, targetStatus, reason.trim())
                            rejectOrInfoTarget = null
                        }
                    }
                ) {
                    Text(if (targetStatus == "rejected") "Confirm Reject" else "Send Request")
                }
            },
            dismissButton = { TextButton(onClick = { rejectOrInfoTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StatBadge(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
