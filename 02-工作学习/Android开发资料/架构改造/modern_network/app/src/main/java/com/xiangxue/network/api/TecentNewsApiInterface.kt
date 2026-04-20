package com.xiangxue.network.api;

import com.xiangxue.network.apiresponse.NetworkResponse
import retrofit2.http.GET;

/**
 * Created by Allen on 2017/7/20.
 * 保留所有版权，未经允许请不要分享到互联网和其他人
 */
interface TecentNewsApiInterface {
    @GET("release/channel")
    suspend fun getNewsChannelsWithoutEnvelope(
    ): NetworkResponse<NewsChannelsBean>

    @GET("release/channel")
    suspend fun getNewsChannelsWithEnvelope(
    ): NetworkResponse<NewsChannelsBeanExtendsBaseResponse>

    @GET("release/channel")
    suspend fun getNewsChannelsWithNpe(
    ): NetworkResponse<NewsChannelsJsonNpeBean>
}
