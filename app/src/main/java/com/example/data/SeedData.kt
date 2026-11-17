package com.example.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DemoPersonaInfo(
    val name: String,
    val email: String,
    val roleLabel: String,
    val note: String
)

object SeedData {

    val demoPersonas = listOf(
        DemoPersonaInfo(
            name = "Govind Singh (Admin)",
            email = "govindsinghpratap123@gmail.com",
            roleLabel = "Admin (Full Super Control)",
            note = "Bootstrapped admin via Google Sign-In; can preview Customer, Owner, and Admin workspaces."
        ),
        DemoPersonaInfo(
            name = "Chef Marco Bellini",
            email = "chef.marco@tabletrail.app",
            roleLabel = "Owner (Osteria Brava, Ember & Oak, Terra Verde)",
            note = "Seeded owner profile managing 3 partner kitchens."
        ),
        DemoPersonaInfo(
            name = "Chef Aisha Khan",
            email = "chef.aisha@tabletrail.app",
            roleLabel = "Owner (Saffron Hearth, Kyoto Lantern, Maris Coastal)",
            note = "Seeded owner profile managing 3 partner kitchens."
        ),
        DemoPersonaInfo(
            name = "Maya Patel",
            email = "maya.customer@tabletrail.app",
            roleLabel = "Customer",
            note = "Sample diner with upcoming confirmed and past completed bookings."
        ),
        DemoPersonaInfo(
            name = "Liam O'Connor",
            email = "liam.customer@tabletrail.app",
            roleLabel = "Customer",
            note = "Sample diner with active reservations."
        ),
        DemoPersonaInfo(
            name = "Priya Sharma",
            email = "priya.customer@tabletrail.app",
            roleLabel = "Customer",
            note = "Sample diner with saved reviews."
        )
    )

