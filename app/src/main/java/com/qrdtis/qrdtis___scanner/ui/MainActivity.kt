package com.qrdtis.qrdtis___scanner.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.zxing.qrcode.encoder.QRCode
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.qrdtis.qrdtis___scanner.R
import com.qrdtis.qrdtis___scanner.databinding.ActivityMainBinding
import okhttp3.Call
import okhttp3.Callback

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.URL

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){
            isGranted: Boolean ->
            if (isGranted){
                showCamera()
            }
            else{
            }
        }

    private val scanLauncher =
        registerForActivityResult(ScanContract()){
            result: ScanIntentResult ->
            run {
                if (result.contents == null){
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
                } else {
                    setResult(result.contents)
                }
                }
            }

    private fun setResult(contents: String) {
        val showContents = "Result:\n$contents"

        Log.d(TAG, "setResult: $showContents")
        binding.textView.text = showContents
        binding.textView.text = showContents

        //sendDataToServer(showContents) //not yet done
    }

    private lateinit var binding: ActivityMainBinding //viewbinding initiation

    private fun showCamera() {
        val options = ScanOptions() //creating an instance of a class from the zxing library
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan QR code")
        options.setCameraId(0)
        options.setBeepEnabled(false) //disable beeping sound when scannimg
        options.setBarcodeImageEnabled(true)
        options.setOrientationLocked(false)

        scanLauncher.launch(options)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initBinding()
        initViews()

    }

    private fun initBinding() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
    private fun initViews(){
        binding.scan.setOnClickListener {
            checkPermissionCamera(this)
        }
    }
    private fun checkPermissionCamera(context: Context) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            showCamera()
        }
        else if (shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)){
            Toast.makeText(context, "Camera Permission Required", Toast.LENGTH_SHORT).show()
        }
        else{
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun sendDataToServer(qrContents: String){
        val client = OkHttpClient()
        val serverUrl = "http://192.168.108.58:5000/"

        val requestBody = qrContents.toRequestBody("text/plain".toMediaTypeOrNull())
        val request = Request.Builder().url(serverUrl).post(requestBody).build()

        client.newCall(request).enqueue(object: Callback
        {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread{binding.textView.text = "Failed to send data"}

            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful){
                        runOnUiThread{binding.textView.text = "Data sent successfully"}
                    } else{
                        runOnUiThread{binding.textView.text = "Failed to send data"}
                    }
                }
            }
        })
    }
}