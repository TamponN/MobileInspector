package com.bestplus.mobileinspector.ui

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.bestplus.mobileinspector.ui.login.LoginViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.navArgument
import com.bestplus.mobileinspector.ui.camera.CameraScreen
import com.bestplus.mobileinspector.ui.inspection.InspectionScreen
import com.bestplus.mobileinspector.ui.login.LoginScreen
import com.bestplus.mobileinspector.ui.login.QrScannerScreen
import com.bestplus.mobileinspector.ui.map.MapScreen
import com.bestplus.mobileinspector.ui.routes.RouteListScreen
import com.bestplus.mobileinspector.ui.routes.SubscriberListScreen
import com.bestplus.mobileinspector.ui.settings.SettingsScreen

/**
 * Навигация приложения.
 * Повторяет C# NavigationPage: LoginPage → MasterPage → PageLists / PageControler / SettingsPage
 */
object Routes {
    const val LOGIN = "login"
    const val ROUTE_LIST = "routes"
    const val SUBSCRIBER_LIST = "routes/{routeUuid}/subscribers"
    const val INSPECTION = "routes/{routeUuid}/subscribers/{subscriberUuid}"
    const val MAP = "routes/{routeUuid}/map"
    const val CAMERA = "camera/{routeUuid}/{subscriberUuid}/{deviceKey}/{scaleKey}/{testimonyKey}"
    const val SETTINGS = "settings"
    const val QR_SCANNER = "qr_scanner"
}

@Composable
fun InspectorNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {

        composable(Routes.LOGIN) { backStackEntry ->
            val viewModel: LoginViewModel = hiltViewModel()

            // backStackEntry.savedStateHandle — это тот же объект, в который
            // QR_SCANNER пишет данные через previousBackStackEntry.savedStateHandle.
            val qrAddress by backStackEntry.savedStateHandle
                .getStateFlow("qr_address", "")
                .collectAsStateWithLifecycle()

            LaunchedEffect(qrAddress) {
                if (qrAddress.isNotBlank()) {
                    val database = backStackEntry.savedStateHandle.get<String>("qr_database").orEmpty()
                    val ssl = backStackEntry.savedStateHandle.get<Boolean>("qr_ssl") ?: false
                    val uuid = backStackEntry.savedStateHandle.get<String>("qr_uuid").orEmpty()
                    viewModel.applyQrData(qrAddress, database, ssl, uuid)
                    backStackEntry.savedStateHandle.remove<String>("qr_address")
                }
            }

            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.ROUTE_LIST) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onScanQr = {
                    navController.navigate(Routes.QR_SCANNER)
                },
            )
        }

        composable(Routes.QR_SCANNER) {
            QrScannerScreen(
                onBack = { navController.popBackStack() },
                onScanned = { qrData ->
                    Log.d("Navigation", "onScanned called: $qrData")
                    val prev = navController.previousBackStackEntry
                    Log.d("Navigation", "previousBackStackEntry=${prev?.destination?.route}")
                    // Передаём данные обратно в LoginScreen через savedStateHandle
                    prev?.savedStateHandle?.set("qr_address", qrData.address)
                    prev?.savedStateHandle?.set("qr_database", qrData.database)
                    prev?.savedStateHandle?.set("qr_ssl", qrData.ssl)
                    prev?.savedStateHandle?.set("qr_uuid", qrData.uuid)
                    Log.d("Navigation", "savedStateHandle values set, calling popBackStack")
                    navController.popBackStack()
                },
            )
        }

        composable(Routes.ROUTE_LIST) {
            RouteListScreen(
                onRouteClick = { routeUuid ->
                    navController.navigate("routes/$routeUuid/subscribers")
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                },
            )
        }

        composable(
            route = Routes.SUBSCRIBER_LIST,
            arguments = listOf(navArgument("routeUuid") { type = NavType.StringType }),
        ) { backStackEntry ->
            val routeUuid = backStackEntry.arguments?.getString("routeUuid").orEmpty()
            SubscriberListScreen(
                routeUuid = routeUuid,
                onSubscriberClick = { subscriberUuid ->
                    navController.navigate("routes/$routeUuid/subscribers/$subscriberUuid")
                },
                onBack = { navController.popBackStack() },
                onMapClick = {
                    navController.navigate("routes/$routeUuid/map")
                },
            )
        }

        composable(
            route = Routes.INSPECTION,
            arguments = listOf(
                navArgument("routeUuid") { type = NavType.StringType },
                navArgument("subscriberUuid") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val routeUuid = backStackEntry.arguments?.getString("routeUuid").orEmpty()
            val subscriberUuid = backStackEntry.arguments?.getString("subscriberUuid").orEmpty()
            val actCheckOcrResult by backStackEntry.savedStateHandle
                .getStateFlow("actcheck_ocr", "")
                .collectAsStateWithLifecycle()
            InspectionScreen(
                routeUuid = routeUuid,
                subscriberUuid = subscriberUuid,
                onBack = { navController.popBackStack() },
                onCameraClick = { deviceKey, scaleKey, testimonyKey ->
                    navController.navigate(
                        "camera/$routeUuid/$subscriberUuid/$deviceKey/$scaleKey/$testimonyKey"
                    )
                },
                actCheckOcrResult = actCheckOcrResult,
                onActCheckOcrConsumed = {
                    backStackEntry.savedStateHandle.remove<String>("actcheck_ocr")
                },
            )
        }

        composable(
            route = Routes.MAP,
            arguments = listOf(navArgument("routeUuid") { type = NavType.StringType }),
        ) { backStackEntry ->
            val routeUuid = backStackEntry.arguments?.getString("routeUuid").orEmpty()
            MapScreen(
                onBack = { navController.popBackStack() },
                onSubscriberClick = { subscriberUuid ->
                    navController.navigate("routes/$routeUuid/subscribers/$subscriberUuid")
                },
            )
        }

        composable(
            route = Routes.CAMERA,
            arguments = listOf(
                navArgument("routeUuid") { type = NavType.StringType },
                navArgument("subscriberUuid") { type = NavType.StringType },
                navArgument("deviceKey") { type = NavType.StringType },
                navArgument("scaleKey") { type = NavType.StringType },
                navArgument("testimonyKey") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            CameraScreen(
                onBack = { navController.popBackStack() },
                onPhotoCapturedAndProcessed = { recognizedText ->
                    if (backStackEntry.arguments?.getString("scaleKey") == "actcheck") {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("actcheck_ocr", recognizedText)
                    }
                    navController.popBackStack()
                },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
