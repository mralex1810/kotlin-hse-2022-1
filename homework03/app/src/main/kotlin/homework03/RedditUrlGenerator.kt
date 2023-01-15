package homework03

object RedditJsonUrls {
    private const val REDDIT = "https://www.reddit.com"
    private const val JSON = ".json"
    fun getCommentsUrlByLink(link: String) = "${REDDIT}${link}${JSON}"
    fun getTopicAboutUrl(topic: String) = "$REDDIT/r/$topic/about$JSON"
    fun getTopicUrl(topic: String) = "$REDDIT/r/$topic/$JSON"
}