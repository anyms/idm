package app.spidy.idm.interfaces

interface CopyListener {
    fun onCopy(progress: Int)
    fun onCopied()
    fun onCopyError(e: Exception)
}