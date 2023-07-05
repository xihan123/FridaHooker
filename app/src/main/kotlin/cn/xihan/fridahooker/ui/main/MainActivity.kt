package cn.xihan.fridahooker.ui.main


import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import cn.xihan.fridahooker.R
import cn.xihan.fridahooker.model.ConfigModel
import cn.xihan.fridahooker.util.KStore
import cn.xihan.fridahooker.util.alert
import cn.xihan.fridahooker.util.androidVersion
import cn.xihan.fridahooker.util.apiLevel
import cn.xihan.fridahooker.util.launchAndCollectIn
import cn.xihan.fridahooker.util.myApplication
import cn.xihan.fridahooker.util.openUrl
import cn.xihan.fridahooker.util.path
import cn.xihan.fridahooker.util.productName
import cn.xihan.fridahooker.util.rememberFridaController
import cn.xihan.fridahooker.util.rememberMutableStateOf
import cn.xihan.fridahooker.util.storeOf
import cn.xihan.fridahooker.util.supportedAbiList
import cn.xihan.fridahooker.util.versionName
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.accompanist.themeadapter.material3.Mdc3Theme
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import java.io.File


class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<MainViewModel>()

    private val _config: KStore<ConfigModel> = storeOf<ConfigModel>(
        "${myApplication.filesDir.absolutePath}/config.json"
    )

    private val configFlow = _config.updates

    init {
        configFlow.launchAndCollectIn(this) {
            it?.let { it1 ->
                viewModel.setConfigModel(it1)
            }
        }
    }

    val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                viewModel.setUri(it)
            } ?: alert(this@MainActivity.getString(R.string.file_error))
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContent {
            Mdc3Theme {
                val systemUiController = rememberSystemUiController()
                val darkIcons = isSystemInDarkTheme()
                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent, darkIcons = !darkIcons
                    )

                    systemUiController.setNavigationBarColor(
                        Color.Transparent, darkIcons = !darkIcons
                    )
                }
                ComposeContent()
            }
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    @Composable
    fun ComposeContent() {
        val state by viewModel.collectAsState()
        val fridaController = rememberFridaController()
        val fridaState by fridaController.collect()
        val fridaRunning = fridaState.isRunning
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        val topAppBarExpanded = rememberMutableStateOf(value = false)
        val menuAppBarExpanded = rememberMutableStateOf(value = false)
        DisposableEffect(lifecycleOwner) {

            val observer = object : DefaultLifecycleObserver {

                override fun onCreate(owner: LifecycleOwner) {
                    updateFridaVersionList()
                }

                override fun onPause(owner: LifecycleOwner) {
                    // TODO: 进入后台就停止 frida-server 暂时不做处理
//                    if (fridaState.isRunning) {
//                        val isSuccess = fridaController.stop()
//                        if (isSuccess) {
//                            fridaController.checkAll()
//                        }
//                    }
                }

                override fun onResume(owner: LifecycleOwner) {

                }

                override fun onStart(owner: LifecycleOwner) {
                    fridaController.checkAll()
                }

                override fun onStop(owner: LifecycleOwner) {
                    // 停止 frida-server 如果被强杀则无法正常释放
                    if (fridaState.isRunning) {
                        val isSuccess = fridaController.stop()
                        if (isSuccess) {
                            fridaController.checkAll()
                        }
                    }
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    // 停止 frida-server 如果被强杀则无法正常释放
                    if (fridaState.isRunning) {
                        val isSuccess = fridaController.stop()
                        if (isSuccess) {
                            fridaController.checkAll()
                        }
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        LaunchedEffect(state.uri) {
            if (state.uri != Uri.EMPTY) {
                val fileName = state.uri.path(context).versionName
                if ("error" == fileName) {
                    alert(getString(R.string.file_error))
                } else {
                    writeConfig(state.configModel.copy(versionName = fileName))
                    contentResolver.openInputStream(state.uri).use { `is` ->
                        `is`?.let {
                            fridaController.setInstallPath("${filesDir.absolutePath}${File.separator}frida${File.separator}$fileName")
                            val isSuccess = fridaController.install(
                                fridaController.extractLocalFrida(
                                    `is`,
                                    filesDir.absolutePath
                                )
                            )
                            if (isSuccess) {
                                fridaController.checkAll()
                                updateFridaVersionList()
                            } else {
                                alert(getString(R.string.file_error))
                            }
                        } ?: alert(getString(R.string.file_error))
                    }
                }
            }
        }

        LaunchedEffect(state.configModel.versionName) {
            val versionName = state.configModel.versionName
            if (versionName.isNotBlank() && "error" != versionName) {
                fridaController.setInstallPath("${filesDir.absolutePath}${File.separator}frida${File.separator}${state.configModel.versionName}")
                fridaController.checkAll()
            }
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(text = stringResource(id = R.string.app_name))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    actions = {
                        AnywhereDropdown(
                            expanded = menuAppBarExpanded.value,
                            onDismissRequest = { menuAppBarExpanded.value = false },
                            onClick = { menuAppBarExpanded.value = true },
                            surface = {
                                IconButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = { menuAppBarExpanded.value = true },
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = null
                                    )
                                }
                            }
                        ) {

                            MyDropdownMenuItem(
                                topAppBarExpanded = menuAppBarExpanded,
                                text = { Text(stringResource(R.string.refresh)) },
                                onClick = {
                                    fridaController.checkAll()
                                }
                            )

                            MyDropdownMenuItem(
                                topAppBarExpanded = menuAppBarExpanded,
                                text = { Text(stringResource(R.string.open_source_link)) },
                                onClick = {
                                    context.openUrl("https://github.com/xihan123/FridaHooker")
                                }
                            )

                        }

                    },
                    navigationIcon = {

                    },
                    scrollBehavior = null
                )

            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                SwitchSetting(
                    checked = fridaRunning,
                    title = stringResource(id = R.string.frida_status),
                    enabled = fridaState.isInstall,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            if (!fridaRunning) {
                                val isFinish = fridaController.start(state.fridaParams)
                                if (isFinish) {
                                    fridaController.checkAll()
                                } else {
                                    alert(getString(R.string.operation_failed_message))
                                }
                            }
                        } else {
                            if (fridaRunning) {
                                val isFinish = fridaController.stop()
                                if (isFinish) {
                                    fridaController.checkAll()
                                } else {
                                    alert(getString(R.string.operation_failed_message))
                                }
                            }
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(10.dp)
                        .background(if (fridaState.isInstall) Color.Green else Color.Red)
                ) {
                    Image(
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(Color.White),
                        imageVector = if (fridaState.isInstall) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(if (fridaState.isInstall) Color.Green else Color.Red)
                    )
                }

                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        readOnly = true,
                        value = stringResource(
                            id = if (fridaState.isInstall) R.string.frida_server_ready else R.string.frida_server_not_ready,
                            state.fridaVersionSelected
                        ),
                        onValueChange = { },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    AnywhereDropdown(
                        expanded = topAppBarExpanded.value,
                        onDismissRequest = { topAppBarExpanded.value = false },
                        onClick = { topAppBarExpanded.value = true },
                        surface = {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { topAppBarExpanded.value = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(text = stringResource(id = R.string.frida_manage))
                            }
                        }
                    ) {

                        MyDropdownMenuItem(
                            topAppBarExpanded = topAppBarExpanded,
                            text = { Text(stringResource(R.string.frida_install)) },
                            onClick = {
                                getContent.launch("*/*")
                            }
                        )

                        MyDropdownMenuItem(
                            topAppBarExpanded = topAppBarExpanded,
                            text = { Text(stringResource(R.string.frida_delete)) },
                            onClick = {
                                val isSuccess = fridaController.delete()
                                if (isSuccess) {
                                    updateFridaVersionList()
                                    fridaController.checkAll()
                                }
                            }
                        )

                    }


                }

                FridaVersionChanged(
                    modifier = Modifier.wrapContentSize(),
                    list = state.fridaVersionList,
                    selected = state.fridaVersionSelected,
                    onSelected = {
                        coroutineScope.launch {
                            writeConfig(state.configModel.copy(versionName = it))
                        }
                    }
                )

                EditTextSetting(
                    title = stringResource(id = R.string.frida_start_params),
                    text = state.fridaParams,
                    onTextChange = { params ->
                        coroutineScope.launch {
                            writeConfig(state.configModel.copy(params = params))
                        }
                    }
                )



                Text(
                    text = stringResource(id = R.string.android_ver, androidVersion, apiLevel)
                )

                Text(
                    text = stringResource(id = R.string.device_name, productName)
                )

                Text(
                    text = stringResource(id = R.string.device_abi, supportedAbiList[0])
                )

                Text(
                    text = stringResource(
                        id = R.string.frida_path,
                        "${filesDir.absolutePath}${File.separator}frida${File.separator}${state.configModel.versionName}"
                    )
                )

                Text(
                    text = stringResource(id = R.string.hint),
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Visible,
                    color = Color.Red
                )
            }
        }

    }

    /**
     * 写入配置文件
     * @param versionName 版本名称
     * @param  params 参数
     */
    private suspend fun writeConfig(
        configModel: ConfigModel
    ) {
        _config.update { configModel }
        viewModel.setConfigModel(configModel)
    }

    fun updateFridaVersionList() = runCatching {
        viewModel.setFridaVersionList(filesDir.listFiles()?.filter { it.isDirectory }
            ?.map { it.list() }?.first()?.toList() ?: emptyList())
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = SnackbarHostState(),
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable (PaddingValues) -> Unit
) {
    androidx.compose.material3.Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding(),
            )
        },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            content.invoke(paddingValues)
        }
    }
}

