package com.fpt.landmarkremark.model

import com.google.android.gms.maps.model.LatLng

class Locations {
    var listLocation: MutableList<DataLocation>? = mutableListOf()
}

data class DataLocation(
    var latLong: LatLng = LatLng(0.0, 0.0),
    var title: String = ""
)

