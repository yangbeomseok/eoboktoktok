package com.dive.weatherwatch.data

import android.os.Parcel
import android.os.Parcelable
import java.time.LocalDateTime
import java.util.UUID

data class TrapLocation(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "통발 #${System.currentTimeMillis().toString().takeLast(4)}",
    val latitude: Double,
    val longitude: Double,
    val deployTime: String = LocalDateTime.now().toString(),
    val memo: String = "",
    val isActive: Boolean = true,
    val estimatedDepth: String = "알 수 없음",
    val baitType: String = "미설정"
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeString(deployTime)
        parcel.writeString(memo)
        parcel.writeByte(if (isActive) 1 else 0)
        parcel.writeString(estimatedDepth)
        parcel.writeString(baitType)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TrapLocation> {
        override fun createFromParcel(parcel: Parcel): TrapLocation = TrapLocation(parcel)
        override fun newArray(size: Int): Array<TrapLocation?> = arrayOfNulls(size)
    }
}

data class TrapNavigationInfo(
    val trap: TrapLocation,
    val currentLatitude: Double,
    val currentLongitude: Double,
    val distanceMeters: Double,
    val bearingDegrees: Double,
    val proximityLevel: ProximityLevel
)

enum class ProximityLevel {
    VERY_FAR,    // 500m+
    FAR,         // 100-500m
    CLOSE,       // 50-100m
    VERY_CLOSE,  // 10-50m
    AT_TARGET    // <10m
}

data class BaitOption(
    val name: String,
    val description: String
)

object TrapOptions {
    val baitOptions = listOf(
        BaitOption("새우", "일반적인 미끼"),
        BaitOption("게", "갑각류용"),
        BaitOption("멸치", "어류용"),
        BaitOption("오징어", "대형 어류용"),
        BaitOption("기타", "직접 입력")
    )
    
    val depthOptions = listOf(
        "얕음 (5m 이하)",
        "보통 (5-15m)",
        "깊음 (15-30m)",
        "매우 깊음 (30m+)",
        "알 수 없음"
    )
}