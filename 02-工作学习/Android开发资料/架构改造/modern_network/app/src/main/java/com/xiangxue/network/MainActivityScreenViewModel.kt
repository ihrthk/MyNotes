package com.xiangxue.network

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiangxue.network.api.HttpbinOrgApiInterface
import com.xiangxue.network.api.TecentNewsApiInterface
import com.xiangxue.network.apiresponse.NetworkResponse
import com.xiangxue.network.environment.EnvironmentActivity
import com.xiangxue.network.utils.MoshiUtils
import kotlinx.coroutines.launch

class MainActivityScreenViewModel(application: Application) : AndroidViewModel(application) {

    fun onGetChannelsClicked() {
        viewModelScope.launch {
            when (val result =
                TecentNetworkWithEnvelopeApi.getService(TecentNewsApiInterface::class.java)
                    .getNewsChannelsWithEnvelope()) {
                is NetworkResponse.ApiError -> {
                    Log.e("ApiError", MoshiUtils.toJson(result.code))
                }
                is NetworkResponse.NetworkError -> {
                    Log.e("NetworkError", MoshiUtils.toJson(result.code))
                }
                is NetworkResponse.Success -> {
                    Log.e("Success", MoshiUtils.toJson(result.body))
                }
                is NetworkResponse.UnknownError -> {
                    Log.e("UnknownError", MoshiUtils.toJson(result.error?.message))
                }
            }
        }
    }

    fun onGetChannelsAndOpenEnvelopeClicked() = viewModelScope.launch {
        when (val result =
            TecentNetworkWithoutEnvelopeApi.getService(TecentNewsApiInterface::class.java)
                .getNewsChannelsWithoutEnvelope()) {
            is NetworkResponse.ApiError -> {
                Log.e("ApiError", MoshiUtils.toJson(result.code))
            }
            is NetworkResponse.NetworkError -> {
                Log.e("NetworkError", MoshiUtils.toJson(result.code))
            }
            is NetworkResponse.Success -> {
                Log.e("Success", MoshiUtils.toJson(result.body))
            }
            is NetworkResponse.UnknownError -> {
                Log.e("UnknownError", MoshiUtils.toJson(result.error?.message))
            }
        }
    }

    fun onGetChannelsWithKotlinNpeClicked() {
        viewModelScope.launch {
            when (val result =
                TecentNetworkWithoutEnvelopeApi.getService(TecentNewsApiInterface::class.java)
                    .getNewsChannelsWithNpe()) {
                is NetworkResponse.ApiError -> {
                    Log.e("ApiError", MoshiUtils.toJson(result.code))
                }
                is NetworkResponse.NetworkError -> {
                    Log.e("NetworkError", MoshiUtils.toJson(result.code))
                }
                is NetworkResponse.Success -> {
                    Log.e("Success", MoshiUtils.toJson(result.body))
                }
                is NetworkResponse.UnknownError -> {
                    Log.e("UnknownError", MoshiUtils.toJson(result.error?.message))
                }
            }
            HttpbinOrgApiInterface.get().status404()
        }
    }


    fun onHttpbinOrg404Clicked() {
        viewModelScope.launch {
            when (val result =
                HttpbinOrgNetworkApi.getService(HttpbinOrgApiInterface::class.java).status404()) {
                is NetworkResponse.ApiError -> {
                    Log.e("ApiError", MoshiUtils.toJson(result.code))
                }
                is NetworkResponse.NetworkError -> {
                    Log.e("NetworkError", MoshiUtils.toJson(result.code))
                }
                is NetworkResponse.Success -> {
                    Log.e("Success", MoshiUtils.toJson(result.body))
                }
                is NetworkResponse.UnknownError -> {
                    Log.e("UnknownError", MoshiUtils.toJson(result.error?.message))
                }
            }
        }
    }

    fun onHttpbinOrg501Clicked() {
        viewModelScope.launch {
            when (val result =
                HttpbinOrgNetworkApi.getService(HttpbinOrgApiInterface::class.java).status501()) {
                is NetworkResponse.ApiError -> {
                    Log.e("ApiError", MoshiUtils.toJson(result.code))
                }
                is NetworkResponse.NetworkError -> {
                    Log.e("NetworkError", MoshiUtils.toJson(result.code))
                }
                is NetworkResponse.Success -> {
                    Log.e("Success", MoshiUtils.toJson(result.body))
                }
                is NetworkResponse.UnknownError -> {
                    Log.e("UnknownError", MoshiUtils.toJson(result.error?.message))
                }
            }
        }
    }

    var clickNumber = 0;
    fun onEnvironmentClicked() {
        if (++clickNumber % 15 == 0) {
            getApplication<Application>().startActivity(
                Intent(
                    getApplication(),
                    EnvironmentActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}