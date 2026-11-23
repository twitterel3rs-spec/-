package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberCoral
import com.example.ui.theme.SeaCyanAccent
import com.example.ui.theme.SeaNavyDeep
import com.example.ui.theme.SeaTealLight
import com.example.ui.theme.SeaTealPrimary
import com.example.ui.theme.StatusSuccess
import com.example.util.ApkInfo
import com.example.util.ApkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ApkDownloadDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val apkInfo: ApkInfo = remember(context) {
        ApkManager.getInstalledApkInfo(context)
    }

    var isExporting by remember { mutableStateOf(false) }
    var exportStatusMessage by remember { mutableStateOf<String?>(null) }
    var isExportSuccess by remember { mutableStateOf<Boolean?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SeaTealPrimary)
            ) {
                Text("إغلاق", color = Color.White)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SeaTealLight.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.InstallMobile,
                        contentDescription = null,
                        tint = SeaTealPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "ملف تطبيق أندرويد الفعلي (APK)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = SeaTealPrimary
                    )
                    Text(
                        text = "فحص حالة البناء وتصدير حزمة APK الأصلية",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .mouseVerticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Actual APK Verification & Build Status
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (apkInfo.exists) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (apkInfo.exists) StatusSuccess.copy(alpha = 0.4f) else Color.Red.copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (apkInfo.exists) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (apkInfo.exists) StatusSuccess else Color.Red,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (apkInfo.exists) "تم التحقق: حزمة APK موجودة وصالحة للتثبيت" else "تنبيه: تعذر قراءة حزمة الـ APK",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (apkInfo.exists) StatusSuccess else Color.Red
                            )
                        }

                        Text(
                            text = "• إصدار التطبيق: v${apkInfo.versionName} (كود البناء: ${apkInfo.versionCode})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "• الحجم الفعلي للملف: ${apkInfo.fileSizeFormatted}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "• معرّف الحزمة: ${apkInfo.packageName}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "• مسار البناء الفعلي في المشروع: app/build/outputs/apk/debug/app-debug.apk",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.DarkGray
                        )
                    }
                }

                // Section 2: Real Action - Export Genuine APK to Downloads
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📁 تصدير ملف الـ APK الأصلي للجهاز:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SeaTealPrimary
                        )

                        Text(
                            text = "يتم استخراج ملف الـ APK الفعلي المثبت وتخزينه كملف تنفيذي حقيقي (.apk) في مجلد التنزيلات (Downloads) بجهازك ليتاح نقله وتثبيته مباشرة.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = Color.Gray
                        )

                        Button(
                            onClick = {
                                if (!isExporting) {
                                    isExporting = true
                                    exportStatusMessage = null
                                    coroutineScope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            ApkManager.exportApkToDownloads(context)
                                        }
                                        isExporting = false
                                        result.fold(
                                            onSuccess = { msg ->
                                                isExportSuccess = true
                                                exportStatusMessage = msg
                                                Toast.makeText(context, "تم حفظ ملف الـ APK بنجاح في التنزيلات", Toast.LENGTH_LONG).show()
                                            },
                                            onFailure = { err ->
                                                isExportSuccess = false
                                                exportStatusMessage = "فشل التصدير: ${err.message}"
                                                Toast.makeText(context, "فشل حفظ الملف: ${err.message}", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                }
                            },
                            enabled = !isExporting && apkInfo.exists,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري استخراج وحفظ ملف APK...", fontSize = 12.sp)
                            } else {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("حفظ ملف APK في مجلد التنزيلات (Downloads)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        // Share Real Binary APK
                        OutlinedButton(
                            onClick = {
                                val result = ApkManager.shareRealApkFile(context)
                                result.fold(
                                    onSuccess = { intent ->
                                        context.startActivity(intent)
                                    },
                                    onFailure = { err ->
                                        Toast.makeText(context, "تعذر مشاركة الملف: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            enabled = apkInfo.exists,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة ملف الـ APK الفعلي (بلوتوث / بريد / واتساب)", fontSize = 11.sp)
                        }

                        // Feedback on export
                        exportStatusMessage?.let { msg ->
                            Surface(
                                color = if (isExportSuccess == true) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    color = if (isExportSuccess == true) Color(0xFF065F46) else Color(0xFF991B1B),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(8.dp),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }


            }
        }
    )
}
