package com.danjan.scopedstorage

import android.content.ContentValues
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.os.BuildCompat.isAtLeastQ
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    companion object {
        var count = 0
    }

    private var mediaRecorder: MediaRecorder? = null
    var currentRecordingUri: Uri? = null //used in Q and above

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    /**
     * Start, Saves automatically in [currentRecordingUri]
     * */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun prepareMediaRecorder() {
        val values = ContentValues(4)
        //val name = if (isStartDaf) "Daf_" else "" + nameFormat.format(Date())
        val name = "file_${count++}"
        values.put(MediaStore.Audio.Media.TITLE, name)
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, name)
        values.put(MediaStore.Audio.Media.DATE_ADDED, (System.currentTimeMillis() / 1000).toInt())
        //values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp3")
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/3gpp")
        //values.put(MediaStore.Audio.Media.RELATIVE_PATH, "stamurai/")
        val uri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        currentRecordingUri = uri
        if (uri == null) {
            //showShortToast("Unable to access storage")
            return
        }
        val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "w")
        //Log.d("durga", "prepareMediaRecorder: audioUri = $uri")

        mediaRecorder = MediaRecorder()
        mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder!!.setOutputFile(parcelFileDescriptor?.fileDescriptor)
    }


    /**
     * GET all saved recordings:
     *
     * if isAtLeastQ, returns media created by app
     * else returns all media files
     *
     * */
    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun loadAllAudioRecordingsQ(): List<AudioModel>? = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME, //displayName = title + file extension
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION
            /* MediaStore.Video.Media.RELATIVE_PATH, "Stamurai/" + "Recordings" */
            /* set creator package name */
        )
        val uri = if (isAtLeastQ()) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        var allRecordings: ArrayList<AudioModel>? = null
        val columnIndexId: Int

        val cursor = contentResolver.query(uri, projection, null, null, null)
            ?: return@withContext null
        if (cursor.count <= 0)
            return@withContext null
        allRecordings = ArrayList()
        columnIndexId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        while (cursor.moveToNext()) {
            val audioId = cursor.getLong(columnIndexId)
            val uriAudio = Uri.withAppendedPath(uri, audioId.toString())

            allRecordings.add(
                AudioModel(
                    cursor.getLong(0),
                    uriAudio,
                    cursor.getString(1),
                    cursor.getLong(2),
                    cursor.getLong(3),
                    cursor.getLong(4)
                )
            )
        }
        cursor.close()
        return@withContext allRecordings
    }

}