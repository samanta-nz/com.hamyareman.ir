package com.hamyareman.admin

import android.content.Context
import com.hamyareman.ir.platform.core.appwrite.AppwriteAuthService
import com.hamyareman.ir.platform.core.appwrite.AppwriteClientProvider
import com.hamyareman.ir.platform.core.appwrite.AppwriteFunctionsService
import com.hamyareman.ir.platform.core.appwrite.AppwriteStorageService
import com.hamyareman.ir.platform.core.appwrite.BillingGateway
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.UserRole

class AdminContainer(context: Context) {
    val store = LocalStore(context.applicationContext, "hamyar_admin")
    val prefs = AdminPrefs(store)

    val appwrite = AppwriteClientProvider(
        context = context,
        endpoint = prefs.endpoint,
        projectId = prefs.projectId,
        databaseId = prefs.databaseId,
    )
    val functions = AppwriteFunctionsService(appwrite)
    val storage = AppwriteStorageService(appwrite)
    val billing = BillingGateway(functions)
    val api = AdminApi(
        endpoint = prefs.endpoint,
        projectId = prefs.projectId,
        apiKey = prefs.apiKey,
        databaseId = prefs.databaseId,
        bucketId = prefs.bucketId,
    )
    val auth = AppwriteAuthService(
        provider = appwrite,
        fallbackRole = UserRole.ZAHRA,
        store = store,
        functions = functions,
        gateContext = { null },
    )
}
