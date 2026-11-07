package com.example.ui.screens.business

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.ui.localization.AppLanguage
import com.example.ui.theme.DarkSlateSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VividEmeraldPrimary
import java.io.File
import java.io.FileOutputStream

object ReportExportHelper {

    data class PdfViewerData(
        val file: File,
        val bitmaps: List<Bitmap>,
        val fileName: String,
        val language: AppLanguage
    )

    fun getLastReportLanguage(context: Context, fallback: AppLanguage): AppLanguage {
        val sp = context.getSharedPreferences("report_prefs", Context.MODE_PRIVATE)
        val code = sp.getString("last_report_lang", fallback.code) ?: fallback.code
        return if (code == "bn") AppLanguage.BANGLA else AppLanguage.ENGLISH
    }

    fun saveLastReportLanguage(context: Context, language: AppLanguage) {
        val sp = context.getSharedPreferences("report_prefs", Context.MODE_PRIVATE)
        sp.edit().putString("last_report_lang", language.code).apply()
    }

    fun savePdfFileToDownloads(context: Context, file: File, fileName: String) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val dest = File(downloadsDir, fileName)
            file.inputStream().use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (_: Exception) {}
    }

    fun sharePdfFile(context: Context, file: File, language: AppLanguage) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val title = if (language == AppLanguage.BANGLA) "রিপোর্ট শেয়ার করুন" else "Share Report"
            context.startActivity(Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {}
    }

    @Composable
    fun ReportLanguageDialog(
        initialLanguage: AppLanguage,
        onConfirm: (AppLanguage, Boolean) -> Unit,
        onDismiss: () -> Unit
    ) {
        var selectedLang by remember { mutableStateOf(initialLanguage) }
        var asCsv by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = DarkSlateSurface,
            title = {
                Text(
                    text = "রিপোর্টের ভাষা ও ফরম্যাট (Report Options)",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedLang == AppLanguage.BANGLA,
                            onClick = { selectedLang = AppLanguage.BANGLA }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("বাংলা (Bengali)", color = TextPrimary)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedLang == AppLanguage.ENGLISH,
                            onClick = { selectedLang = AppLanguage.ENGLISH }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("English", color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = !asCsv,
                            onClick = { asCsv = false }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PDF Document", color = TextPrimary)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = asCsv,
                            onClick = { asCsv = true }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CSV (Excel Spreadsheet)", color = TextPrimary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onConfirm(selectedLang, asCsv) },
                    colors = ButtonDefaults.buttonColors(containerColor = VividEmeraldPrimary)
                ) {
                    Text("তৈরি করুন (Generate)")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("বাতিল (Cancel)", color = TextMuted)
                }
            }
        )
    }

    @Composable
    fun PdfViewerDialog(
        data: PdfViewerData,
        onDownload: () -> Unit,
        onShare: () -> Unit,
        onClose: () -> Unit
    ) {
        Dialog(
            onDismissRequest = onClose,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = DarkSlateSurface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top App Bar for viewer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                        }

                        Text(
                            text = data.fileName,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )

                        Row {
                            IconButton(onClick = onDownload, modifier = Modifier.testTag("btn_pdf_download")) {
                                Icon(Icons.Default.Download, contentDescription = "Download", tint = TextPrimary)
                            }
                            IconButton(onClick = onShare, modifier = Modifier.testTag("btn_pdf_share")) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = TextPrimary)
                            }
                        }
                    }

                    // Content list of bitmap pages
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(data.bitmaps) { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "PDF Page",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}
