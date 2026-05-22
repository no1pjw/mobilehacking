package com.example.vulnbank

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var store: BankStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = BankStore(this)

        val idInput = EditText(this).apply {
            hint = "아이디"
            setText("jiyun")
        }
        val nameInput = EditText(this).apply {
            hint = "이름"
            setText("지윤")
        }
        val pinInput = EditText(this).apply {
            hint = "간편 비밀번호"
            setText("1234")
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        val root = screenRoot().apply {
            addView(TextView(this@MainActivity).apply {
                text = "VulnBank"
                textSize = 40f
                setTextColor(Palette.Ink)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(0, dp(36), 0, dp(6))
            })
            addView(subtitle("매일 쓰는 돈 관리, 더 가볍게"))

            addView(card().apply {
                addView(TextView(this@MainActivity).apply {
                    text = "시작하기"
                    textSize = 22f
                    setTextColor(Palette.Ink)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                })
                addView(idInput.withTopMargin(this@MainActivity, 12))
                addView(nameInput)
                addView(pinInput)
                addView(primaryButton("로그인").apply {
                    setOnClickListener {
                        val id = idInput.text.toString()
                        val pin = pinInput.text.toString()
                        if (store.login(id, pin)) {
                            Session.username = id.trim().lowercase()
                            startActivity(Intent(this@MainActivity, BalanceActivity::class.java))
                        } else {
                            Toast.makeText(this@MainActivity, "아이디나 비밀번호를 확인해주세요", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.withTopMargin(this@MainActivity, 10))
                addView(secondaryButton("새 계좌 만들기").apply {
                    setOnClickListener {
                        val ok = store.createProfile(
                            idInput.text.toString(),
                            nameInput.text.toString(),
                            pinInput.text.toString()
                        )
                        Toast.makeText(
                            this@MainActivity,
                            if (ok) "계좌가 만들어졌어요. 로그인해주세요." else "다른 아이디를 사용해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            })

            addView(section("오늘의 금융"))
            addView(card().apply {
                addView(TextView(this@MainActivity).apply {
                    text = "송금, 카드, 저축 목표를 한 곳에서 확인하세요."
                    textSize = 16f
                    setTextColor(Palette.Muted)
                })
            })
            animateChildren()
        }

        setContentView(ScrollView(this).apply { addView(root) })
    }
}
