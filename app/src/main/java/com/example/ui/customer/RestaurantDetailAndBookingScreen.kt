package com.example.ui.customer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.MenuCategory
import com.example.model.MenuItem
import com.example.model.Restaurant
import com.example.model.Review
import com.example.model.SlotBookingCount
import com.example.model.TableConfig
import com.example.ui.components.FoodNetworkImage
import com.example.ui.components.RatingPill
import com.example.ui.components.VegIndicatorDot
import com.example.ui.theme.HerbGreen
import com.example.ui.theme.RoastedOrange
import com.example.ui.theme.SaffronGold
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantDetailScreen(
    restaurant: Restaurant,
    categories: List<MenuCategory>,
    menuItems: List<MenuItem>,
    tables: List<TableConfig>,
    slotBookings: List<SlotBookingCount>,
    reviews: List<Review>,
    isFavorite: Boolean,
    defaultGuestName: String,
    defaultGuestPhone: String,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onBookTable: (
        date: String,
        timeSlot: String,
        epochMillis: Long,
        partySize: Long,
        guestName: String,
        guestPhone: String,
        specialRequests: String,
        onDone: (Result<String>) -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("All") }
    var showBookingSheet by remember { mutableStateOf(false) }

    val gallery = remember(restaurant) {
        (listOf(restaurant.coverPhotoUrl) + restaurant.galleryUrls).filter { it.isNotBlank() }.distinct()
    }
    val categoryNames = remember(categories, menuItems) {
        val fromCats = categories.map { it.name }
        val fromItems = menuItems.map { it.categoryName }.distinct()
        listOf("All") + (fromCats + fromItems).distinct()
    }
    val visibleMenu = remember(menuItems, selectedCategory) {
        if (selectedCategory == "All") menuItems
        else menuItems.filter { it.categoryName.equals(selectedCategory, ignoreCase = true) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${restaurant.priceRange} • ${restaurant.cuisines.firstOrNull() ?: "Kitchen"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${tables.sumOf { it.count }.coerceAtLeast(4)} tables available",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { showBookingSheet = true },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("open_booking_sheet_button")
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Book a Table", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Gallery Header + Overlay Controls
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = if (gallery.size > 1) 16.dp else 0.dp)
                    ) {
                        items(gallery.ifEmpty { listOf("") }) { url ->
                            FoodNetworkImage(
                                url = url,
                                contentDescription = restaurant.name,
                                modifier = Modifier
                                    .then(
                                        if (gallery.size > 1) Modifier.width(320.dp)
                                        else Modifier.fillParentMaxWidth()
                                    )
                                    .height(230.dp)
                                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp, topStart = if (gallery.size > 1) 18.dp else 0.dp, topEnd = if (gallery.size > 1) 18.dp else 0.dp))
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.92f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("detail_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.Black
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.92f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier.testTag("detail_favorite_button")
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.Black
                                )
                            }
                        }
                    }
                }
            }

            // Info Block
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = restaurant.name,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.weight(1f)
                        )
                        RatingPill(rating = restaurant.ratingAvg, reviewCount = restaurant.reviewCount)
                    }
                    Text(
                        text = "${restaurant.cuisines.joinToString(" • ")}  |  ${restaurant.priceRange}  |  ${restaurant.city}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = restaurant.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InfoLine(Icons.Filled.LocationOn, "${restaurant.address}, ${restaurant.city}")
                            InfoLine(Icons.Filled.AccessTime, "Hours: ${restaurant.openingHours}")
                            InfoLine(Icons.Filled.Phone, "${restaurant.phone} • ${restaurant.email}")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (restaurant.holidayClosures.isNotEmpty()) {
                                    Text(
                                        text = "Holiday closures: ${restaurant.holidayClosures.joinToString(", ")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RoastedOrange,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                                OutlinedButton(
                                    onClick = {
                                        val geoUri = Uri.parse("geo:0,0?q=" + Uri.encode("${restaurant.address}, ${restaurant.city}"))
                                        val intent = Intent(Intent.ACTION_VIEW, geoUri)
                                        runCatching { context.startActivity(intent) }
                                    },
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Map", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            // Menu Header & Categories
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Menu",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryNames.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            }

            if (visibleMenu.isEmpty()) {
                item {
                    Text(
                        text = "No dishes listed in this category yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                items(visibleMenu, key = { it.id }) { dish ->
                    MenuDishRowCard(
                        dish = dish,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Reviews Section
            item {
                Text(
                    text = "Guest Reviews (${reviews.size})",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (reviews.isEmpty()) {
                item {
                    Text(
                        text = "Be the first guest to review after dining here!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                items(reviews, key = { it.id }) { review ->
                    ReviewCard(
                        review = review,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    if (showBookingSheet) {
        BookingBottomSheet(
            restaurant = restaurant,
            tables = tables,
            slotBookings = slotBookings,
            defaultName = defaultGuestName,
            defaultPhone = defaultGuestPhone,
            onDismiss = { showBookingSheet = false },
            onConfirm = { date, slot, epochMs, partySize, name, phone, notes, done ->
                onBookTable(date, slot, epochMs, partySize, name, phone, notes) { res ->
                    done(res)
                    if (res.isSuccess) showBookingSheet = false
                }
            }
        )
    }
}

@Composable
private fun InfoLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MenuDishRowCard(dish: MenuItem, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VegIndicatorDot(isVeg = dish.veg)
                    if (dish.popular) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "POPULAR",
                                style = MaterialTheme.typography.labelSmall,
                                color = RoastedOrange,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (!dish.available) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "SOLD OUT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = dish.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dish.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$${String.format("%.2f", dish.price)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            FoodNetworkImage(
                url = dish.imageUrl,
                contentDescription = dish.name,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
        }
    }
}

@Composable
private fun ReviewCard(review: Review, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(review.customerName, style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(review.rating.toInt().coerceIn(1, 5)) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = SaffronGold,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (review.ownerReply.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Owner Response",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = review.ownerReply,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingBottomSheet(
    restaurant: Restaurant,
    tables: List<TableConfig>,
    slotBookings: List<SlotBookingCount>,
    defaultName: String,
    defaultPhone: String,
    onDismiss: () -> Unit,
    onConfirm: (
        date: String,
        slot: String,
        epochMillis: Long,
        partySize: Long,
        name: String,
        phone: String,
        notes: String,
        onDone: (Result<String>) -> Unit
    ) -> Unit
) {
    val next7Days = remember {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val disp = SimpleDateFormat("EEE, MMM d", Locale.US)
        (0..6).map { offset ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
            fmt.format(cal.time) to disp.format(cal.time)
        }
    }

    var selectedDate by remember { mutableStateOf(next7Days.first().first) }
    var partySize by remember { mutableIntStateOf(2) }
    var selectedSlot by remember { mutableStateOf(restaurant.timeSlots.firstOrNull() ?: "19:30") }
    var guestName by remember { mutableStateOf(defaultName) }
    var guestPhone by remember { mutableStateOf(defaultPhone) }
    var specialRequests by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val totalTables = remember(tables) {
        tables.filter { it.active }.sumOf { it.count }.coerceAtLeast(1L)
    }
    val maxSingleTableCap = remember(tables) {
        tables.filter { it.active }.maxOfOrNull { it.capacity } ?: 6L
    }
    val isHoliday = restaurant.holidayClosures.contains(selectedDate)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Reserve a Table at ${restaurant.name}",
                style = MaterialTheme.typography.headlineSmall
            )

            // Date selector
            Text("Select Date", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                next7Days.forEach { (iso, label) ->
                    val closed = restaurant.holidayClosures.contains(iso)
                    FilterChip(
                        selected = selectedDate == iso,
                        onClick = { selectedDate = iso },
                        label = { Text(if (closed) "$label (Closed)" else label) }
                    )
                }
            }

            // Party size stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Party Size", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Max single table: $maxSingleTableCap guests",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (partySize > 1) partySize-- }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease guests")
                    }
                    Text(
                        text = "$partySize",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = { if (partySize < maxSingleTableCap.toInt().coerceAtLeast(8)) partySize++ }) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase guests")
                    }
                }
            }

            // Live Slot Capacity Check
            Text("Live Time Slots", style = MaterialTheme.typography.titleSmall)
            if (isHoliday) {
                Text(
                    text = "Closed for holiday on $selectedDate. Please choose another date.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    restaurant.timeSlots.forEach { slot ->
                        val slotCount = slotBookings.firstOrNull { it.date == selectedDate && it.timeSlot == slot }
                        val bookedTables = slotCount?.bookedTables ?: 0L
                        val remaining = (totalTables - bookedTables).coerceAtLeast(0L)
                        val isFull = remaining <= 0L
                        FilterChip(
                            selected = selectedSlot == slot && !isFull,
                            enabled = !isFull,
                            onClick = { selectedSlot = slot },
                            label = {
                                Text(if (isFull) "$slot (Full)" else "$slot • $remaining left")
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = guestName,
                onValueChange = { guestName = it },
                label = { Text("Guest Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = guestPhone,
                onValueChange = { guestPhone = it },
                label = { Text("Phone Number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = specialRequests,
                onValueChange = { specialRequests = it },
                label = { Text("Special Requests (optional)") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMsg != null) {
                Text(
                    text = errorMsg ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    if (isHoliday) {
                        errorMsg = "Restaurant is closed on $selectedDate."
                        return@Button
                    }
                    if (guestName.isBlank()) {
                        errorMsg = "Please enter a guest name."
                        return@Button
                    }
                    val epochMs = parseDateSlotMillis(selectedDate, selectedSlot)
                    if (epochMs <= System.currentTimeMillis()) {
                        errorMsg = "Selected slot has already passed today. Choose a later time or tomorrow."
                        return@Button
                    }
                    errorMsg = null
                    isSubmitting = true
                    onConfirm(
                        selectedDate,
                        selectedSlot,
                        epochMs,
                        partySize.toLong(),
                        guestName,
                        guestPhone,
                        specialRequests
                    ) { res ->
                        isSubmitting = false
                        res.onFailure { errorMsg = it.localizedMessage ?: "Booking failed." }
                    }
                },
                enabled = !isSubmitting && !isHoliday,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(bottom = 16.dp)
                    .testTag("confirm_booking_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Confirm Reservation", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun parseDateSlotMillis(dateStr: String, timeSlot: String): Long {
    return try {
        val parts = timeSlot.trim().split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 19
        val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsed = sdf.parse(dateStr) ?: return System.currentTimeMillis() + 3600_000L
        val cal = Calendar.getInstance().apply {
            time = parsed
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, min)
            set(Calendar.SECOND, 0)
        }
        cal.timeInMillis
    } catch (_: Exception) {
        System.currentTimeMillis() + 3600_000L
    }
}
