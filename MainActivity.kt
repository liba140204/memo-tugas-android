package id.memotugas.app

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val dao by lazy { AppDatabase.get(this).taskDao() }
    private lateinit var list: LinearLayout
    private lateinit var summary: TextView
    private var filter = "Semua"
    private var instructionUri: String? = null
    private var resultUri: String? = null
    private var selectedDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
    private var editing: Task? = null
    private var currentItems: List<Task> = emptyList()
    private var instructionLabel: TextView? = null
    private var resultLabel: TextView? = null

    private val pickInstruction = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { persist(it); instructionUri = it.toString(); instructionLabel?.text = fileName(it) }
    }
    private val pickResult = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { persist(it); resultUri = it.toString(); resultLabel?.text = fileName(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        buildScreen()
        lifecycleScope.launch { dao.observeAll().collectLatest { render(it) } }
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(16))
            setBackgroundColor(Color.rgb(247, 248, 252))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val header = TextView(this).apply {
            text = "Memo Tugas\\nCatat, pantau, selesaikan."
            textSize = 25f; setTextColor(Color.rgb(25, 39, 70)); setPadding(0, 0, 0, dp(14))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        content.addView(header)
        summary = TextView(this).apply {
            textSize = 14f; setTextColor(Color.DKGRAY); setPadding(0, 0, 0, dp(14))
        }
        content.addView(summary)
        val add = button("+  Tambah tugas") { showTaskDialog(null) }
        content.addView(add)
        val filters = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(12), 0, dp(10)) }
        listOf("Semua", "Belum", "Selesai").forEach { f ->
            filters.addView(button(f) { filter = f; render(currentItems) },
                LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginEnd = dp(5) })
        }
        content.addView(filters)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(list)
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun render(items: List<Task>) {
        currentItems = items
        list.removeAllViews()
        val done = items.count { it.status == "Selesai" }
        summary.text = "${items.size} tugas  •  ${items.size-done} belum selesai  •  $done selesai"
        val filtered = items.filter {
            when (filter) {
                "Belum" -> it.status != "Selesai"
                "Selesai" -> it.status == "Selesai"
                else -> true
            }
        }
        if (filtered.isEmpty()) {
            list.addView(label("Belum ada tugas. Tekan Tambah tugas untuk memulai.", 15f))
        }
        filtered.forEach { task ->
            val card = MaterialCardView(this).apply {
                radius = dp(14).toFloat(); cardElevation = dp(2).toFloat()
                setCardBackgroundColor(Color.WHITE)
                setContentPadding(dp(14), dp(12), dp(14), dp(12))
            }
            val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            box.addView(label(task.course, 12f, Color.GRAY))
            box.addView(label(task.title, 18f, Color.rgb(25, 39, 70), true))
            val left = daysLeft(task.deadline)
            val deadlineText = when {
                task.status == "Selesai" -> "SELESAI"
                left < 0 -> "TERLAMBAT ${-left} HARI"
                left == 0 -> "DEADLINE HARI INI"
                else -> "H-$left"
            }
            val deadlineColor = when {
                task.status == "Selesai" -> Color.rgb(22, 140, 85)
                left <= 1 -> Color.rgb(205, 65, 45)
                left <= 3 -> Color.rgb(205, 135, 25)
                else -> Color.rgb(40, 125, 85)
            }
            box.addView(label("$deadlineText  •  ${dateText(task.deadline)}", 16f, deadlineColor, true))
            box.addView(label(task.status, 12f, Color.GRAY))
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            actions.addView(button("Detail") { showDetail(task) }, LinearLayout.LayoutParams(0, dp(42), 1f))
            actions.addView(button("Edit") { showTaskDialog(task) }, LinearLayout.LayoutParams(0, dp(42), 1f))
            actions.addView(button("Hapus") {
                android.app.AlertDialog.Builder(this@MainActivity).setMessage("Hapus tugas ini?")
                    .setNegativeButton("Batal", null).setPositiveButton("Hapus") { _, _ ->
                        lifecycleScope.launch { dao.delete(task) }
                    }.show()
            }, LinearLayout.LayoutParams(0, dp(42), 1f))
            box.addView(actions)
            card.addView(box)
            val lp = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
            list.addView(card, lp)
        }
    }

    private fun showTaskDialog(task: Task?) {
        editing = task
        instructionUri = task?.instructionUri
        resultUri = task?.resultUri
        selectedDate = Calendar.getInstance().apply {
            timeInMillis = task?.deadline ?: Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }.timeInMillis
        }
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0) }
        val course = field("Mata kuliah", task?.course ?: "")
        val title = field("Nama tugas", task?.title ?: "")
        val desc = field("Deskripsi (opsional)", task?.description ?: "")
        val dateButton = button("Deadline: ${dateText(selectedDate.timeInMillis)}") {
            DatePickerDialog(this, { _, y, m, d ->
                selectedDate.set(y, m, d, 12, 0, 0)
                dateButtonText?.text = "Deadline: ${dateText(selectedDate.timeInMillis)}"
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }
        dateButtonText = dateButton
        panel.addView(course); panel.addView(title); panel.addView(desc); panel.addView(dateButton)
        panel.addView(label("Lampiran opsional", 13f, Color.GRAY, true))
        instructionLabel = label(if (instructionUri == null) "Belum ada file instruksi" else "File instruksi dipilih", 12f)
        panel.addView(button("Pilih file instruksi") { pickInstruction.launch(arrayOf("*/*")) })
        panel.addView(instructionLabel)
        resultLabel = label(if (resultUri == null) "Belum ada file hasil" else "File hasil dipilih", 12f)
        panel.addView(button("Pilih hasil tugas") { pickResult.launch(arrayOf("*/*")) })
        panel.addView(resultLabel)
        val status = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                listOf("Belum dikerjakan", "Proses", "Selesai"))
            setSelection(listOf("Belum dikerjakan", "Proses", "Selesai").indexOf(task?.status ?: "Belum dikerjakan").coerceAtLeast(0))
        }
        panel.addView(label("Status", 13f, Color.GRAY, true)); panel.addView(status)
        val scroll = ScrollView(this).apply { addView(panel) }
        val dialog = android.app.AlertDialog.Builder(this).setTitle(if (task == null) "Tambah tugas" else "Edit tugas")
            .setView(scroll).setNegativeButton("Batal", null).setPositiveButton("Simpan", null).create()
        dialog.setOnShowListener {
            dialog.getButton(-1).setOnClickListener {
                if (course.text.toString().isBlank() || title.text.toString().isBlank()) {
                    Toast.makeText(this, "Mata kuliah dan nama tugas wajib diisi.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val item = Task(
                    id = task?.id ?: 0,
                    course = course.text.toString().trim(),
                    title = title.text.toString().trim(),
                    description = desc.text.toString().trim(),
                    deadline = selectedDate.timeInMillis,
                    instructionUri = instructionUri,
                    resultUri = resultUri,
                    status = status.selectedItem.toString()
                )
                lifecycleScope.launch {
                    if (task == null) dao.insert(item) else dao.update(item)
                    scheduleReminders(item)
                }
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private var dateButtonText: TextView? = null

    private fun showDetail(task: Task) {
        val msg = buildString {
            append("Mata kuliah: ${task.course}\\n")
            append("Tugas: ${task.title}\\n\\n")
            append("Deskripsi: ${task.description.ifBlank { "—" }}\\n")
            append("Deadline: ${dateText(task.deadline)}\\n")
            append("Status: ${task.status}\\n\\n")
            append("Instruksi: ${task.instructionUri?.let { fileName(Uri.parse(it)) } ?: "Tidak ada"}\\n")
            append("Hasil: ${task.resultUri?.let { fileName(Uri.parse(it)) } ?: "Belum ada"}")
        }
        val builder = android.app.AlertDialog.Builder(this).setTitle("Detail tugas").setMessage(msg)
            .setPositiveButton("Tutup", null)
        if (task.resultUri != null || task.instructionUri != null) {
            builder.setNeutralButton("Buka file") { _, _ ->
                val uri = Uri.parse(task.resultUri ?: task.instructionUri!!)
                try { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, contentResolver.getType(uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                catch (_: Exception) { Toast.makeText(this, "Tidak ada aplikasi untuk membuka file ini.", Toast.LENGTH_SHORT).show() }
            }
        }
        builder.show()
    }

    private fun scheduleReminders(task: Task) {
        if (task.status == "Selesai") return
        val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(3, 1, 0).forEach { days ->
            val reminder = Calendar.getInstance().apply {
                timeInMillis = task.deadline
                add(Calendar.DAY_OF_YEAR, -days)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val at = reminder.timeInMillis
            if (at <= System.currentTimeMillis()) return@forEach
            val intent = Intent(this, DeadlineReceiver::class.java).putExtra("title", task.title).putExtra("days", days)
            val pi = PendingIntent.getBroadcast(this, task.id.toInt() * 10 + days,
                intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            try { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi) } catch (_: SecurityException) { }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 20)
        }
    }

    private fun persist(uri: Uri) {
        try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
    }
    private fun fileName(uri: Uri): String = runCatching {
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME)) else null
        } ?: uri.lastPathSegment ?: "File terpilih"
    }.getOrDefault("File terpilih")

    private fun field(hint: String, value: String) = EditText(this).apply {
        this.hint = hint; setText(value); textSize = 15f; singleLine = hint != "Deskripsi (opsional)"
    }
    private fun label(text: String, size: Float, color: Int = Color.DKGRAY, bold: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color); setPadding(0, dp(5), 0, dp(5))
        if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
    }
    private fun button(text: String, action: () -> Unit) = MaterialButton(this).apply {
        this.text = text; isAllCaps = false; textSize = 13f; setOnClickListener { action() }
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun daysLeft(time: Long): Int {
        val today = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }
        val due = Calendar.getInstance().apply { timeInMillis = time; set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }
        return ((due.timeInMillis - today.timeInMillis) / 86400000L).toInt()
    }
    private fun dateText(time: Long) = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date(time))
}\n