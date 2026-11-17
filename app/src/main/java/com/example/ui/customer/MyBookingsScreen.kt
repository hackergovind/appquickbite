package com.example.ui.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Booking
import com.example.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FoodNetworkImage
import com.example.ui.components.SkeletonCard
import com.example.ui.components.StatusBadgeChip
import com.example.ui.theme.SaffronGold

@Composable
fun MyBookingsScreen(
    bookingsState: UiState<List<Booking>>,
    customerName: String,
    onExploreClick: () -> Unit,
    onCancelBooking: (Booking) -> Unit,
    onModifyBooking: (Booking, String, String, Long, Long, String) -> Unit,
    onSubmitReview: (Booking, String, Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var reviewTarget by remember { mutableStateOf<Booking?>(null) }
    var modifyTarget by remember { mutableStateOf<Booking?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "My Bookings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Upcoming") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Past & Closed") }
            )
        }

        when (bookingsState) {
            is UiState.Loading -> {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SkeletonCard()
                    SkeletonCard()
                }
            }
            is UiState.Error -> {
                EmptyStateCard(
                    title = "Couldn't load bookings",
                    subtitle = bookingsState.message,
                    modifier = Modifier.padding(16.dp)
                )
            }
            is UiState.Success -> {
                val upcoming = bookingsState.data.filter { it.status == "pending" || it.status == "confirmed" }
                val past = bookingsState.data.filter { it.status in listOf("completed", "cancelled", "no-show") }
                val activeList = if (selectedTab == 0) upcoming else past

                if (activeList.isEmpty()) {
                    EmptyStateCard(
                        title = if (selectedTab == 0) "No upcoming reservations" else "No past bookings yet",
                        subtitle = "Discover curated neighborhood restaurants and book a table in seconds.",
                        actionLabel = "Browse Restaurants",
                        onAction = onExploreClick,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(activeList, key = { it.id }) { booking ->
                            BookingItemCard(
                                booking = booking,
                                onCancel = { onCancelBooking(booking) },
                                onModify = { modifyTarget = booking },
                                onReview = { reviewTarget = booking }
                            )
                        }
                    }
                }
            }
        }
    }

    reviewTarget?.let { booking ->
        ReviewBookingDialog(
            restaurantName = booking.restaurantName,
            onDismiss = { reviewTarget = null },
            onSubmit = { rating, comment ->
                onSubmitReview(booking, customerName, rating, comment)
                reviewTarget = null
            }
        )
    }

    modifyTarget?.let { booking ->
        ModifyBookingDialog(
            booking = booking,
            onDismiss = { modifyTarget = null },
            onSave = { newSlot, newPartySize, newRequests ->
                val epoch = parseDateSlotMillis(booking.date, newSlot)
                onModifyBooking(booking, booking.date, newSlot, epoch, newPartySize, newRequests)
                modifyTarget = null
            }
        )
    }
}

@Composable
private fun BookingItemCard(
    booking: Booking,
    onCancel: () -> Unit,
    onModify: () -> Unit,
    onReview: () -> Unit
) {
    val twoHoursMs = 2L * 3600L * 1000L
    val canModifyOrCancel = (booking.status == "pending" || booking.status == "confirmed") &&
        (booking.bookingEpochMillis - System.currentTimeMillis() >= twoHoursMs)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("booking_card_${booking.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FoodNetworkImage(
                    url = booking.restaurantImageUrl,
                    contentDescription = booking.restaurantName,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(booking.restaurantName, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${booking.date} at ${booking.timeSlot}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Group,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${booking.partySize} guests • ${booking.tableLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusBadgeChip(status = booking.status)
            }

            if (booking.specialRequests.isNotBlank()) {
                Text(
                    text = "Note: ${booking.specialRequests}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (booking.status == "pending" || booking.status == "confirmed") {
                if (!canModifyOrCancel) {
                    Text(
                        text = "Within 2 hours of reservation — online changes & cancellations are locked.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onModify,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Modify")
                        }
                        OutlinedButton(
                            onClick = onCancel,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } else if (booking.status == "completed" && !booking.reviewed) {
                Button(
                    onClick = onReview,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.RateReview, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rate & Review Experience")
                }
            } else if (booking.reviewed) {
                Text(
                    text = "Thanks for leaving a review!",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ReviewBookingDialog(
    restaurantName: String,
    onDismiss: () -> Unit,
    onSubmit: (Long, String) -> Unit
) {
    var rating by remember { mutableLongStateOf(5L) }
    var comment by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review $restaurantName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    (1L..5L).forEach { star ->
                        IconButton(onClick = { rating = star }) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "$star stars",
                                tint = SaffronGold,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("How was the food & service?") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (comment.trim().length < 2) {
                    error = "Please write at least a short comment."
                } else {
                    onSubmit(rating, comment.trim())
                }
            }) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ModifyBookingDialog(
    booking: Booking,
    onDismiss: () -> Unit,
    onSave: (String, Long, String) -> Unit
) {
    var slot by remember { mutableStateOf(booking.timeSlot) }
    var partySizeText by remember { mutableStateOf(booking.partySize.toString()) }
    var requests by remember { mutableStateOf(booking.specialRequests) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modify ${booking.restaurantName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Date: ${booking.date}", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("18:00", "19:30", "21:00").forEach { s ->
                        Text(
                            text = s,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (slot == s) FontWeight.Bold else FontWeight.Normal,
                            color = if (slot == s) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (slot == s) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { slot = s }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = partySizeText,
                    onValueChange = { partySizeText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Party Size") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = requests,
                    onValueChange = { requests = it },
                    label = { Text("Special Requests") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val pSize = partySizeText.toLongOrNull()?.coerceIn(1L, 20L) ?: booking.partySize
                onSave(slot, pSize, requests)
            }) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
