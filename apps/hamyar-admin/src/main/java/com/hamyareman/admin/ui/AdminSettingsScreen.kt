package com.hamyareman.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hamyareman.admin.AdminApi
import com.hamyareman.admin.LocalAdmin
import com.hamyareman.admin.adminIo
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import kotlinx.coroutines.launch

@Composable
fun AdminSettingsScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val container = LocalAdmin.current
    val prefs = container.prefs
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(prefs.name) }
    var endpoint by remember { mutableStateOf(prefs.endpoint) }
    var project by remember { mutableStateOf(prefs.projectId) }
    var database by remember { mutableStateOf(prefs.databaseId) }
    var key by remember { mutableStateOf(prefs.apiKey) }
    var bucket by remember { mutableStateOf(prefs.bucketId) }
    var info by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("اتصال سرور", onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("اگر روزی سرور عوض شد، همین‌جا نام، آدرس، شناسه پروژه، دیتابیس، کلید API و سطل فایل را بگذار. دانش‌آموز این‌ها را ندارد.")
            OutlinedTextField(name, { name = it }, label = { Text("نام پروژه") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(endpoint, { endpoint = it }, label = { Text("Endpoint") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(project, { project = it }, label = { Text("Project ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(database, { database = it }, label = { Text("Database ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(bucket, { bucket = it }, label = { Text("Storage Bucket ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(
                value = key,
                onValueChange = { key = it.trim() },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            info?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            OutlinedButton(
                onClick = {
                    busy = true; error = null; info = null
                    scope.launch {
                        val test = AdminApi(endpoint.trim().trimEnd('/'), project.trim(), key.trim(), database.trim(), bucket.trim())
                        when (val r = adminIo { test.ping() }) {
                            is AppResult.Ok -> info = r.value
                            is AppResult.Err -> error = r.error.userMessage
                        }
                        busy = false
                    }
                },
                enabled = !busy && endpoint.startsWith("http") && project.isNotBlank() && key.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "…" else "آزمایش اتصال") }
            Button(
                onClick = {
                    prefs.save(name, endpoint, project, database, key, bucket)
                    onSaved()
                },
                enabled = endpoint.startsWith("http") && project.isNotBlank() && database.isNotBlank() && key.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("ذخیره و اعمال") }
        }
    }
}
