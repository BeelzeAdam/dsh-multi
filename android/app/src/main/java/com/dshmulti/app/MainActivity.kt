package com.dshmulti.app

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var hasContent = false

    private fun dp(v: Int) = Ui.dp(this, v)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Ui.BG
        window.navigationBarColor = Ui.BG

        seed()
        if (CoreStore.autoOpen(this)) {
            val last = CoreStore.last(this)
            if (last != null) {
                openWeb(last)
                return
            }
        }
        render()
    }

    override fun onResume() {
        super.onResume()
        if (hasContent) render()
    }

    private fun seed() {
        if (CoreStore.load(this).isEmpty()) {
            CoreStore.save(
                this,
                listOf(
                    Core("本机 DSH", "http://127.0.0.1:3080"),
                    Core("电脑（改地址）", "http://192.168.1.5:3080")
                )
            )
        }
    }

    private fun openWeb(url: String) {
        startActivity(Intent(this, WebActivity::class.java).putExtra(WebActivity.EXTRA_URL, url))
    }

    private fun render() {
        hasContent = true
        val cores = CoreStore.load(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
            fitsSystemWindows = true
        }

        // 标题栏
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(18), dp(10), dp(4))
        }
        header.addView(Ui.text(this, "DSH 核心", 24f, Ui.TEXT, true))
        header.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        header.addView(Ui.text(this, "＋", 26f, Ui.ACCENT, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(2), dp(14), dp(6))
            setOnClickListener { editCore(null) }
        })
        root.addView(header)

        root.addView(Ui.text(this, "点一下直接进入 · 长按编辑或删除", 12f, Ui.SUB).apply {
            setPadding(dp(20), 0, dp(20), dp(12))
        })

        // 列表
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), dp(16))
        }
        if (cores.isEmpty()) {
            list.addView(Ui.text(this, "还没有核心。点右上角 ＋ 添加一个地址，\n例如本机 http://127.0.0.1:3080", 14f, Ui.SUB))
        }
        cores.forEach { list.addView(card(it)) }

        root.addView(
            ScrollView(this).apply {
                addView(list)
                clipToPadding = false
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        // 底部开关
        root.addView(CheckBox(this).apply {
            text = "启动时直接进入上次的核心"
            textSize = 13f
            setTextColor(Ui.SUB)
            isChecked = CoreStore.autoOpen(this@MainActivity)
            buttonTintList = android.content.res.ColorStateList.valueOf(Ui.ACCENT)
            setPadding(dp(14), dp(4), dp(14), dp(10))
            setOnCheckedChangeListener { _, v -> CoreStore.setAutoOpen(this@MainActivity, v) }
        })

        setContentView(root)
    }

    private fun card(core: Core): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.round(this@MainActivity, 14, Ui.CARD, Ui.STROKE, 1)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            isClickable = true
            setOnClickListener { openWeb(core.url) }
            setOnLongClickListener { coreMenu(core); true }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(Ui.text(this, core.name, 16f, Ui.TEXT, true))
        row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        if (CoreStore.last(this) == core.url) {
            row.addView(Ui.text(this, "上次使用", 11f, Ui.OK))
        }
        box.addView(row)

        box.addView(Ui.text(this, CoreStore.pretty(core.url), 12f, Ui.SUB).apply {
            setPadding(0, dp(5), 0, 0)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        })
        return box
    }

    private fun coreMenu(core: Core) {
        AlertDialog.Builder(this)
            .setTitle(core.name)
            .setItems(arrayOf("编辑", "删除", "设为启动默认")) { _, which ->
                when (which) {
                    0 -> editCore(core)
                    1 -> confirmDelete(core)
                    2 -> {
                        CoreStore.setLast(this, core.url)
                        CoreStore.setAutoOpen(this, true)
                        Toast.makeText(this, "下次打开直接进入 ${core.name}", Toast.LENGTH_SHORT).show()
                        render()
                    }
                }
            }
            .show()
    }

    private fun confirmDelete(core: Core) {
        AlertDialog.Builder(this)
            .setMessage("删除核心「${core.name}」？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                CoreStore.remove(this, core.url)
                render()
            }
            .show()
    }

    private fun editCore(core: Core?) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(0))
        }

        val nameInput = EditText(this).apply {
            hint = "名字，比如 本机 / 电脑 / 服务器"
            setText(core?.name ?: "")
            setTextColor(Ui.TEXT)
            setHintTextColor(Ui.SUB)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val urlInput = EditText(this).apply {
            hint = "地址，比如 127.0.0.1:3080"
            setText(core?.url ?: "")
            setTextColor(Ui.TEXT)
            setHintTextColor(Ui.SUB)
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine()
        }

        container.addView(nameInput)
        container.addView(urlInput)
        container.addView(Ui.text(this, "不写 http:// 会自动补上", 12f, Ui.SUB).apply {
            setPadding(0, dp(2), 0, dp(8))
        })

        AlertDialog.Builder(this)
            .setTitle(if (core == null) "添加核心" else "编辑核心")
            .setView(container)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val url = CoreStore.normalize(urlInput.text.toString())
                if (url.isBlank()) {
                    Toast.makeText(this, "地址不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val name = nameInput.text.toString().trim().ifBlank { CoreStore.pretty(url) }
                if (core != null && core.url != url) CoreStore.remove(this, core.url)
                CoreStore.upsert(this, Core(name, url))
                render()
            }
            .show()
    }
}
