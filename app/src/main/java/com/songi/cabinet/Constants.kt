package com.songi.cabinet

object Constants {
    var filesDir: String? = null
    val PACKAGE_NAME = "com.songi.cabinet"

    // 상수
    val IMAGEVIEW_ALPHA = 0.5f
    val EXTEND_SPACE_ALPHA = 0.5f
    val PERMISSION_READ_EXTERNAL_STORAGE = 1249
    val TIME_INTERVAL: Long = 2000
    val BACK_SPACE_TIME_INTERVAL: Long = 600
    val DRAWER_TIME_INTERVAL: Long = 600
    val FOLDER_TIME_INTERVAL: Long = 1300

    // worker
    val COPY_PROGRESS = "COPY_PROGRESS"
    val COPY_BUILDER = "COPY_BUILDER"
    val IMAGE_PROGRESS = "IMAGE_PROGRESS"
    val IMAGE_BUILDER = "IMAGE_BUILDER"
    val RENDER_BUILDER = "RENDER_BUILDER"

    // 파일 종류
    val OBJECT_BLANK = 224
    val OBJECT_DIR = 225        // 폴더
    val OBJECT_FILE = 226       // 정의되지 않은 파일
    val OBJECT_IMAGE = 227      // image/*
    val OBJECT_VIDEO = 228      // video/*
    val OBJECT_TEXT = 229       // txt
    val OBJECT_PDF = 230        // pdf
    val OBJECT_PPT = 231        // ppt
    val OBJECT_EXCEL = 232      // excel
    val OBJECT_WORD = 233

    // 설정 파일 이름
    val FILENAME_VIEW_COLUMN_CONFIG = "view_column.config"
    val FILENAME_IMAGE_THUMBNAIL = "image_thumbnail_check.config"

    // 폴더 이름
    val FOLDER_USER = "__Cabinet_user_folder"
    val FOLDER_CLIPBOARD = "__Cabinet_temp_folder"
    val FOLDER_THUMBNAIL = "__thumbnail"

}