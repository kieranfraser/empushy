package eu.aempathy.empushy.utils

class Constants {
    interface ACTION {
        companion object {
            val OPEN_ACTION = "com.aempathy.empushy.listenerservice.action.open"
            val REMOVAL_ACTION = "com.aempathy.empushy.listenerservice.action.removal"
            val REMOVAL_MUL_ACTION = "com.aempathy.empushy.listenerservice.action.removal.multiple"
            val STARTFOREGROUND_ACTION = "com.aempathy.empushy.foregroundservice.action.startforeground"
            val STOPFOREGROUND_ACTION = "com.aempathy.empushy.foregroundservice.action.stopforeground"
        }
    }
}