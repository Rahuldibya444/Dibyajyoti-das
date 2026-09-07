package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PartyDetailScreen
import com.example.ui.theme.OutlineDark
import com.example.ui.theme.PartyCalculatorTheme
import com.example.ui.viewmodel.DarkModeSetting
import com.example.ui.viewmodel.PartyViewModel
import com.example.util.NotificationHelper

class MainActivity : ComponentActivity() {

    private val viewModel: PartyViewModel by viewModels {
        PartyViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(applicationContext)

        setContent {
            val darkModeSetting by viewModel.darkModeSetting.collectAsStateWithLifecycle()
            val isDarkTheme = when (darkModeSetting) {
                DarkModeSetting.SYSTEM -> isSystemInDarkTheme()
                DarkModeSetting.LIGHT -> false
                DarkModeSetting.DARK -> true
            }

            PartyCalculatorTheme(darkTheme = isDarkTheme) {
                PartyCalculatorApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyCalculatorApp(viewModel: PartyViewModel) {
    val context = LocalContext.current
    val parties by viewModel.parties.collectAsStateWithLifecycle()
    val selectedPartyId by viewModel.selectedPartyId.collectAsStateWithLifecycle()
    val currentSummary by viewModel.currentPartySummary.collectAsStateWithLifecycle()
    val userAccount by viewModel.userAccount.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val darkModeSetting by viewModel.darkModeSetting.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var currentNavIndex by remember { mutableIntStateOf(0) }

    // Request notification permission on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // Auto-select first party for calculator tab if none selected
    LaunchedEffect(parties, currentNavIndex) {
        if (currentNavIndex == 1 && selectedPartyId == null && parties.isNotEmpty()) {
            viewModel.selectParty(parties.first().id)
        }
    }

    // If a party is selected, show detail screen directly
    if (selectedPartyId != null) {
        PartyDetailScreen(
            summary = currentSummary,
            onBack = { viewModel.selectParty(null) },
            onAddMember = { name, contact, color ->
                selectedPartyId?.let { viewModel.addMember(it, name, contact, color) }
            },
            onDeleteMember = { viewModel.deleteMember(it) },
            onAddExpense = { title, amt, payerId, payerName, cat, splitIds ->
                selectedPartyId?.let { viewModel.addExpense(it, title, amt, payerId, payerName, cat, splitIds) }
            },
            onDeleteExpense = { viewModel.deleteExpense(it) },
            onUpdateParty = { viewModel.updateParty(it) },
            onUpdateMember = { viewModel.updateMember(it) },
            onUpdateExpense = { viewModel.updateExpense(it) },
            onToggleSettled = { settled ->
                selectedPartyId?.let { viewModel.togglePartySettled(it, settled) }
            },
            onExportPdf = { viewModel.exportPdf(context) },
            onCreateBillPdf = { config -> viewModel.createBillPdf(context, config) },
            onSendAlert = { debtor, creditor, amt ->
                currentSummary?.party?.let { party ->
                    viewModel.sendSettlementNotification(context, party.title, debtor, creditor, amt)
                }
            }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "EVENT TRACKER",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = when (currentNavIndex) {
                                    0 -> "Party Calculator"
                                    1 -> "Expense Calculator"
                                    else -> "Settings"
                                },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 19.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
                        // Dark mode quick toggle
                        IconButton(
                            onClick = {
                                val next = when (darkModeSetting) {
                                    DarkModeSetting.SYSTEM -> DarkModeSetting.DARK
                                    DarkModeSetting.DARK -> DarkModeSetting.LIGHT
                                    DarkModeSetting.LIGHT -> DarkModeSetting.SYSTEM
                                }
                                viewModel.setDarkMode(next)
                            },
                            modifier = Modifier.testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (darkModeSetting == DarkModeSetting.DARK) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = "Toggle Theme"
                            )
                        }

                        // Settings action
                        IconButton(
                            onClick = { currentNavIndex = 2 },
                            modifier = Modifier.padding(end = 4.dp).testTag("nav_settings_action")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (currentNavIndex == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.drawBehind {
                        drawLine(
                            color = OutlineDark,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                ) {
                    val itemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    NavigationBarItem(
                        selected = currentNavIndex == 0,
                        onClick = { currentNavIndex = 0 },
                        icon = { Icon(Icons.Default.Celebration, contentDescription = "Parties") },
                        label = { Text("Overview", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = itemColors,
                        modifier = Modifier.testTag("nav_parties")
                    )
                    NavigationBarItem(
                        selected = currentNavIndex == 1,
                        onClick = {
                            currentNavIndex = 1
                            if (parties.isNotEmpty()) {
                                viewModel.selectParty(parties.first().id)
                            }
                        },
                        icon = { Icon(Icons.Default.Calculate, contentDescription = "Calculator") },
                        label = { Text("Calculator", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = itemColors,
                        modifier = Modifier.testTag("nav_calculator")
                    )
                    NavigationBarItem(
                        selected = currentNavIndex == 2,
                        onClick = { currentNavIndex = 2 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = itemColors,
                        modifier = Modifier.testTag("nav_settings")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentNavIndex) {
                    0 -> HomeScreen(
                        parties = parties,
                        userAccount = userAccount,
                        isOffline = isOffline,
                        onSelectParty = { viewModel.selectParty(it) },
                        onCreateParty = { title, desc, curr ->
                            viewModel.createParty(title, desc, curr, System.currentTimeMillis())
                        },
                        onUpdateParty = { viewModel.updateParty(it) },
                        onDeleteParty = { viewModel.deleteParty(it) }
                    )
                    1 -> {
                        if (parties.isEmpty()) {
                            HomeScreen(
                                parties = parties,
                                userAccount = userAccount,
                                isOffline = isOffline,
                                onSelectParty = { viewModel.selectParty(it) },
                                onCreateParty = { title, desc, curr ->
                                    viewModel.createParty(title, desc, curr, System.currentTimeMillis())
                                },
                                onUpdateParty = { viewModel.updateParty(it) },
                                onDeleteParty = { viewModel.deleteParty(it) }
                            )
                        } else {
                            viewModel.selectParty(parties.first().id)
                        }
                    }
                    2 -> AccountScreen(
                        darkModeSetting = darkModeSetting,
                        onSetDarkMode = { viewModel.setDarkMode(it) },
                        onSendTestNotification = {
                            NotificationHelper.showSettlementReminder(
                                context = context,
                                partyTitle = "Weekend Party",
                                debtorName = "Alex",
                                creditorName = "Sam",
                                amount = "₹350.00"
                            )
                        }
                    )
                }
            }
        }
    }
}
