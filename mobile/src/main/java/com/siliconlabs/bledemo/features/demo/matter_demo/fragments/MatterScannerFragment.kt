package com.siliconlabs.bledemo.features.demo.matter_demo.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import chip.setuppayload.SetupPayload
import chip.setuppayload.SetupPayloadParser
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.FragmentMatterScannerBinding
import com.siliconlabs.bledemo.features.demo.matter_demo.activities.MatterDemoActivity
import com.siliconlabs.bledemo.features.demo.matter_demo.model.CHIPDeviceInfo
import com.siliconlabs.bledemo.features.demo.matter_demo.model.ProvisionNetworkType
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.FragmentUtils
import com.siliconlabs.bledemo.features.demo.matter_demo.utils.MessageDialogFragment
import com.siliconlabs.bledemo.features.iop_test.utils.DialogDeviceInfoFragment
import timber.log.Timber
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MatterScannerFragment : Fragment() {

    private lateinit var dialog: MessageDialogFragment
    val dialogTag = "MessageDialog"
    private lateinit var binding: FragmentMatterScannerBinding
    var networkSelectionDialog: MatterNetworkSelectionInputDialogFragment? = null

    private lateinit var networkMode: String
    private lateinit var payload: SetupPayload
    private var isShortDiscriminator = false
    private lateinit var qrCodeManualInput: String
    private var deviceDialog: DialogDeviceInfoFragment? = null
    private var deviceInfodialogShown = false

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasLocationPermission()) {
            requestLocationPermission()
        }

        if (!hasCameraPermission()) {
            requestCameraPermission()
        }

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MatterDemoActivity).showQRScanner()
        binding.submitEntry.setOnClickListener {
            qrCodeManualInput = binding.manualCodeEditText.text.toString().trim()
            println("Input MT : ${qrCodeManualInput}")
            handleManualInput(qrCodeManualInput)
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as MatterDemoActivity).hideQRScanner()
        startCamera()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentMatterScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun startCamera() {
        Timber.tag(TAG).e("startCamera")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val metrics = DisplayMetrics().also { binding.cameraView.display?.getRealMetrics(it) }
            val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)

            if (binding.cameraView.display.rotation != null) {
                val preview: Preview = Preview.Builder()
                    .setTargetAspectRatio(screenAspectRatio)
                    .setTargetRotation(binding.cameraView.display.rotation)
                    .build()

                preview.setSurfaceProvider(binding.cameraView.surfaceProvider)

                // Setup barcode scanner
                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetAspectRatio(screenAspectRatio)
                    .setTargetRotation(binding.cameraView.display.rotation)
                    .build()
                val cameraExecutor = Executors.newSingleThreadExecutor()
                val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()
                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(barcodeScanner, imageProxy)
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
                } catch (exc: Exception) {
                    Timber.tag(TAG).e("Use case binding failed: $exc")
                }
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ) {
        val inputImage =
            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcode ->
                Timber.tag(TAG).e("Barcodes" + barcode)
                barcode.forEach {
                    handleScannedQrCode(it)
                }
            }
            .addOnFailureListener {
                Timber.tag(TAG).e(it.message ?: it.toString())
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun handleManualInput(qrCode: String) {
        if (qrCode.isNullOrBlank()) {
            showMessage(getString(R.string.matter_validation_manual_qrcode))
            return
        }
        try {
            payload = SetupPayloadParser().parseQrCode(qrCode)
            println("Payload : $payload")

            if (networkSelectionDialog == null) {
                val prev =
                    requireActivity().supportFragmentManager.findFragmentByTag(
                        ALERT_NTW_MODE_DIALOG_TAG
                    )
                if (prev == null) {
                    val chipDeviceInfo = """
    Version: ${payload.version ?: "N/A"}
    
    Vendor ID: ${payload.vendorId ?: "N/A"}
    
    Product ID: ${payload.productId ?: "N/A"}
    
    Discriminator: ${payload.discriminator ?: "N/A"}
    
    Setup PIN Code: ${payload.setupPinCode ?: "N/A"}
    
    Discovery Capabilities: ${
                        payload.discoveryCapabilities?.joinToString(", ") ?: "N/A"
                    }
    
    Commissioning Flow: ${payload.commissioningFlow ?: "N/A"}
""".trimIndent()
                    showDeviceInfoDialog(chipDeviceInfo)



                }
            }


        } catch (ex: SetupPayloadParser.SetupPayloadException) {
            try {
                payload = SetupPayloadParser().parseManualEntryCode(qrCode)
                isShortDiscriminator = true
            } catch (ex: Exception) {
                Timber.tag(TAG).e("Unrecognized Manual Pairing Code $ex")
                showMessageDialog(getString(R.string.matter_invalid_manual_qr_code))
            }
        } catch (ex: SetupPayloadParser.UnrecognizedQrCodeException) {
            Timber.tag(TAG).e("Unrecognized QR Code $ex")
            // Toast.makeText(requireContext(), "Unrecognized QR Code", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Timber.tag(TAG).e("Unrecognized QR Exception $e")
        }

    }

    private fun handleScannedQrCode(barcode: Barcode) {
        println("Matter Camera Scanned QR")

        Handler(Looper.getMainLooper()).post {

            try {
                payload = SetupPayloadParser().parseQrCode(barcode.displayValue)
                isShortDiscriminator = true


                if (networkSelectionDialog == null) {
                    val prev =
                        requireActivity().supportFragmentManager.findFragmentByTag(
                            ALERT_NTW_MODE_DIALOG_TAG
                        )
                    if (prev == null) {
                        val chipDeviceInfo = """
    Version: ${payload.version ?: "N/A"}
    
    Vendor ID: ${payload.vendorId ?: "N/A"}
    
    Product ID: ${payload.productId ?: "N/A"}
    
    Discriminator: ${payload.discriminator ?: "N/A"}
    
    Setup PIN Code: ${payload.setupPinCode ?: "N/A"}
    
    Discovery Capabilities: ${
                            payload.discoveryCapabilities?.joinToString(", ") ?: "N/A"
                        }
    
    Commissioning Flow: ${payload.commissioningFlow ?: "N/A"}
""".trimIndent()
                        showDeviceInfoDialog(chipDeviceInfo)


                    }
                }

            } catch (ex: SetupPayloadParser.UnrecognizedQrCodeException) {
                Timber.tag(TAG).e("Unrecognized QR Code $ex")
                //  showMessageDialog()
                return@post
            } catch (e: SetupPayloadParser.InvalidEntryCodeFormatException) {
                Timber.tag(TAG).e("InvalidEntryCodeFormatException $e")
                showMessageDialog(getString(R.string.qr_code_unrecognized))
            } catch (d: Exception) {
                Timber.tag(TAG).e("Unrecognized QR Exception $d")
                showMessageDialog(getString(R.string.qr_code_unrecognized))
            }

        }
    }

    private fun showMessageDialog(string: String) {

        try {
            if (isAdded && requireActivity() != null && !requireActivity().isFinishing) {
                requireActivity().runOnUiThread {
                    if (!MessageDialogFragment.isDialogShowing()) {
                        dialog = MessageDialogFragment()
                        dialog.setMessage(string)
                        dialog.setOnDismissListener {
                            dialog.dismiss()
                        }
                        val transaction: FragmentTransaction =
                            requireActivity().supportFragmentManager.beginTransaction()

                        dialog.show(transaction, dialogTag)
                    }
                }
            } else {
                Timber.e("Unrecognized QR Exception")
            }
        } catch (e: Exception) {
            Timber.e("Unrecognized QR Exception $e" )
        }

    }

    private fun navigateToThreadDevice(setupPayload: SetupPayload, isShortDiscriminator: Boolean) {
        FragmentUtils.getHost(this@MatterScannerFragment, CallBack::class.java)
            .setNetworkType(ProvisionNetworkType.THREAD)
        FragmentUtils.getHost(this@MatterScannerFragment, CallBack::class.java)
            .onChipDeviceInfoReceived(
                CHIPDeviceInfo.fromSetupPayload(
                    setupPayload,
                    isShortDiscriminator
                ), INPUT_NETWORK_THREAD_TYPE_SELECTED
            )
    }

    private fun navigateToWiFiDevice(setupPayload: SetupPayload, isShortDiscriminator: Boolean) {
        FragmentUtils.getHost(this@MatterScannerFragment, CallBack::class.java)
            .setNetworkType(ProvisionNetworkType.WIFI)
        FragmentUtils.getHost(this@MatterScannerFragment, CallBack::class.java)
            .onChipDeviceInfoReceived(
                CHIPDeviceInfo.fromSetupPayload(
                    setupPayload,
                    isShortDiscriminator
                ), INPUT_NETWORK_WIFI_TYPE_SELECTED
            )
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun hasCameraPermission(): Boolean {
        return (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ))
    }

    private fun requestCameraPermission() {
        val permissions = arrayOf(Manifest.permission.CAMERA)
        requestPermissions(permissions, REQUEST_CODE_CAMERA_PERMISSION)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestLocationPermission() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        requestPermissions(permissions, REQUEST_CODE_LOCATION_PERMISSION)
    }

    private fun hasLocationPermission(): Boolean {
        val locationPermissionGranted =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        // Android 12 new permission
        var bleScanPermissionGranted = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bleScanPermissionGranted =
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED
        }

        return locationPermissionGranted && bleScanPermissionGranted
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                showCameraPermissionAlert()
            }
        } else if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                showLocationPermissionAlert()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun showBluetoothEnableAlert() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.toast_bluetooth_not_enabled)
            .setMessage(R.string.bluetooth_adapter_bar_turning_on)
            .setPositiveButton(R.string.matter_camera_unavailable_alert_exit) { _, _ ->
                requireActivity().finish()
            }
            .setCancelable(false)
            .create().show()
    }

    private fun showCameraPermissionAlert() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.matter_camera_unavailable_alert_title)
            .setMessage(R.string.matter_camera_unavailable_alert_subtitle)
            .setPositiveButton(R.string.matter_camera_unavailable_alert_exit) { _, _ ->
                requireActivity().finish()
            }
            .setCancelable(false)
            .create().show()
    }

    private fun showLocationPermissionAlert() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.matter_location_unavailable_alert_title)
            .setMessage(R.string.matter_location_unavailable_alert_subtitle)
            .setPositiveButton(R.string.matter_camera_unavailable_alert_exit) { _, _ ->
                requireActivity().finish()
            }
            .setCancelable(false)
            .create().show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == WIFI_REQ_CODE) {
            println("Matter Wifi Selected")
            networkMode = data!!.getStringExtra(ARG_PROVISION_NETWORK_TYPE).toString()
            navigateToWiFiDevice(payload, isShortDiscriminator)
        } else if (resultCode == THREAD_REQ_CODE) {
            println("Matter Thread Selected")
            networkMode = data!!.getStringExtra(ARG_PROVISION_NETWORK_TYPE).toString()
            navigateToThreadDevice(payload, isShortDiscriminator)
        } else if (resultCode == CANCEL_REQ_CODE) {
            cancelNetworkInputDialog()
            startCamera()
        }
    }

    private fun cancelNetworkInputDialog() {
        if (networkSelectionDialog != null) {
            networkSelectionDialog = null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cancelNetworkInputDialog()
    }

    private fun showMessage(msg: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeviceInfoDialog(message: String) {
        try {
            if (isAdded && requireActivity() != null && !requireActivity().isFinishing) {
                requireActivity().runOnUiThread {
                    if (!deviceInfodialogShown && (deviceDialog == null || !deviceDialog!!.isShowing())) {
                        deviceInfodialogShown=true
                        deviceDialog = DialogDeviceInfoFragment.Builder()
                            .setTitle(getString(R.string.qr_code_info))
                            .setMessage(message)
                            .setPositiveButton(getString(R.string.start_commissioning)) { dialog, which ->
                                // Positive button click logic
                                networkSelectionDialog =
                                    MatterNetworkSelectionInputDialogFragment.newInstance()

                                networkSelectionDialog!!.setTargetFragment(this, DIALOG_NTW_MODE_FRAGMENT)
                                networkSelectionDialog!!.show(
                                    requireActivity().supportFragmentManager, ALERT_NTW_MODE_DIALOG_TAG
                                )
                                deviceDialog?.isShowing()==false
                                deviceInfodialogShown=false
                            }
                            .setNegativeButton("Cancel") { dialog, which ->
                                deviceDialog?.dismiss()
                                deviceDialog?.isShowing()==false
                                deviceInfodialogShown=false
                            }
                            .build()

                        deviceDialog?.show(parentFragmentManager, "DialogDeviceInfoFragment")
                    }
                }
            } else {
                Timber.e("Fail to read device detail")
            }
        } catch (e: Exception) {
            Timber.e("Fail to read device detail $e")
        }
    }

    interface CallBack {
        fun onChipDeviceInfoReceived(deviceInfo: CHIPDeviceInfo, ntwInputType: String)

        fun setNetworkType(type: ProvisionNetworkType)
    }


    companion object {
        private val TAG = MatterScannerFragment::class.java.classes.toString()
        private const val REQUEST_CODE_LOCATION_PERMISSION = 101
        private const val REQUEST_CODE_CAMERA_PERMISSION = 100
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        private const val DIALOG_NTW_MODE_FRAGMENT = 998
        const val CANCEL_REQ_CODE = 5000
        const val WIFI_REQ_CODE = 5001
        const val THREAD_REQ_CODE = 5002
        const val INPUT_WIFI_REQ_CODE = 5003
        const val INPUT_NETWORK_WIFI_TYPE_SELECTED = "wifi"
        const val INPUT_NETWORK_THREAD_TYPE_SELECTED = "thread"
        const val WIFI_INPUT_SSID = "wifi_input_ssid"
        const val WIFI_INPUT_PASSWORD = "wifi_input_password"
        const val ARG_PROVISION_NETWORK_TYPE = "dialog_network_mode_info"
        private const val ALERT_NTW_MODE_DIALOG_TAG = "alert_ntw_mode_dialog_tag"
        fun newInstance(): MatterScannerFragment {
            val args = Bundle()

            val fragment = MatterScannerFragment()
            fragment.arguments = args
            return fragment
        }
    }
}