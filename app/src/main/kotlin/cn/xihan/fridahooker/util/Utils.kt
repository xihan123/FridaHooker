package cn.xihan.fridahooker.util

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.view.KeyEvent
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.DeprecationLevel.ERROR


/**
 * @项目名 : FridaHooker
 * @作者 : MissYang
 * @创建时间 : 2023/7/1 14:54
 * @介绍 :
 */
object Utils {
    const val FRIDA_CHECKER_DELAY = 3000
    const val TAG = "FridaHooker"

    init {
        System.loadLibrary("fridahooker")
    }

    external fun execute(cmd: String): Int

    fun conventToFridaAbi(systemAbi: String): String {
        return when (systemAbi) {
            "armeabi-v7a" -> "arm"
            "arm64-v8a" -> "arm64"
            "x86" -> "x86"
            "x86_64" -> "x86_64"
            else -> ""
        }
    }
}

/**
 * 获取文件名中的版本号属性函数
 */
val String.versionName: String
    get() = runCatching {
        Regex("frida-server-(.*?)-").find(this)?.groupValues?.get(1) ?: "error"
    }.getOrElse { "error" }

/**
 * 获取支持的Abi列表
 */
val supportedAbiList = Build.SUPPORTED_ABIS

/** 产品名称 */
val productName = "${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})"

/** android版本 */
val androidVersion = Build.VERSION.RELEASE

/** api级别 */
val apiLevel = Build.VERSION.SDK_INT

@SuppressLint("Range")
fun Uri.path(context: Context): String {
    context.contentResolver.query(this, null, null, null, null)?.use {
        if (it.moveToFirst()) {
            return it.getString(
                it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            )
        }
    }
    return "error"
}

/**
 * 默认浏览器打开url
 */
fun Context.openUrl(url: String) = runCatching {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse(url)
    startActivity(intent)
}

internal const val NO_GETTER: String = "Property does not have a getter"
internal fun noGetter(): Nothing = throw NotImplementedError(NO_GETTER)
typealias AlertBuilderFactory<D> = (Context) -> AlertBuilder<D>

val Material: AlertBuilderFactory<DialogInterface> = { context ->
    object : AlertDialogBuilder() {
        override val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(context)
    }
}
private var defaultAlertBuilderFactory: AlertBuilderFactory<*> = Material

fun initAlertBuilderFactory(factory: AlertBuilderFactory<*>) {
    defaultAlertBuilderFactory = factory
}

fun Context.alert(
    message: CharSequence,
    title: CharSequence? = null,
    block: (AlertBuilder<*>.() -> Unit)? = null
) =
    alert(defaultAlertBuilderFactory, message, title, block)

inline fun <D : DialogInterface> Context.alert(
    factory: AlertBuilderFactory<D>,
    message: CharSequence,
    title: CharSequence? = null,
    noinline block: (AlertBuilder<D>.() -> Unit)? = null
) =
    alertDialog(factory) {
        title?.let { this.title = it }
        this.message = message
        block?.invoke(this)
    }.show()

fun Context.alertDialog(block: AlertBuilder<*>.() -> Unit) =
    alertDialog(defaultAlertBuilderFactory, block)

inline fun <D : DialogInterface> Context.alertDialog(
    factory: AlertBuilderFactory<D>,
    block: AlertBuilder<D>.() -> Unit
) =
    factory(this).apply(block)

fun AlertBuilder<*>.okButton(onClicked: (dialog: DialogInterface) -> Unit) =
    positiveButton(android.R.string.ok, onClicked)

fun AlertBuilder<*>.cancelButton(onClicked: (dialog: DialogInterface) -> Unit = { it.dismiss() }) =
    negativeButton(android.R.string.cancel, onClicked)

interface AlertBuilder<out D : DialogInterface> {
    val context: Context

    var title: CharSequence
        @Deprecated(NO_GETTER, level = ERROR) get
    var titleResource: Int
        @Deprecated(NO_GETTER, level = ERROR) get

    var message: CharSequence
        @Deprecated(NO_GETTER, level = ERROR) get
    var messageResource: Int
        @Deprecated(NO_GETTER, level = ERROR) get

    var icon: Drawable
        @Deprecated(NO_GETTER, level = ERROR) get
    var iconResource: Int
        @Deprecated(NO_GETTER, level = ERROR) get

    var customTitle: View
        @Deprecated(NO_GETTER, level = ERROR) get

    var customView: View
        @Deprecated(NO_GETTER, level = ERROR) get

    var isCancelable: Boolean
        @Deprecated(NO_GETTER, level = ERROR) get

    fun onCancelled(handler: (DialogInterface) -> Unit)

    fun onKeyPressed(handler: (DialogInterface, keyCode: Int, e: KeyEvent) -> Boolean)

    fun positiveButton(buttonText: String, onClicked: (dialog: DialogInterface) -> Unit)
    fun positiveButton(
        @StringRes buttonTextResource: Int,
        onClicked: (dialog: DialogInterface) -> Unit
    )

    fun negativeButton(buttonText: String, onClicked: (dialog: DialogInterface) -> Unit)
    fun negativeButton(
        @StringRes buttonTextResource: Int,
        onClicked: (dialog: DialogInterface) -> Unit
    )

    fun neutralPressed(buttonText: String, onClicked: (dialog: DialogInterface) -> Unit)
    fun neutralPressed(
        @StringRes buttonTextResource: Int,
        onClicked: (dialog: DialogInterface) -> Unit
    )

    fun items(
        items: List<CharSequence>,
        onItemSelected: (dialog: DialogInterface, index: Int) -> Unit
    )

