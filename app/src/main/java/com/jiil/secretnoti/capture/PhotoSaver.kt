package com.jiil.secretnoti.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Log
import java.io.File

/**
 * 알림에 함께 온 사진 미리보기를 앱 안에 작게 저장한다.
 * 앱이 알림에 넣어 준 주소는 알림이 사라지면 못 읽을 수 있어서, 받자마자 복사한다.
 */
object PhotoSaver {
    private const val MAX_SIDE = 1280

    fun imageDir(context: Context) = File(context.filesDir, "images").apply { mkdirs() }

    fun save(context: Context, uri: String, conversationId: Long, messageId: Long): String? = try {
        val u = Uri.parse(uri)
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null
        } else {
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val bitmap = resolver.openInputStream(u)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }
            bitmap?.let { decoded ->
                val bmp = rotateByExif(context, u, decoded)
                // 투명 배경(이모티콘)은 PNG로, 사진은 JPEG로 저장한다.
                val png = bmp.hasAlpha()
                val file = File(imageDir(context), "$conversationId-$messageId.${if (png) "png" else "jpg"}")
                file.outputStream().use {
                    if (png) bmp.compress(Bitmap.CompressFormat.PNG, 100, it) else bmp.compress(Bitmap.CompressFormat.JPEG, 85, it)
                }
                bmp.recycle()
                if (bmp !== decoded) decoded.recycle()
                file.absolutePath
            }
        }
    } catch (t: Throwable) {
        // 앱이 읽기 권한을 주지 않은 경우 등. 사진 없이 글자만 남긴다.
        Log.i("PhotoSaver", "사진 미리보기를 읽지 못함: ${t.javaClass.simpleName}")
        null
    }

    /** 카메라 사진은 회전 정보(EXIF)만 붙어 있고 픽셀은 눕혀져 있을 수 있다. 저장 전에 바로 세운다. */
    private fun rotateByExif(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val degrees = runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        }.getOrDefault(0)
        if (degrees == 0) return bitmap
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }

}
