package com.xiangxue.network.application;

import android.app.Application;

import com.xiangxue.network.HttpbinOrgNetworkApi;
import com.xiangxue.network.base.BaseNetworkApi;

public class XiangxueApplication extends Application {
    public void onCreate(){
        super.onCreate();
        BaseNetworkApi.Companion.init(new XiangxueNetwork(this));
    }
}
