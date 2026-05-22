package com.example.vulnbank

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class BalanceActivity : AppCompatActivity() {
    private lateinit var store: BankStore
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Session.username == null) return finish()
        store = BankStore(this)
        root = screenRoot()
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val username = Session.username ?: return
        val s = store.snapshot(username)
        root.removeAllViews()

        root.addView(subtitle("${s.displayName}님, 오늘도 좋은 하루예요"))
        root.addView(TextView(this).apply {
            text = store.money(s.balance)
            textSize = 38f
            setTextColor(Palette.Ink)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(18))
        })

        root.addView(card().apply {
            addView(metric("VulnBank 통장", s.accountNo, Palette.Ink))
            addView(secondaryButton("계좌번호 복사").apply {
                setOnClickListener {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("account", store.copyPayload(username)))
                    Toast.makeText(this@BalanceActivity, "복사했어요", Toast.LENGTH_SHORT).show()
                }
            }.withTopMargin(this@BalanceActivity, 8))
        })

        root.addView(card().apply {
            orientation = LinearLayout.HORIZONTAL
            addView(primaryButton("송금").apply {
                setOnClickListener { startActivity(Intent(this@BalanceActivity, TransferActivity::class.java)) }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(secondaryButton("카드").apply {
                setOnClickListener { startActivity(Intent(this@BalanceActivity, CardControlActivity::class.java)) }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(8), 0, 0, 0)
                }
            })
        })

        root.addView(section("소비와 목표"))
        root.addView(card().apply {
            addView(metric("이번 달 쓴 금액", store.money(s.monthlySpent), Palette.Red))
            addView(metric("카드 사용", "${store.money(s.cardSpent)} / ${store.money(s.cardLimit)}", Palette.Amber).withTopMargin(this@BalanceActivity, 12))
            addView(metric("저축 목표", "${store.money(s.saved)} / ${store.money(s.savingGoal)}", Palette.Green).withTopMargin(this@BalanceActivity, 12))
        })

        root.addView(section("최근 내역"))
        root.addView(card().apply {
            val rows = store.timeline(username)
            addView(TextView(this@BalanceActivity).apply {
                text = if (rows.isEmpty()) {
                    "아직 거래내역이 없어요."
                } else {
                    rows.joinToString("\n\n") {
                        val sign = if (it.direction == "OUT") "-" else "+"
                        "${it.title}\n${store.date(it.createdAt)}  $sign${store.money(it.amount)}"
                    }
                }
                textSize = 15f
                setTextColor(Palette.Muted)
            })
        })

        root.addView(section("더보기"))
        root.addView(card().apply {
            addView(secondaryButton("고객센터").apply {
                setOnClickListener { startActivity(Intent(this@BalanceActivity, HybridSupportActivity::class.java)) }
            })
            addView(secondaryButton("공지사항").apply {
                setOnClickListener { startActivity(Intent(this@BalanceActivity, WebActivity::class.java)) }
            })
        })
        root.animateChildren()
    }
}
