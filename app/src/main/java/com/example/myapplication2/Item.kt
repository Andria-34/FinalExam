package com.example.myapplication2

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date


data class Item(

    var id: String = "",

    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val location: String = "",
    val imageUrl: String = "",
    val status: String = "", // Will be "LOST" or "FOUND"

    // This annotation tells Firestore to automatically populate this field
    // with the server's timestamp when the document is created.
    @ServerTimestamp
    val datePosted: Date? = null,

    // This field is only used for "FOUND" items. It will be null for "LOST" items.
    val secretQuestion: String? = null
) {

    constructor() : this("", "", "", "", "", "", "", "", null, null)
}
