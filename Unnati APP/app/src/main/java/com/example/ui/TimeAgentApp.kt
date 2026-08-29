package com.example.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.TimeAgentTopBar
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LandingScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProjectSelectorSheet
import com.example.ui.screens.SuccessScreen
import com.example.ui.screens.UpdatesScreen
import com.example.ui.screens.WorkerSelectorSheet
import com.example.ui.theme.UxBorder
import com.example.ui.theme.UxOrange
import com.example.ui.theme.UxOrangeDark
import com.example.ui.theme.UxOrangeLight
import com.example.ui.theme.UxPrimaryText
import com.example.ui.theme.UxSecondaryText
import com.example.ui.theme.UxSurfaceBright
import com.example.ui.theme.UxTextMuted
import com.example.ui.theme.UxWhite
import com.example.viewmodel.AppTab
import com.example.viewmodel.TimeAgentViewModel
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    data object Landing : Screen("landing")
    data object Login : Screen("login")
    data object Home : Screen("home")
    data object Success : Screen("success")
}

@Composable
fun TimeAgentApp(
    viewModel: TimeAgentViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Landing.route
    val currentTab by viewModel.currentTab.collectAsState()
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val selectedWorker by viewModel.selectedWorker.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val workerStats by viewModel.workerStats.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showProjectSheet by remember { mutableStateOf(false) }
    var showWorkerSheet by remember { mutableStateOf(false) }

    val isAuthRoute = currentRoute == Screen.Landing.route || currentRoute == Screen.Login.route

    // Synchronize authentication changes with top-level navigation
    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated && !isAuthRoute) {
            val target = if (!viewModel.isOnboardingCompleted.value) Screen.Landing.route else Screen.Login.route
            navController.navigate(target) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isAuthRoute,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(310.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 22.dp)
                ) {
                    // 1. Sleek, Minimal Operator Profile Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFF1EB))
                                .border(1.dp, Color(0xFFFFD8C2), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Engineering,
                                contentDescription = null,
                                tint = UxOrangeDark,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedWorker.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = UxPrimaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${selectedWorker.role} • ${selectedProject.name}",
                                fontSize = 12.sp,
                                color = UxSecondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Primary Navigation Links (ViewModel State-Hoisted)
                    Text(
                        text = "NAVIGATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = UxTextMuted,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    DrawerNavLink(
                        icon = Icons.Default.Mic,
                        label = "Voice Agent",
                        isSelected = currentTab == AppTab.HOME && currentRoute == Screen.Home.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.selectTab(AppTab.HOME)
                            if (currentRoute == Screen.Success.route) {
                                navController.popBackStack(Screen.Home.route, false)
                            }
                        }
                    )

                    DrawerNavLink(
                        icon = Icons.Default.HistoryEdu,
                        label = "Work Logs",
                        badge = if (workerStats.pendingUpdates > 0) "${workerStats.pendingUpdates}" else null,
                        isSelected = currentTab == AppTab.UPDATES && currentRoute == Screen.Home.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.selectTab(AppTab.UPDATES)
                            if (currentRoute == Screen.Success.route) {
                                navController.popBackStack(Screen.Home.route, false)
                            }
                        }
                    )

                    DrawerNavLink(
                        icon = Icons.Default.Person,
                        label = "Profile",
                        isSelected = currentTab == AppTab.PROFILE && currentRoute == Screen.Home.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.selectTab(AppTab.PROFILE)
                            if (currentRoute == Screen.Success.route) {
                                navController.popBackStack(Screen.Home.route, false)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Quick Preferences & Switchers
                    Text(
                        text = "PREFERENCES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = UxTextMuted,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    DrawerActionLink(
                        icon = Icons.Default.Business,
                        label = "Project Site",
                        value = selectedProject.name,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showProjectSheet = true
                        }
                    )

                    DrawerActionLink(
                        icon = Icons.Default.Engineering,
                        label = "Worker Profile",
                        value = selectedWorker.name,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showWorkerSheet = true
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. Clean Sign Out & Version Info
                    DrawerNavLink(
                        icon = Icons.Default.Logout,
                        label = "Sign Out",
                        isSelected = false,
                        isDestructive = true,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.logout()
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Time Agent v2.4 • UX4G Open Data",
                        fontSize = 11.sp,
                        color = UxTextMuted,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                if (!isAuthRoute) {
                    TimeAgentBottomNavigation(
                        currentTab = currentTab,
                        isSuccessScreen = currentRoute == Screen.Success.route,
                        onSelectTab = { tab ->
                            viewModel.selectTab(tab)
                            if (currentRoute == Screen.Success.route) {
                                navController.popBackStack(Screen.Home.route, false)
                            }
                        }
                    )
                }
            },
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
            containerColor = if (isAuthRoute) UxWhite else UxSurfaceBright,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            val initialStartDestination = remember {
                if (!viewModel.isOnboardingCompleted.value) {
                    Screen.Landing.route
                } else if (!viewModel.isAuthenticated.value) {
                    Screen.Login.route
                } else {
                    Screen.Home.route
                }
            }

            NavHost(
                navController = navController,
                startDestination = initialStartDestination,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        if (isAuthRoute) androidx.compose.foundation.layout.PaddingValues(0.dp)
                        else androidx.compose.foundation.layout.PaddingValues(bottom = innerPadding.calculateBottomPadding())
                    )
            ) {
                composable(Screen.Landing.route) {
                    LandingScreen(
                        onGetStartedClick = {
                            viewModel.completeOnboarding()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Landing.route) { inclusive = true }
                            }
                        },
                        onDirectLoginClick = {
                            viewModel.completeOnboarding()
                            viewModel.login(
                                workerId = "WK-10245",
                                pin = "4892",
                                onSuccess = {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Landing.route) { inclusive = true }
                                    }
                                },
                                onError = { /* no-op */ }
                            )
                        }
                    )
                }

                composable(Screen.Login.route) {
                    LoginScreen(
                        viewModel = viewModel,
                        onLoginSuccess = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        },
                        onBackToLanding = {
                            navController.navigate(Screen.Landing.route)
                        }
                    )
                }

                composable(Screen.Home.route) {
                    when (currentTab) {
                        AppTab.HOME -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onOpenProjectSelector = { showProjectSheet = true },
                                onOpenWorkerSelector = { showWorkerSheet = true },
                                onNavigateToSuccess = {
                                    navController.navigate(Screen.Success.route) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        AppTab.UPDATES -> {
                            UpdatesScreen(
                                viewModel = viewModel,
                                onOpenDrawer = { scope.launch { drawerState.open() } }
                            )
                        }
                        AppTab.PROFILE -> {
                            ProfileScreen(
                                viewModel = viewModel,
                                onOpenWorkerSelector = { showWorkerSheet = true },
                                onOpenProjectSelector = { showProjectSheet = true },
                                onOpenDrawer = { scope.launch { drawerState.open() } }
                            )
                        }
                    }
                }

                composable(Screen.Success.route) {
                    SuccessScreen(
                        viewModel = viewModel,
                        onViewMyUpdatesClick = {
                            viewModel.selectTab(AppTab.UPDATES)
                            navController.popBackStack(Screen.Home.route, false)
                        },
                        onRecordAnotherClick = {
                            viewModel.resetRecordingState()
                            viewModel.selectTab(AppTab.HOME)
                            navController.popBackStack(Screen.Home.route, false)
                        }
                    )
                }
            }

            if (showProjectSheet) {
                ProjectSelectorSheet(
                    viewModel = viewModel,
                    onDismiss = { showProjectSheet = false }
                )
            }

            if (showWorkerSheet) {
                WorkerSelectorSheet(
                    viewModel = viewModel,
                    onDismiss = { showWorkerSheet = false }
                )
            }
        }
    }
}

