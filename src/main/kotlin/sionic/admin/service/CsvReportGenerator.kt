package sionic.admin.service

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.springframework.stereotype.Component
import sionic.chat.domain.Chat
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.stream.Stream

@Component
class CsvReportGenerator {

    fun generate(chats: Stream<Chat>, outputStream: OutputStream) {
        val writer = OutputStreamWriter(outputStream, Charsets.UTF_8)

        val csvFormat = CSVFormat.DEFAULT.builder()
            .setHeader("Chat ID", "Thread ID", "User ID", "User Name", "User Email", "Question", "Answer", "Created At")
            .build()

        CSVPrinter(writer, csvFormat).use { printer ->
            chats.forEach { chat ->
                printer.printRecord(
                    chat.id,
                    chat.thread.id,
                    chat.thread.user.id,
                    chat.thread.user.name,
                    chat.thread.user.email.value,
                    chat.question.value,
                    chat.answer.value,
                    chat.createdAt.toString()
                )
            }
        }
    }
}
