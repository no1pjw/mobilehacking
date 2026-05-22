package com.example.vulnbank

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CardControlActivity : AppCompatActivity() {
    private lateinit var store: BankStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Session.username == null) return finish()
        store = BankStore(this)
        render()
    }

    private fun render() {
        val username = Session.username ?: return
        val s = store.snapshot(username)
        val merchantInput = EditText(this).apply {
            hint = "가맹점"
            setText("taxi")
        }
        val amountInput = EditText(this).apply {
            hint = "결제 금액"
            setText("14200")
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val limitInput = EditText(this).apply {
            hint = "카드 한도"
            setText(s.cardLimit.toString())
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val resultView = TextView(this).apply {
            text = "이번 달 ${store.money(s.cardSpent)}를 사용했어요."
            textSize = 15f
            setTextColor(Palette.Muted)
        }

        val root = screenRoot().apply {
            addView(title("카드"))
            addView(subtitle("한도와 결제를 관리하세요."))
            addView(card().apply {
                addView(metric("이번 달 사용", store.money(s.cardSpent), Palette.Amber))
                addView(metric("남은 한도", store.money(s.cardLimit - s.cardSpent), Palette.Blue).withTopMargin(this@CardControlActivity, 12))
                addView(metric("포인트", "${store.money(s.rewardPoints)} P", Palette.Green).withTopMargin(this@CardControlActivity, 12))
            })

            addView(section("한도"))
            addView(card().apply {
                addView(limitInput)
                addView(primaryButton("한도 변경").apply {
                    setOnClickListener {
                        store.setCardLimit(username, limitInput.text.toString().toLongOrNull() ?: s.cardLimit)
                        Toast.makeText(this@CardControlActivity, "변경했어요", Toast.LENGTH_SHORT).show()
                        recreate()
                    }
                })
            })

            addView(section("결제"))
            addView(card().apply {
                addView(merchantInput)
                addView(amountInput)
                addView(secondaryButton("결제하기").apply {
                    setOnClickListener {
                        runCatching {
                            store.payCard(username, merchantInput.text.toString().trim(), amountInput.text.toString().toLong())
                        }.onSuccess {
                            resultView.text = "결제가 완료됐어요.\n영수증 $it"
                        }.onFailure {
                            Toast.makeText(this@CardControlActivity, it.message ?: "결제할 수 없어요", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            })

            addView(section("상태"))
            addView(card().apply { addView(resultView) })
            animateChildren()
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }
}
