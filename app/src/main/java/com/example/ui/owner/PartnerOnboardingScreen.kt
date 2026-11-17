package com.example.ui.owner

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.RestaurantRequest
import com.example.model.UiState
import com.example.ui.components.StatusBadgeChip

@Composable
fun PartnerOnboardingScreen(
    requestsState: UiState<List<RestaurantRequest>>,
    ownerName: String,
    ownerEmail: String,
    onBack: (() -> Unit)? = null,
    onSubmitRequest: (RestaurantRequest, (Result<Unit>) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val requests = (requestsState as? UiState.Success)?.data ?: emptyList()
    val editableRequest = requests.firstOrNull { it.status == "info_requested" }

    var editingId by remember(editableRequest?.id) { mutableStateOf(editableRequest?.id ?: "") }
    var restaurantName by remember(editableRequest?.id) { mutableStateOf(editableRequest?.restaurantName ?: "") }
    var selectedCuisines by remember(editableRequest?.id) {
        mutableStateOf(editableRequest?.cuisines?.toSet() ?: setOf("Italian"))
    }
    var address by remember(editableRequest?.id) { mutableStateOf(editableRequest?.address ?: "") }
    var city by remember(editableRequest?.id) { mutableStateOf(editableRequest?.city ?: "San Francisco") }
    var phone by remember(editableRequest?.id) { mutableStateOf(editableRequest?.phone ?: "") }
    var email by remember(editableRequest?.id) { mutableStateOf(editableRequest?.email ?: ownerEmail) }
    var licenseNumber by remember(editableRequest?.id) { mutableStateOf(editableRequest?.licenseNumber ?: "") }
    var description by remember(editableRequest?.id) { mutableStateOf(editableRequest?.description ?: "") }
    var logoUrl by remember(editableRequest?.id) { mutableStateOf(editableRequest?.logoUrl ?: "") }
    var coverUrl by remember(editableRequest?.id) {
        mutableStateOf(
            editableRequest?.coverPhotoUrl
                ?: "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=1000&q=80"
        )
    }
    var galleryUrlsText by remember(editableRequest?.id) {
        mutableStateOf(editableRequest?.galleryUrls?.joinToString(", ") ?: "")
    }
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pickedPhotoUri = uri
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Icon(Icons.Filled.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Partner Restaurant Portal", style = MaterialTheme.typography.headlineMedium)
            }
        }

        // Existing Requests Status Cards
        if (requests.isNotEmpty()) {
            item {
                Text("Your Applications", style = MaterialTheme.typography.titleLarge)
            }
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
                        Text(
                            text = "${req.cuisines.joinToString(", ")} • ${req.city} • License: ${req.licenseNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (req.adminNote.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Admin Note: ${req.adminNote}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Onboarding Form
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
                    Text(
                        text = if (editingId.isNotBlank()) "Update & Resubmit Application" else "Become a Partner Restaurant",
                        style = MaterialTheme.typography.titleLarge
                    )
                    OutlinedTextField(
                        value = restaurantName,
                        onValueChange = { restaurantName = it },
                        label = { Text("Restaurant Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Cuisine Types *", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Italian", "Indian", "Japanese", "Grill", "Mediterranean", "Mexican", "Desserts").forEach { c ->
                            FilterChip(
                                selected = selectedCuisines.contains(c),
                                onClick = {
                                    selectedCuisines = if (selectedCuisines.contains(c)) {
                                        selectedCuisines - c
                                    } else selectedCuisines + c
                                },
                                label = { Text(c) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Street Address *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Business Phone *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Business Email *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = licenseNumber,
                        onValueChange = { licenseNumber = it },
                        label = { Text("Food License / Registration # *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description & Signature Style") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = logoUrl,
                        onValueChange = { logoUrl = it },
                        label = { Text("Logo Image URL (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = coverUrl,
                        onValueChange = { coverUrl = it },
                        label = { Text("Cover Photo URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = galleryUrlsText,
                        onValueChange = { galleryUrlsText = it },
                        label = { Text("Gallery Photo URLs (comma-separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Preview Cover Photo from Device")
                    }

                    if (pickedPhotoUri != null) {
                        AsyncImage(
                            model = pickedPhotoUri,
                            contentDescription = "Selected preview photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }

                    if (errorMsg != null) {
                        Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = {
                            if (restaurantName.trim().length < 2) {
                                errorMsg = "Enter a restaurant name (at least 2 characters)."
                                return@Button
                            }
                            if (selectedCuisines.isEmpty()) {
                                errorMsg = "Select at least one cuisine."
                                return@Button
                            }
                            if (address.isBlank() || city.isBlank() || phone.isBlank() || email.isBlank() || licenseNumber.isBlank()) {
                                errorMsg = "Please complete all required (*) fields."
                                return@Button
                            }
                            errorMsg = null
                            submitting = true
                            val galleryList = galleryUrlsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            val req = RestaurantRequest(
                                id = editingId,
                                ownerName = ownerName,
                                restaurantName = restaurantName.trim(),
                                cuisines = selectedCuisines.toList(),
                                address = address.trim(),
                                city = city.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                licenseNumber = licenseNumber.trim(),
                                description = description.trim(),
                                logoUrl = logoUrl.trim(),
                                coverPhotoUrl = coverUrl.trim(),
                                galleryUrls = galleryList
                            )
                            onSubmitRequest(req) { res ->
                                submitting = false
                                res.onSuccess {
                                    editingId = ""
                                    restaurantName = ""
                                    address = ""
                                    licenseNumber = ""
                                    description = ""
                                }.onFailure { err ->
                                    errorMsg = err.localizedMessage ?: "Submission failed."
                                }
                            }
                        },
                        enabled = !submitting,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_partner_request_button")
                    ) {
                        Text(if (submitting) "Submitting..." else "Submit Partner Application", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
