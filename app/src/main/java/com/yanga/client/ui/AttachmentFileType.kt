package com.yanga.client.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.ui.graphics.vector.ImageVector

enum class AttachmentFileCategory {
  Image,
  Audio,
  Video,
  Archive,
  Document,
  Other,
}

object AttachmentFileType {
  private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic", "heif")
  private val audioExtensions = setOf("mp3", "wav", "ogg", "m4a", "flac", "aac", "wma", "opus")
  private val videoExtensions = setOf("mp4", "webm", "mkv", "avi", "mov", "wmv", "flv", "m4v")
  private val archiveExtensions = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")
  private val documentExtensions = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "csv")

  fun category(name: String, url: String = ""): AttachmentFileCategory =
    when (extension(name, url)) {
      in imageExtensions -> AttachmentFileCategory.Image
      in audioExtensions -> AttachmentFileCategory.Audio
      in videoExtensions -> AttachmentFileCategory.Video
      in archiveExtensions -> AttachmentFileCategory.Archive
      in documentExtensions -> AttachmentFileCategory.Document
      else -> AttachmentFileCategory.Other
    }

  fun icon(category: AttachmentFileCategory): ImageVector =
    when (category) {
      AttachmentFileCategory.Image -> Icons.Outlined.Image
      AttachmentFileCategory.Audio -> Icons.Outlined.AudioFile
      AttachmentFileCategory.Video -> Icons.Outlined.VideoFile
      AttachmentFileCategory.Archive -> Icons.Outlined.FolderZip
      AttachmentFileCategory.Document -> Icons.Outlined.Description
      AttachmentFileCategory.Other -> Icons.Outlined.InsertDriveFile
    }

  private fun extension(name: String, url: String): String {
    val fromName = name.substringAfterLast('.', "").lowercase()
    if (fromName.isNotBlank() && fromName.length <= 8 && !fromName.contains('/')) {
      return fromName
    }
    val path = url.substringBefore('?').substringBefore('#')
    return path.substringAfterLast('.', "").lowercase()
  }
}
