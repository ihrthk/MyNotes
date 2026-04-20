package com.xiangxue.network.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Created by Allen on 2017/7/20.
 * 保留所有版权，未经允许请不要分享到互联网和其他人
 */
@JsonClass(generateAdapter = true)
data class NewsChannelsJsonNpeBean(
    @Json(name = "channelList")
    val channelList: List<NpeChannel>,
    @Json(name = "ret_code")
    val retCode: Int,
    @Json(name = "totalNum")
    val totalNum: Int
)

@JsonClass(generateAdapter = true)
data class NpeChannel(
    @Json(name = "channelId")
    val channelId: String,
    @Json(name = "name")
    val name: String,
    @Json(name = "npe_name")
    val npeName: String
)
