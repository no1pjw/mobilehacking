package com.example.vulnbank

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TransferActivity : AppCompatActivity() {
    private lateinit var store: BankStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Session.username == null) return finish()
        store = BankStore(this)
        render()
    }

    private fun render() {
        val username = Session.username ?: return
        val targetInput = EditText(this).apply {
            hint = "받는 사람 아이디"
            setText("coffee")
        }
        val amountInput = EditText(this).apply {
            hint = "금액"
            setText("18000")
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val memoInput = EditText(this).apply {
            hint = "메모"
            setText("커피")
        }
        val aliasInput = EditText(this).apply {
            hint = "즐겨찾기 이름"
            setText("자주 가는 카페")
        }
        val resultView = TextView(this).apply {
            text = "받는 사람과 금액을 확인해주세요."
            textSize = 15f
            setTextColor(Palette.Muted)
        }

        val root = screenRoot().apply {
            addView(title("송금"))
            addView(subtitle("수수료는 10만원 미만 100원, 그 이상은 무료예요."))
            addView(card().apply {
                addView(targetInput)
                addView(amountInput)
                addView(memoInput)
                addView(primaryButton("보내기").apply {
                    setOnClickListener {
                        runCatching {
                            store.sendMoney(
                                username,
                                targetInput.text.toString().trim(),
                                amountInput.text.toString().toLong(),
                                memoInput.text.toString()
                            )
                        }.onSuccess {
                            resultView.text = "송금이 완료됐어요.\n영수증 $it"
                        }.onFailure {
                            Toast.makeText(this@TransferActivity, it.message ?: "송금할 수 없어요", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.withTopMargin(this@TransferActivity, 8))
            })

            addView(section("즐겨찾기"))
            addView(card().apply {
                addView(aliasInput)
                addView(secondaryButton("즐겨찾기에 추가").apply {
                    setOnClickListener {
                        val ok = store.addFavorite(username, targetInput.text.toString().trim(), aliasInput.text.toString())
                        Toast.makeText(this@TransferActivity, if (ok) "추가했어요" else "추가할 수 없어요", Toast.LENGTH_SHORT).show()
                    }
                })
                val favorites = store.favorites(username)
                addView(TextView(this@TransferActivity).apply {
                    text = if (favorites.isEmpty()) "즐겨찾기가 없어요." else favorites.joinToString("\n") { "${it.second} (${it.first})" }
                    textSize = 14f
                    setTextColor(Palette.Muted)
                    setPadding(0, dp(10), 0, 0)
                })
            })

            addView(section("결과"))
            addView(card().apply { addView(resultView) })
            animateChildren()
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }
}
