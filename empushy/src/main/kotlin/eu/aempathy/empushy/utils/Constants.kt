package eu.aempathy.empushy.utils

class Constants {

    interface ACTION {
        companion object {
            val MAIN_ACTION = "eu.artificialempathy.cempy.foregroundservice.action.main"
            val INIT_ACTION = "eu.artificialempathy.cempy.foregroundservice.action.init"
            val PREV_ACTION = "eu.artificialempathy.cempy.foregroundservice.action.prev"
            val PLAY_ACTION = "eu.artificialempathy.cempy.foregroundservice.action.play"
            val NEXT_ACTION = "eu.artificialempathy.cempy.foregroundservice.action.next"
            val STARTFOREGROUND_ACTION = "eu.artificialempathy.cempy.foregroundservice.action.startforeground"
            val STOPFOREGROUND_ACTION = "eu.artificialempathy.cempy.foregroundservice.action.stopforeground"
        }
    }

    interface NOTIFICATION_ID {
        companion object {
            val FOREGROUND_SERVICE = 101
        }
    }
}