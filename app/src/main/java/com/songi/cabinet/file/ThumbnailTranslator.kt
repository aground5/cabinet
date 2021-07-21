package com.songi.cabinet.file

import com.songi.cabinet.Constants
import java.io.File

object ThumbnailTranslator {

    fun getThumbnailFile(file: File) : File {
        val absolutePath = file.absolutePath
        val path = absolutePath.substring(absolutePath.indexOf(Constants.PACKAGE_NAME) + "${Constants.PACKAGE_NAME}/files".length, absolutePath.lastIndexOf('/'))

        val thumbnailPath = "${Constants.filesDir}/${Constants.FOLDER_THUMBNAIL}$path"
        val thumbnailName = "${file.name}.thumbnail"
        val thumbnailFile = File(thumbnailPath, thumbnailName)

        return thumbnailFile
    }
}