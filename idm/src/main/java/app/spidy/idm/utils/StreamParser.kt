package app.spidy.idm.utils

class StreamParser(private val url: String) {
    private var baseUrl: String? = null
    private val streams = ArrayList<String>()

    fun parse(content: String): ArrayList<String> {
        val streamsNodes = ArrayList<String>()
        val lines = content.split("\n")

        for (line in lines) {
            if (line.trim() != "" && !line.startsWith("#")) {
                streamsNodes.add(line)
            }
        }

        val tmpStream = streamsNodes[0]
        if (tmpStream.startsWith("/")) {
            val nodes = url.split("://")
            val protocol = nodes[0]
            baseUrl = "$protocol://${nodes[1].split("/")[0]}"
        } else if (!tmpStream.startsWith("https://") && !tmpStream.startsWith("http://")) {
            val nodes = url.split("?")[0].split("://")
            val protocol = nodes[0]
            val peaces = nodes[1].split("/") as ArrayList
            peaces.removeAt(peaces.lastIndex)
            baseUrl = "$protocol://${peaces.joinToString("/")}"
        }

        for (node in streamsNodes) {
            val url = if (!baseUrl!!.endsWith("/") && !node.startsWith("/")) {
                "$baseUrl/$node"
            } else {
                "$baseUrl$node"
            }
            streams.add(url)
        }

        return streams
    }
}

