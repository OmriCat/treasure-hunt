package com.omricat.treasurehunt

import android.app.Activity
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.slack.circuitx.android.AndroidScreen
import com.slack.circuitx.android.AndroidScreenStarter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.parcelize.Parcelize

class QrScannerAndroidScreenStarter(
    val activity: Activity,
    val coroutineScope: CoroutineScope,
    val onComplete: (Barcode?) -> Unit,
) : AndroidScreenStarter {

    override fun start(screen: AndroidScreen): Boolean =
        when (screen) {
            is QrScannerAndroidScreen -> screen.startWith(activity, coroutineScope, onComplete)
            else -> false
        }
}

sealed interface QrScannerResult {
    data class Success(val barcode: Barcode) : QrScannerResult

    data class Failure(val exception: Exception) : QrScannerResult

    data object Cancelled : QrScannerResult
}

private val options =
    GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .enableAutoZoom()
        .build()

@Parcelize
data object QrScannerAndroidScreen : AndroidScreen {
    fun startWith(
        activity: Activity,
        coroutineScope: CoroutineScope,
        onResult: (Barcode?) -> Unit,
    ): Boolean {
        val scanner = GmsBarcodeScanning.getClient(activity, options)
        coroutineScope.launch { onResult(scanner.startScan().await()) }
        return true
    }
}
