package com.ledger.simpleledger.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Transactions : Screen("transactions")
    object People : Screen("people")
    object Reports : Screen("reports")
    object Settings : Screen("settings")

    object NewTransaction : Screen("new_transaction?type={type}&editId={editId}") {
        fun build(type: String? = null, editId: Long? = null): String {
            val t = type ?: ""
            val e = editId?.toString() ?: ""
            return "new_transaction?type=$t&editId=$e"
        }
    }

    object TransactionDetail : Screen("transaction_detail/{id}") {
        fun build(id: Long) = "transaction_detail/$id"
    }

    object PersonDetail : Screen("person_detail/{id}") {
        fun build(id: Long) = "person_detail/$id"
    }

    object AddEditPerson : Screen("add_person?editId={editId}") {
        fun build(editId: Long? = null) = "add_person?editId=${editId ?: ""}"
    }

    object Categories : Screen("categories")
}

val bottomNavScreens = listOf(
    Screen.Dashboard, Screen.Transactions, Screen.People, Screen.Reports, Screen.Settings
)