    fun singleChoiceItems(
        items: List<CharSequence>,
        checkedIndex: Int,
        onItemSelected: (dialog: DialogInterface, index: Int) -> Unit
    )

    fun multiChoiceItems(
        items: List<CharSequence>,
        checkedItems: BooleanArray,
        onItemSelected: (dialog: DialogInterface, index: Int, isChecked: Boolean) -> Unit
    )

    fun build(): D
    fun show(): D
}

abstract class AlertDialogBuilder : AlertBuilder<AlertDialog> {

    abstract val builder: AlertDialog.Builder
    override val context get() = builder.context

    override var title: CharSequence
        @Deprecated(NO_GETTER, level = ERROR)
        get() = noGetter()
        set(value) {
            builder.setTitle(value)
        }

    override var titleResource: Int
        @Deprecated(NO_GETTER, level = ERROR)
        get() = noGetter()
        set(value) {
            builder.setTitle(value)
        }

    override var message: CharSequence
        @Deprecated(NO_GETTER, level = ERROR)
        get() = noGetter()
        set(value) {
            builder.setMessage(value)
        }

    override var messageResource: Int
        @Deprecated(NO_GETTER, level = ERROR)
        get() = noGetter()
        set(value) {
            builder.setMessage(value)
        }

    override var icon: Drawable
        @Deprecated(NO_GETTER, level = ERROR)
        get() = noGetter()
        set(value) {
            builder.setIcon(value)
        }

    override var iconResource: Int
        @Deprecated(NO_GETTER, level = ERROR)
        get() = noGetter()
        set(value) {
            builder.setIcon(value)
        }

    override var customTitle: View
        @Deprecated(NO_GETTER, level = ERROR)
        get() = noGetter()
        set(value) {
            builder.setCustomTitle(value)
        }

    override var customView: View
        @Deprecated(NO_GETTER, level = ERROR)
        get() = noGetter()
        set(value) {
            builder.setView(value)
        }

    override var isCancelable: Boolean
        @Deprecated(NO_GETTER, level = ERROR)
        get() = noGetter()
        set(value) {
            builder.setCancelable(value)
        }

    override fun onCancelled(handler: (DialogInterface) -> Unit) {
        builder.setOnCancelListener(handler)
    }

    override fun onKeyPressed(handler: (DialogInterface, keyCode: Int, e: KeyEvent) -> Boolean) {
        builder.setOnKeyListener(handler)
    }

    override fun positiveButton(buttonText: String, onClicked: (DialogInterface) -> Unit) {
        builder.setPositiveButton(buttonText) { dialog, _ -> onClicked(dialog) }
    }

    override fun positiveButton(buttonTextResource: Int, onClicked: (DialogInterface) -> Unit) {
        builder.setPositiveButton(buttonTextResource) { dialog, _ -> onClicked(dialog) }
    }

    override fun negativeButton(buttonText: String, onClicked: (DialogInterface) -> Unit) {
        builder.setNegativeButton(buttonText) { dialog, _ -> onClicked(dialog) }
    }

    override fun negativeButton(buttonTextResource: Int, onClicked: (DialogInterface) -> Unit) {
        builder.setNegativeButton(buttonTextResource) { dialog, _ -> onClicked(dialog) }
    }

    override fun neutralPressed(buttonText: String, onClicked: (DialogInterface) -> Unit) {
        builder.setNeutralButton(buttonText) { dialog, _ -> onClicked(dialog) }
    }

    override fun neutralPressed(buttonTextResource: Int, onClicked: (DialogInterface) -> Unit) {
        builder.setNeutralButton(buttonTextResource) { dialog, _ -> onClicked(dialog) }
    }

    override fun items(items: List<CharSequence>, onItemSelected: (DialogInterface, Int) -> Unit) {
        builder.setItems(items.toTypedArray()) { dialog, which ->
            onItemSelected(dialog, which)
        }
    }

    override fun singleChoiceItems(
        items: List<CharSequence>,
        checkedIndex: Int,
        onItemSelected: (DialogInterface, Int) -> Unit
    ) {
        builder.setSingleChoiceItems(items.toTypedArray(), checkedIndex) { dialog, which ->
            onItemSelected(dialog, which)
        }
    }

    override fun multiChoiceItems(
        items: List<CharSequence>,
        checkedItems: BooleanArray,
        onItemSelected: (DialogInterface, Int, Boolean) -> Unit
    ) {
        builder.setMultiChoiceItems(
            items.toTypedArray(),
            checkedItems
        ) { dialog, which, isChecked ->
            onItemSelected(dialog, which, isChecked)
        }
    }

    override fun build(): AlertDialog = builder.create()

    override fun show(): AlertDialog = builder.show()
}

@Composable
fun <T> rememberMutableStateOf(value: T): MutableState<T> = remember { mutableStateOf(value) }

fun <T> MutableStateFlow<T>.set(block: T.() -> T) {
    this.value = this.value.block()
}

inline fun <T> Flow<T>.launchAndCollectIn(
    owner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: suspend CoroutineScope.(T) -> Unit,
) = owner.lifecycleScope.launch {
    owner.repeatOnLifecycle(minActiveState) {
        collect {
            action(it)
        }
    }
}

class FlowDebouncer<T>(timeoutMillis: Long) : Flow<T> {

    private val sourceChannel: Channel<T> = Channel(capacity = 1)

    @OptIn(FlowPreview::class)
    private val flow: Flow<T> = sourceChannel.consumeAsFlow().debounce(timeoutMillis)

    suspend fun put(item: T) {
        sourceChannel.send(item)
    }

    @InternalCoroutinesApi
    override suspend fun collect(collector: FlowCollector<T>) {
        flow.collect(collector)
    }

}

lateinit var myApplication: Application
    internal set