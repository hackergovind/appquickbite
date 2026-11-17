package com.example.ui.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Favorite
import com.example.model.MenuItem
import com.example.model.PlatformSettings
import com.example.model.Restaurant
import com.example.model.RestaurantFilters
import com.example.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FoodNetworkImage
import com.example.ui.components.RatingPill
import com.example.ui.components.SkeletonCard
import com.example.ui.components.VegIndicatorDot
import com.example.ui.theme.HerbGreen
import com.example.ui.theme.SaffronGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    restaurantsState: UiState<List<Restaurant>>,
    filteredRestaurants: List<Restaurant>,
    popularDishesState: UiState<List<MenuItem>>,
    favorites: List<Favorite>,
    platformSettings: PlatformSettings,
    filters: RestaurantFilters,
    selectedCity: String,
    unreadNotificationCount: Int,
    onSelectCity: (String) -> Unit,
    onUpdateFilters: ((RestaurantFilters) -> RestaurantFilters) -> Unit,
    onClearFilters: () -> Unit,
    onOpenRestaurant: (Restaurant) -> Unit,
    onToggleFavorite: (Restaurant, Boolean) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenPartnerApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCityMenu by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val favoriteIds = remember(favorites) { favorites.map { it.restaurantId }.toSet() }
    val allRestaurants = (restaurantsState as? UiState.Success)?.data ?: emptyList()

    val cuisinesList = remember(platformSettings) {
        listOf("Featured") + platformSettings.cuisines
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Brand + Notifications + Profile Header (matches reference screenshot)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Restaurant,
                            contentDescription = "TableTrail",
                            tint = Color.White,
                            modifier = Modifier.padding(8.dp).size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "TableTrail",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "TABLES WORTH THE TRAIL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .size(44.dp)
                            .clickable(onClick = onOpenNotifications)
                            .testTag("notifications_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            if (unreadNotificationCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(7.dp)
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .size(44.dp)
                            .clickable(onClick = onOpenProfile)
                            .testTag("profile_header_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Location Selector & Search Pill Row (like reference location bar)
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { showCityMenu = true }
                                    .testTag("city_selector")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = "City",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Dining in",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = selectedCity,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowDown,
                                            contentDescription = "Choose city",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = showCityMenu,
                                onDismissRequest = { showCityMenu = false }
                            ) {
                                (listOf("All Cities") + platformSettings.cities).forEach { city ->
                                    DropdownMenuItem(
                                        text = { Text(city) },
                                        onClick = {
                                            onSelectCity(city)
                                            showCityMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clickable { showFilterSheet = true }
                                .testTag("open_filters_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FilterList,
                                    contentDescription = "Filter",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Filters",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = filters.query,
                    onValueChange = { q -> onUpdateFilters { it.copy(query = q) } },
                    placeholder = { Text("Search restaurants, dishes, cuisines...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (filters.query.isNotEmpty()) {
                            IconButton(onClick = { onUpdateFilters { it.copy(query = "") } }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_restaurants_input")
                )
            }
        }

        // 3. Bold Crimson Hero Banner (directly inspired by uploaded reference image)
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable {
                        val target = filteredRestaurants.firstOrNull() ?: allRestaurants.firstOrNull()
                        if (target != null) onOpenRestaurant(target)
                    }
                    .testTag("hero_banner_card")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_feast),
                    contentDescription = "Featured culinary experience",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xF08B1313),
                                    Color(0xBD8B1313),
                                    Color(0x22000000)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.fillMaxWidth(0.65f)) {
                        Text(
                            text = platformSettings.heroBannerTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = platformSettings.heroBannerSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 2
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = platformSettings.heroBannerCta,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF8B1313),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF8B1313),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Popular Cuisines Square Icon Tiles Row (matching reference screenshot)
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cuisinesList) { cuisine ->
                    val selected = (filters.cuisine ?: "Featured") == cuisine
                    CuisineCategoryTile(
                        label = cuisine,
                        selected = selected,
                        icon = cuisineIcon(cuisine),
                        onClick = {
                            onUpdateFilters {
                                it.copy(cuisine = if (cuisine == "Featured") null else cuisine)
                            }
                        }
                    )
                }
            }
        }

        // Active Filter Summary Banner if filters applied
        if (filters.priceRange != null || filters.dietaryTag != null || filters.minRating > 0.0 || filters.cuisine != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    filters.cuisine?.let {
                        FilterChip(selected = true, onClick = { onUpdateFilters { f -> f.copy(cuisine = null) } }, label = { Text(it) })
                    }
                    filters.priceRange?.let {
                        FilterChip(selected = true, onClick = { onUpdateFilters { f -> f.copy(priceRange = null) } }, label = { Text(it) })
                    }
                    filters.dietaryTag?.let {
                        FilterChip(selected = true, onClick = { onUpdateFilters { f -> f.copy(dietaryTag = null) } }, label = { Text(it.uppercase()) })
                    }
                    if (filters.minRating > 0.0) {
                        FilterChip(selected = true, onClick = { onUpdateFilters { f -> f.copy(minRating = 0.0) } }, label = { Text("${filters.minRating}+★") })
                    }
                    TextButton(onClick = onClearFilters) {
                        Text("Reset All")
                    }
                }
            }
        }

        // 5. Top-Rated / Popular Dishes Row (like "Popular Picks" in reference screenshot)
        val popularDishes = (popularDishesState as? UiState.Success)?.data ?: emptyList()
        if (popularDishes.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Popular Picks",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Top Rated Dishes",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(popularDishes.take(8), key = { it.id }) { dish ->
                            val parentRest = allRestaurants.firstOrNull { it.id == dish.restaurantId }
                            PopularDishCard(
                                dish = dish,
                                restaurantName = parentRest?.name ?: "",
                                onClick = {
                                    if (parentRest != null) onOpenRestaurant(parentRest)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 6. Featured & All Approved Restaurants Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (filters.cuisine != null || filters.query.isNotBlank()) "Matching Kitchens (${filteredRestaurants.size})" else "Featured & Approved Tables",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        when (restaurantsState) {
            is UiState.Loading -> {
                items(3) {
                    SkeletonCard(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
            is UiState.Error -> {
                item {
                    EmptyStateCard(
                        title = "Couldn't load restaurants",
                        subtitle = restaurantsState.message,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            is UiState.Success -> {
                if (filteredRestaurants.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = if (allRestaurants.isEmpty()) "No restaurants yet" else "No matching tables",
                            subtitle = if (allRestaurants.isEmpty()) {
                                "Sign in with the Admin account or tap Seed Sample Data in Profile > Dev Panel to populate 6 curated restaurants."
                            } else {
                                "Try clearing your dietary or price filters to discover more kitchens."
                            },
                            actionLabel = if (allRestaurants.isNotEmpty()) "Clear Filters" else null,
                            onAction = if (allRestaurants.isNotEmpty()) onClearFilters else null,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    items(filteredRestaurants, key = { it.id }) { restaurant ->
                        RestaurantDiscoveryCard(
                            restaurant = restaurant,
                            isFavorite = favoriteIds.contains(restaurant.id),
                            onToggleFavorite = { onToggleFavorite(restaurant, favoriteIds.contains(restaurant.id)) },
                            onClick = { onOpenRestaurant(restaurant) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }

        // 7. "How TableTrail Works" Section
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "How TableTrail Works",
                        style = MaterialTheme.typography.titleLarge
                    )
                    HowItWorksStep("1", "Discover Approved Kitchens", "Filter verified dining rooms by cuisine, price, and dietary options.")
                    HowItWorksStep("2", "Pick a Live Table Slot", "Real-time table availability prevents double-bookings automatically.")
                    HowItWorksStep("3", "Dine & Share Reviews", "Modify or cancel up to 2 hours prior, and review after dining.")
                }
            }
        }

        // 8. Partner Banner Card (like Arby's Rewards banner at bottom of reference image)
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Storefront,
                                contentDescription = "Partner",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp).size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "OWN A RESTAURANT?",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Manage menus, table slots & live bookings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onOpenPartnerApply,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Refine Your Table Search", style = MaterialTheme.typography.headlineSmall)

                Text("Price Range", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("$", "$$", "$$$", "$$$$").forEach { price ->
                        FilterChip(
                            selected = filters.priceRange == price,
                            onClick = {
                                onUpdateFilters {
                                    it.copy(priceRange = if (it.priceRange == price) null else price)
                                }
                            },
                            label = { Text(price) }
                        )
                    }
                }

                Text("Dietary Options", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("veg", "vegan", "halal", "gluten-free").forEach { tag ->
                        FilterChip(
                            selected = filters.dietaryTag == tag,
                            onClick = {
                                onUpdateFilters {
                                    it.copy(dietaryTag = if (it.dietaryTag == tag) null else tag)
                                }
                            },
                            label = { Text(tag.replaceFirstChar { c -> c.uppercase() }) }
                        )
                    }
                }

                Text("Minimum Rating", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.0 to "Any", 4.0 to "4.0★+", 4.5 to "4.5★+", 4.8 to "4.8★+").forEach { (r, label) ->
                        FilterChip(
                            selected = filters.minRating == r,
                            onClick = { onUpdateFilters { it.copy(minRating = r) } },
                            label = { Text(label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        onClearFilters()
                        showFilterSheet = false
                    }) {
                        Text("Reset Filters")
                    }
                    Button(
                        onClick = { showFilterSheet = false },
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Apply (${filteredRestaurants.size})")
                    }
                }
            }
        }
    }
}

@Composable
private fun CuisineCategoryTile(
    label: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PopularDishCard(
    dish: MenuItem,
    restaurantName: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .width(156.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box {
                FoodNetworkImage(
                    url = dish.imageUrl,
                    contentDescription = dish.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                VegIndicatorDot(
                    isVeg = dish.veg,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = dish.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (restaurantName.isNotBlank()) {
                Text(
                    text = restaurantName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${String.format("%.2f", dish.price)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "View restaurant",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RestaurantDiscoveryCard(
    restaurant: Restaurant,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("restaurant_card_${restaurant.id}")
    ) {
        Column {
            Box {
                FoodNetworkImage(
                    url = restaurant.coverPhotoUrl,
                    contentDescription = restaurant.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                )
                if (restaurant.featured) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "FEATURED",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(40.dp)
                        .clickable(onClick = onToggleFavorite)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove favorite" else "Save favorite",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.DarkGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                RatingPill(
                    rating = restaurant.ratingAvg,
                    reviewCount = restaurant.reviewCount,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = restaurant.name,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${restaurant.priceRange} • ${restaurant.city}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = restaurant.cuisines.joinToString(" • "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = restaurant.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (restaurant.dietaryOptions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        restaurant.dietaryOptions.take(4).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = tag.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HerbGreen,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HowItWorksStep(num: String, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(30.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = num,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun cuisineIcon(name: String): ImageVector = when (name.lowercase()) {
    "featured" -> Icons.Filled.Star
    "italian", "mediterranean" -> Icons.Filled.DinnerDining
    "japanese" -> Icons.Filled.RamenDining
    "grill", "mexican" -> Icons.Filled.LocalFireDepartment
    "desserts" -> Icons.Filled.Icecream
    "indian" -> Icons.Filled.Spa
    else -> Icons.Filled.Restaurant
}
