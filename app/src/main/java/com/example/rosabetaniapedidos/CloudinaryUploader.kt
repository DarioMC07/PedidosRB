package com.example.rosabetaniapedidos

import android.net.Uri
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Sube una imagen a Cloudinary usando upload unsigned.
 * No requiere API Secret — solo cloud_name y upload_preset.
 */
object CloudinaryUploader {

    private const val CLOUD_NAME   = "dygvegeag"
    private const val UPLOAD_PRESET = "pedidos_rosa_betania"
    private const val UPLOAD_URL   = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Sube la imagen referenciada por [uri] a Cloudinary.
     * Ejecutar desde una coroutine — usa Dispatchers.IO internamente.
     * @return URL segura (https) de la imagen subida, o null si falla.
     */
    suspend fun uploadImage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            // Copiar el contenido del URI a un archivo temporal
            val tempFile = createTempFileFromUri(context, uri) ?: return@withContext null

            val mediaType = "image/*".toMediaTypeOrNull()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    tempFile.name,
                    tempFile.asRequestBody(mediaType)
                )
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .build()

            val request = Request.Builder()
                .url(UPLOAD_URL)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            // Limpiar archivo temporal
            tempFile.delete()

            if (!response.isSuccessful || body == null) return@withContext null

            // Parsear la URL segura de la respuesta JSON
            val json = JSONObject(body)
            json.optString("secure_url").takeIf { it.isNotBlank() }

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Copia el contenido de un content URI a un File temporal en caché. */
    private fun createTempFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { out ->
                inputStream.copyTo(out)
            }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
