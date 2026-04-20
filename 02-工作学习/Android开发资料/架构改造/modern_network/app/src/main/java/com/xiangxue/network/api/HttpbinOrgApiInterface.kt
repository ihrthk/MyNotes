package com.xiangxue.network.api

import com.xiangxue.network.HttpbinOrgNetworkApi
import com.xiangxue.network.apiresponse.NetworkResponse
import com.xiangxue.network.beans.HttpbinOrgBaseResponse
import retrofit2.http.GET

interface HttpbinOrgApiInterface {

//    companion object {
//        @JvmStatic
//        fun get(): PassportApi = lazy {
//            Network.getInstance().retrofit().create(PassportApi::class.java)
//        }.value
//    }
    companion object {
        @JvmStatic
        fun get(): HttpbinOrgApiInterface = lazy {
            HttpbinOrgNetworkApi.getService(HttpbinOrgApiInterface::class.java)
        }.value
    }

    @GET("status/404")
    suspend fun status404(
    ): NetworkResponse<HttpbinOrgBaseResponse>


    @GET("status/501")
    suspend fun status501(
    ): NetworkResponse<HttpbinOrgBaseResponse>
}