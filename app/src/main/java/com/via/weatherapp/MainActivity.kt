package com.via.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.via.weatherapp.models.WeatherResponse
import com.via.weatherapp.network.WeatherService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private var mProgressDialog: Dialog? = null

    private val locationPermissionLaunch: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
            permission.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value

                if (isGranted) {
                    when (permissionName) {
                        Manifest.permission.ACCESS_FINE_LOCATION -> {
                            Toast.makeText(
                                this,
                                "Permission fine location is granted",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        Manifest.permission.ACCESS_COARSE_LOCATION -> {
                            Toast.makeText(
                                this,
                                "Permission coarse location is granted",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (!isLocationEnable()) {
            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on",
                Toast.LENGTH_SHORT
            ).show()
        }
        when {
            (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) -> {
                requestLocationData()
            }
            (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                    && shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) -> {
                showRationalDialogPermission()
            }
            else -> locationPermissionLaunch.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        mFusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            val latitude = location?.latitude
            val longitude = location?.longitude
            getLocationWeatherDetail(latitude, longitude)
            Log.i("via", "latitude =  $latitude longitude = $longitude")

        }
    }

    private fun getLocationWeatherDetail(lat: Double?, lon: Double?) {
        if (Constants.isNetworkAvailable(this)) {
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service: WeatherService = retrofit.create(WeatherService::class.java)

            val listCall: Call<WeatherResponse> = service.getWeather(
                lat!!, lon!!,Constants.METRIC_UNIT, Constants.APP_ID
            )

            listCall.enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>?) {
                    if(response!!.isSuccessful){
                        val weatherList: WeatherResponse? = response.body()
                        Log.i("Response Result", "$weatherList")
                    } else {
                        when(response.code()){
                            400 -> {
                                Log.e("Error 400","Bad connection")
                            }

                            404 -> {
                                Log.e("Error 404","Not found")
                            } else -> {
                                Log.e("Error","Generic error")
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e("Errorr.....",t.message.toString())
                }

            })
        } else {
            Toast.makeText(this, "No internet connection available", Toast.LENGTH_LONG).show()
        }
    }

    private fun showRationalDialogPermission() {
        AlertDialog.Builder(this).setMessage("It looks like you have turn off permission ")
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }.show()
    }

    private fun isLocationEnable(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showCustomProgressBar(){
        mProgressDialog = Dialog(this)
        mProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog?.show()
    }
}