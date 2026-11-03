package id.memotugas.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class DeadlineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Tugas kuliah"
        val days = intent.getIntExtra("days", 0)
        val channelId = "deadline_tasks"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Pengingat deadline", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val message = when (days) {
            3 -> "Deadline 3 hari lagi. Yuk mulai kerjakan!"
            1 -> "Deadline besok. Pastikan tugas siap dikumpulkan."
            else -> "Deadline tugas hari ini!"
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification) }
        catch (_: SecurityException) { }
    }
}
