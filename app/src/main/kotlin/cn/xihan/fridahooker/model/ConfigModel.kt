package cn.xihan.fridahooker.model

import kotlinx.serialization.Serializable

/**
 * @项目名 : FridaHooker
 * @作者 : MissYang
 * @创建时间 : 2023/7/4 22:00
 * @介绍 :
 */
@Serializable
data class ConfigModel(
    val versionName: String = "",
    val params: String = ""
)