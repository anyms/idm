package app.spidy.idm.interfaces

import app.spidy.idm.models.Snapshot

interface IDMListener {
    fun onPrepare()
    fun onStart(snapshot: Snapshot)
    fun onProgress(snapshot: Snapshot)
    fun onFail(snapshot: Snapshot)
    fun onPause(snapshot: Snapshot)
    fun onFinish(snapshot: Snapshot)
    fun onSpeed(snapshot: Snapshot)
    fun onFail(code: Int) {}

    fun onFetchStream(count: Int, total: Int) {}
}