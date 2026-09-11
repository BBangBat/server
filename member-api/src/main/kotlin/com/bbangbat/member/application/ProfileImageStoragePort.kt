package com.bbangbat.member.application

interface ProfileImageStoragePort {
    fun generateUpload(contentType: String): ProfileImageUpload

    fun buildUrl(objectKey: String): String
}

data class ProfileImageUpload(
    val presignedUrl: String,
    val objectKey: String,
)
