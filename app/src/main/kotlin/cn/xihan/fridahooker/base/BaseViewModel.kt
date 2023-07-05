package cn.xihan.fridahooker.base

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

/**
 * @项目名 : FridaHooker
 * @作者 : MissYang
 * @创建时间 : 2023/7/4 22:01
 * @介绍 :
 */

@Keep
interface IUiIntent

@Keep
interface IUiState

@Keep
interface IViewModel<S : IUiState, I : IUiIntent> : ContainerHost<S, I> {
    fun initViewState(): S
}

abstract class BaseViewModel<S : IUiState, I : IUiIntent> : IViewModel<S, I>, ViewModel() {
    override val container: Container<S, I> =
        container(
            initialState = initViewState(),
        )

    /**
     * 主线程执行
     */
    fun mainLaunch(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(Dispatchers.Main) {
            block.invoke(this)
        }
    }

    /**
     * IO线程执行
     */
    fun ioLaunch(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            block.invoke(this)
        }
    }

}
