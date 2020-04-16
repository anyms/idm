package app.spidy.idm.interfaces

import app.spidy.idm.data.Detect

interface DetectListener {
    fun onDetect(detect: Detect)
}