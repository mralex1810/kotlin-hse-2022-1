package homework03

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.soywiz.korio.async.async
import com.soywiz.korio.async.launch
import com.sun.jdi.request.InvalidRequestStateException
import homework03.model.CommentsSnapshot
import homework03.model.RedditComment
import homework03.model.TopicSnapshot
import homework03.model.json.*
import kotlinx.coroutines.*


class App {
    suspend fun processTopicsWithComments(topicsName: List<String>) = runBlocking {
        val comments = ArrayList<RedditComment>()
        val topics = ArrayList<RedditThread>()
        topicsName.map {
            launch {
                processTopicWithComments(it, topics, comments)
            }
        }.joinAll()
        FilesWriter.writeTopicsInFiles(topics, comments)
    }

    private suspend fun processTopicWithComments(
        topicName: String, topics: MutableList<RedditThread>, comments: MutableList<RedditComment>
    ) = coroutineScope {
        try {
            val topicSnapshot = getTopic(topicName)
            topics.addAll(topicSnapshot.threads)
            topicSnapshot.threads.map {
                async(Dispatchers.Default) { getComments(it.permalink).comments }
            }.awaitAll().forEach(comments::addAll)
        } catch (e: InvalidRequestStateException) {
            println("Can't process topic $topicName")
        }
    }


    private suspend fun getTopic(name: String): TopicSnapshot {
        val aboutJob = async(Dispatchers.Default) {
            val response = HttpRequestClient.processRequest(RedditJsonUrls.getTopicAboutUrl(name))
            return@async objectMapper.readValue(response, TopicAboutJsonModel::class.java).data
        }
        val threadsJob = async(Dispatchers.Default) {
            val response = HttpRequestClient.processRequest(RedditJsonUrls.getTopicUrl(name))
            return@async objectMapper.readValue(response, CommentsJsonModel::class.java).data.children
                .map { (it as CommentInfoChildKind3).data }
        }
        return TopicSnapshot(aboutJob.await(), threadsJob.await())
    }

    private fun parseComments(
        node: List<CommentInfoChildKind>, list: MutableList<RedditComment>, topicId: String, replyTo: String?
    ) {
        node
            .filter { it.kind == "t1" }
            .map { it as CommentInfoChildKind1 }
            .map { it.data }
            .forEach {
                list.add(RedditComment.fromJson(it, topicId, replyTo))
                if (it.replies != null) parseComments(it.replies.data.children, list, topicId, it.id)
            }
    }


    private suspend fun getComments(link: String): CommentsSnapshot {
        val list = ArrayList<RedditComment>()
        return try {
            val response = HttpRequestClient.processRequest(RedditJsonUrls.getCommentsUrlByLink(link))
            val comments = objectMapper.readerForListOf(CommentsJsonModel::class.java)
                .readValue<List<CommentsJsonModel>>(response)
            val topicId = (comments.first().data.children.first() as CommentInfoChildKind3).data.id
            comments.drop(1).forEach {
                parseComments(it.data.children, list, topicId, null)
            }
            CommentsSnapshot(list)
        } catch (e: InvalidRequestStateException) {
            println("Can't process comments from $link")
            CommentsSnapshot(emptyList())
        }
    }

    companion object {
        private val objectMapper: ObjectMapper =
            jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
    }
}

suspend fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Please, use args <TopicName1> <TopicName2> ... to get info about topics")
    }
    App().processTopicsWithComments(args.toList())
}
