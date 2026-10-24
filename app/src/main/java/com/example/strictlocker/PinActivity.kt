package com.example.strictlocker

import android.app.Activity
import android.os.Bundle
import android.os.SystemClock
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * A local, in-app PIN gate. This does not lock Android Settings or prevent
 * the device owner from stopping or uninstalling the app.
 */
class PinActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("strict_locker_prefs", MODE_PRIVATE) }
    private val bg = Color.rgb(10, 13, 16)
    private val panel = Color.rgb(22, 28, 32)
    private val green = Color.rgb(164, 255, 0)
    private val muted = Color.rgb(165, 177, 181)
    private var mode = "entry"
    private var stage = 0
    private var firstEntry = ""
    private lateinit var root: LinearLayout
    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var input: EditText
    private lateinit var primary: Button
    private lateinit var secondary: Button
    private lateinit var message: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        mode = if (prefs.getString("pin_hash", null) == null) "setup" else "entry"
        render()
    }

    private fun render() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(26), dp(28), dp(26), dp(24))
            setBackgroundColor(bg)
        }

        val brand = TextView(this).apply {
            text = "S / L"
            textSize = 15f
            letterSpacing = 0.24f
            setTextColor(green)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        val brandWrap = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = shape(panel, 18)
            addView(brand, LinearLayout.LayoutParams(dp(76), dp(64)))
        }
        root.addView(brandWrap, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(28) })

        title = TextView(this).apply {
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        subtitle = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(muted)
        }
        root.addView(subtitle, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(26) })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(18))
            background = shape(panel, 22)
        }
        input = EditText(this).apply {
            hint = "أدخل 4 إلى 8 أرقام"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(android.text.InputFilter.LengthFilter(8))
            background = shape(Color.rgb(32, 40, 44), 14)
            setPadding(dp(12), dp(14), dp(12), dp(14))
        }
        card.addView(input, LinearLayout.LayoutParams(-1, dp(56)).apply { bottomMargin = dp(14) })

        message = TextView(this).apply {
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(muted)
        }
        card.addView(message, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })

        primary = makeButton("متابعة", true)
        card.addView(primary, LinearLayout.LayoutParams(-1, dp(52)))
        secondary = makeButton("رجوع", false)
        card.addView(secondary, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8) })
        root.addView(card, LinearLayout.LayoutParams(-1, -2))

        val footer = TextView(this).apply {
            text = "حماية داخل التطبيق • تخزين محلي على الجهاز"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(muted)
        }
        root.addView(footer, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) })
        setContentView(root)
        updateScreen()
    }

    private fun updateScreen() {
        when (mode) {
            "setup" -> {
                title.text = if (stage == 0) "أنشئ رمزك الخاص" else "تأكيد الرمز"
                subtitle.text = if (stage == 0) "اختر رمزًا من 4 إلى 8 أرقام لا يعرفه غيرك." else "أعد إدخال الرمز نفسه للتأكد."
                input.hint = if (stage == 0) "رمز جديد" else "أعد كتابة الرمز"
                primary.text = if (stage == 0) "التالي" else "حفظ الرمز"
                secondary.visibility = if (stage == 0) View.GONE else View.VISIBLE
                message.text = "لا يوجد رمز افتراضي."
                primary.setOnClickListener { setupPin() }
                secondary.setOnClickListener { stage = 0; firstEntry = ""; input.setText(""); updateScreen() }
            }
            "entry" -> {
                title.text = "أهلًا بعودتك"
                subtitle.text = "أدخل رمز PIN لفتح لوحة التطبيق."
                input.hint = "رمز PIN"
                primary.text = "فتح التطبيق"
                secondary.visibility = View.GONE
                message.text = lockoutMessage()
                primary.setOnClickListener { unlock() }
            }
            "home" -> showHome()
            "change-old" -> {
                title.text = "تغيير رمز PIN"
                subtitle.text = "أدخل الرمز الحالي أولًا."
                input.hint = "الرمز الحالي"
                primary.text = "تحقق"
                secondary.visibility = View.VISIBLE
                message.text = ""
                primary.setOnClickListener {
                    if (verify(input.text.toString())) {
                        mode = "change-new"; input.setText(""); updateScreen()
                    } else wrongPin()
                }
                secondary.setOnClickListener { mode = "home"; input.setText(""); updateScreen() }
            }
            "change-new" -> {
                title.text = if (stage == 0) "رمز جديد" else "تأكيد الرمز الجديد"
                subtitle.text = if (stage == 0) "اختر من 4 إلى 8 أرقام." else "أعد إدخال الرمز الجديد."
                input.hint = "رمز PIN جديد"
                primary.text = if (stage == 0) "التالي" else "حفظ التغيير"
                secondary.visibility = View.VISIBLE
                message.text = ""
                primary.setOnClickListener { changePin() }
                secondary.setOnClickListener { mode = "home"; stage = 0; firstEntry = ""; input.setText(""); updateScreen() }
            }
        }
    }

    private fun setupPin() {
        val value = input.text.toString()
        if (!validPin(value)) { message.text = "استخدم 4 إلى 8 أرقام."; return }
        if (stage == 0) {
            firstEntry = value; stage = 1; input.setText(""); updateScreen()
        } else if (value != firstEntry) {
            firstEntry = ""; stage = 0; input.setText(""); message.text = "الرمزان غير متطابقين. حاول مجددًا."; updateScreen()
        } else {
            savePin(value); mode = "home"; stage = 0; input.setText(""); updateScreen()
            Toast.makeText(this, "تم إعداد رمز PIN بنجاح", Toast.LENGTH_SHORT).show()
        }
    }

    private fun unlock() {
        val wait = remainingLockout()
        if (wait > 0) { message.text = "انتظر $wait ثانية قبل المحاولة مجددًا."; return }
        if (verify(input.text.toString())) {
            prefs.edit().putInt("failed_count", 0).putLong("lock_until", 0L).apply()
            mode = "home"; input.setText(""); updateScreen()
        } else {
            wrongPin()
        }
    }

    private fun wrongPin() {
        val count = prefs.getInt("failed_count", 0) + 1
        val delay = when {
            count >= 12 -> 300
            count >= 8 -> 120
            count >= 5 -> 30
            else -> 0
        }
        val until = if (delay > 0) System.currentTimeMillis() + delay * 1000L else 0L
        prefs.edit().putInt("failed_count", count).putLong("lock_until", until).apply()
        input.setText("")
        message.text = if (delay > 0) "محاولات كثيرة. انتظر $delay ثانية." else "الرمز غير صحيح. المحاولات الخاطئة: $count"
    }

    private fun changePin() {
        val value = input.text.toString()
        if (!validPin(value)) { message.text = "استخدم 4 إلى 8 أرقام."; return }
        if (stage == 0) {
            firstEntry = value; stage = 1; input.setText(""); updateScreen()
        } else if (value != firstEntry) {
            firstEntry = ""; stage = 0; input.setText(""); updateScreen(); message.text = "الرمزان غير متطابقين."
        } else {
            savePin(value); mode = "home"; stage = 0; firstEntry = ""; input.setText(""); updateScreen()
            Toast.makeText(this, "تم تغيير الرمز", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showHome() {
        title.text = "تم فتح التطبيق"
        subtitle.text = "لوحة Strict Locker"
        input.visibility = View.GONE
        message.text = "رمزك محفوظ محليًا، ولا يوجد رمز افتراضي."
        primary.text = "قفل الشاشة"
        secondary.visibility = View.VISIBLE
        secondary.text = "تغيير رمز PIN"
        primary.setOnClickListener { mode = "entry"; input.visibility = View.VISIBLE; input.setText(""); updateScreen() }
        secondary.setOnClickListener { mode = "change-old"; input.visibility = View.VISIBLE; input.setText(""); updateScreen() }
        val note = TextView(this).apply {
            text = "تنبيه: هذا القفل يعمل داخل التطبيق فقط. يمكن لمالك الجهاز إيقاف التطبيق أو إزالة تثبيته من إعدادات Android."
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(muted)
            setPadding(dp(8), dp(16), dp(8), 0)
        }
        // Add the note only once for this rendered screen.
        if (root.childCount < 4) root.addView(note)
    }

    private fun validPin(value: String) = value.length in 4..8 && value.all { it in '0'..'9' }

    private fun savePin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString("pin_salt", salt.joinToString("") { "%02x".format(it) })
            .putString("pin_hash", hash(pin, salt))
            .apply()
    }

    private fun verify(pin: String): Boolean {
        val saltHex = prefs.getString("pin_salt", null) ?: return false
        val stored = prefs.getString("pin_hash", null) ?: return false
        val salt = hexToBytes(saltHex)
        return MessageDigest.isEqual(stored.toByteArray(), hash(pin, salt).toByteArray())
    }

    private fun hash(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(pin.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun hexToBytes(hex: String): ByteArray = ByteArray(hex.length / 2) { i ->
        hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }

    private fun remainingLockout(): Int {
        val until = prefs.getLong("lock_until", 0L)
        return (((until - System.currentTimeMillis()).coerceAtLeast(0L)) / 1000L).toInt().let {
            if (until > System.currentTimeMillis()) it + 1 else 0
        }
    }

    private fun lockoutMessage(): String {
        val wait = remainingLockout()
        return if (wait > 0) "المحاولات متوقفة مؤقتًا: $wait ثانية." else "بعد 5 محاولات خاطئة، يتفعّل انتظار مؤقت."
    }

    private fun makeButton(label: String, filled: Boolean) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (filled) Color.BLACK else green)
        background = shape(if (filled) green else Color.rgb(32, 40, 44), 14)
    }

    private fun shape(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
