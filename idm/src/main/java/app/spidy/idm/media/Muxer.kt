package app.spidy.idm.media

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.util.Log
import java.io.File
import java.nio.ByteBuffer


class Muxer {

    fun mux() {
        var outputFile = ""
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        val file = File("$dir${File.separator}final.mp4")
        file.createNewFile()
        outputFile = file.absolutePath


        val videoExtractor = MediaExtractor()
        videoExtractor.setDataSource("$dir${File.separator}output.mp4")
        videoExtractor.selectTrack(0)
        val videoFormat = videoExtractor.getTrackFormat(0)
        val videoMimeType = videoFormat.getString(MediaFormat.KEY_MIME)
        val videoFrameMaxInputSize = videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        val videoFrameRate = videoFormat.getInteger(MediaFormat.KEY_FRAME_RATE)
        val videoDuration = videoFormat.getLong(MediaFormat.KEY_DURATION)

        val audioExtractor = MediaExtractor()
        audioExtractor.setDataSource("$dir${File.separator}output.mp3")
        audioExtractor.selectTrack(0)
        val audioFormat = audioExtractor.getTrackFormat(0)
        val audioMimeType = audioFormat.getString(MediaFormat.KEY_MIME)

        // start muxer
        val mediaMuxer = MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val videoTrackIndex = mediaMuxer.addTrack(videoFormat)
        val audioTrackIndex = mediaMuxer.addTrack(audioFormat)
        val byteBuffer = ByteBuffer.allocate(videoFrameMaxInputSize)
        mediaMuxer.start()

        // writing video
        val videoBufferInfo = MediaCodec.BufferInfo()
        videoExtractor.selectTrack(videoTrackIndex)

        while (true) {
            val readVideoSampleSize = videoExtractor.readSampleData(byteBuffer, 0)
            if (readVideoSampleSize < 0) {
                videoExtractor.unselectTrack(videoTrackIndex)
                break
            }
            val videoSampleTime = videoExtractor.sampleTime
            videoBufferInfo.size = readVideoSampleSize
            videoBufferInfo.presentationTimeUs = videoSampleTime
            //videoBufferInfo.presentationTimeUs += 1000 * 1000 / frameRate;
            videoBufferInfo.offset = 0
            videoBufferInfo.flags = videoExtractor.sampleFlags
            mediaMuxer.writeSampleData(videoTrackIndex, byteBuffer, videoBufferInfo)
            videoExtractor.advance()

            Log.d("hello", readVideoSampleSize.toString())
        }

        // writing audio
        val audioBufferInfo = MediaCodec.BufferInfo()
        audioExtractor.selectTrack(audioTrackIndex)

        while (true) {
            val readAudioSampleSize = audioExtractor.readSampleData(byteBuffer, 0)
            if (readAudioSampleSize < 0) {
                break
            }
            val audioSampleTime = audioExtractor.sampleTime
            audioBufferInfo.size = readAudioSampleSize
            audioBufferInfo.presentationTimeUs = audioSampleTime
            if (audioBufferInfo.presentationTimeUs > videoDuration) {
                audioExtractor.unselectTrack(audioTrackIndex)
                break
            }
            audioBufferInfo.offset = 0
            audioBufferInfo.flags = audioExtractor.sampleFlags
            mediaMuxer.writeSampleData(audioTrackIndex, byteBuffer, audioBufferInfo)
            audioExtractor.advance()
        }


        // releasing resources
        try {
            mediaMuxer.stop()
            mediaMuxer.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            videoExtractor.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            audioExtractor.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}