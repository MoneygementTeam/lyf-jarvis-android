data class Location(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

data class ActivityRequest(
    val startPoint: Location,
    val voiceText: String
)