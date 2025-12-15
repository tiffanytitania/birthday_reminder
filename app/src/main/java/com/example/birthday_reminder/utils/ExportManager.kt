package com.example.birthday_reminder.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.birthday_reminder.data.model.Member
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object ExportManager {

    /**
     * Export birthday list ke CSV
     */
    fun exportBirthdaysToCSV(context: Context, members: List<Member>): File? {
        return try {
            val fileName = "birthday_list_${getTimestamp()}.csv"
            val file = File(context.getExternalFilesDir(null), fileName)

            FileWriter(file).use { writer ->
                // Header
                writer.append("Nama,Tanggal Lahir,Usia,Nomor HP,Email,Role\n")

                // Data
                members.forEach { member ->
                    writer.append("${member.name},")
                    writer.append("${member.birthDate},")
                    writer.append("${member.getAge() ?: "-"},")
                    writer.append("${member.phone},")
                    writer.append("${member.email},")
                    writer.append("${member.role}\n")
                }
            }

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Export contacts ke CSV
     */
    fun exportContactsToCSV(context: Context, members: List<Member>): File? {
        return try {
            val fileName = "contacts_${getTimestamp()}.csv"
            val file = File(context.getExternalFilesDir(null), fileName)

            FileWriter(file).use { writer ->
                // Header
                writer.append("Nama,Nomor HP,Email,Tanggal Lahir\n")

                // Data (only members with phone or email)
                members.filter { it.phone.isNotEmpty() || it.email.isNotEmpty() }
                    .forEach { member ->
                        writer.append("${member.name},")
                        writer.append("${member.phone},")
                        writer.append("${member.email},")
                        writer.append("${member.birthDate}\n")
                    }
            }

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Export to text format (simple & readable)
     */
    fun exportToText(context: Context, members: List<Member>, type: ExportType): File? {
        return try {
            val fileName = when (type) {
                ExportType.BIRTHDAYS -> "birthday_list_${getTimestamp()}.txt"
                ExportType.CONTACTS -> "contacts_${getTimestamp()}.txt"
            }
            val file = File(context.getExternalFilesDir(null), fileName)

            FileWriter(file).use { writer ->
                writer.append("=" * 50 + "\n")
                writer.append("BIRTHDAY REMINDER - ${type.name}\n")
                writer.append("Exported: ${getCurrentDate()}\n")
                writer.append("=" * 50 + "\n\n")

                members.forEach { member ->
                    writer.append("Nama: ${member.name}\n")
                    if (type == ExportType.BIRTHDAYS) {
                        writer.append("Tanggal Lahir: ${member.getFormattedBirthDate()}\n")
                        member.getAge()?.let { writer.append("Usia: $it tahun\n") }
                    }
                    if (member.phone.isNotEmpty()) {
                        writer.append("HP: ${member.phone}\n")
                    }
                    if (member.email.isNotEmpty()) {
                        writer.append("Email: ${member.email}\n")
                    }
                    writer.append("-" * 50 + "\n\n")
                }

                writer.append("\nTotal: ${members.size} anggota\n")
            }

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Share file via any app
     */
    fun shareFile(context: Context, file: File, title: String = "Bagikan Data") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = when {
                    file.name.endsWith(".csv") -> "text/csv"
                    file.name.endsWith(".txt") -> "text/plain"
                    else -> "*/*"
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getTimestamp(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
        return sdf.format(Date())
    }

    private operator fun String.times(n: Int): String = this.repeat(n)

    enum class ExportType {
        BIRTHDAYS,
        CONTACTS
    }
}