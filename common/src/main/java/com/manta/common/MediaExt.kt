package com.manta.common

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min


internal class CustomTakePicture : ActivityResultContract<Unit, Uri?>() {
    private var uri: Uri? = null

    override fun createIntent(context: Context, input: Unit): Intent {
        this.uri = kotlin.runCatching {
            val file = context.createImageFilePrivate()
            context.getContentUriFromFile(file)
        }.getOrNull()
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE).also {
            it.putExtra(MediaStore.EXTRA_OUTPUT, this.uri)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) {
            this.uri
        } else {
            uri?.toFile()?.delete()
            null
        }
    }
}

internal class CustomTakeVideo : ActivityResultContract<Unit, Uri?>() {
    private var uri: Uri? = null

    override fun createIntent(context: Context, input: Unit): Intent {
        this.uri = kotlin.runCatching {
            val file = context.createVideoFilePrivate()
            context.getContentUriFromFile(file)
        }.getOrNull()
        return Intent(MediaStore.ACTION_VIDEO_CAPTURE).also {
            it.putExtra(MediaStore.EXTRA_OUTPUT, this.uri)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) {
            this.uri
        } else {
            uri?.toFile()?.delete()
            null
        }
    }
}

/**
 * this method should called on [AppCompatActivity.onCreate]
 * because this method create file from context and use [AppCompatActivity.registerForActivityResult]
 */
internal fun ComponentActivity.registerTakePictureLauncher(
    onSuccess: (fileUri: Uri) -> Unit,
    onFailed: (Throwable) -> Unit
): ActivityResultLauncher<Unit> {
    return registerForActivityResult(CustomTakePicture()) { uri ->
        if (uri == null) {
            onFailed(NullPointerException("TakePicture result in null"))
        } else {
            onSuccess(uri)
        }
    }
}


internal fun ComponentActivity.registerTakeVideoLauncher(
    onSuccess: (fileUri: Uri) -> Unit,
    onFailed: (Throwable) -> Unit
): ActivityResultLauncher<Unit> {
    return registerForActivityResult(CustomTakeVideo()) { uri ->
        if (uri == null) {
            onFailed(NullPointerException("TakeVideo result in null"))
        } else {
            onSuccess(uri)
        }
    }
}

internal fun Context.getContentUriFromFile(file: File): Uri =
    FileProvider.getUriForFile(this, applicationContext.packageName + ".com.moongchipicker.fileprovider", file)

internal fun Context.createImageFilePrivate(
    prefix: String = ""
): File {
    return if (isExternalStorageWritable()) {
        createImageFileToPrivateExternalStorage(prefix) ?: createImageFileToInternalStorage(prefix)
    } else {
        createImageFileToInternalStorage(prefix)
    }
}

internal fun Context.createVideoFilePrivate(
    prefix: String = ""
): File {
    return if (isExternalStorageWritable()) {
        createVideoFileToPrivateExternalStorage(prefix) ?: createVideoFileToInternalStorage(prefix)
    } else {
        createVideoFileToInternalStorage(prefix)
    }
}

/**
 * @return uri is sorted ascending order base on modified date
 */
internal suspend fun Context.loadVideosFromInternalStorage(maxFileCount: Int): List<Uri> {
    return loadFilesFromInternalStorage(".mp4", maxFileCount)
}

/**
 * @return uri is sorted ascending order base on modified date
 */
internal suspend fun Context.loadImagesFromInternalStorage(maxFileCount: Int): List<Uri> {
    return loadFilesFromInternalStorage(".jpg", maxFileCount)
}

/**
 * @param format : ex ) ".jpg", ".png"
 * @return uri is sorted ascending order base on modified date
 */
internal suspend fun Context.loadFilesFromInternalStorage(
    format: String,
    maxFileCount: Int
): List<Uri> {
    return withContext(Dispatchers.IO) {
        val files =
            filesDir.listFiles()?.toList()?.apply { subList(0, min(size, maxFileCount)) }.toSafe()
        files.filter { it.canRead() && it.isFile && it.name.endsWith(format) }
            .sortedBy {
                it.lastModified()
            }.map { file ->
                Uri.fromFile(file)
            }
    }
}

/**
 * @return uri is sorted ascending order base on modified date
 */
internal suspend fun Context.loadVideosFromPublicExternalStorage(maxFileCount: Int): List<Uri> {
    return withContext(Dispatchers.IO) {
        val collection = sdkAndUp(Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } ?: MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media._ID
        )

        contentResolver.query(
            collection,
            projection,
            null,
            null,
            MediaStore.Video.VideoColumns.DATE_ADDED + " DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)

            val uris = mutableListOf<Uri>()
            while (cursor.moveToNext() && uris.size < maxFileCount) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                uris.add(contentUri)
            }
            uris
        } ?: listOf<Uri>()
    }
}

/**
 * @return uri is sorted ascending order base on modified date
 */
internal suspend fun Context.loadImagesFromPublicExternalStorage(maxFileCount: Int): List<Uri> {
    return withContext(Dispatchers.IO) {
        val collection = sdkAndUp(Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID
        )

        contentResolver.query(
            collection,
            projection,
            null,
            null,
            MediaStore.Images.ImageColumns.DATE_ADDED + " DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            val uris = mutableListOf<Uri>()
            while (cursor.moveToNext() && uris.size < maxFileCount) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                uris.add(contentUri)
            }
            uris
        } ?: listOf<Uri>()
    }
}

internal fun Context.createImageFileToInternalStorage(
    prefix: String = ""
): File {
    val storageDir: File = filesDir

    return File.createTempFile(
        "JPEG_${prefix}_", /* prefix */
        ".jpg", /* suffix */
        storageDir /* directory */
    )
}

internal fun Context.createImageFileToPrivateExternalStorage(
    prefix: String = ""
): File? {
    val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null

    return File.createTempFile(
        "JPEG_${prefix}_", /* prefix */
        ".jpg", /* suffix */
        storageDir /* directory */
    )
}

internal fun Context.createVideoFileToInternalStorage(
    prefix: String = ""
): File {
    val storageDir: File = filesDir

    return File.createTempFile(
        "MPEG_${prefix}_", /* prefix */
        ".mp4", /* suffix */
        storageDir /* directory */
    )
}

internal fun Context.createVideoFileToPrivateExternalStorage(
    prefix: String = ""
): File? {
    val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: return null

    return File.createTempFile(
        "MPEG_${prefix}_", /* prefix */
        ".mp4", /* suffix */
        storageDir /* directory */
    )
}


internal fun isExternalStorageWritable(): Boolean {
    return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
}


internal fun isExternalStorageReadable(): Boolean {
    return Environment.getExternalStorageState() in
            setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)
}


