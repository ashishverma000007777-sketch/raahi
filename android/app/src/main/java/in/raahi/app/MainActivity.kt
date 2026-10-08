package `in`.raahi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import `in`.raahi.app.ui.components.RaahiTab
import `in`.raahi.app.ui.screens.aimechanic.AiMechanicScreen
import `in`.raahi.app.ui.screens.auth.OtpScreen
import `in`.raahi.app.ui.screens.auth.PhoneLoginScreen
import `in`.raahi.app.ui.screens.daily.DailyTipsScreen
import `in`.raahi.app.ui.screens.daily.HighwayAlertsScreen
import `in`.raahi.app.ui.screens.daily.NearbyPlacesScreen
import `in`.raahi.app.ui.screens.daily.StreakScreen
import `in`.raahi.app.ui.screens.commerce.FuelRatesScreen
import `in`.raahi.app.ui.screens.commerce.ShopScreen
import `in`.raahi.app.ui.screens.commerce.SubscriptionScreen
import `in`.raahi.app.ui.screens.helper.HelperApplicationScreen
import `in`.raahi.app.ui.screens.fuel.AddFuelScreen
import `in`.raahi.app.ui.screens.home.HomeScreen
import `in`.raahi.app.ui.screens.notifications.NotificationsScreen
import `in`.raahi.app.ui.screens.jobs.HelperDashboardScreen
import `in`.raahi.app.ui.screens.jobs.JobStatusScreen
import `in`.raahi.app.ui.screens.jobs.MyJobsScreen
import `in`.raahi.app.ui.screens.jobs.RequestHelpScreen
import `in`.raahi.app.ui.screens.mechanics.MechanicDetailScreen
import `in`.raahi.app.ui.screens.mechanics.MechanicsMapScreen
import `in`.raahi.app.ui.screens.profile.ProfileScreen
import `in`.raahi.app.ui.screens.profile.ProfileSetupScreen
import `in`.raahi.app.ui.screens.savings.PaisaBachaoScreen
import `in`.raahi.app.ui.screens.sos.EmergencyContactsScreen
import `in`.raahi.app.ui.screens.sos.SosScreen
import `in`.raahi.app.ui.screens.vehicle.CarReportCardScreen
import `in`.raahi.app.ui.screens.vehicle.ServiceHistoryScreen
import `in`.raahi.app.ui.screens.vehicle.VehicleSetupScreen
import `in`.raahi.app.ui.screens.trip.TripActiveScreen
import `in`.raahi.app.ui.screens.trip.TripHistoryScreen
import `in`.raahi.app.ui.screens.trip.TripOverviewScreen
import `in`.raahi.app.ui.screens.trip.TripPlanScreen
import `in`.raahi.app.ui.screens.trip.TripSummaryScreen
import `in`.raahi.app.ui.screens.splash.RaahiSplashScreen
import `in`.raahi.app.ui.theme.RaahiTheme
import androidx.lifecycle.ViewModel
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.TokenManager
import javax.inject.Inject