@Composable
fun SwitchSetting(
    checked: Boolean,
    modifier: Modifier = Modifier,
    title: String = "",
    enabled: Boolean = true,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onCheckedChange: (Boolean) -> Unit = {},
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .padding(8.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            overflow = TextOverflow.Visible,
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            color = textColor,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Start
        )
        Switch(
            checked = checked, onCheckedChange = onCheckedChange, enabled = enabled
        )
    }

}

@Composable
fun EditTextSetting(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    onTextChange: (String) -> Unit = {},
) {
    Row(
        modifier = modifier.padding(15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            overflow = TextOverflow.Visible,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start
        )

        TextField(
            modifier = Modifier.weight(1f),
            value = text,
            onValueChange = onTextChange,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun FridaVersionChanged(
    list: List<String>,
    selected: String,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit = {},
) {
    val expanded = rememberMutableStateOf(value = false)
    Row(
        modifier = modifier.padding(15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.frida_version_changed),
            fontWeight = FontWeight.Bold,
            overflow = TextOverflow.Visible
        )

        Spacer(modifier = Modifier.width(5.dp))

        ExposedDropdownMenuBox(
            expanded = expanded.value,
            onExpandedChange = {
                expanded.value = !expanded.value
            }
        ) {
            TextField(
                readOnly = true,
                value = selected,
                onValueChange = { },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded.value
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            ExposedDropdownMenu(
                expanded = expanded.value,
                onDismissRequest = {
                    expanded.value = false
                },
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.surface)
            ) {
                list.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = {
                            Text(text = selectionOption)
                        },
                        onClick = {
                            onSelected(selectionOption)
                            expanded.value = false
                        },
                    )
                }

            }
        }


    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnywhereDropdown(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    surface: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val indication = LocalIndication.current
    val interactionSource = remember { MutableInteractionSource() }
    val state by interactionSource.interactions.collectAsState(null)
    var offset by rememberMutableStateOf(Offset.Zero)
    val dpOffset = with(LocalDensity.current) {
        DpOffset(offset.x.toDp(), offset.y.toDp())
    }

    LaunchedEffect(state) {
        if (state is PressInteraction.Press) {
            val i = state as PressInteraction.Press
            offset = i.pressPosition
        }
        if (state is PressInteraction.Release) {
            val i = state as PressInteraction.Release
            offset = i.press.pressPosition
        }
    }

    Box(
        modifier = modifier
            .combinedClickable(
                interactionSource = interactionSource,
                indication = indication,
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        surface()
        Box {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = onDismissRequest,
                offset = dpOffset,
                content = content
            )
        }
    }
}

@Composable
fun MyDropdownMenuItem(
    topAppBarExpanded: MutableState<Boolean>,
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    DropdownMenuItem(
        text = text,
        onClick = {
            topAppBarExpanded.value = false
            onClick()
        },
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    )
}

/*
@Preview
@Composable
fun AppIcon() {
    Box(
        modifier = Modifier
            .size(80.dp, 80.dp)
            .background(Color.White, RoundedCornerShape(10.dp))
    ) {
        Text(
            text = "Я",
            fontWeight = FontWeight.Bold,
            overflow = TextOverflow.Visible,
            color =  Color(0xFFEF6456),
            fontSize = 80.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

 */

