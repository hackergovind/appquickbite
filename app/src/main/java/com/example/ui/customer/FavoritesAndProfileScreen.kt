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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SeedData
import com.example.model.Favorite
import com.example.model.Restaurant
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FoodNetworkImage
import com.example.ui.components.RatingPill

@Composable
fun FavoritesScreen(
    favoritesState: UiState<List<Favorite>>,
    allRestaurants: List<Restaurant>,
    onOpenRestaurant: (Restaurant) -> Unit,
    onRemoveFavorite: (Restaurant) -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Saved Favorites",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        when (favoritesState) {
            is UiState.Loading -> {
                Text("Loading favorites...", modifier = Modifier.padding(16.dp))
            }
            is UiState.Error -> {
                EmptyStateCard(
                    title = "Couldn't load favorites",
                    subtitle = favoritesState.message,
                    modifier = Modifier.padding(16.dp)
                )
            }
            is UiState.Success -> {
                if (favoritesState.data.isEmpty()) {
                    EmptyStateCard(
                        title = "No saved spots yet",
                        subtitle = "Tap the heart icon on any restaurant to save it to your dining trail.",
                        actionLabel = "Discover Restaurants",
                        onAction = onExploreClick,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(favoritesState.data, key = { it.id }) { fav ->
                            val match = allRestaurants.firstOrNull { it.id == fav.restaurantId }
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (match != null) onOpenRestaurant(match) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FoodNetworkImage(
                                        url = fav.restaurantCoverUrl,
                                        contentDescription = fav.restaurantName,
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(fav.restaurantName, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = fav.restaurantCity,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        RatingPill(rating = fav.restaurantRating)
                                    }
                                    if (match != null) {
                                        IconButton(onClick = { onRemoveFavorite(match) }) {
                                            Icon(
                                                imageVector = Icons.Filled.Favorite,
                                                contentDescription = "Remove favorite",
                                                tint = MaterialTheme.colorScheme.primary
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
    }
}

@Composable
fun ProfileAndDevScreen(
    userProfile: UserProfile?,
    activeWorkspaceRole: UserRole,
    isDarkMode: Boolean,
    isSeeding: Boolean,
    onToggleDarkMode: () -> Unit,
    onSaveProfile: (String, String, (Result<Unit>) -> Unit) -> Unit,
    onSwitchWorkspaceRole: (UserRole) -> Unit,
    onSeedSampleData: () -> Unit,
    onOpenPartnerApply: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(userProfile?.name ?: "") }
    var phone by remember { mutableStateOf(userProfile?.phone ?: "") }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(userProfile) {
        if (userProfile != null) {
            if (name.isBlank()) name = userProfile.name
            if (phone.isBlank()) phone = userProfile.phone
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Account & Settings", style = MaterialTheme.typography.headlineMedium)
        }

        // Profile Form
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(userProfile?.email ?: "Signed in", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Stored Role: ${(userProfile?.role ?: "customer").uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            saving = true
                            onSaveProfile(name, phone) { saving = false }
                        },
                        enabled = !saving && name.isNotBlank(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_profile_button")
                    ) {
                        Text(if (saving) "Saving..." else "Save Profile")
                    }
                }
            }
        }

        // Theme & Partner Onboarding
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Dark Mode", style = MaterialTheme.typography.titleSmall)
                                Text("Warm espresso dining theme", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(checked = isDarkMode, onCheckedChange = { onToggleDarkMode() })
                    }

                    OutlinedButton(
                        onClick = onOpenPartnerApply,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Become a Partner Restaurant")
                    }
                }
            }
        }

        // Dev & Demo Role Switcher Panel
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dev Panel: Role Switcher & Seed Data", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "Test all three workspaces immediately. (Admin and full platform seeding require the bootstrapped admin Google account; any account can apply as a Partner Owner.)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UserRole.entries.forEach { role ->
                            FilterChip(
                                selected = activeWorkspaceRole == role,
                                onClick = { onSwitchWorkspaceRole(role) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (role) {
                                            UserRole.CUSTOMER -> Icons.Filled.Restaurant
                                            UserRole.OWNER -> Icons.Filled.Storefront
                                            UserRole.ADMIN -> Icons.Filled.AdminPanelSettings
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(role.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                modifier = Modifier.testTag("switch_role_${role.wire}")
                            )
                        }
                    }

                    Button(
                        onClick = onSeedSampleData,
                        enabled = !isSeeding,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("seed_sample_data_button")
                    ) {
                        if (isSeeding) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Seed / Reset 6 Sample Restaurants & Menus")
                    }

                    Text("Demo Credentials Reference", style = MaterialTheme.typography.titleSmall)
                    SeedData.demoPersonas.forEach { persona ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("${persona.name} (${persona.roleLabel})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(persona.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Sign Out
        item {
            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }
}
