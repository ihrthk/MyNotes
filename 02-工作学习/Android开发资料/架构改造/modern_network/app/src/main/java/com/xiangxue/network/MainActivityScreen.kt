package com.xiangxue.network

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainActivityScreen(viewModel: MainActivityScreenViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(20.dp)
            .clickable(true, null, null) {
                viewModel.onEnvironmentClicked()
            }
    ) {
        Button(onClick = {
            viewModel.onGetChannelsClicked()
        }, modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth()) {
            Text(text = "获取带信封的腾讯新闻栏目", fontSize = 22.sp)
        }

        Button(onClick = {
            viewModel.onGetChannelsAndOpenEnvelopeClicked()
        }, modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth()) {
            Text(text = "获取腾讯新闻栏目并打开信封", fontSize = 22.sp)
        }

        Button(onClick = {
            viewModel.onGetChannelsWithKotlinNpeClicked()
        }, modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth()) {
            Text(text = "Moshi的Kotlin空安全", fontSize = 22.sp)
        }

        Button(onClick = {
            viewModel.onHttpbinOrg404Clicked()
        }, modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth()) {
            Text(text = "httpbin.org的404返回", fontSize = 22.sp)
        }

        Button(onClick = {
            viewModel.onHttpbinOrg501Clicked()
        }, modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth()) {
            Text(text = "httpbin.org的501返回", fontSize = 22.sp)
        }
    }
}