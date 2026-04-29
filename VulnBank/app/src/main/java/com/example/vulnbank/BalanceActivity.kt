package com.example.vulnbank

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BalanceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val username = intent.getStringExtra("username") ?: "unknown"
        val balance = intent.getStringExtra("balance") ?: "0"

        Log.d("VulnBank", "BalanceActivity opened username=$username balance=$balance")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 80, 40, 40)
        }

        root.addView(TextView(this).apply {
            text = "Account Balance"
            textSize = 28f
        })

        root.addView(TextView(this).apply {
            text = "User: $username"
            textSize = 20f
        })

        root.addView(TextView(this).apply {
            text = "Balance: $balance KRW"
            textSize = 20f
        })

        setContentView(root)
    }
}
