package com.example.lab1.ui.main

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import androidx.core.graphics.set
import androidx.core.graphics.createBitmap

@Composable
fun BarcodeView(barcodeNumber: String, modifier: Modifier = Modifier) {
    // Генерируем реальный Bitmap штрихкода
    val bitmap = remember(barcodeNumber) {
        generateBarcodeBitmap(barcodeNumber, 600, 200)
    }

    Column(
        modifier = modifier.fillMaxWidth().background(Color.White).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Штрихкод",
                modifier = Modifier.fillMaxWidth().height(100.dp)
            )
        } else {
            Text("Ошибка генерации штрихкода", color = Color.Red)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = barcodeNumber,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            color = Color.Black
        )
    }
}


private fun generateBarcodeBitmap(content: String, width: Int, height: Int): Bitmap? {
    return try {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.CODE_128,
            width,
            height
        )
        val bmp = createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp[x, y] =
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        bmp
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}