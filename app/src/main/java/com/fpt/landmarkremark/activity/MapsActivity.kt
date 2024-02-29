package com.fpt.landmarkremark.activity

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.fpt.landmarkremark.R
import com.fpt.landmarkremark.databinding.ActivityMapsBinding
import com.fpt.landmarkremark.model.DataLocation
import com.fpt.landmarkremark.model.Locations
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import java.util.Locale


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private var lastLocation: Location? = null
    private var currentMarket: Marker? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var markers: MutableList<Marker> = mutableListOf()
    private lateinit var binding: ActivityMapsBinding
    private val shareLocationFile = "listLocationSharePrefFIle"
    private val keyLocation = "keyLocation"
    private var data: Locations? = null
    private var listLocation: MutableList<DataLocation>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        data = readData()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fetchLocation()
    }

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1000
            )
            return
        }

        val task = fusedLocationProviderClient?.lastLocation
        task!!.addOnSuccessListener { location ->
            if (location != null) {
                this.lastLocation = location
                val mapFragment =
                    supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                mapFragment.getMapAsync(this)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1000 -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.mMap = googleMap
        //Handle Location
        val locationLatLong = lastLocation?.let { LatLng(it.latitude, lastLocation!!.longitude) }
        drawMarket(locationLatLong)

        //Handle Location
        if (data == null) {
            data = Locations()
        } else {
            listLocation = data?.listLocation
            data?.listLocation?.forEach {
                mMap.addMarker(MarkerOptions().position(it.latLong).title(it.title))
            }
        }
        /**
         * BUTTON SAVE
         */
        mMap.setOnMapClickListener { latLng ->
            showAlertDialog(latLng)
        }
        /**
         * BUTTON DELETE
         */
        mMap.setOnInfoWindowClickListener { markerToDelete ->
            showAlertDialogDelete(markerToDelete)
        }
    }

    /**
     * SHOW DIALOG DELETE LOCATION
     */
    private fun showAlertDialogDelete(markerToDelete: Marker) {
        val placeFormView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_place, null)
        val dialog = AlertDialog.Builder(this).setTitle(getString(R.string.tv_confirm))
            .setView(placeFormView).setNegativeButton(getString(R.string.tv_Exit), null)
            .setPositiveButton(getString(R.string.tv_Delete), null).show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            markers.remove(markerToDelete)
            markerToDelete.remove()
            data?.let {
                it.listLocation = listLocation?.filter { it1 ->
                    it1.latLong.latitude != markerToDelete.position.latitude && it1.latLong.longitude != markerToDelete.position.longitude
                } as MutableList
                saveData(it)
            }
            dialog.dismiss()
        }
    }

    /**
     * MOVE TO MY LOCATION
     */
    private fun drawMarket(latLong: LatLng?) {
        val markerOptions =
            MarkerOptions().position(latLong!!).title(getString(R.string.tv_location_me))
                .snippet(getAddress(latLong.latitude, latLong.longitude)).draggable(true)
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLong))
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLong, 15f))
        mMap.addMarker(markerOptions)
        currentMarket = mMap.addMarker(markerOptions)
        currentMarket!!.showInfoWindow()
    }

    private fun getAddress(latitude: Double, longitude: Double): String {
        val geoCoder = Geocoder(this, Locale.getDefault())
        val address = geoCoder.getFromLocation(latitude, longitude, 1)
        return address?.get(0)?.getAddressLine(0).toString()
    }

    /**
     * DIALOG SAVE LOCATION
     */
    private fun showAlertDialog(latLng: LatLng) {
        val placeFormView = LayoutInflater.from(this).inflate(R.layout.dialog_create_place, null)
        val dialog = AlertDialog.Builder(this).setTitle(getString(R.string.title_dialog_tv))
            .setView(placeFormView).setNegativeButton(getString(R.string.tv_Exit), null)
            .setPositiveButton(getString(R.string.tv_Save), null).show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {

            val title = placeFormView.findViewById<EditText>(R.id.edtLocation).text.toString()
            if (title.trim().isEmpty()) {
                Toast.makeText(this, getString(R.string.tv_toast_edit), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mMap.addMarker(MarkerOptions().position(latLng).title(title))

            listLocation?.add(DataLocation(latLng, title))
            data?.let {
                it.listLocation = listLocation
                saveData(it)
            }

            dialog.dismiss()
        }
    }

    /**
     * SAVE DATA LOCATION SHARE PREFERENCES
     */
    private fun saveData(lists: Locations) {
        val sharedPreferences: SharedPreferences =
            getSharedPreferences(shareLocationFile, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()

        val gson = Gson()
        val locationJson = gson.toJson(lists)

        editor.putString(keyLocation, locationJson)
        editor.apply()
    }

    /**
     * READ DATA LOCATION SHARE PREFERENCES
     */
    private fun readData(): Locations? {
        val sharedPreferences: SharedPreferences =
            getSharedPreferences(shareLocationFile, Context.MODE_PRIVATE)
        val locationJson = sharedPreferences.getString(keyLocation, null)
        return if (locationJson != null) {
            val gson = Gson()
            gson.fromJson(locationJson, Locations::class.java)
        } else {
            null
        }
    }

}