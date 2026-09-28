package com.nary.gym.navigation

import kotlinx.serialization.Serializable

// Outer graph
@Serializable object LoginRoute
@Serializable object CreateAccountRoute
@Serializable object MainRoute
@Serializable object ProfileRoute

// Tabs inside MainRoute
@Serializable object TodayRoute
@Serializable object WorkoutsRoute
@Serializable object CheckInsRoute
