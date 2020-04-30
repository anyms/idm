package app.spidy.idm.interfaces

import app.spidy.idm.data.Snapshot

interface CopyListener {
    fun onCopy(snapshot: Snapshot, progress: Int)
    fun onCopied(snapshot: Snapshot)
    fun onCopyError(e: Exception, snapshot: Snapshot)
}