package com.xiangxue.network

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mainActivityScreenViewModel =
            ViewModelProvider(this).get(MainActivityScreenViewModel::class.java)
        setContent {
            MainActivityScreen(mainActivityScreenViewModel)
        }
    }
}