package app.spidy.idm.interfaces

import app.spidy.idm.data.Snapshot

interface IdmListener {
    fun onDone()
    fun onFinish(snapshot: Snapshot)
    fun onStart(snapshot: Snapshot)
    fun onProgress(snapshot: Snapshot, progress: Int)
    fun onInterrupt(snapshot: Snapshot, e: Exception?)
    fun onFail(snapshot: Snapshot)
}