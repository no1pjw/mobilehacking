package com.example.vulnbank

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.widget.*
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val adminUser = "admin"
    private val adminPassword = "admin123"
    private val apiKey = "BANK_API_KEY_SUPER_SECRET_12345"
    private lateinit var db: SQLiteDatabase
    private lateinit var resultView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = openOrCreateDatabase("vulnbank.db", MODE_PRIVATE, null)
        setupDatabase()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 70, 36, 36)
        }

        val title = TextView(this).apply {
            text = "VulnBank"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Deliberately vulnerable mobile lab"
            textSize = 14f
            gravity = Gravity.CENTER
        }

        val usernameInput = EditText(this).apply {
            hint = "Username"
            setText("admin")
        }

        val passwordInput = EditText(this).apply {
            hint = "Password"
            setText("admin123")
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val loginButton = Button(this).apply { text = "Login" }

        val searchInput = EditText(this).apply {
            hint = "Search user by name"
        }

        val searchButton = Button(this).apply { text = "Search User" }

        val sqliHint = TextView(this).apply {
            text = "Try search: ' OR '1'='1"
            textSize = 12f
        }

        resultView = TextView(this).apply {
            text = "Search results will appear here."
            textSize = 15f
            setPadding(0, 20, 0, 20)
        }

        val webButton = Button(this).apply { text = "Open Notice WebView" }

        root.addView(title)
        root.addView(subtitle)
        root.addView(usernameInput)
        root.addView(passwordInput)
        root.addView(loginButton)
        root.addView(searchInput)
        root.addView(searchButton)
        root.addView(sqliHint)
        root.addView(resultView)
        root.addView(webButton)

        setContentView(root)

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()

            Log.d("VulnBank", "Login attempt username=$username password=$password apiKey=$apiKey")

            getSharedPreferences("login_data", MODE_PRIVATE).edit()
                .putString("username", username)
                .putString("password", password)
                .putString("apiKey", apiKey)
                .apply()

            if (username == adminUser && password == adminPassword) {
                Toast.makeText(this, "Login success", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, BalanceActivity::class.java).apply {
                    putExtra("username", username)
                    putExtra("balance", "10000000")
                })
            } else {
                Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
            }
        }

        searchButton.setOnClickListener {
            val keyword = searchInput.text.toString()
            val query = "SELECT username, balance FROM users WHERE username = '$keyword'"
            Log.d("VulnBank", "Executing SQL: $query")

            try {
                val cursor = db.rawQuery(query, null)
                val result = StringBuilder()
                while (cursor.moveToNext()) {
                    result.append("User: ")
                        .append(cursor.getString(0))
                        .append(", Balance: ")
                        .append(cursor.getString(1))
                        .append("\n")
                }
                cursor.close()
                resultView.text = if (result.isEmpty()) "No result" else result.toString()
            } catch (e: Exception) {
                resultView.text = "SQL Error: ${e.message}"
                Log.e("VulnBank", "SQL error", e)
            }
        }

        webButton.setOnClickListener {
            startActivity(Intent(this, WebActivity::class.java).apply {
                putExtra("url", "http://example.com")
            })
        }
    }

    private fun setupDatabase() {
        db.execSQL("CREATE TABLE IF NOT EXISTS users (username TEXT, password TEXT, balance TEXT)")
        db.execSQL("DELETE FROM users")
        db.execSQL("INSERT INTO users VALUES ('admin', 'admin123', '10000000')")
        db.execSQL("INSERT INTO users VALUES ('alice', 'alicepass', '50000')")
        db.execSQL("INSERT INTO users VALUES ('bob', 'bobpass', '30000')")
    }
}