private object Routes {
    const val PHONE_LOGIN = "phone_login"
    const val OTP = "otp"
    const val PROFILE_SETUP = "profile_setup"
    const val PROFILE_EDIT = "profile_edit"
    const val PROFILE = "profile"
    const val VEHICLE_SETUP = "vehicle_setup"
    const val VEHICLE_EDIT = "vehicle_edit"
    const val SERVICE_HISTORY = "service_history"
    const val HOME = "home"
    const val REQUEST_HELP = "request_help"
    const val JOB_STATUS = "job_status/{jobId}"
    const val MY_JOBS = "my_jobs"
    const val HELPER_DASHBOARD = "helper_dashboard"
    const val MECHANICS_MAP = "mechanics_map"
    const val MECHANIC_DETAIL = "mechanic_detail/{userId}"
    const val SOS = "sos"
    const val EMERGENCY_CONTACTS = "emergency_contacts"
    const val HELPER_APPLICATION = "helper_application"
    const val AI_MECHANIC = "ai_mechanic"
    const val STREAK = "streak"
    const val HIGHWAY_ALERTS = "highway_alerts"
    const val NEARBY_PLACES = "nearby_places"
    const val DAILY_TIPS = "daily_tips"
    const val FUEL_RATES = "fuel_rates"
    const val ADD_FUEL = "add_fuel"
    const val NOTIFICATIONS = "notifications"
    const val SHOP = "shop"
    const val SUBSCRIPTION = "subscription"
    const val TRIP_PLAN = "trip_plan"
    const val TRIP_OVERVIEW = "trip_overview/{tripId}"
    const val TRIP_ACTIVE = "trip_active/{tripId}"
    const val TRIP_SUMMARY = "trip_summary/{tripId}"
    const val TRIP_HISTORY = "trip_history"
    const val CAR_REPORT_CARD = "car_report_card"
    const val PAISA_BACHAO = "paisa_bachao"

    private fun safeId(id: String): String = android.net.Uri.encode(id.ifBlank { "unknown" })

    fun jobStatus(jobId: String) = "job_status/${safeId(jobId)}"
    fun mechanicDetail(userId: String) = "mechanic_detail/${safeId(userId)}"
    fun tripOverview(tripId: String) = "trip_overview/${safeId(tripId)}"
    fun tripActive(tripId: String) = "trip_active/${safeId(tripId)}"
    fun tripSummary(tripId: String) = "trip_summary/${safeId(tripId)}"

    fun forTab(tab: RaahiTab): String = when (tab) {
        RaahiTab.HOME -> HOME
        RaahiTab.MECHANICS -> MECHANICS_MAP
        RaahiTab.AI_MECHANIC -> AI_MECHANIC
        RaahiTab.SHOP -> SHOP
        RaahiTab.PROFILE -> PROFILE
    }
}

@HiltViewModel
class AuthGateViewModel @Inject constructor(
    private val tokens: TokenManager,
    private val authRepository: `in`.raahi.app.data.AuthRepository,
) : ViewModel() {

    fun isAuthenticated(): Boolean = !tokens.getCachedToken().isNullOrBlank()

    suspend fun ensureDebugAuth(): Boolean = isAuthenticated()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The manifest theme (Theme.Raahi.Splash) exists only to paint the splash's near-black
        // starting window; switch back to the regular app theme before the window is created so
        // everything after the splash looks exactly as before.
        setTheme(R.style.Theme_Raahi)
        super.onCreate(savedInstanceState)
        setContent {
            RaahiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RaahiRoot()
                }
            }
        }
    }
}

/**
 * Launch splash, then the existing app. The splash only owns the visual startup: when it
 * finishes, control is handed to [RaahiNavHost] untouched, so the start destination and the
 * auth / new-user / Home routing behave exactly as before. The NavHost (and its ViewModels)
 * is composed once, after the splash. `rememberSaveable` keeps rotation / process restore
 * from replaying the splash.
 */
@Composable
private fun RaahiRoot() {
    var splashDone by rememberSaveable { mutableStateOf(false) }
    Crossfade(targetState = splashDone, animationSpec = tween(200), label = "splash") { done ->
        if (done) RaahiNavHost() else RaahiSplashScreen(onFinished = { splashDone = true })
    }
}

/**
 * Phase 6A (Home) + 6B (Jobs) + 6C (Mechanics/Map) + 6D (SOS + WebSocket) all wired here.
 * The "UI migration" pass (design spec v2) added Routes.PROFILE — a real screen backed by
 * ProfileViewModel — plus the standard bottom-nav popUpTo/launchSingleTop/restoreState
 * pattern in [navigateToTab], shared by every screen that now renders RaahiBottomNavBar.
 *
 * Job IDs and mechanic user IDs are UUID strings with no '/' or reserved route characters,
 * so passing them as plain path segments is safe — unlike the phone/verificationId values in
 * the auth flow below, which go through SavedStateHandle instead for that reason.
 */
