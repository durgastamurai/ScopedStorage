package com.danjan.scopedstorage

import android.net.Uri

data class AudioModel(
    val id: Long,
    val uri: Uri,
    //val title: String?,
    val displayName: String?, //title + .mp3
    val date: Long,
    val size: Long, //bytes
    val durationMillis: Long
    /*val realPath: String?*/
)