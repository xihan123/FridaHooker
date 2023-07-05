package cn.xihan.fridahooker.ui.main

import android.net.Uri
import cn.xihan.fridahooker.base.BaseViewModel
import cn.xihan.fridahooker.base.IUiIntent
import cn.xihan.fridahooker.base.IUiState
import cn.xihan.fridahooker.model.ConfigModel
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce

/**
 * @项目名 : FridaHooker
 * @作者 : MissYang
 * @创建时间 : 2023/7/2 23:35
 * @介绍 :
 */
class MainViewModel : BaseViewModel<MainState, IUiIntent>() {

    override fun initViewState(): MainState = MainState()

    fun setUri(uri: Uri) = intent {
        reduce {
            state.copy(uri = uri)
        }
    }

    fun setConfigModel(configModel: ConfigModel) = intent {
        reduce {
            state.copy(
                configModel = configModel,
                fridaVersionSelected = configModel.versionName,
                fridaParams = configModel.params
            )
        }
    }

    fun setFridaVersionList(fridaVersionList: List<String>) = intent {
        reduce {
            state.copy(fridaVersionList = fridaVersionList)
        }
    }
}

data class MainState(
    val uri: Uri = Uri.EMPTY,
    val configModel: ConfigModel = ConfigModel(),
    val fridaVersionList: List<String> = emptyList(),
    val fridaVersionSelected: String = "",
    val fridaParams: String = ""
) : IUiState

