package homework03

import com.soywiz.korio.file.std.toVfs
import homework03.model.RedditComment
import homework03.model.json.RedditThread
import java.io.File

object FilesWriter {
    const val SUBJECTS_CSV = "subjects.csv"
    const val COMMENTS_CSV = "comments.csv"
    suspend fun writeTopicsInFiles(topics: List<RedditThread>, comments: List<RedditComment>) {
        File(SUBJECTS_CSV).toVfs().writeString(CsvSerializer.csvSerialize(topics, RedditThread::class))
        File(COMMENTS_CSV).toVfs().writeString(CsvSerializer.csvSerialize(comments, RedditComment::class))
    }
}