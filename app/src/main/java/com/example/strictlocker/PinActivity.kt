package com.example.strictlocker

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * Demo PIN screen only.
 * Android device owners remain able to control and uninstall this app.
 */
class PinActivity : Activity() {
    private val demoPin = "1234"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
            gravity = Gravity.CENTER
        }

        val title = TextView(this).apply {
            text = "Strict Locker\nأدخل رمز PIN التجريبي"
            textSize = 22f
            gravity = Gravity.CENTER
        }

        val input = EditText(this).apply {
            hint = "رمز PIN (التجريبي: 1234)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            gravity = Gravity.CENTER
        }

        val unlockButton = Button(this).apply {
            text = "تحقق من الرمز"
            setOnClickListener {
                val message = if (input.text.toString() == demoPin) {
                    "تم قبول الرمز التجريبي"
                } else {
                    "الرمز غير صحيح"
                }
                Toast.makeText(this@PinActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        val info = TextView(this).apply {
            text = "هذا نموذج تجريبي لقفل داخل التطبيق فقط، ولا يمنع إلغاء تثبيت التطبيق أو إيقافه من إعدادات Android."
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }

        layout.addView(title)
        layout.addView(input)
        layout.addView(unlockButton)
        layout.addView(info)
        setContentView(layout)
    }
}
