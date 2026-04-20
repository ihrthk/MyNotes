package com.xiangxue.network.api;

import com.squareup.moshi.Json;
import com.xiangxue.network.beans.TecentBaseResponse;

import java.util.List;

/**
 * Created by Allen on 2017/7/20.
 * 保留所有版权，未经允许请不要分享到互联网和其他人
 */
public class NewsChannelsBeanExtendsBaseResponse extends TecentBaseResponse {
    @Json(name = "showapi_res_body")
    public ShowapiResBody showapiResBody;

    public static class ChannelList {
        @Json(name = "channelId")
        public String channelId;

        @Json(name = "name")
        public String name;
    }

    public static class ShowapiResBody {
        @Json(name = "totalNum")
        public Integer totalNum;

        @Json(name = "ret_code")
        public Integer retCode;

        @Json(name = "channelList")
        public List<ChannelList> channelList = null;
    }
}
