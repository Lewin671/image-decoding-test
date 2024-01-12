package com.example.imagedecodingtest

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity() {
    private var mTextView: TextView? = null
    private fun fetchImage(imageUrl: String, heif: Boolean = false): InputStream? {
        try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            if (heif) {
                connection.setRequestProperty("Accept", "image/webp,image/heic,image/heif")
            }
            connection.doInput = true
            connection.connect()
            return connection.inputStream
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }

    @Throws(IOException::class)
    fun convertInputStreamToBytes(inputStream: InputStream): ByteArray? {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead)
        }
        byteArrayOutputStream.close()
        return byteArrayOutputStream.toByteArray()
    }

    private fun log(tag: String, msg: String) {
        Log.e(tag, msg)
        runOnUiThread {
            mTextView?.append("tag:$tag, msg:$msg\n\n")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val handlerThread = HandlerThread("imageDecoder")
        handlerThread.start()
        val handler = Handler(handlerThread.looper)

        val button = findViewById<Button>(R.id.startDecodingTestButton)
        mTextView = findViewById<TextView>(R.id.textView)
        findViewById<Button>(R.id.clearLog).setOnClickListener {
            mTextView?.text = ""
        }

        button.setOnClickListener {
            handler.post {
                val start = SystemClock.uptimeMillis()
                val webpImageUrl =
                    "https://img.alicdn.com/bao/uploaded/i1/128156088/O1CN01KYhBkT1uqLy4t00qq_!!0-saturn_solar.jpg_790x10000Q75.jpg_.webp"
                val heifImageUrl =
                    "https://img.alicdn.com/bao/uploaded/i1/128156088/O1CN01KYhBkT1uqLy4t00qq_!!0-saturn_solar.jpg_790x10000Q75.jpg_.heic"
                var webpImgBytes: ByteArray? = null
                var heifImgBytes: ByteArray? = null

                fetchImage(webpImageUrl)?.let { inputStream ->
                    webpImgBytes = convertInputStreamToBytes(inputStream)!!
                    log(
                        "myTag",
                        "download img costs ${SystemClock.uptimeMillis() - start}, $webpImageUrl"
                    )
                    inputStream.close()
                }

                fetchImage(heifImageUrl, true)?.let { inputStream ->
                    heifImgBytes = convertInputStreamToBytes(inputStream)!!
                    log(
                        "myTag",
                        "download img costs ${SystemClock.uptimeMillis() - start}, $heifImageUrl"
                    )
                    inputStream.close()
                }

                if (webpImgBytes == null || heifImgBytes == null) {
                    throw IllegalStateException("image download failed.")
                }
                assert(webpImgBytes != null && heifImgBytes != null)
                log(
                    "myTag",
                    "webpImageSize: ${webpImgBytes!!.size}, heifImageSize: ${heifImgBytes!!.size}"
                )

                var webpTotalCost = 0L
                var heifTotalCost = 0L
                val sampleNumber = 1
                var beforeDecoding = 0L
                var webpDecodeEnd = 0L
                for (i in 0 until sampleNumber) {
                    beforeDecoding = SystemClock.uptimeMillis()
                    BitmapFactory.decodeByteArray(heifImgBytes, 0, heifImgBytes!!.size)
                    webpDecodeEnd = SystemClock.uptimeMillis()
                    heifTotalCost += (webpDecodeEnd - beforeDecoding)

                    beforeDecoding = SystemClock.uptimeMillis()
                    BitmapFactory.decodeByteArray(webpImgBytes, 0, webpImgBytes!!.size)
                    webpDecodeEnd = SystemClock.uptimeMillis()
                    webpTotalCost += (webpDecodeEnd - beforeDecoding)
                }

                log(
                    "myTag",
                    "average webp decoding cost: ${webpTotalCost / sampleNumber}, average heif decoding cost: ${heifTotalCost / sampleNumber}"
                )
            }
        }

    }
}