package com.jiil.secretnoti.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import com.jiil.secretnoti.NotiApp
import com.jiil.secretnoti.R
import com.jiil.secretnoti.capture.AppLabels
import com.jiil.secretnoti.capture.TargetApps
import com.jiil.secretnoti.settings.Prefs
import com.jiil.secretnoti.ui.model.ContactKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 문의 메일. 시크릿노티에는 인터넷 권한이 없으므로 폰의 메일 앱을 내용이 채워진 채로 열어 줄 뿐이고,
 * 보내기는 사용자가 메일 앱에서 직접 누른다.
 */
object SupportMail {
    const val ADDRESS = "jiil@kakao.com"

    suspend fun compose(context: Context, kind: ContactKind, attachDump: Boolean) {
        val attachment = if (attachDump) runCatching { writeDumpFile(context) }.getOrNull() else null
        val body = context.getString(kind.bodyRes) + "\n\n" + deviceInfo(context)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = if (attachment != null) "application/json" else "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(ADDRESS))
            putExtra(Intent.EXTRA_SUBJECT, context.getString(kind.subjectRes))
            putExtra(Intent.EXTRA_TEXT, body)
            if (attachment != null) {
                putExtra(Intent.EXTRA_STREAM, attachment)
                clipData = ClipData.newRawUri(null, attachment)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            // 메일 앱만 고르게 한다 (메신저·메모 앱으로 새지 않게).
            selector = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        }
        try {
            context.startActivity(send)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.contact_no_mail_app, ADDRESS), Toast.LENGTH_LONG).show()
        }
    }

    private fun deviceInfo(context: Context): String {
        val info = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
        @Suppress("DEPRECATION")
        val version = info?.let { "${it.versionName} (${it.versionCode})" } ?: "?"
        val apps = Prefs.enabledApps.value.map {
            if (it == TargetApps.SMS_TOKEN) context.getString(R.string.source_sms) else AppLabels.label(context, it)
        }.sorted().joinToString(", ")
        return context.getString(R.string.contact_body_info, version, Build.MODEL, Build.VERSION.RELEASE, apps)
    }

    /** 진단 기록 전체(최근 3일)를 캐시에 JSON으로 써서 메일 앱이 읽을 수 있는 주소로 돌려준다. */
    private suspend fun writeDumpFile(context: Context): Uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "reports").apply { deleteRecursively(); mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(dir, "noti-dumps-$stamp.json")
        val array = JSONArray()
        NotiApp.instance.db.dumpDao().all().forEach { array.put(JSONObject(it.json)) }
        file.writeText(array.toString(2))
        FileProvider.getUriForFile(context, context.packageName + ".files", file)
    }
}
