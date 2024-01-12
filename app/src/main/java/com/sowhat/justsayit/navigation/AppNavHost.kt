package com.sowhat.justsayit.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.sowhat.presentation.navigation.CONFIGURATION
import com.sowhat.presentation.navigation.ONBOARDING
import com.sowhat.presentation.navigation.onBoardingScreen
import com.sowhat.presentation.navigation.userConfigScreen
import com.sowhat.user_presentation.navigation.configEditScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState
) {
    NavHost(navController = navController, startDestination = ONBOARDING) {
        onBoardingScreen()
        userConfigScreen()
        configEditScreen()
    }
}