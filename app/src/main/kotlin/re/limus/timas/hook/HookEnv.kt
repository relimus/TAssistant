package re.limus.timas.hook

object HookEnv {
    lateinit var hostAppPackageName: String
        private set
    fun setHostAppPackageName(pkg: String) {
        hostAppPackageName = pkg
    }
}