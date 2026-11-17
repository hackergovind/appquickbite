package com.example.ui.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Booking
import com.example.model.MenuCategory
import com.example.model.MenuItem
import com.example.model.Restaurant
import com.example.model.RestaurantRequest
import com.example.model.Review
import com.example.model.TableConfig
import com.example.model.UiState
import com.example.ui.components.AnalyticsBarChart
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadgeChip
import com.example.ui.components.VegIndicatorDot
import com.example.viewmodel.OwnerAnalytics

@Composable
fun OwnerDashboardScreen(
    restaurantsState: UiState<List<Restaurant>>,
    selectedRestaurant: Restaurant?,
    requestsState: UiState<List<RestaurantRequest>>,
    bookingsState: UiState<List<Booking>>,
    reviewsState: UiState<List<Review>>,
    categories: List<MenuCategory>,
    menuItems: List<MenuItem>,
    tables: List<TableConfig>,
    analytics: OwnerAnalytics,
    ownerName: String,
    ownerEmail: String,
    onSelectRestaurant: (String) -> Unit,
    onSubmitPartnerRequest: (RestaurantRequest, (Result<Unit>) -> Unit) -> Unit,
    onUpdateBookingStatus: (Booking, String) -> Unit,
    onToggleDishAvailable: (String, Boolean) -> Unit,
    onSaveDish: (MenuItem) -> Unit,
    onDeleteDish: (String) -> Unit,
    onSaveCategory: (String, String, String?, String, Long) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSaveTable: (TableConfig) -> Unit,
    onDeleteTable: (String) -> Unit,
    onUpdateRestaurantProfile: (
        restaurantId: String,
        name: String,
        desc: String,
        cuisines: List<String>,
        city: String,
        address: String,
        phone: String,
        email: String,
        priceRange: String,
        hours: String,
        holidays: List<String>,
        slots: List<String>
    ) -> Unit,
    onReplyReview: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val restaurants = (restaurantsState as? UiState.Success)?.data ?: emptyList()

    if (restaurantsState is UiState.Success && restaurants.isEmpty()) {
        PartnerOnboardingScreen(
            requestsState = requestsState,
            ownerName = ownerName,
            ownerEmail = ownerEmail,
            onSubmitRequest = onSubmitPartnerRequest,
            modifier = modifier
        )
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showDishDialog by remember { mutableStateOf<MenuItem?>(null) }
    var showNewDishDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showTableDialog by remember { mutableStateOf(false) }
    var showReplyDialog by remember { mutableStateOf<Review?>(null) }
    var showProfileEditDialog by remember { mutableStateOf(false) }
    var showApplyNewPartner by remember { mutableStateOf(false) }

    if (showApplyNewPartner) {
        PartnerOnboardingScreen(
            requestsState = requestsState,
            ownerName = ownerName,
            ownerEmail = ownerEmail,
            onBack = { showApplyNewPartner = false },
            onSubmitRequest = onSubmitPartnerRequest,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header + Restaurant Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Owner Studio", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = selectedRestaurant?.name ?: "Loading kitchen...",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            TextButton(onClick = { showApplyNewPartner = true }) {
                Text("+ Partner Form")
            }
        }

        if (restaurants.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                restaurants.forEach { rest ->
                    FilterChip(
                        selected = rest.id == selectedRestaurant?.id,
                        onClick = { onSelectRestaurant(rest.id) },
                        label = { Text(rest.name) }
                    )
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 12.dp
        ) {
            listOf("Analytics", "Bookings", "Menu (${menuItems.size})", "Tables & Hours", "Reviews").forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                // Analytics + Chart
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricTile("Total Bookings", "${analytics.totalBookings}", Modifier.weight(1f))
                            MetricTile("Today", "${analytics.todayBookings}", Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricTile("Est. Revenue", "$${analytics.estimatedRevenue.toInt()}", Modifier.weight(1f))
                            MetricTile("Avg Rating", "${analytics.averageRating} ★", Modifier.weight(1f))
                        }
                    }
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Bookings by Date", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(10.dp))
                                AnalyticsBarChart(
                                    data = analytics.dailyBookings,
                                    barColor = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Booking Status Mix", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(10.dp))
                                AnalyticsBarChart(
                                    data = analytics.statusCounts.entries.map { it.key to it.value },
                                    barColor = MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                // Bookings management
                val bookings = (bookingsState as? UiState.Success)?.data ?: emptyList()
                if (bookings.isEmpty()) {
                    EmptyStateCard(
                        title = "No reservations yet",
                        subtitle = "Incoming table bookings will appear here for confirmation.",
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(bookings, key = { it.id }) { b ->
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
                                        Column {
                                            Text(b.customerName, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                "${b.date} • ${b.timeSlot} • ${b.partySize} guests",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        StatusBadgeChip(status = b.status)
                                    }
                                    if (b.specialRequests.isNotBlank()) {
                                        Text("Request: ${b.specialRequests}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (b.status == "pending") {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { onUpdateBookingStatus(b, "confirmed") },
                                                shape = RoundedCornerShape(50),
                                                modifier = Modifier.weight(1f)
                                            ) { Text("Accept") }
                                            OutlinedButton(
                                                onClick = { onUpdateBookingStatus(b, "cancelled") },
                                                shape = RoundedCornerShape(50),
                                                modifier = Modifier.weight(1f)
                                            ) { Text("Decline") }
                                        }
                                    } else if (b.status == "confirmed") {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { onUpdateBookingStatus(b, "completed") },
                                                shape = RoundedCornerShape(50),
                                                modifier = Modifier.weight(1f)
                                            ) { Text("Completed") }
                                            OutlinedButton(
                                                onClick = { onUpdateBookingStatus(b, "no-show") },
                                                shape = RoundedCornerShape(50),
                                                modifier = Modifier.weight(1f)
                                            ) { Text("No-Show") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Full Menu CRUD
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showNewDishDialog = true },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f).testTag("add_dish_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Dish")
                            }
                            OutlinedButton(
                                onClick = { showCategoryDialog = true },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+ Category")
                            }
                        }
                    }
                    if (categories.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categories.forEach { cat ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { onDeleteCategory(cat.id) },
                                        label = { Text("${cat.name} ✕") }
                                    )
                                }
                            }
                        }
                    }
                    items(menuItems, key = { it.id }) { dish ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VegIndicatorDot(isVeg = dish.veg)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(dish.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${dish.categoryName} • $${String.format("%.2f", dish.price)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = dish.available,
                                    onCheckedChange = { onToggleDishAvailable(dish.id, it) }
                                )
                                IconButton(onClick = { showDishDialog = dish }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit dish")
                                }
                                IconButton(onClick = { onDeleteDish(dish.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete dish", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                // Tables & Restaurant Hours/Profile
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    selectedRestaurant?.let { r ->
                        item {
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
                                        Text("Hours, Closures & Time Slots", style = MaterialTheme.typography.titleMedium)
                                        TextButton(onClick = { showProfileEditDialog = true }) {
                                            Text("Edit")
                                        }
                                    }
                                    Text("Hours: ${r.openingHours}", style = MaterialTheme.typography.bodySmall)
                                    Text("Time Slots: ${r.timeSlots.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "Holiday Closures: ${r.holidayClosures.ifEmpty { listOf("None") }.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = { showTableDialog = true },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Table Group")
                        }
                    }

                    items(tables, key = { it.id }) { tbl ->
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
                                    Text(tbl.label, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${tbl.count} tables • ${tbl.capacity} seats each",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteTable(tbl.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete table", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            4 -> {
                // Reviews + Owner Reply
                val reviews = (reviewsState as? UiState.Success)?.data ?: emptyList()
                if (reviews.isEmpty()) {
                    EmptyStateCard(
                        title = "No reviews yet",
                        subtitle = "Guest feedback will appear here once completed diners leave ratings.",
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(reviews, key = { it.id }) { rev ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${rev.customerName} • ${rev.rating}★", style = MaterialTheme.typography.titleSmall)
                                        TextButton(onClick = { showReplyDialog = rev }) {
                                            Icon(Icons.Filled.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (rev.ownerReply.isBlank()) "Reply" else "Edit Reply")
                                        }
                                    }
                                    Text(rev.comment, style = MaterialTheme.typography.bodySmall)
                                    if (rev.ownerReply.isNotBlank()) {
                                        Text(
                                            "Your Reply: ${rev.ownerReply}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if ((showNewDishDialog || showDishDialog != null) && selectedRestaurant != null) {
        val editing = showDishDialog
        DishEditorDialog(
            initial = editing ?: MenuItem(
                restaurantId = selectedRestaurant.id,
                ownerId = selectedRestaurant.ownerId,
                categoryId = categories.firstOrNull()?.id ?: "${selectedRestaurant.id}_cat_2",
                categoryName = categories.firstOrNull()?.name ?: "Mains"
            ),
            categories = categories,
            onDismiss = {
                showNewDishDialog = false
                showDishDialog = null
            },
            onSave = { item ->
                onSaveDish(item)
                showNewDishDialog = false
                showDishDialog = null
            }
        )
    }

    if (showCategoryDialog && selectedRestaurant != null) {
        var catName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Add Menu Category") },
            text = {
                OutlinedTextField(
                    value = catName,
                    onValueChange = { catName = it },
                    label = { Text("Category Name (e.g. Brunch)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (catName.isNotBlank()) {
                        onSaveCategory(
                            selectedRestaurant.id,
                            selectedRestaurant.ownerId,
                            null,
                            catName.trim(),
                            (categories.size + 1).toLong()
                        )
                        showCategoryDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showCategoryDialog = false }) { Text("Cancel") } }
        )
    }

    if (showTableDialog && selectedRestaurant != null) {
        var label by remember { mutableStateOf("") }
        var cap by remember { mutableStateOf("4") }
        var cnt by remember { mutableStateOf("3") }
        AlertDialog(
            onDismissRequest = { showTableDialog = false },
            title = { Text("Add Table Group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label (e.g. Patio 4-Top)") })
                    OutlinedTextField(value = cap, onValueChange = { cap = it.filter { c -> c.isDigit() } }, label = { Text("Seats per Table") })
                    OutlinedTextField(value = cnt, onValueChange = { cnt = it.filter { c -> c.isDigit() } }, label = { Text("Number of Tables") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (label.isNotBlank()) {
                        onSaveTable(
                            TableConfig(
                                restaurantId = selectedRestaurant.id,
                                ownerId = selectedRestaurant.ownerId,
                                label = label.trim(),
                                capacity = cap.toLongOrNull()?.coerceIn(1L, 24L) ?: 4L,
                                count = cnt.toLongOrNull()?.coerceIn(1L, 100L) ?: 2L,
                                active = true
                            )
                        )
                        showTableDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showTableDialog = false }) { Text("Cancel") } }
        )
    }

    showReplyDialog?.let { rev ->
        var replyText by remember { mutableStateOf(rev.ownerReply) }
        AlertDialog(
            onDismissRequest = { showReplyDialog = null },
            title = { Text("Reply to ${rev.customerName}") },
            text = {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    label = { Text("Your Response") },
                    minLines = 2
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (replyText.isNotBlank()) {
                        onReplyReview(rev.id, replyText.trim())
                        showReplyDialog = null
                    }
                }) { Text("Post Reply") }
            },
            dismissButton = { TextButton(onClick = { showReplyDialog = null }) { Text("Cancel") } }
        )
    }

    if (showProfileEditDialog && selectedRestaurant != null) {
        var hours by remember { mutableStateOf(selectedRestaurant.openingHours) }
        var slots by remember { mutableStateOf(selectedRestaurant.timeSlots.joinToString(", ")) }
        var holidays by remember { mutableStateOf(selectedRestaurant.holidayClosures.joinToString(", ")) }
        var desc by remember { mutableStateOf(selectedRestaurant.description) }
        AlertDialog(
            onDismissRequest = { showProfileEditDialog = false },
            title = { Text("Edit ${selectedRestaurant.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, maxLines = 3)
                    OutlinedTextField(value = hours, onValueChange = { hours = it }, label = { Text("Opening Hours") })
                    OutlinedTextField(value = slots, onValueChange = { slots = it }, label = { Text("Time Slots (comma-separated)") })
                    OutlinedTextField(value = holidays, onValueChange = { holidays = it }, label = { Text("Holiday Dates (YYYY-MM-DD)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val slotList = slots.split(",").map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { listOf("19:00") }
                    val holList = holidays.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onUpdateRestaurantProfile(
                        selectedRestaurant.id,
                        selectedRestaurant.name,
                        desc,
                        selectedRestaurant.cuisines,
                        selectedRestaurant.city,
                        selectedRestaurant.address,
                        selectedRestaurant.phone,
                        selectedRestaurant.email,
                        selectedRestaurant.priceRange,
                        hours,
                        holList,
                        slotList
                    )
                    showProfileEditDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showProfileEditDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DishEditorDialog(
    initial: MenuItem,
    categories: List<MenuCategory>,
    onDismiss: () -> Unit,
    onSave: (MenuItem) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var desc by remember { mutableStateOf(initial.description) }
    var price by remember { mutableStateOf(if (initial.price > 0) initial.price.toString() else "18.0") }
    var imageUrl by remember { mutableStateOf(initial.imageUrl) }
    var catName by remember { mutableStateOf(initial.categoryName.ifBlank { "Mains" }) }
    var veg by remember { mutableStateOf(initial.veg) }
    var popular by remember { mutableStateOf(initial.popular) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "New Menu Dish" else "Edit ${initial.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Dish Name") }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, maxLines = 2)
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price ($)") }, singleLine = true)
                OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Photo URL") }, singleLine = true)
                if (categories.isNotEmpty()) {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.forEach { c ->
                            FilterChip(
                                selected = catName == c.name,
                                onClick = { catName = c.name },
                                label = { Text(c.name) }
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = veg, onCheckedChange = { veg = it })
                    Text("Vegetarian", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(12.dp))
                    Checkbox(checked = popular, onCheckedChange = { popular = it })
                    Text("Popular Tag", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    val matchedCat = categories.firstOrNull { it.name == catName }
                    onSave(
                        initial.copy(
                            name = name.trim(),
                            description = desc.trim(),
                            price = price.toDoubleOrNull() ?: 18.0,
                            imageUrl = imageUrl.trim(),
                            categoryId = matchedCat?.id ?: initial.categoryId,
                            categoryName = catName,
                            veg = veg,
                            popular = popular
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
