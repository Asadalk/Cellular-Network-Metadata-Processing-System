package com.tower.locator

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
import android.widget.TextView
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellIdentityNr
import android.telephony.CellSignalStrengthNr


class MainActivity : AppCompatActivity() {

    private lateinit var telephonyManager: TelephonyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        outputText = findViewById(R.id.outputText)

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

    private lateinit var outputText: TextView

    private fun updateCellTowerData(cellInfoList: List<CellInfo>) {

        val sb = StringBuilder()

        for (cellInfo in cellInfoList) {
            // ignore secondary cells
            if (!cellInfo.isRegistered) continue

            when (cellInfo) {
                is CellInfoLte -> {
                    val id = cellInfo.cellIdentity
                    val ss = cellInfo.cellSignalStrength

                    val mcc = id.mccString?.toIntOrNull() ?: 0
                    val mnc = id.mncString?.toIntOrNull() ?: 0
                    val tac = id.tac
                    val ci = id.ci
                    val rsrp = if (ss.rsrp != CellInfo.UNAVAILABLE) ss.rsrp else null

                    sb.append(
                        """
                        LTE CELL
                        MCC: $mcc
                        MNC: $mnc
                        TAC: $tac
                        CI: $ci
                        RSRP: $rsrp dBm
                        
                        """.trimIndent()
                    )
                }

                is CellInfoNr -> {
                    val id = cellInfo.cellIdentity as CellIdentityNr
                    val ss = cellInfo.cellSignalStrength as CellSignalStrengthNr

                    val mcc = id.mccString?.toIntOrNull() ?: 0
                    val mnc = id.mncString?.toIntOrNull() ?: 0
                    val tac = id.tac
                    val nci = id.nci
                    val ssRsrp : Int? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ss.ssRsrp != CellInfo.UNAVAILABLE) ss.ssRsrp else null
                        } else {
                            null
                        }

                    sb.append(
                        """
                        5G NR CELL
                        MCC: $mcc
                        MNC: $mnc
                        TAC: $tac
                        NCI: $nci
                        SS-RSRP: $ssRsrp dBm
                        
                        """.trimIndent()
                    )
                }
            }
        }

        outputText.text = sb.toString()
    }
}