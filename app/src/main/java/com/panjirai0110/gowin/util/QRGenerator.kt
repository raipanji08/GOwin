package com.panjirai0110.gowin.util

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter


fun generateQR(
    text: String,
    width: Int = 512,
    height: Int = 512
): Bitmap {

    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height)

    val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)

    for (x in 0 until width) {
        for (y in 0 until height) {

            bitmap[x, y] = if (bitMatrix[x, y]) {
                android.graphics.Color.BLACK
            } else {
                android.graphics.Color.WHITE
            }

        }
    }

    return bitmap
}
