package cn.xihan.fridahooker.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import cn.xihan.fridahooker.util.Utils.FRIDA_CHECKER_DELAY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize
import org.tukaani.xz.XZInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.ServerSocket


/**
 * @项目名 : FridaHooker
 * @作者 : MissYang
 * @创建时间 : 2023/7/2 16:31
 * @介绍 :
 */
@Composable
fun rememberFridaController(
    installPath: String? = null,
): DefaultFridaController {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    return rememberSaveable(
        context, coroutineScope,
        saver = object : Saver<DefaultFridaController, FridaStatus> {
            override fun restore(value: FridaStatus): DefaultFridaController {
                return DefaultFridaController(
                    context = context,
                    initialState = value,
                    coroutineScope = coroutineScope
                )
            }

            override fun SaverScope.save(value: DefaultFridaController): FridaStatus {
                return value.currentState { it }
            }
        },
        init = {
            DefaultFridaController(
                context = context,
                initialState = FridaStatus(),
                coroutineScope = coroutineScope
            ).apply {
                installPath?.let { setInstallPath(it) }
            }
        }
    )

}

class DefaultFridaController(
    context: Context,
    private val initialState: FridaStatus,
    private val coroutineScope: CoroutineScope
) : FridaController {

    private val TAG = "FridaAgent"

    private val _state = MutableStateFlow(initialState)

    override val state: StateFlow<FridaStatus>
        get() = _state.asStateFlow()

    fun <T> currentState(filter: (FridaStatus) -> T): T {
        return filter(_state.value)
    }

    @Composable
    fun collect(): State<FridaStatus> {
        return _state.collectAsState()
    }

    @SuppressLint("StateFlowValueCalledInComposition")
    @Composable
    fun <T> collect(filter: FridaStatus.() -> T): State<T> {
        return remember(filter) {
            _state.map { it.filter() }
        }.collectAsState(
            initial = _state.value.filter()
        )
    }

    override fun start(params: String): Boolean {
        if (!_state.value.isSupported || !_state.value.isInstall) {
            return false
        }
        val cmds = arrayOf(
            "chmod +x ${_state.value.installPath}${File.separator}frida-server",
            "su -c ${_state.value.installPath}${File.separator}frida-server $params &"
        )
        for (cmd in cmds) {
            val code: Int = Utils.execute(cmd)
            if (0 != code) {
                return false
            }
        }
        return true
    }

    override fun stop(): Boolean {
        if (!_state.value.isSupported || !_state.value.isInstall) {
            return false
        }
        val cmds = arrayOf(
            "su -c kill -9 $(su -c pidof frida-server)"
        )
        for (cmd in cmds) {
            val code: Int = Utils.execute(cmd)
            if (0 != code) {
                return false
            }
        }
        return true
    }

    override fun delete(): Boolean {
        if (!_state.value.isSupported || !_state.value.isInstall) {
            return false
        }
        val code: Int = Utils.execute("rm -rf ${_state.value.installPath}")
        return 0 == code
    }

    override fun install(cacheFile: File): Boolean {
        if (!_state.value.isSupported) {
            return false
        }
        val cmd = "mv " + cacheFile.absolutePath + " " + _state.value.installPath + File.separator
        File(_state.value.installPath).mkdirs()
        val code: Int = Utils.execute(cmd)
        return 0 == code
    }

    fun extractLocalFrida(`is`: InputStream, to: String): File {
        val temp = File(to + File.separator + "frida-server")
        XZInputStream(`is`).use { xz ->
            FileOutputStream(temp).use {
                it.write(xz.readBytes())
            }
        }
        return temp
    }

    fun checkAll(success: () -> Unit = {}, failure: () -> Unit = {}) {
        _state.set {
            copy(
                isSupported = checkSupported()
            )
        }
        _state.set {
            copy(
                isInstall = checkInstallation()
            )
        }
        checkRunning(
            success = {
                _state.set {
                    copy(
                        isRunning = true
                    )
                }
                success()
            },
            failure = {
                _state.set {
                    copy(
                        isRunning = false
                    )
                }
                failure()
            }
        )
    }

    fun setInstallPath(installPath: String) {
        _state.set {
            copy(
                installPath = installPath
            )
        }
    }

    private fun checkSupported() = runCatching {
        Utils.conventToFridaAbi(supportedAbiList[0]).isNotBlank()
    }.getOrElse { false }

    private fun checkInstallation(): Boolean {
        if (!_state.value.isSupported) {
            return false
        }
        val code: Int =
            Utils.execute("ls ${_state.value.installPath}${File.separator}frida-server")
        return 0 == code
    }

    private fun checkRunning(
        success: () -> Unit = {}, failure: () -> Unit = {}
    ) {
        if (!_state.value.isSupported) {
            failure()
            return
        }
        Thread {
            runCatching {
                Thread.sleep(FRIDA_CHECKER_DELAY.toLong())
            }
            try {
                val serverSocket = ServerSocket(27042)
                serverSocket.close()
//                Log.d(TAG, "checkRunning try to bind tcp:27042 success, frida is not running.")
                failure()
            } catch (e: Exception) {
//                Log.d(TAG, "checkRunning try to bind tcp:27042 but failed, frida is still running. e: ${e.message}")
                success()
            }
        }.apply {
            isDaemon = true
            name = "Thread-checkFrida"
        }.also {
            it.start()
        }
    }

}

/**
 * Frida 控制器
 */
interface FridaController {
    fun start(params: String = ""): Boolean
    fun stop(): Boolean
    fun delete(): Boolean
    fun install(cacheFile: File): Boolean

    val state: StateFlow<FridaStatus>
}

/**
 * Frida 状态
 */
@Parcelize
@Keep
data class FridaStatus(
    var versionName: String = "",
    var installPath: String = "",
    var isRunning: Boolean = false,
    var isInstall: Boolean = false,
    var isSupported: Boolean = false,
) : Parcelable