    suspend fun seedAllIfNeeded(
        db: FirebaseFirestore,
        currentAdminUid: String,
        currentAdminEmail: String,
        forceReseed: Boolean = false
    ): Result<Boolean> {
        return try {
            if (!forceReseed) {
                val existing = db.collection("restaurants").limit(1).get().await()
                if (!existing.isEmpty) {
                    return Result.success(false)
                }
            }

            val batch = db.batch()
            val now = FieldValue.serverTimestamp()

            // 1. Platform Settings
            batch.set(
                db.collection("platformSettings").document("config"),
                mapOf(
                    "cuisines" to listOf("Italian", "Indian", "Japanese", "Grill", "Mediterranean", "Mexican", "Desserts"),
                    "cities" to listOf("San Francisco", "New York", "London", "Austin"),
                    "featuredRestaurantIds" to listOf("rest_osteria_brava", "rest_saffron_hearth", "rest_kyoto_lantern"),
                    "heroBannerTitle" to "CRAFTED TABLES.\nDINING PERFECTED.",
                    "heroBannerSubtitle" to "Big flavor. Warm tables. Reserve neighborhood favorites in real time.",
                    "heroBannerCta" to "Reserve Now",
                    "published" to true,
                    "updatedAt" to now
                )
            )

            // 2. Demo Users (1 admin marker + 2 owners + 3 customers)
            val seedUsers = listOf(
                Triple("seed_owner_marco", "Chef Marco Bellini", "chef.marco@tabletrail.app") to "owner",
                Triple("seed_owner_aisha", "Chef Aisha Khan", "chef.aisha@tabletrail.app") to "owner",
                Triple("seed_cust_maya", "Maya Patel", "maya.customer@tabletrail.app") to "customer",
                Triple("seed_cust_liam", "Liam O'Connor", "liam.customer@tabletrail.app") to "customer",
                Triple("seed_cust_priya", "Priya Sharma", "priya.customer@tabletrail.app") to "customer"
            )
            seedUsers.forEach { (info, role) ->
                val (uid, name, email) = info
                batch.set(
                    db.collection("users").document(uid),
                    mapOf(
                        "uid" to uid,
                        "name" to name,
                        "email" to email,
                        "phone" to "+1 415-555-0140",
                        "photoUrl" to "",
                        "role" to role,
                        "status" to "active",
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
            }

            // 3. 6 Sample Restaurants with verified Unsplash photography
            data class SeedRest(
                val id: String,
                val ownerId: String,
                val name: String,
                val desc: String,
                val cuisines: List<String>,
                val city: String,
                val address: String,
                val price: String,
                val dietary: List<String>,
                val cover: String,
                val gallery: List<String>,
                val rating: Double,
                val reviews: Long,
                val featured: Boolean
            )

            val restaurants = listOf(
                SeedRest(
                    id = "rest_osteria_brava",
                    ownerId = "seed_owner_marco",
                    name = "Osteria Brava",
                    desc = "Wood-fired Roman hearth, hand-rolled tagliolini, and seasonal farm produce served in a candlelit brick dining room.",
                    cuisines = listOf("Italian", "Desserts"),
                    city = "San Francisco",
                    address = "412 Valencia St, San Francisco, CA",
                    price = "$$$",
                    dietary = listOf("veg", "gluten-free"),
                    cover = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=1000&q=80",
                    gallery = listOf(
                        "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=900&q=80",
                        "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&w=900&q=80"
                    ),
                    rating = 4.9,
                    reviews = 128L,
                    featured = true
                ),
                SeedRest(
                    id = "rest_saffron_hearth",
                    ownerId = "seed_owner_aisha",
                    name = "Saffron Hearth",
                    desc = "Modern regional Indian tasting plates, charcoal tandoor skewers, and smoky saffron curries paired with house botanicals.",
                    cuisines = listOf("Indian", "Grill"),
                    city = "London",
                    address = "28 Soho Square, London",
                    price = "$$",
                    dietary = listOf("veg", "vegan", "halal", "gluten-free"),
                    cover = "https://images.unsplash.com/photo-1585937421612-70a008356fbe?auto=format&fit=crop&w=1000&q=80",
                    gallery = listOf(
                        "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=900&q=80",
                        "https://images.unsplash.com/photo-1565557623262-b51c2513a641?auto=format&fit=crop&w=900&q=80"
                    ),
                    rating = 4.8,
                    reviews = 94L,
                    featured = true
                ),
                SeedRest(
                    id = "rest_kyoto_lantern",
                    ownerId = "seed_owner_aisha",
                    name = "Kyoto Lantern",
                    desc = "Intimate counter robata grill, dry-aged sashimi, and slow-simmered dashi bowls crafted to order.",
                    cuisines = listOf("Japanese"),
                    city = "New York",
                    address = "114 Mercer St, New York, NY",
                    price = "$$$$",
                    dietary = listOf("gluten-free", "veg"),
                    cover = "https://images.unsplash.com/photo-1579871494447-9811cf80d66c?auto=format&fit=crop&w=1000&q=80",
                    gallery = listOf(
                        "https://images.unsplash.com/photo-1553621042-f6e147245754?auto=format&fit=crop&w=900&q=80",
                        "https://images.unsplash.com/photo-1617196034796-73dfa7b1fd56?auto=format&fit=crop&w=900&q=80"
                    ),
                    rating = 4.9,
                    reviews = 152L,
                    featured = true
                ),
                SeedRest(
                    id = "rest_ember_oak",
                    ownerId = "seed_owner_marco",
                    name = "Ember & Oak Smokehouse",
                    desc = "Live-fire prime cuts, smoked brisket, blistered heirloom vegetables, and bourbon-glazed desserts.",
                    cuisines = listOf("Grill", "Desserts"),
                    city = "Austin",
                    address = "704 South Congress Ave, Austin, TX",
                    price = "$$$",
                    dietary = listOf("halal", "gluten-free"),
                    cover = "https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=1000&q=80",
                    gallery = listOf(
                        "https://images.unsplash.com/photo-1558030006-450675393462?auto=format&fit=crop&w=900&q=80",
                        "https://images.unsplash.com/photo-1529692236671-f1f6cf9683ba?auto=format&fit=crop&w=900&q=80"
                    ),
                    rating = 4.7,
                    reviews = 76L,
                    featured = false
                ),
                SeedRest(
                    id = "rest_terra_verde",
                    ownerId = "seed_owner_marco",
                    name = "Terra Verde Kitchen",
                    desc = "100% plant-forward Mediterranean mezze, wood-roasted cauliflower, saffron tahini, and organic sourdough flatbreads.",
                    cuisines = listOf("Mediterranean", "Italian"),
                    city = "San Francisco",
                    address = "890 Hayes St, San Francisco, CA",
                    price = "$$",
                    dietary = listOf("veg", "vegan", "halal"),
                    cover = "https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?auto=format&fit=crop&w=1000&q=80",
                    gallery = listOf(
                        "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=900&q=80",
                        "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=900&q=80"
                    ),
                    rating = 4.6,
                    reviews = 63L,
                    featured = false
                ),
                SeedRest(
                    id = "rest_maris_coastal",
                    ownerId = "seed_owner_aisha",
                    name = "Maris Coastal Cantina",
                    desc = "Charred Baja tacos, citrus-cured aguachile, heirloom masa tostadas, and salted agave sorbets.",
                    cuisines = listOf("Mexican", "Desserts"),
                    city = "New York",
                    address = "218 Wythe Ave, Brooklyn, NY",
                    price = "$$",
                    dietary = listOf("gluten-free", "veg", "vegan"),
                    cover = "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?auto=format&fit=crop&w=1000&q=80",
                    gallery = listOf(
                        "https://images.unsplash.com/photo-1551504734-5ee1c4a1479b?auto=format&fit=crop&w=900&q=80",
                        "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=900&q=80"
                    ),
                    rating = 4.7,
                    reviews = 89L,
                    featured = false
                )
            )

            val slots = listOf("12:00", "13:30", "18:00", "19:30", "21:00")
            restaurants.forEach { r ->
                batch.set(
                    db.collection("restaurants").document(r.id),
                    mapOf(
                        "ownerId" to r.ownerId,
                        "name" to r.name,
                        "description" to r.desc,
                        "cuisines" to r.cuisines,
                        "city" to r.city,
                        "address" to r.address,
                        "phone" to "+1 415-555-0188",
                        "email" to "reservations@${r.id}.tabletrail.app",
                        "priceRange" to r.price,
                        "dietaryOptions" to r.dietary,
                        "openingHours" to "11:30 AM - 11:00 PM",
                        "holidayClosures" to listOf("2026-12-25"),
                        "timeSlots" to slots,
                        "logoUrl" to r.cover,
                        "coverPhotoUrl" to r.cover,
                        "galleryUrls" to r.gallery,
                        "ratingAvg" to r.rating,
                        "reviewCount" to r.reviews,
                        "status" to "active",
                        "featured" to r.featured,
                        "approved" to true,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )

                // Categories (Starters, Mains, Desserts, Drinks)
                val catNames = listOf("Starters", "Mains", "Desserts", "Drinks")
                catNames.forEachIndexed { idx, catName ->
                    val catId = "${r.id}_cat_${idx + 1}"
                    batch.set(
                        db.collection("categories").document(catId),
                        mapOf(
                            "restaurantId" to r.id,
                            "ownerId" to r.ownerId,
                            "name" to catName,
                            "sortOrder" to (idx + 1L),
                            "published" to true,
                            "createdAt" to now,
                            "updatedAt" to now
                        )
                    )
                }

                // Tables per restaurant
                val tableConfigs = listOf(
                    Triple("${r.id}_tbl_2top", "2-Top Cozy Bistro", 2L to 4L),
                    Triple("${r.id}_tbl_4top", "4-Top Window Table", 4L to 4L),
                    Triple("${r.id}_tbl_6top", "6-Top Chef's Booth", 6L to 2L)
                )
                tableConfigs.forEach { (tid, label, capCount) ->
                    batch.set(
                        db.collection("tables").document(tid),
                        mapOf(
                            "restaurantId" to r.id,
                            "ownerId" to r.ownerId,
                            "label" to label,
                            "capacity" to capCount.first,
                            "count" to capCount.second,
                            "active" to true,
                            "createdAt" to now,
                            "updatedAt" to now
                        )
                    )
                }
            }

            // 4. Seed Menu Items across the 6 restaurants
            data class SeedDish(
                val id: String,
                val restId: String,
                val ownerId: String,
                val catIdx: Int,
                val catName: String,
                val name: String,
                val desc: String,
                val price: Double,
                val img: String,
                val veg: Boolean,
                val popular: Boolean,
                val tags: List<String>,
                val rating: Double
            )

            val dishes = listOf(
                // Osteria Brava
                SeedDish("dish_ob_1", "rest_osteria_brava", "seed_owner_marco", 1, "Starters", "Burrata & Heirloom Figs", "Creamy Pugliese burrata, roasted figs, aged balsamic, toasted focaccia.", 16.0, "https://images.unsplash.com/photo-1608897013039-887f21d8c804?auto=format&fit=crop&w=700&q=80", true, true, listOf("veg", "gluten-free"), 4.9),
                SeedDish("dish_ob_2", "rest_osteria_brava", "seed_owner_marco", 2, "Mains", "Truffle Cacio e Pepe", "Hand-cut tagliolini tossed in pecorino crema, cracked Kampot pepper, shaved black truffle.", 28.0, "https://images.unsplash.com/photo-1473093295043-cdd812d0e601?auto=format&fit=crop&w=700&q=80", true, true, listOf("veg"), 4.9),
                SeedDish("dish_ob_3", "rest_osteria_brava", "seed_owner_marco", 2, "Mains", "Wood-Fired Ribeye Tagliata", "Sliced dry-aged ribeye with rosemary jus, charred lemon, and wild arugula.", 44.0, "https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=700&q=80", false, false, listOf("gluten-free"), 4.8),
                SeedDish("dish_ob_4", "rest_osteria_brava", "seed_owner_marco", 3, "Desserts", "Espresso Tiramisu", "Savoiardi soaked in single-origin espresso with whipped mascarpone.", 13.0, "https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?auto=format&fit=crop&w=700&q=80", true, true, listOf("veg"), 4.9),
                SeedDish("dish_ob_5", "rest_osteria_brava", "seed_owner_marco", 4, "Drinks", "Blood Orange Spritz", "Sparkling Sicilian blood orange, rosemary sprig, zero-proof bitters.", 11.0, "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?auto=format&fit=crop&w=700&q=80", true, false, listOf("veg", "vegan", "gluten-free"), 4.7),

                // Saffron Hearth
                SeedDish("dish_sh_1", "rest_saffron_hearth", "seed_owner_aisha", 1, "Starters", "Crispy Okra & Tamarind Chaat", "Crisp spiced okra ribbons with yogurt drizzle, pomegranate, and mint chutney.", 14.0, "https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=700&q=80", true, true, listOf("veg", "halal", "gluten-free"), 4.8),
                SeedDish("dish_sh_2", "rest_saffron_hearth", "seed_owner_aisha", 2, "Mains", "Charcoal Butter Chicken", "Smoked tandoor chicken simmered in velvety heirloom tomato and fenugreek makhani.", 26.0, "https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?auto=format&fit=crop&w=700&q=80", false, true, listOf("halal", "gluten-free"), 4.9),
                SeedDish("dish_sh_3", "rest_saffron_hearth", "seed_owner_aisha", 2, "Mains", "Smoked Eggplant Bharta", "Fire-charred aubergine mash with roasted garlic, cumin, and cilantro.", 21.0, "https://images.unsplash.com/photo-1585937421612-70a008356fbe?auto=format&fit=crop&w=700&q=80", true, false, listOf("veg", "vegan", "halal", "gluten-free"), 4.7),
                SeedDish("dish_sh_4", "rest_saffron_hearth", "seed_owner_aisha", 3, "Desserts", "Saffron Pistachio Kulfi", "Slow-reduced milk gelato infused with Kashmiri saffron and toasted pistachios.", 11.0, "https://images.unsplash.com/photo-1563805042-7684c019e1cb?auto=format&fit=crop&w=700&q=80", true, true, listOf("veg", "gluten-free"), 4.8),

                // Kyoto Lantern
                SeedDish("dish_kl_1", "rest_kyoto_lantern", "seed_owner_aisha", 1, "Starters", "Crispy Rice Spicy Tuna", "Seared sushi rice cakes topped with spicy bluefin tuna and jalapeno.", 21.0, "https://images.unsplash.com/photo-1579871494447-9811cf80d66c?auto=format&fit=crop&w=700&q=80", false, true, listOf("gluten-free"), 4.9),
                SeedDish("dish_kl_2", "rest_kyoto_lantern", "seed_owner_aisha", 2, "Mains", "Miso Glazed Black Cod", "72-hour saikyo miso marinated black cod grilled over binchotan charcoal.", 38.0, "https://images.unsplash.com/photo-1553621042-f6e147245754?auto=format&fit=crop&w=700&q=80", false, true, listOf("gluten-free"), 4.9),
                SeedDish("dish_kl_3", "rest_kyoto_lantern", "seed_owner_aisha", 3, "Desserts", "Matcha Sesame Tart", "Ceremonial Uji matcha ganache in a toasted black sesame shell.", 14.0, "https://images.unsplash.com/photo-1563805042-7684c019e1cb?auto=format&fit=crop&w=700&q=80", true, false, listOf("veg"), 4.8),

                // Ember & Oak
                SeedDish("dish_eo_1", "rest_ember_oak", "seed_owner_marco", 1, "Starters", "Charred Corn & Queso Dip", "Fire-roasted sweet corn, smoked paprika crema, warm tortilla crisps.", 13.0, "https://images.unsplash.com/photo-1541529086526-db283c563270?auto=format&fit=crop&w=700&q=80", true, false, listOf("veg", "gluten-free"), 4.6),
                SeedDish("dish_eo_2", "rest_ember_oak", "seed_owner_marco", 2, "Mains", "14-Hour Oak Smoked Brisket", "Prime beef brisket sliced to order with espresso BBQ glaze and pickled red onions.", 34.0, "https://images.unsplash.com/photo-1529692236671-f1f6cf9683ba?auto=format&fit=crop&w=700&q=80", false, true, listOf("halal", "gluten-free"), 4.9),
                SeedDish("dish_eo_3", "rest_ember_oak", "seed_owner_marco", 3, "Desserts", "Skillet Pecan Cobbler", "Warm caramelized Texas pecans with Madagascar vanilla bean ice cream.", 12.0, "https://images.unsplash.com/photo-1551024601-bec78aea704b?auto=format&fit=crop&w=700&q=80", true, true, listOf("veg"), 4.8),

                // Terra Verde
                SeedDish("dish_tv_1", "rest_terra_verde", "seed_owner_marco", 1, "Starters", "Whipped Tahini & Zaatar Flatbread", "Wood-fired sourdough pita with smoked olive oil and sumac.", 12.0, "https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?auto=format&fit=crop&w=700&q=80", true, true, listOf("veg", "vegan", "halal"), 4.7),
                SeedDish("dish_tv_2", "rest_terra_verde", "seed_owner_marco", 2, "Mains", "Roasted Cauliflower Shawarma", "Whole-roasted spiced cauliflower over saffron herbed quinoa and pomegranate.", 23.0, "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=700&q=80", true, true, listOf("veg", "vegan", "halal", "gluten-free"), 4.8),

                // Maris Coastal
                SeedDish("dish_mc_1", "rest_maris_coastal", "seed_owner_aisha", 1, "Starters", "Ahi Tuna Tostadas", "Crisp blue corn tortilla, chipotle aioli, avocado mousse, sesame.", 17.0, "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?auto=format&fit=crop&w=700&q=80", false, true, listOf("gluten-free"), 4.8),
                SeedDish("dish_mc_2", "rest_maris_coastal", "seed_owner_aisha", 2, "Mains", "Crispy Cauliflower Baja Tacos", "Tempura-battered cauliflower, pickled cabbage slaw, lime crema.", 19.0, "https://images.unsplash.com/photo-1551504734-5ee1c4a1479b?auto=format&fit=crop&w=700&q=80", true, false, listOf("veg", "vegan"), 4.7)
            )

            dishes.forEach { d ->
                batch.set(
                    db.collection("menuItems").document(d.id),
                    mapOf(
                        "restaurantId" to d.restId,
                        "ownerId" to d.ownerId,
                        "categoryId" to "${d.restId}_cat_${d.catIdx}",
                        "categoryName" to d.catName,
                        "name" to d.name,
                        "description" to d.desc,
                        "price" to d.price,
                        "imageUrl" to d.img,
                        "veg" to d.veg,
                        "popular" to d.popular,
                        "dietaryTags" to d.tags,
                        "available" to true,
                        "published" to true,
                        "rating" to d.rating,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
            }

            // 5. Seed 1 Pending Partner Request for Admin Queue
            batch.set(
                db.collection("restaurantRequests").document("req_velvet_bistro"),
                mapOf(
                    "ownerId" to "seed_owner_marco",
                    "ownerName" to "Chef Marco Bellini",
                    "restaurantName" to "Velvet Bistro & Hearth",
                    "cuisines" to listOf("Italian", "Mediterranean"),
                    "address" to "152 Fillmore St",
                    "city" to "San Francisco",
                    "phone" to "+1 415-555-0194",
                    "email" to "velvet@tabletrail.app",
                    "licenseNumber" to "SF-FD-2026-441",
                    "description" to "Late-night natural wine bar and handmade pasta kitchen.",
                    "logoUrl" to "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=800&q=80",
                    "coverPhotoUrl" to "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&w=1000&q=80",
                    "galleryUrls" to listOf("https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=900&q=80"),
                    "status" to "pending",
                    "adminNote" to "",
                    "createdAt" to now,
                    "updatedAt" to now
                )
            )

            // 6. Seed Sample Bookings (including upcoming + completed for current user so all tabs look alive!)
            val cal = Calendar.getInstance()
            val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayStr = dateFmt.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val tomorrowStr = dateFmt.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -4)
            val pastStr = dateFmt.format(cal.time)

            val futureEpoch = System.currentTimeMillis() + 36L * 3600L * 1000L
            val todayLaterEpoch = System.currentTimeMillis() + 5L * 3600L * 1000L
            val pastEpoch = System.currentTimeMillis() - 72L * 3600L * 1000L

            val seedBookings = listOf(
                mapOf(
                    "id" to "seed_book_1",
                    "restaurantId" to "rest_osteria_brava",
                    "restaurantName" to "Osteria Brava",
                    "restaurantImageUrl" to "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=1000&q=80",
                    "ownerId" to "seed_owner_marco",
                    "customerId" to currentAdminUid,
                    "customerName" to "Alex Diner",
                    "customerPhone" to "+1 415-555-0101",
                    "date" to tomorrowStr,
                    "timeSlot" to "19:30",
                    "bookingEpochMillis" to futureEpoch,
                    "partySize" to 4L,
                    "tableLabel" to "4-Top Window Table",
                    "specialRequests" to "Anniversary corner booth please",
                    "estimatedSpend" to 185.0,
                    "status" to "confirmed",
                    "reviewed" to false
                ),
                mapOf(
                    "id" to "seed_book_2",
                    "restaurantId" to "rest_saffron_hearth",
                    "restaurantName" to "Saffron Hearth",
                    "restaurantImageUrl" to "https://images.unsplash.com/photo-1585937421612-70a008356fbe?auto=format&fit=crop&w=1000&q=80",
                    "ownerId" to "seed_owner_aisha",
                    "customerId" to currentAdminUid,
                    "customerName" to "Alex Diner",
                    "customerPhone" to "+1 415-555-0101",
                    "date" to pastStr,
                    "timeSlot" to "20:00",
                    "bookingEpochMillis" to pastEpoch,
                    "partySize" to 2L,
                    "tableLabel" to "2-Top Cozy Bistro",
                    "specialRequests" to "",
                    "estimatedSpend" to 96.0,
                    "status" to "completed",
                    "reviewed" to false
                ),
                mapOf(
                    "id" to "seed_book_3",
                    "restaurantId" to "rest_osteria_brava",
                    "restaurantName" to "Osteria Brava",
                    "restaurantImageUrl" to "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=1000&q=80",
                    "ownerId" to "seed_owner_marco",
                    "customerId" to "seed_cust_maya",
                    "customerName" to "Maya Patel",
                    "customerPhone" to "+1 415-555-0112",
                    "date" to todayStr,
                    "timeSlot" to "19:30",
                    "bookingEpochMillis" to todayLaterEpoch,
                    "partySize" to 2L,
                    "tableLabel" to "2-Top Cozy Bistro",
                    "specialRequests" to "Vegetarian tasting menu",
                    "estimatedSpend" to 110.0,
                    "status" to "pending",
                    "reviewed" to false
                ),
                mapOf(
                    "id" to "seed_book_4",
                    "restaurantId" to "rest_kyoto_lantern",
                    "restaurantName" to "Kyoto Lantern",
                    "restaurantImageUrl" to "https://images.unsplash.com/photo-1579871494447-9811cf80d66c?auto=format&fit=crop&w=1000&q=80",
                    "ownerId" to "seed_owner_aisha",
                    "customerId" to "seed_cust_liam",
                    "customerName" to "Liam O'Connor",
                    "customerPhone" to "+1 212-555-0166",
                    "date" to todayStr,
                    "timeSlot" to "21:00",
                    "bookingEpochMillis" to todayLaterEpoch,
                    "partySize" to 4L,
                    "tableLabel" to "4-Top Window Table",
                    "specialRequests" to "Chef counter seats",
                    "estimatedSpend" to 240.0,
                    "status" to "confirmed",
                    "reviewed" to false
                )
            )
            seedBookings.forEach { b ->
                val id = b["id"] as String
                val payload = HashMap<String, Any>(b.filterKeys { it != "id" })
                payload["createdAt"] = now
                payload["updatedAt"] = now
                batch.set(db.collection("bookings").document(id), payload)
            }

            // 7. Seed Sample Reviews
            val seedReviews = listOf(
                mapOf(
                    "id" to "seed_rev_1",
                    "restaurantId" to "rest_osteria_brava",
                    "ownerId" to "seed_owner_marco",
                    "bookingId" to "seed_book_past_1",
                    "customerId" to "seed_cust_maya",
                    "customerName" to "Maya Patel",
                    "rating" to 5L,
                    "comment" to "The truffle cacio e pepe and warm burrata were unforgettable. Seamless table check-in!",
                    "ownerReply" to "Grazie Maya! Looking forward to welcoming you back soon.",
                    "status" to "visible"
                ),
                mapOf(
                    "id" to "seed_rev_2",
                    "restaurantId" to "rest_saffron_hearth",
                    "ownerId" to "seed_owner_aisha",
                    "bookingId" to "seed_book_past_2",
                    "customerId" to "seed_cust_priya",
                    "customerName" to "Priya Sharma",
                    "rating" to 5L,
                    "comment" to "Incredible smoky charcoal butter chicken and crispy okra chaat. Cozy lighting!",
                    "ownerReply" to "",
                    "status" to "visible"
                ),
                mapOf(
                    "id" to "seed_rev_3",
                    "restaurantId" to "rest_kyoto_lantern",
                    "ownerId" to "seed_owner_aisha",
                    "bookingId" to "seed_book_past_3",
                    "customerId" to "seed_cust_liam",
                    "customerName" to "Liam O'Connor",
                    "rating" to 5L,
                    "comment" to "Miso black cod melts like butter. Best date-night spot in Manhattan.",
                    "ownerReply" to "Thank you Liam! Our binchotan grill team appreciates you.",
                    "status" to "visible"
                )
            )
            seedReviews.forEach { rev ->
                val id = rev["id"] as String
                val payload = HashMap<String, Any>(rev.filterKeys { it != "id" })
                payload["createdAt"] = now
                payload["updatedAt"] = now
                batch.set(db.collection("reviews").document(id), payload)
            }

            // 8. Seed initial Activity Log
            batch.set(
                db.collection("activityLogs").document("seed_log_init"),
                mapOf(
                    "adminId" to currentAdminUid,
                    "adminEmail" to currentAdminEmail,
                    "action" to "SEED_PLATFORM",
                    "targetType" to "settings",
                    "targetId" to "config",
                    "details" to "Populated 6 restaurants, menus, tables, 2 owners, 3 customers, and sample bookings.",
                    "createdAt" to now
                )
            )

            batch.commit().await()
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "seedAllIfNeeded")
            Result.failure(e)
        }
    }
}
