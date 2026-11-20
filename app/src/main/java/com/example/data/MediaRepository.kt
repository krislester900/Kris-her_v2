package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Dépôt des médias : préparation inline (base64) et envoi vers Firebase Storage.
 *
 * Le mode par défaut (sans plan Blaze) envoie les médias DIRECTEMENT dans le message Firestore
 * via une adresse « data: » en base64, sous le plafond de MAX_INLINE_MEDIA_BYTES.
 * Le mode Storage (optionnel, nécessite Blaze) téléverse le fichier et renvoie son URL.
 *
 * Extrait de FirestoreChatRepository (responsabilité « média »).
 */
class MediaRepository(
    private val context: Context
) {

    // ------------------------------------------------------------------
    //  Mode par défaut : média inline dans le message (base64, aucun plan Blaze)
    // ------------------------------------------------------------------

    /** Prépare un média pour l'envoyer DIRECTEMENT dans le message Firestore. */
    suspend fun prepareInlineMedia(
        localUri: String,
        type: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val sourceUri: android.net.Uri = when {
                localUri.startsWith("content://") || localUri.startsWith("file://") -> android.net.Uri.parse(localUri)
                else -> android.net.Uri.fromFile(java.io.File(localUri))
            }
            var mime = resolver.getType(sourceUri)
                ?: when (localUri.substringAfterLast('.', "").lowercase()) {
                    "m4a", "mp4", "aac" -> "audio/mp4"
                    "wav" -> "audio/wav"
                    "mp3" -> "audio/mpeg"
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    "pdf" -> "application/pdf"
                    else -> "application/octet-stream"
                }

            val bytes: ByteArray
            if (type == "IMAGE" || type == "STICKER") {
                var result: ByteArray? = null
                for (maxSide in listOf(1024, 800, 640, 480)) {
                    val bitmap = decodeBitmapForSending(resolver, sourceUri, maxSide) ?: continue
                    val longest = maxOf(bitmap.width, bitmap.height).toFloat()
                    val scale = maxSide / longest
                    val scaled = if (scale < 1f) {
                        android.graphics.Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * scale).toInt().coerceAtLeast(1),
                            (bitmap.height * scale).toInt().coerceAtLeast(1),
                            true
                        )
                    } else bitmap
                    for (quality in listOf(80, 65, 50)) {
                        val out = java.io.ByteArrayOutputStream()
                        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
                        val candidate = out.toByteArray()
                        if (result == null || candidate.size < result.size) result = candidate
                        if (candidate.size <= MAX_INLINE_MEDIA_BYTES) break
                    }
                    if (result != null && result.size <= MAX_INLINE_MEDIA_BYTES) break
                }
                val finalBytes = result ?: ByteArray(0)
                if (finalBytes.isEmpty()) return@withContext Result.failure(IllegalStateException("Image illisible"))
                if (finalBytes.size > MAX_INLINE_MEDIA_BYTES) {
                    return@withContext Result.failure(MediaTooLargeException())
                }
                mime = "image/jpeg"
                bytes = finalBytes
            } else {
                val raw = resolver.openInputStream(sourceUri)?.use { it.readBytes() }
                    ?: return@withContext Result.failure(IllegalStateException("Fichier illisible"))
                if (type == "AUDIO" && raw.size > MAX_INLINE_MEDIA_BYTES) {
                    return@withContext Result.failure(MediaTooLargeException())
                }
                if (type != "AUDIO" && raw.size > MAX_INLINE_MEDIA_BYTES) {
                    return@withContext Result.failure(MediaTooLargeException())
                }
                bytes = raw
            }

            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            val dataUrl = "data:$mime;base64,$base64"
            Result.success(dataUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun decodeBitmapForSending(
        resolver: android.content.ContentResolver,
        uri: android.net.Uri,
        maxSide: Int
    ): android.graphics.Bitmap? {
        return try {
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            var longest = maxOf(bounds.outWidth, bounds.outHeight)
            while (longest / sample > maxSide * 2) sample *= 2
            val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
            resolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, opts)
            }
        } catch (e: Exception) {
            null
        }
    }

    // ------------------------------------------------------------------
    //  Mode optionnel : envoi vers Firebase Storage (nécessite le plan Blaze)
    // ------------------------------------------------------------------

    /** Envoie un média vers Firebase Storage et renvoie son URL de téléchargement. */
    suspend fun uploadToStorage(
        chatId: String,
        messageId: String,
        localUri: String,
        type: String
    ): Result<String> = withContext(Dispatchers.IO) {
        var tempFile: java.io.File? = null
        try {
            val sourceUri: android.net.Uri = when {
                localUri.startsWith("content://") || localUri.startsWith("file://") -> android.net.Uri.parse(localUri)
                else -> android.net.Uri.fromFile(java.io.File(localUri))
            }
            var uploadUri = sourceUri
            var mime = context.contentResolver.getType(sourceUri) ?: "application/octet-stream"

            if (type == "IMAGE" || type == "STICKER") {
                try {
                    val resolver = context.contentResolver
                    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    resolver.openInputStream(sourceUri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
                    var sample = 1
                    var longest = maxOf(bounds.outWidth, bounds.outHeight)
                    while (longest / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
                    val decodeOpts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
                    val bitmap = resolver.openInputStream(sourceUri)?.use {
                        android.graphics.BitmapFactory.decodeStream(it, null, decodeOpts)
                    }
                    if (bitmap != null) {
                        val out = java.io.File.createTempFile("upload_", ".jpg", context.cacheDir)
                        out.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, it) }
                        bitmap.recycle()
                        tempFile = out
                        uploadUri = android.net.Uri.fromFile(out)
                        mime = "image/jpeg"
                    }
                } catch (_: Exception) {
                    // on envoie l'original si la réduction échoue
                }
            }

            val path = "chats/$chatId/media/$messageId"
            val ref = com.google.firebase.storage.FirebaseStorage.getInstance()
                .reference.child(path)
            val metadata = com.google.firebase.storage.StorageMetadata.Builder().setContentType(mime).build()
            ref.putFile(uploadUri, metadata).await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            tempFile?.delete()
        }
    }
}
