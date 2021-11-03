package com.example.gdrive

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        findViewById<Button>(R.id.gSignIn).setOnClickListener(View.OnClickListener {toActivity(  Intent(this, GoogleSignIn::class.java)) })
        findViewById<Button>(R.id.oneTap).setOnClickListener(View.OnClickListener { toActivity(  Intent(this, OneTap::class.java)) })

    }

    private fun toActivity(intent: Intent) {
        startActivity(intent)
    }


}