@Composable
fun TimeAgentBottomNavigation(
    currentTab: AppTab,
    isSuccessScreen: Boolean = false,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple(AppTab.HOME, Icons.Filled.Home, Icons.Outlined.Home),
        Triple(AppTab.UPDATES, Icons.Filled.HistoryEdu, Icons.Outlined.HistoryEdu),
        Triple(AppTab.PROFILE, Icons.Filled.Person, Icons.Outlined.Person)
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 10.dp, ambientColor = Color(0x0A000000), spotColor = Color(0x18000000)),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(66.dp)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (tab, activeIcon, inactiveIcon) ->
                val isActive = (currentTab == tab && !isSuccessScreen) || (tab == AppTab.HOME && isSuccessScreen)
                val iconTint = if (isActive) UxOrangeDark else Color(0xFF64748B)
                val textColor = if (isActive) UxOrangeDark else Color(0xFF64748B)
                val labelFontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelectTab(tab) }
                        )
                        .testTag("nav_item_${tab.route}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // Pill indicator around active icon
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isActive) Color(0xFFFFF1EB) else Color.Transparent)
                                .padding(horizontal = 18.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isActive) activeIcon else inactiveIcon,
                                contentDescription = tab.title,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Text(
                            text = tab.title,
                            fontSize = 11.5.sp,
                            fontWeight = labelFontWeight,
                            color = textColor,
                            letterSpacing = 0.1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerNavLink(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    badge: String? = null,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) Color(0xFFFFF1EB) else Color.Transparent
    val contentColor = when {
        isDestructive -> Color(0xFFDC2626)
        isSelected -> UxOrangeDark
        else -> UxPrimaryText
    }
    val iconColor = when {
        isDestructive -> Color(0xFFDC2626)
        isSelected -> UxOrange
        else -> UxSecondaryText
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = if (isDestructive) Color(0xFFDC2626) else UxOrange),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )

            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(UxOrange)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerActionLink(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = UxOrange),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = UxSecondaryText,
                modifier = Modifier.size(20.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = UxPrimaryText
                )
                Text(
                    text = value,
                    fontSize = 11.5.sp,
                    color = UxSecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = UxTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
