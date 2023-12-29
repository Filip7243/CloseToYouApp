package com.example.closetoyou

/*
    {
        "phoneNumber": "+48575603061",
        "latitude": 29.02,
        "longitude": 11.2,
        "hasPermission": true
    }
*/

data class Localization(
    val phoneNumber: String,
    val latitude: Double,
    val longitude: Double,
    val hasPermission: Boolean
)