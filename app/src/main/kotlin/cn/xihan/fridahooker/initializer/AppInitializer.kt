package cn.xihan.fridahooker.initializer

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import cn.xihan.fridahooker.util.Utils.TAG
import cn.xihan.fridahooker.util.myApplication

/**
 * @项目名 : FridaHooker
 * @作者 : MissYang
 * @创建时间 : 2023/7/4 21:44
 * @介绍 :
 */
class AppInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        myApplication = context as Application
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()

}