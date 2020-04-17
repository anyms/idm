package app.spidy.idm.interfaces

import app.spidy.idm.data.Snapshot
import java.lang.Exception

interface IdmListener {
    fun onStart(snapshot: Snapshot)
    fun onProgress(snapshot: Snapshot)
    fun onComplete(snapshot: Snapshot)
    fun onFail(snapshot: Snapshot)
    fun onPause(snapshot: Snapshot)
    fun onResume(snapshot: Snapshot)
    fun onError(e: Exception, uId: String)
    fun onCopy(progress: Int)
    fun onCopied(snapshot: Snapshot)
    fun onCopyError(e: Exception, snapshot: Snapshot)
    fun onInit(uId: String, message: String)
}