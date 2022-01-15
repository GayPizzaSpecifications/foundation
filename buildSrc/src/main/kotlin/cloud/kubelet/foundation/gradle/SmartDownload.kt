package cloud.kubelet.foundation.gradle

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

class SmartDownload(val localFilePath: Path, val remoteDownloadUrl: URI, val sha256: String) {
  fun download(): Boolean {
    if (!checkLocalFileHash()) {
      downloadRemoteFile()
      return true
    }
    return false
  }

  private fun downloadRemoteFile() {
    val url = remoteDownloadUrl.toURL()
    val remoteFileStream = url.openStream()
    val localFileStream = Files.newOutputStream(localFilePath)
    remoteFileStream.transferTo(localFileStream)
    if (!checkLocalFileHash()) {
      throw RuntimeException("Download of ${remoteDownloadUrl} did not result in valid hash.")
    }
  }

  private fun checkLocalFileHash(): Boolean {
    if (!Files.exists(localFilePath)) {
      return false
    }

    val digest = MessageDigest.getInstance("SHA-256")
    val localFileStream = Files.newInputStream(localFilePath)
    val buffer = ByteArray(16 * 1024)

    while (true) {
      val size = localFileStream.read(buffer)
      if (size <= 0) {
        break
      }

      val bytes = buffer.take(size).toByteArray()
      digest.update(bytes)
    }

    val sha256Bytes = digest.digest()
    val localSha256Hash = bytesToHex(sha256Bytes)
    return localSha256Hash.equals(sha256, ignoreCase = true)
  }

  private fun bytesToHex(hash: ByteArray): String {
    val hexString = StringBuilder(2 * hash.size)
    for (i in hash.indices) {
      val hex = Integer.toHexString(0xff and hash[i].toInt())
      if (hex.length == 1) {
        hexString.append('0')
      }
      hexString.append(hex)
    }
    return hexString.toString()
  }
}