@Composable
fun RaahiNavHost(
    navController: NavHostController = rememberNavController(),
    gate: AuthGateViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val navigateToTab: (RaahiTab) -> Unit = { tab ->
        navController.navigate(Routes.forTab(tab)) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Splash -> real auth state -> Home or Phone Login.
    var authReady by remember { mutableStateOf(false) }
    var authenticated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authenticated = gate.isAuthenticated()
        authReady = true
    }

    if (!authReady) {
        return
    }

    val startDestination = if (authenticated) Routes.HOME else Routes.PHONE_LOGIN

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.PHONE_LOGIN) {
            PhoneLoginScreen(
                onOtpSent = { verificationId, phone ->
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("verificationId", verificationId)
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("phone", phone)
                    navController.navigate(Routes.OTP)
                },
                onSignedIn = { isNewUser -> navigateAfterSignIn(navController, isNewUser) }
            )
        }

        composable(Routes.OTP) {
            val verificationId = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<String>("verificationId").orEmpty()
            val phone = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<String>("phone").orEmpty()

            OtpScreen(
                verificationId = verificationId,
                phone = phone,
                onSignedIn = { isNewUser -> navigateAfterSignIn(navController, isNewUser) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PROFILE_SETUP) {
            ProfileSetupScreen(
                onDone = {
                    // New-user flow: name/account profile, then straight into "Set up your
                    // car" per the approved Car Health spec ("after authentication, if the
                    // user's vehicle profile is incomplete, show a proper 'Set up your car'
                    // onboarding step"). Not gated on every returning-user login too — that
                    // would mean restructuring the sign-in redirect this app already relies
                    // on; instead, returning users who skip it see a "Set up your car" CTA on
                    // Home and on Profile's Car Health tab whenever they open either.
                    navController.navigate(Routes.VEHICLE_SETUP) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(Routes.PROFILE_EDIT) {
            ProfileSetupScreen(
                isEditing = true,
                onDone = { navController.popBackStack() },
            )
        }

        composable(Routes.VEHICLE_SETUP) {
            VehicleSetupScreen(
                onDone = {
                    navController.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
                },
            )
        }

        composable(Routes.VEHICLE_EDIT) {
            VehicleSetupScreen(
                isEditing = true,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }

        composable(Routes.SERVICE_HISTORY) {
            ServiceHistoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.HOME) {
            HomeScreen(
                onRequestHelp = { navController.navigate(Routes.REQUEST_HELP) },
                onNearbyMechanics = { navController.navigate(Routes.MECHANICS_MAP) },
                onMyJobs = { navController.navigate(Routes.MY_JOBS) },
                onSos = { navController.navigate(Routes.SOS) },
                onOpenActiveJob = { job -> navController.navigate(Routes.jobStatus(job.id)) },
                onHelperDashboard = { navController.navigate(Routes.HELPER_DASHBOARD) },
                onBecomeHelper = { navController.navigate(Routes.HELPER_APPLICATION) },
                onAiMechanic = { navController.navigate(Routes.AI_MECHANIC) },
                onCarHealth = { navController.navigate(Routes.CAR_REPORT_CARD) },
                onSetupVehicle = { navController.navigate(Routes.VEHICLE_SETUP) },
                onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onAddFuel = { navController.navigate(Routes.ADD_FUEL) },
                onPlanTrip = { navController.navigate(Routes.TRIP_PLAN) },
                onOpenActiveTrip = { tripId -> navController.navigate(Routes.tripActive(tripId)) },
                onTripHistory = { navController.navigate(Routes.TRIP_HISTORY) },
                onOpenDaily = { key ->
                    val route = when (key) {
                        "trips" -> Routes.TRIP_HISTORY
                        "paisa" -> Routes.PAISA_BACHAO
                        "savings" -> Routes.PAISA_BACHAO
                        "report" -> Routes.CAR_REPORT_CARD
                        "streak" -> Routes.STREAK
                        "alerts" -> Routes.HIGHWAY_ALERTS
                        "places" -> Routes.NEARBY_PLACES
                        "fuel" -> Routes.FUEL_RATES
                        "shop" -> Routes.SHOP
                        "plans" -> Routes.SUBSCRIPTION
                        else -> Routes.DAILY_TIPS
                    }
                    navController.navigate(route)
                },
                onNavigateTab = navigateToTab,
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onEditProfile = { navController.navigate(Routes.PROFILE_EDIT) },
                onSignedOut = {
                    navController.navigate(Routes.PHONE_LOGIN) { popUpTo(0) { inclusive = true } }
                },
                onSetupVehicle = { navController.navigate(Routes.VEHICLE_SETUP) },
                onEditVehicle = { navController.navigate(Routes.VEHICLE_EDIT) },
                onServiceHistory = { navController.navigate(Routes.SERVICE_HISTORY) },
                onNavigateTab = navigateToTab,
                onHelper = { navController.navigate(Routes.HELPER_DASHBOARD) },
            )
        }

        composable(Routes.HELPER_APPLICATION) {
            HelperApplicationScreen(
                onBack = { navController.popBackStack() },
                onOpenDashboard = {
                    navController.navigate(Routes.HELPER_DASHBOARD) { popUpTo(Routes.HELPER_APPLICATION) { inclusive = true } }
                },
            )
        }

        composable(Routes.AI_MECHANIC) {
            AiMechanicScreen(onBack = { navController.popBackStack() }, onNavigateTab = navigateToTab)
        }

        composable(Routes.STREAK) { StreakScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.HIGHWAY_ALERTS) { HighwayAlertsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.NEARBY_PLACES) { NearbyPlacesScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.DAILY_TIPS) { DailyTipsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.ADD_FUEL) { AddFuelScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.NOTIFICATIONS) { NotificationsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.FUEL_RATES) { FuelRatesScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SHOP) { ShopScreen(onBack = { navController.popBackStack() }, onNavigateTab = navigateToTab) }
        composable(Routes.SUBSCRIPTION) { SubscriptionScreen(onBack = { navController.popBackStack() }) }

        composable(Routes.REQUEST_HELP) {
            RequestHelpScreen(
                onBack = { navController.popBackStack() },
                onCreated = { jobId ->
                    // Replace Request Help with the status screen so back from there goes to
                    // Home, not back into the form for a request that's already been created.
                    navController.navigate(Routes.jobStatus(jobId)) {
                        popUpTo(Routes.REQUEST_HELP) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.JOB_STATUS,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType }),
        ) {
            JobStatusScreen(
                onBack = { navController.popBackStack() },
                onDone = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                },
            )
        }

        composable(Routes.MY_JOBS) {
            MyJobsScreen(
                onBack = { navController.popBackStack() },
                onOpenJob = { jobId -> navController.navigate(Routes.jobStatus(jobId)) },
            )
        }

        composable(Routes.HELPER_DASHBOARD) {
            HelperDashboardScreen(
                onBack = { navController.popBackStack() },
                onJobAccepted = { jobId ->
                    navController.navigate(Routes.jobStatus(jobId)) {
                        popUpTo(Routes.HELPER_DASHBOARD) { inclusive = true }
                    }
                },
                onOpenJob = { jobId -> navController.navigate(Routes.jobStatus(jobId)) },
                onApplyOrReview = { navController.navigate(Routes.HELPER_APPLICATION) },
            )
        }

        composable(Routes.MECHANICS_MAP) {
            MechanicsMapScreen(
                onBack = { navController.popBackStack() },
                onMechanicClick = { userId -> navController.navigate(Routes.mechanicDetail(userId)) },
                onNavigateTab = navigateToTab,
            )
        }

        composable(
            route = Routes.MECHANIC_DETAIL,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
        ) {
            MechanicDetailScreen(
                onBack = { navController.popBackStack() },
                onRequestHelp = { navController.navigate(Routes.REQUEST_HELP) },
            )
        }

        composable(Routes.SOS) {
            SosScreen(
                onBack = { navController.popBackStack() },
                onEmergencyContacts = { navController.navigate(Routes.EMERGENCY_CONTACTS) },
            )
        }

        composable(Routes.EMERGENCY_CONTACTS) {
            EmergencyContactsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.TRIP_PLAN) {
            TripPlanScreen(
                onBack = { navController.popBackStack() },
                onTripCreated = { tripId ->
                    navController.navigate(Routes.tripOverview(tripId)) {
                        popUpTo(Routes.TRIP_PLAN) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.TRIP_OVERVIEW,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
        ) {
            TripOverviewScreen(
                onBack = { navController.popBackStack() },
                onStartTrip = { tripId ->
                    navController.navigate(Routes.tripActive(tripId)) {
                        popUpTo(Routes.TRIP_OVERVIEW) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.TRIP_ACTIVE,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
        ) {
            TripActiveScreen(
                onBack = { navController.popBackStack() },
                onTripCompleted = { tripId ->
                    navController.navigate(Routes.tripSummary(tripId)) {
                        popUpTo(Routes.TRIP_ACTIVE) { inclusive = true }
                    }
                },
                onSos = { navController.navigate(Routes.SOS) },
                onNearbyMechanics = { navController.navigate(Routes.MECHANICS_MAP) }
            )
        }

        composable(
            route = Routes.TRIP_SUMMARY,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
        ) {
            TripSummaryScreen(
                onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.TRIP_HISTORY) {
            TripHistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenTrip = { trip ->
                    if (trip.status == "COMPLETED") {
                        navController.navigate(Routes.tripSummary(trip.id))
                    } else if (trip.status == "IN_PROGRESS") {
                        navController.navigate(Routes.tripActive(trip.id))
                    } else {
                        navController.navigate(Routes.tripOverview(trip.id))
                    }
                },
                onPlanTrip = { navController.navigate(Routes.TRIP_PLAN) }
            )
        }

        composable(Routes.CAR_REPORT_CARD) {
            CarReportCardScreen(
                onBack = { navController.popBackStack() },
                onSetupVehicle = { navController.navigate(Routes.VEHICLE_SETUP) },
                onEditVehicle = { navController.navigate(Routes.VEHICLE_EDIT) },
                onServiceHistory = { navController.navigate(Routes.SERVICE_HISTORY) },
                onAddFuel = { navController.navigate(Routes.ADD_FUEL) },
                onPlanTrip = { navController.navigate(Routes.TRIP_PLAN) },
            )
        }

        composable(Routes.PAISA_BACHAO) {
            PaisaBachaoScreen(
                onBack = { navController.popBackStack() },
                onAddFuel = { navController.navigate(Routes.ADD_FUEL) },
                onFuelRates = { navController.navigate(Routes.FUEL_RATES) },
                onServiceHistory = { navController.navigate(Routes.SERVICE_HISTORY) },
                onPlanTrip = { navController.navigate(Routes.TRIP_PLAN) },
            )
        }
    }
}

private fun navigateAfterSignIn(navController: NavHostController, isNewUser: Boolean) {
    // Auth (and, for new users, profile setup) screens are popped from the back stack so the
    // hardware/gesture back button can't return a signed-in user to phone entry — same intent
    // as the Flutter GoRouter redirect logic (auth screens are dead ends once signed in).
    val destination = if (isNewUser) Routes.PROFILE_SETUP else Routes.HOME
    navController.navigate(destination) {
        popUpTo(0) { inclusive = true }
    }
}
