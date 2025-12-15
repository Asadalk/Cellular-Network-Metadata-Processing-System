package com.tower.locator

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import okhttp3.ResponseBody
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.CellInfo
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.telephony.PhoneStateListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellIdentityNr
import android.telephony.CellSignalStrengthNr
import android.util.Log
import com.tower.locator.model.CellPayload
import com.google.gson.Gson
import com.tower.locator.model.LocateResponse
import com.tower.locator.network.ApiService
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon


class MainActivity : AppCompatActivity() {

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(5.0)
        map.controller.setCenter(org.osmdroid.util.GeoPoint(20.5937, 78.9629)) // India

        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        // checking permissions
        checkPermissions()

    }

    private fun checkPermissions() {

        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED ) {
            
            permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)
        }

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED ) {

            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(),101)
        } else {
            getCellTowerInfo()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)

        if (requestCode == 101) {
            if (grantResults.isNotEmpty()) {
                var allGranted = true

                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false
                        break
                    }
                }

                if (allGranted) {
                    getCellTowerInfo()
                }
            }
        }
    }

    private var telephonyCallback: TelephonyCallback? = null
    private var phoneStateListener: PhoneStateListener? = null

    private fun getCellTowerInfo() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            //Using TelephonyCallback for android 12+

            telephonyCallback = object: TelephonyCallback(), TelephonyCallback.CellInfoListener {
                override fun onCellInfoChanged (cellInfo: List<CellInfo>) {
                    updateCellTowerData(cellInfo)
                }
            }  

            telephonyManager.registerTelephonyCallback(mainExecutor, telephonyCallback!!) 
        } else {
            // Using PhoneStateListener for Android < 12

            phoneStateListener = object: PhoneStateListener() {
                override fun onCellInfoChanged(cellInfo: List<CellInfo>) {
                    updateCellTowerData(cellInfo)
                }
            }
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CELL_INFO)
        }
    }

    private fun updateCellTowerData(cellInfoList: List<CellInfo>) {

        for (cellInfo in cellInfoList) {
            // ignore secondary cells
            if (!cellInfo.isRegistered) continue

            when (cellInfo) {
                is CellInfoLte -> {
                    Log.d("CELL", "Entered LTE block")
                    val id = cellInfo.cellIdentity
                    val ss = cellInfo.cellSignalStrength

                    val payload = CellPayload(
                        mcc = id.mccString?.toIntOrNull() ?: -1,
                        mnc = id.mncString?.toIntOrNull() ?: -1,
                        tac = id.tac,
                        cid = id.ci.toLong(),
                        rsrp = if (ss.rsrp != CellInfo.UNAVAILABLE) ss.rsrp else null
//                          mcc = 404,
//                          mnc = 5,
//                          tac = 221,
//                          cid = 2171,
//                          rsrp = -80
                    )
                    val gson = Gson()
                    val json = gson.toJson(payload)

                    Log.d("CELL_JSON", json)


                    // send to backend
                    val retrofit = Retrofit.Builder()
                        .baseUrl("http://10.60.233.22:8000/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()

                    val api = retrofit.create(ApiService::class.java)

                    api.sendCellData(payload).enqueue(object : Callback<ResponseBody> {
                        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                            Log.d("NETWORK", "Success")
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            Log.e("NETWORK", "Failed", t)
                        }
                    })

                    Log.e("LOCATE_DEBUG", "ABOUT TO CALL /locate")

                    api.locateCell(payload).enqueue(object : Callback<LocateResponse> {

                        override fun onResponse(
                            call: Call<LocateResponse>,
                            response: Response<LocateResponse>
                        ) {
                            Log.e("LOCATE_DEBUG", "onResponse HIT")

                            val body = response.body()

                            Log.e("LOCATE_DEBUG", "raw response = ${response.body()}")

                            if (body == null) {
                                Log.e("LOCATE", "Response body is null")
                                return
                            }

                            Log.e(
                                "LOCATE_DEBUG",
                                "lat=${body.lat}, lon=${body.lon}, radius=${body.radius}"
                            )

                            if (body.error != null) {
                                Log.d("LOCATE", "Tower not found")
                                return
                            }

                            val lat = body.lat!!
                            val lon = body.lon!!
                            val radius = body.radius!!.toDouble()

                            runOnUiThread {

                                val point = GeoPoint(lat, lon)

                                // Clear previous overlays
                                map.overlays.clear()

                                // Marker
                                val marker = Marker(map)
                                marker.position = point
                                marker.title = "Serving Tower"
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                map.overlays.add(marker)

                                // Radius circle
                                val circle = Polygon()
                                circle.points = Polygon.pointsAsCircle(point, radius)
                                circle.fillColor = 0x221E88E5
                                circle.strokeColor = 0xFF1E88E5.toInt()
                                circle.strokeWidth = 2f
                                map.overlays.add(circle)

                                // Move camera
                                map.controller.setZoom(15.0)
                                map.controller.setCenter(point)

                                map.invalidate()

                                Log.d("LOCATE", "Rendered tower on map")
                            }
                        }


                        override fun onFailure(call: Call<LocateResponse>, t: Throwable) {
                            Log.e("LOCATE", "Request failed", t)
                        }
                    })
                }

                is CellInfoNr -> {
                    Log.d("CELL", "Entered NR block")
                    val id = cellInfo.cellIdentity as CellIdentityNr
                    val ss = cellInfo.cellSignalStrength as CellSignalStrengthNr

                    val payload = CellPayload(
                        mcc = id.mccString?.toIntOrNull() ?: -1,
                        mnc = id.mncString?.toIntOrNull() ?: -1,
                        tac = id.tac,
                        cid = id.nci,
                        rsrp =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ss.ssRsrp != CellInfo.UNAVAILABLE) ss.ssRsrp else null
                            } else {
                                null
                            }
//                        mcc = 404,
//                        mnc = 5,
//                        tac = 221,
//                        cid = 2171,
//                        rsrp = -80
                    )
                    val gson = Gson()
                    val json = gson.toJson(payload)

                    Log.d("CELL_JSON", json)

                    // send to backend
                    val retrofit = Retrofit.Builder()
                        .baseUrl("http://10.60.233.22:8000/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()

                    val api = retrofit.create(ApiService::class.java)

                    api.sendCellData(payload).enqueue(object : Callback<ResponseBody> {
                        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                            Log.d("NETWORK", "Success")
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            Log.e("NETWORK", "Failed", t)
                        }
                    })

                    Log.e("LOCATE_DEBUG", "ABOUT TO CALL /locate")

                    api.locateCell(payload).enqueue(object : Callback<LocateResponse> {

                        override fun onResponse(
                            call: Call<LocateResponse>,
                            response: Response<LocateResponse>
                        ) {
                            Log.e("LOCATE_DEBUG", "onResponse HIT")

                            val body = response.body()

                            Log.e("LOCATE_DEBUG", "raw response = ${response.body()}")

                            if (body == null) {
                                Log.e("LOCATE", "Response body is null")
                                return
                            }

                            Log.e(
                                "LOCATE_DEBUG",
                                "lat=${body.lat}, lon=${body.lon}, radius=${body.radius}"
                            )

                            if (body.error != null) {
                                Log.d("LOCATE", "Tower not found")
                                return
                            }

                            val lat = body.lat!!
                            val lon = body.lon!!
                            val radius = body.radius!!.toDouble()

                            runOnUiThread {

                                val point = GeoPoint(lat, lon)

                                // Clear previous overlays
                                map.overlays.clear()

                                // Marker
                                val marker = Marker(map)
                                marker.position = point
                                marker.title = "Serving Tower"
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                map.overlays.add(marker)

                                // Radius circle
                                val circle = Polygon()
                                circle.points = Polygon.pointsAsCircle(point, radius)
                                circle.fillColor = 0x221E88E5
                                circle.strokeColor = 0xFF1E88E5.toInt()
                                circle.strokeWidth = 2f
                                map.overlays.add(circle)

                                // Move camera
                                map.controller.setZoom(15.0)
                                map.controller.setCenter(point)

                                map.invalidate()

                                Log.d("LOCATE", "Rendered tower on map")
                            }
                        }


                        override fun onFailure(call: Call<LocateResponse>, t: Throwable) {
                            Log.e("LOCATE", "Request failed", t)
                        }
                    })

                }
            }
        }
    }
}