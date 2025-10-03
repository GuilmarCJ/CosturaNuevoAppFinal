package com.costura.pro.utils

object Constants {
    const val PREF_NAME = "CosturaProPrefs"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
    const val KEY_USER_ID = "user_id"
    const val KEY_USER_ROLE = "user_role"
    const val KEY_USERNAME = "username"

    // Firebase Collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_WORKERS = "workers"
    const val COLLECTION_OPERATIONS = "operations"
    const val COLLECTION_PRODUCTION = "production"

    // Default Admin Credentials (hardcoded for security)
    const val ADMIN_USERNAME = "admin"
    const val ADMIN_PASSWORD = "admin123" // Change this in production
}