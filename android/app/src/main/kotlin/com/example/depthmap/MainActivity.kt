// android/app/src/main/kotlin/com/example/depthmap/MainActivity.kt

package com.example.depthmap

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.core.exceptions.UnavailableException
import java.io.File

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.depth"
    private var arSession: Session? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "initSession" -> initSession(result)
                    "captureImage" -> captureImage(result)
                    else -> result.notImplemented()
                }
            }
    }

    private fun initSession(result: MethodChannel.Result) {
        try {
            val session = Session(this)
            // Depth API 설정
            val config = session.config
            if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                config.depthMode = Config.DepthMode.AUTOMATIC
            }
            session.configure(config)
            arSession = session
            result.success(null)
        } catch (e: UnavailableException) {
            result.error("UNAVAILABLE", e.message, null)
        }
    }

    private fun captureImage(result: MethodChannel.Result) {
        try {
            val session = arSession ?: throw IllegalStateException("Session not initialized")
            session.resume()
            val frame = session.update()

            // 컬러 이미지 데이터 복사
            val cameraImage = frame.acquireCameraImage()
            val colorBuffer = cameraImage.planes[0].buffer
            val colorBytes = ByteArray(colorBuffer.remaining()).also { colorBuffer.get(it) }
            cameraImage.close()

            // Depth 이미지 (16-bit) 복사
            val depthPath: String? = try {
                frame.acquireDepthImage16Bits().use { depthImage ->
                    val depthBuffer = depthImage.planes[0].buffer
                    val depthBytes = ByteArray(depthBuffer.remaining()).also { depthBuffer.get(it) }
                    val dir = File(filesDir, "depth").apply { if (!exists()) mkdirs() }
                    val ts = System.currentTimeMillis()
                    val file = File(dir, "depth_${ts}.raw").apply { writeBytes(depthBytes) }
                    file.absolutePath
                }
            } catch (_: NotYetAvailableException) {
                null
            }

            session.pause()

            // 컬러 파일 저장
            val dir = File(filesDir, "depth").apply { if (!exists()) mkdirs() }
            val ts = System.currentTimeMillis()
            val colorFile = File(dir, "color_${ts}.raw").apply { writeBytes(colorBytes) }

            result.success(
                mapOf(
                    "colorPath" to colorFile.absolutePath,
                    "depthPath" to (depthPath ?: "unavailable")
                )
            )
        } catch (e: Exception) {
            result.error("CAPTURE_ERROR", e.localizedMessage, null)
        }
    }
}
