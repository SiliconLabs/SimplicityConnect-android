package com.siliconlabs.bledemo.features.demo.wifi_throughput.activities

import android.content.Context
import android.net.InetAddresses
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.Fragment
import com.siliconlabs.bledemo.R
import com.siliconlabs.bledemo.databinding.ActivityWifiThroughputBinding
import com.siliconlabs.bledemo.features.demo.wifi_throughput.fragments.WifiThroughPutDetailScreen
import com.siliconlabs.bledemo.features.demo.wifi_throughput.utils.ThroughputUtils
import com.siliconlabs.bledemo.features.iop_test.utils.Utils
import com.siliconlabs.bledemo.utils.AppUtil
import com.siliconlabs.bledemo.utils.ApppUtil
import com.siliconlabs.bledemo.utils.CustomToastManager
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WifiThroughputActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWifiThroughputBinding
    private val throughPutDemos = ThroughputUtils.WiFiThroughPutFeature.values()
    private lateinit var context: Context
    private var isConfirmCalled = mutableStateOf(false)
    var ipAddress by mutableStateOf("")
    var portNumber by mutableStateOf("")
    var userSelectedFeature: Int by mutableIntStateOf(0)
    private var isDownload = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this@WifiThroughputActivity
        binding = ActivityWifiThroughputBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Edge-to-edge & insets handling
        AppUtil.setEdgeToEdge(window, this)

        // Proper ActionBar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.matter_back)
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.wifi_title_Throughput)
        }
        findViewById<ComposeView>(R.id.my_composable).setContent {
            GridLayout(this, throughPutDemos)
            val isConfirm = remember { isConfirmCalled }
            if (isConfirm.value) {
                //FragmentContainer()
            }
        }
    }

    // Function to update the ActionBar title
    fun updateActionBarTitle(title: String) {
        supportActionBar?.title = title
    }

    // Function to reset the title back to the static one
    fun resetActionBarTitle() {
        supportActionBar?.title = getString(R.string.wifi_title_Throughput)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {

        resetActionBarTitle()
        val myFragment: WifiThroughPutDetailScreen? =
            supportFragmentManager.findFragmentByTag("tag") as WifiThroughPutDetailScreen?
        if (myFragment != null && myFragment.isVisible()) {
            supportFragmentManager.popBackStack()
        }
        super.onBackPressed()
    }

    private fun showThroughputDetailScreen(
        userSelectedOption: Int,
        ipAddress: String,
        portNumber: String
    ) {
        val mBundle = Bundle()
        mBundle.putString(
            ThroughputUtils.throughPutType,
            ThroughputUtils.getTitle(userSelectedOption, context)
        )
        mBundle.putString(ThroughputUtils.ipAddress, ipAddress)
        mBundle.putString(ThroughputUtils.portNumber, portNumber)
        val fragment = WifiThroughPutDetailScreen()
        showFragment(
            fragment,
            mBundle,
            fragment::class.java.simpleName
        )
    }

    private fun showFragment(
        fragment: Fragment, bundle: Bundle, tag: String? = null,
    ) {
        val fManager = supportFragmentManager
        val fTransaction = fManager.beginTransaction()
        fragment.setArguments(bundle)

        fTransaction.add(binding.throughputContainer, fragment, tag)
            .addToBackStack(null)
            .commit()

    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun GridLayout(
        context: Context,
        throughPutDemos: Array<ThroughputUtils.WiFiThroughPutFeature>
    ) {
        val dialogState: MutableState<Boolean> = remember {
            mutableStateOf(false)
        }
        val dialogHeaderTitle: MutableState<String> = remember { mutableStateOf("") }
        val dialogHeaderSubTitle: MutableState<String> = remember { mutableStateOf("") }
        LazyVerticalGrid(columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 12.dp,
                top = 16.dp,
                end = 12.dp,
                bottom = 16.dp
            ),
            content = {
                items(throughPutDemos.size) {
                    Card(
                        backgroundColor = colorResource(R.color.silabs_white),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(4.dp)
                            .size(150.dp)
                            .fillMaxWidth(),
                        elevation = 8.dp,
                        onClick = {
                            dialogState.value = true
                            if (ThroughputUtils.getTitle(
                                    it,
                                    context
                                ) == ThroughputUtils.THROUGHPUT_TYPE_TCP_DOWNLOAD
                            ) {
                                dialogHeaderTitle.value = getString(R.string.tcp_server)
                                dialogHeaderSubTitle.value =
                                    getString(R.string.tcp_download_sub_title)
                            } else if (ThroughputUtils.getTitle(
                                    it,
                                    context
                                ) == ThroughputUtils.THROUGHPUT_TYPE_TCP_UPLOAD
                            ) {
                                dialogHeaderTitle.value = getString(R.string.tcp_client)
                                dialogHeaderSubTitle.value =
                                    getString(R.string.tcp_upload_sub_title)
                            } else if (ThroughputUtils.getTitle(
                                    it,
                                    context
                                ) == ThroughputUtils.THROUGHPUT_TYPE_UDP_DOWNLOAD
                            ) {
                                dialogHeaderTitle.value = getString(R.string.udp_server)
                                dialogHeaderSubTitle.value =
                                    getString(R.string.dialog_udp_download_sub_title)
                            } else if (ThroughputUtils.getTitle(
                                    it,
                                    context
                                ) == ThroughputUtils.THROUGHPUT_TYPE_UDP_UPLOAD
                            ) {
                                dialogHeaderTitle.value = getString(R.string.udp_client)
                                dialogHeaderSubTitle.value =
                                    getString(R.string.dialog_udp_upload_sub_title)
                            } else if (ThroughputUtils.getTitle(
                                    it,
                                    context
                                ) == ThroughputUtils.THROUGHPUT_TYPE_TLS_DOWNLOAD
                            ) {
                                dialogHeaderTitle.value = getString(R.string.tls_server)
                                dialogHeaderSubTitle.value =
                                    getString(R.string.dialog_tls_download_sub_title)
                            } else if (ThroughputUtils.getTitle(
                                    it,
                                    context
                                ) == ThroughputUtils.THROUGHPUT_TYPE_TLS_UPLOAD
                            ) {
                                dialogHeaderTitle.value = getString(R.string.tls_server)
                                dialogHeaderSubTitle.value =
                                    getString(R.string.dialog_sub_title_tls_upload)
                            }
                            userSelectedFeature = it
                            if (ThroughputUtils.isThroughPutTypeDownload(it)) {
                                isDownload.value = false
                                ipAddress = getLocalIpAddress(context)
                            } else {
                                isDownload.value = true
                                ipAddress = ""
                            }
                        }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = ThroughputUtils.getTitle(it, context),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = colorResource(id = R.color.silabs_black),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                    }
                }
            })

        if (dialogState.value) {
            Dialog(
                onDismissRequest = ({
                    dialogState.value = false
                    isConfirmCalled.value = false
                }),
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false
                )
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = dialogHeaderTitle.value,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = colorResource(id = R.color.silabs_black),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp),
                        )
                        Text(
                            text = dialogHeaderSubTitle.value,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            color = colorResource(id = R.color.silabs_primary_text),
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                        )
                        TextField(
                            textStyle = TextStyle(fontSize = 20.sp),
                            modifier = Modifier.padding(16.dp),
                            enabled = isDownload.value,
                            value = ipAddress,
                            onValueChange = {
                                if (it.length <= 15) {
                                    ipAddress = it
                                }

                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text(stringResource(R.string.enter_ip_address)) },
                            colors = TextFieldDefaults.textFieldColors(
                                unfocusedLabelColor = colorResource(R.color.silabs_black),
                                focusedLabelColor = colorResource(R.color.silabs_dark_blue),
                                textColor = colorResource(R.color.silabs_black),  // Text color
                                backgroundColor = Color.LightGray,  // Background color
                                placeholderColor = Color.Gray,  // Placeholder text color
                                cursorColor = colorResource(R.color.silabs_dark_blue),  // Cursor color
                                focusedIndicatorColor = colorResource(R.color.silabs_dark_blue),  // Focused indicator color
                                unfocusedIndicatorColor = colorResource(id = R.color.silabs_dark_gray_text) // Unfocused indicator color
                            )
                        )

                        val maxChar = 4
                        TextField(
                            textStyle = TextStyle(fontSize = 20.sp),
                            modifier = Modifier.padding(16.dp),
                            value = portNumber,
                            onValueChange = {
                                if (it.length <= maxChar)
                                    portNumber = it
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = {
                                Text(context.getString(R.string.enter_port_number))
                            },
                            colors = TextFieldDefaults.textFieldColors(
                                unfocusedLabelColor = colorResource(R.color.silabs_black),
                                focusedLabelColor = colorResource(R.color.silabs_dark_blue),
                                textColor = colorResource(R.color.silabs_black),  // Text color
                                backgroundColor = Color.LightGray,  // Background color
                                placeholderColor = Color.Gray,  // Placeholder text color
                                cursorColor = colorResource(R.color.silabs_dark_blue),  // Cursor color
                                focusedIndicatorColor = colorResource(R.color.silabs_dark_blue),  // Focused indicator color
                                unfocusedIndicatorColor = colorResource(id = R.color.silabs_dark_gray_text) // Unfocused indicator color
                            )
                        )


                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            TextButton(
                                onClick = {
                                    dialogState.value = false
                                    isConfirmCalled.value = false
                                },
                                modifier = Modifier.padding(8.dp),
                            ) {
                                Text(
                                    text = context.getString(R.string.matter_cancel),
                                    color = colorResource(id = R.color.silabs_dark_blue),
                                    fontSize = 18.sp
                                )
                            }

                            var isValidIp = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                isValidIp = InetAddresses.isNumericAddress(ipAddress)
                            } else {
                                isValidIp = Patterns.IP_ADDRESS.matcher(ipAddress).matches()
                            }
                            Button(
                                onClick = {
                                    if (/*isValidIp
                                     && */portNumber.length >= maxChar) {
                                        dialogState.value = false
                                        showThroughputDetailScreen(
                                            userSelectedFeature,
                                            ipAddress,
                                            portNumber
                                        )
                                        isConfirmCalled.value = true
                                    } else {
                                        /*Toast.makeText(
                                            context,
                                            getString(R.string.please_enter_valid_port_number),
                                            Toast.LENGTH_LONG
                                        ).show()*/
                                        CustomToastManager.show(
                                            context,getString(R.string.please_enter_valid_port_number),5000
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(8.dp), // Rounded corners with a 16 dp radius
                                colors = ButtonDefaults.buttonColors(colorResource(R.color.silabs_dark_blue)),
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(
                                    text = context.getString(R.string.dialog_start_server),
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    private fun getLocalIpAddress(context: Context): String {
        val wifiManager =
            (context.getSystemService(Context.WIFI_SERVICE) as WifiManager)
        val wifiInfo = wifiManager.connectionInfo
        val ipInt = wifiInfo.ipAddress
        return InetAddress.getByAddress(
            ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()
        ).hostAddress
    }
}
