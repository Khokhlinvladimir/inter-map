/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.osmdroid.releasehelper

import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FilenameFilter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.Authenticator
import java.net.HttpURLConnection
import java.net.PasswordAuthentication
import java.net.URL
import java.nio.file.Files
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.SystemUtils
import org.apache.maven.model.CiManagement
import org.apache.maven.model.Dependency
import org.apache.maven.model.Developer
import org.apache.maven.model.DistributionManagement
import org.apache.maven.model.Exclusion
import org.apache.maven.model.IssueManagement
import org.apache.maven.model.Organization
import org.apache.maven.model.Scm
import org.apache.maven.model.Site
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.apache.maven.model.io.xpp3.MavenXpp3Writer

/**
 * @author AO
 */
class Main {
    companion object {
        @JvmStatic
        @Throws(Exception::class)
        fun main(args: Array<String>) {
            val props = Properties()
            FileInputStream(File("gradle.properties")).use(props::load)
            FileInputStream(File("local.properties")).use(props::load)

            val settingsGradle = FileUtils.readLines(File("settings.gradle"), "UTF-8")

            val target = File("target")
            FileUtils.deleteQuietly(target)
            target.mkdirs()

            val groupId = props.getProperty("pom.groupId")
            val version = props.getProperty("pom.version")
            val userHomeDir = if (SystemUtils.IS_OS_WINDOWS) {
                System.getenv("USERPROFILE")
            } else {
                System.getenv("HOME")
            }
            val m2 = userHomeDir + File.separator + ".m2" + File.separator + "repository"
            val groupHome = m2 + File.separator + groupId.replace(".", File.separator)

            var modules = findModules(false)
            trim(modules, settingsGradle)
            copyToTarget(target, modules, groupHome, version)
            injectJavadocJars(target, version, modules)
            fixPoms(target, props)
            signFiles(target, props)
            hashFiles(target)
            push(target, props)

            modules = findModules(true)
            trim(modules, settingsGradle)
            for (module in modules) {
                val folder = if (props.getProperty("pom.version").contains("-SNAPSHOT")) {
                    "debug"
                } else {
                    "release"
                }
                val files = File("$module/build/outputs/apk/$folder").listFiles(
                    FilenameFilter { _, name -> name.endsWith(".apk") },
                )
                if (files != null) {
                    for (file in files) {
                        FileUtils.copyFileToDirectory(file, File("target"))
                    }
                }
            }
            makeDistZip(props)
        }

        @JvmStatic
        @Throws(Exception::class)
        private fun findModules(apksOnly: Boolean): MutableList<String> {
            val result = ArrayList<String>()
            val directories = File(".").listFiles { pathname -> pathname.isDirectory }
            if (directories != null) {
                for (directory in directories) {
                    val buildFiles = directory.listFiles(
                        FilenameFilter { _, name -> name == "build.gradle" },
                    )
                    if (buildFiles != null) {
                        for (buildFile in buildFiles) {
                            val content = FileUtils.readFileToString(buildFile, "UTF-8")
                            if (apksOnly) {
                                if (content.contains("com.android.application")) {
                                    result.add(directory.name)
                                }
                            } else if (
                                content.contains("com.android.library") ||
                                content.contains("java") ||
                                content.contains("war")
                            ) {
                                result.add(directory.name)
                            }
                        }
                    }
                }
            }
            if (result.isEmpty()) {
                printError()
                throw Exception("failed to find any modules to publish")
            }
            return result
        }

        @JvmStatic
        private fun trim(modules: MutableList<String>, settingsGradle: List<String>) {
            val removeMe = ArrayList<String>()
            for (module in modules) {
                if (!settingsGradle.contains("include ':$module'")) {
                    removeMe.add(module)
                }
            }
            modules.removeAll(removeMe.toSet())
        }

        @JvmStatic
        @Throws(IOException::class)
        private fun copyToTarget(
            target: File,
            modules: List<String>,
            groupHome: String,
            version: String,
        ) {
            for (module in modules) {
                println("$module copy")
                val files = File(File(groupHome, module), version).listFiles()
                if (files != null) {
                    for (file in files) {
                        if (file.name == "maven-metadata-local.xml" || file.name.endsWith(".module")) {
                            continue
                        }
                        FileUtils.copyFileToDirectory(file, target)
                    }
                }
            }
        }

        @JvmStatic
        @Throws(Exception::class)
        private fun injectJavadocJars(target: File, version: String, modules: List<String>) {
            // Keep the original lookup: it may warm the directory listing even though its result is unused.
            target.listFiles { pathname -> pathname.name.contains("javadoc.jar") }

            for (module in modules) {
                if (hasDocFiles("$module/src/main/java/")) {
                    println("$module has doc-files, injecting")
                    val temp = File(target, "tmp")
                    temp.mkdirs()
                    val source = File(target, "$module-$version-javadoc.jar")
                    unzipFolder(source, temp)
                    copyDocFiles(File("$module/src/main/java/"), temp)
                    zipFolder(temp, File("temp.jar"))
                    source.delete()
                    FileUtils.moveFile(File("temp.jar"), source)
                    FileUtils.deleteQuietly(temp)
                }
            }
        }

        @JvmStatic
        private fun hasDocFiles(module: String): Boolean {
            val folders = File(module).listFiles { pathname -> pathname.isDirectory }
            if (folders != null) {
                for (folder in folders) {
                    if (folder.name == "doc-files" || hasDocFiles(folder.absolutePath)) {
                        return true
                    }
                }
            }
            return false
        }

        @JvmStatic
        @Throws(Exception::class)
        private fun unzipFolder(archiveFile: File, zipDestinationFolder: File) {
            JarArchiveInputStream(FileInputStream(archiveFile)).use { input ->
                while (true) {
                    val entry = input.nextEntry ?: break
                    if (!input.canReadEntryData(entry)) {
                        continue
                    }
                    val file = File("$zipDestinationFolder/$entry")
                    if (entry.isDirectory) {
                        if (!file.isDirectory && !file.mkdirs()) {
                            throw IOException("failed to create directory $file")
                        }
                    } else {
                        val parent = file.parentFile
                        if (!parent.isDirectory && !parent.mkdirs()) {
                            printError()
                            printError()
                            throw IOException("failed to create directory $parent")
                        }
                        Files.newOutputStream(file.toPath()).use { output ->
                            IOUtils.copy(input, output)
                        }
                    }
                }
            }
        }

        @JvmStatic
        @Throws(IOException::class)
        private fun copyDocFiles(source: File, temp: File) {
            if (source.name == "doc-files" && source.isDirectory) {
                temp.mkdirs()
                FileUtils.copyDirectory(source, temp)
            }
            val folders = source.listFiles(
                FilenameFilter { directory, _ -> directory.isDirectory },
            ) ?: return
            for (folder in folders) {
                copyDocFiles(folder, File(temp, folder.name))
            }
        }

        @JvmStatic
        @Throws(Exception::class)
        private fun zipFolder(sourceDirectory: File, destinationJar: File) {
            val filesToArchive = ArrayList<File>()
            buildFileList(filesToArchive, sourceDirectory)
            println("Compressing ${filesToArchive.size} files")
            JarArchiveOutputStream(FileOutputStream(destinationJar)).use { output ->
                for (file in filesToArchive) {
                    var entryName = file.absolutePath.replace(sourceDirectory.absolutePath, "")
                    entryName = entryName.replace("\\", "/")
                    if (entryName.startsWith("/")) {
                        entryName = entryName.substring(1)
                    }
                    val entry = output.createArchiveEntry(file, entryName)
                    output.putArchiveEntry(entry)
                    if (file.isFile) {
                        Files.newInputStream(file.toPath()).use { input ->
                            IOUtils.copy(input, output)
                        }
                    }
                    output.closeArchiveEntry()
                }
                output.finish()
            }
        }

        @JvmStatic
        @Throws(Exception::class)
        private fun makeDistZip(props: Properties) {
            println("making dist zip")
            JarArchiveOutputStream(
                FileOutputStream(File("osmdroid-dist-${props.getProperty("pom.version")}.zip")),
            ).use { output ->
                addTargetFilesToArchive(output, "apk") { it.endsWith(".apk") }
                addTargetFilesToArchive(output, "aar") { it.endsWith(".aar") }
                addTargetFilesToArchive(output, "libs") {
                    it.endsWith(".jar") || it.endsWith(".war")
                }
                addTargetFilesToArchive(output, "distributions") { it.endsWith(".zip") }
                output.finish()
            }
        }

        private inline fun addTargetFilesToArchive(
            output: JarArchiveOutputStream,
            directory: String,
            crossinline accepts: (String) -> Boolean,
        ) {
            val files = File("target").listFiles(
                FilenameFilter { _, name -> accepts(name) },
            )!!
            for (file in files) {
                var entryName = "$directory/${file.name}".replace("\\", "/")
                if (entryName.startsWith("/")) {
                    entryName = entryName.substring(1)
                }
                output.putArchiveEntry(output.createArchiveEntry(file, entryName))
                if (file.isFile) {
                    Files.newInputStream(file.toPath()).use { input -> IOUtils.copy(input, output) }
                }
                output.closeArchiveEntry()
            }
        }

        @JvmStatic
        private fun buildFileList(filesToArchive: MutableCollection<File>, sourceDirectory: File) {
            val files = sourceDirectory.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        buildFileList(filesToArchive, file)
                    } else if (!file.isHidden) {
                        filesToArchive.add(file)
                    }
                }
            }
        }

        @JvmStatic
        @Throws(Exception::class)
        private fun hashFiles(target: File) {
            println("hashing files")
            for (file in target.listFiles()!!) {
                FileInputStream(file).use { input ->
                    FileUtils.write(File("$file.sha512"), DigestUtils.sha512Hex(input), "UTF-8")
                }
                FileInputStream(file).use { input ->
                    FileUtils.write(File("$file.md5"), DigestUtils.md5Hex(input), "UTF-8")
                }
                FileInputStream(file).use { input ->
                    FileUtils.write(File("$file.sha1"), DigestUtils.sha1Hex(input), "UTF-8")
                }
            }
        }

        @JvmStatic
        @Throws(Exception::class)
        private fun fixPoms(target: File, props: Properties) {
            val poms = target.listFiles(FilenameFilter { _, name -> name.endsWith(".pom") })
            if (poms != null) {
                for (pom in poms) {
                    val model = MavenXpp3Reader().read(FileReader(pom))
                    model.name = model.artifactId
                    model.url = props.getProperty("pom.url")
                    model.inceptionYear = props.getProperty("pom.inceptionYear")
                    model.organization = Organization().apply {
                        name = props.getProperty("pom.organization.name")
                        url = props.getProperty("pom.organization.url")
                    }
                    model.issueManagement = IssueManagement().apply {
                        url = props.getProperty("pom.issueManagement.url")
                        system = props.getProperty("pom.issueManagement.system")
                    }
                    model.ciManagement = CiManagement().apply {
                        system = props.getProperty("pom.ciManagement.system")
                        url = props.getProperty("pom.ciManagement.url")
                    }
                    model.scm = Scm().apply {
                        url = props.getProperty("pom.scm.url")
                        connection = props.getProperty("pom.scm.connection")
                        developerConnection = props.getProperty("pom.scm.developerConnection")
                    }
                    model.distributionManagement = DistributionManagement().apply {
                        site = Site().apply {
                            id = props.getProperty("pom.distributionManagement.site.id")
                            url = props.getProperty("pom.distributionManagement.site.url")
                        }
                    }

                    model.developers = ArrayList()
                    var developerIndex = 0
                    while (props.containsKey("pom.developers.developer.$developerIndex.id")) {
                        val developer = Developer().apply {
                            id = props.getProperty("pom.developers.developer.$developerIndex.id")
                            name = props.getProperty("pom.developers.developer.$developerIndex.name")
                            roles = ArrayList()
                        }
                        var roleIndex = 0
                        while (
                            props.containsKey(
                                "pom.developers.developer.$developerIndex.role.$roleIndex",
                            )
                        ) {
                            developer.roles.add(
                                props.getProperty(
                                    "pom.developers.developer.$developerIndex.role.$roleIndex",
                                ),
                            )
                            roleIndex++
                        }
                        model.developers.add(developer)
                        developerIndex++
                    }

                    if (model.dependencies == null) {
                        model.dependencies = ArrayList()
                    }
                    if (model.packaging == null) {
                        model.packaging = "jar"
                    }

                    if (
                        model.packaging.equals("aar", ignoreCase = true) ||
                        model.packaging.equals("apk", ignoreCase = true) ||
                        model.packaging.equals("war", ignoreCase = true)
                    ) {
                        val lines = FileUtils.readLines(File("${model.artifactId}/build.gradle"), "UTF-8")
                        var index = 0
                        while (index < lines.size) {
                            val line = lines[index].trim()
                            if (!line.startsWith("//")) {
                                when {
                                    line.startsWith("api") -> {
                                        index = parseDependency(lines, index, "api", props, model.dependencies)
                                    }
                                    line.startsWith("implementation") -> {
                                        index = parseDependency(
                                            lines,
                                            index,
                                            "implementation",
                                            props,
                                            model.dependencies,
                                        )
                                    }
                                }
                            }
                            index++
                        }
                    }

                    MavenXpp3Writer().write(FileOutputStream(pom), model)
                }
            }
        }

        private fun parseDependency(
            lines: List<String>,
            startIndex: Int,
            configuration: String,
            props: Properties,
            dependencies: MutableList<Dependency>,
        ): Int {
            var index = startIndex
            var line = lines[index].trim().replaceFirst("$configuration ", "").trim()
            if (line.startsWith("project(")) {
                addProjectDependency(props, line, dependencies)
                return index
            }

            line = line.replace("'", "").replace("\"", "").trim()
            if (line.startsWith("(")) {
                line = line.substring(1)
            }

            val dependency = Dependency()
            if (line.contains("group:") && line.contains("name:") && line.contains("version")) {
                var group: String? = null
                var artifact: String? = null
                var version: String? = null
                for (part in line.replace("implementation", "").replace("api", "").trim().split(",")) {
                    if (part.contains("group:")) group = part.replace("group:", "").trim()
                    if (part.contains("name:")) artifact = part.replace("name:", "").trim()
                    if (part.contains("version:")) version = part.replace("version:", "").trim()
                }
                if (group != null && artifact != null && version != null) {
                    dependency.groupId = group
                    dependency.artifactId = artifact
                    dependency.version = version.substringBefore(")")
                    if (!contains(dependencies, dependency)) dependencies.add(dependency)
                }
            } else {
                val parts = line.split(":")
                dependency.groupId = parts[0]
                dependency.artifactId = parts[1]
                dependency.version = parts[2].substringBefore(")")
                if (!contains(dependencies, dependency)) dependencies.add(dependency)
            }

            if (line.endsWith("{")) {
                index++
                line = lines[index].trim()
                while (!line.startsWith("}")) {
                    if (line.contains("exclude")) {
                        val exclusionParts = line.replaceFirst("exclude", "").trim().split(",")
                        var group: String? = null
                        var artifact: String? = null
                        for (part in exclusionParts) {
                            val trimmed = part.trim()
                            if (trimmed.startsWith("group:")) {
                                group = trimmed.replace("group:", "").replace("'", "")
                                    .replace("\"", "").trim()
                            }
                            if (trimmed.startsWith("module:")) {
                                artifact = trimmed.replace("module:", "").replace("'", "")
                                    .replace("\"", "").trim()
                            }
                        }
                        if (group != null && artifact != null) {
                            dependencies.add(dependency)
                            if (dependency.exclusions == null) dependency.exclusions = ArrayList()
                            dependency.exclusions.add(
                                Exclusion().apply {
                                    groupId = group
                                    artifactId = artifact
                                },
                            )
                        }
                    }
                    index++
                    line = lines[index].trim()
                }
            }
            return index
        }

        @JvmStatic
        @Throws(Exception::class)
        private fun signFiles(target: File, props: Properties) {
            println("signing files")
            for (file in target.listFiles()!!) {
                Thread.sleep(500)
                val command = arrayOf(
                    props.getProperty("GPG_PATH"),
                    "--always-trust",
                    "--pinentry-mode=loopback",
                    "--yes",
                    "-a",
                    "--output",
                    file.absolutePath + ".asc",
                    "--detach-sig",
                    file.canonicalPath,
                )
                println(StringUtils.join(command, " "))
                val process = ProcessBuilder(*command).start()
                StreamGobbler(process.errorStream).start()
                StreamGobbler(process.inputStream).start()
                process.waitFor()
                println("Signing exit code for ${file.name} was ${process.exitValue()}")
                if (process.exitValue() != 0) {
                    printError()
                    throw Exception("signing failed for ${file.absolutePath}")
                }
            }
        }

        @JvmStatic
        private fun addProjectDependency(
            props: Properties,
            line: String,
            dependencies: MutableList<Dependency>,
        ) {
            val dependency = Dependency().apply {
                groupId = props.getProperty("pom.groupId")
                version = props.getProperty("pom.version")
                artifactId = line.replace("project(':" , "").replace("')", "")
                    .replace("'", "").replace(")", "").trim()
            }
            if (!contains(dependencies, dependency)) dependencies.add(dependency)
        }

        @JvmStatic
        @Suppress("UNUSED_PARAMETER")
        private fun copyApksForDist(props: Properties) {
            throw UnsupportedOperationException("Not supported yet.")
        }

        @JvmStatic
        private fun contains(dependencies: List<Dependency>, dependency: Dependency): Boolean {
            for (existing in dependencies) {
                if (
                    existing.groupId == dependency.groupId &&
                    existing.artifactId == dependency.artifactId &&
                    existing.version == dependency.version
                ) {
                    return true
                }
            }
            return false
        }

        @JvmStatic
        @Throws(Exception::class)
        fun uploadFile(input: File, urlDestination: String, contentType: String) {
            println(urlDestination)
            try {
                val connection = URL(urlDestination).openConnection() as HttpURLConnection
                connection.doOutput = true
                connection.requestMethod = "PUT"
                connection.setRequestProperty("Content-Type", contentType)
                connection.setRequestProperty("Content-Length", input.length().toString())
                connection.useCaches = false
                connection.doOutput = true
                connection.connect()

                val bufferedInput = BufferedInputStream(FileInputStream(input))
                val output = connection.outputStream
                val bytes = ByteArray(4096)
                var bytesRead: Int
                var totalBytesRead = 0
                while (bufferedInput.read(bytes).also { bytesRead = it } > 0) {
                    output.write(bytes, 0, bytesRead)
                    totalBytesRead += bytesRead
                }
                output.flush()
                output.close()

                println("Upload $urlDestination : ${connection.responseCode}: ${connection.responseMessage}")
                if (connection.responseCode >= 300) {
                    StreamGobbler(connection.inputStream).start()
                }
                connection.disconnect()
                if (connection.responseCode < 200 || connection.responseCode >= 300) {
                    throw Exception(
                        "unexcepted response code. Published failed. ${connection.responseCode}: " +
                            connection.responseMessage,
                    )
                }
            } catch (exception: Exception) {
                printError()
                throw Exception(
                    "unexcepted exception. Published failed. ${exception.message}",
                    exception,
                )
            }
        }

        @JvmStatic
        fun printError() {
            println(
                "     _.-^^---....,,--\n" +
                    " _--                  --_\n" +
                    "<                        >)\n" +
                    "|                         |\n" +
                    " \\._                   _./\n" +
                    "    ```--. . , ; .--'''\n" +
                    "          | |   |\n" +
                    "       .-=||  | |=-.\n" +
                    "       `-=#\\\$%&%\\\$#=-'\n" +
                    "          | ;  :|\n" +
                    " _____.,-#%&\\\$@%#&#~,._____ ",
            )
        }

        @JvmStatic
        @Throws(Exception::class)
        private fun push(target: File, props: Properties) {
            println("publishing to nexus repo")
            var password = props.getProperty("NEXUS_PASSWORD")
            if (mightBeEncrypted(password)) password = tryDecrypt(password)
            val finalPassword = password
            Authenticator.setDefault(
                object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication =
                        PasswordAuthentication(
                            props.getProperty("NEXUS_USERNAME"),
                            finalPassword.toCharArray(),
                        )
                },
            )

            var repositoryUrl = if (props.getProperty("pom.version").contains("-SNAPSHOT")) {
                props.getProperty("SNAPSHOT_REPOSITORY_URL")
            } else {
                props.getProperty("RELEASE_REPOSITORY_URL")
            }
            if (!repositoryUrl.endsWith("/")) repositoryUrl = "/"
            val groupUrl = props.getProperty("pom.groupId").replace(".", "/")
            for (file in target.listFiles()!!) {
                val versionMarker = "-${props.getProperty("pom.version")}"
                val artifactId = file.name.substring(0, file.name.indexOf(versionMarker))
                val contentType = when {
                    file.name.endsWith(".pom") -> "text/xml; charset=utf-8"
                    file.name.endsWith(".md5") || file.name.endsWith(".sha1") ||
                        file.name.endsWith(".sha512") -> "text/plain; charset=utf-8"
                    else -> "application/octet-stream; charset=utf-8"
                }
                uploadFile(
                    file,
                    "$repositoryUrl$groupUrl/$artifactId/${props.getProperty("pom.version")}/${file.name}",
                    contentType,
                )
            }
        }

        @JvmStatic
        fun hexToBytes(value: String): ByteArray = hexToBytes(value.toCharArray())

        @JvmStatic
        @Throws(FileNotFoundException::class, IOException::class)
        fun loadKey(): String {
            var usersHome = System.getProperty("user.home").replace("\\", "/")
            usersHome += "/.gradle/"
            val keys = File(File(usersHome).absolutePath + "/fury-keys.properties")
            if (keys.exists()) {
                val properties = Properties()
                properties.load(FileInputStream(keys))
                if (properties.containsKey("FURY_MASTER_PASSWORD")) {
                    return properties.getProperty("FURY_MASTER_PASSWORD")
                }
            }
            return ""
        }

        @JvmStatic
        fun tryDecrypt(ciphertext: String): String {
            val textToDecrypt = ciphertext.substring(1, ciphertext.length - 1)
            try {
                return decrypt(textToDecrypt, loadKey())
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return ciphertext
        }

        @JvmStatic
        @Throws(Exception::class)
        fun decrypt(ciphertext: String, key: String): String {
            val secretKey = SecretKeySpec(hexToBytes(key), "AES")
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            return String(cipher.doFinal(hexToBytes(ciphertext)))
        }

        @JvmStatic
        fun hexToBytes(hex: CharArray): ByteArray {
            val raw = ByteArray(hex.size / 2)
            for (index in raw.indices) {
                val high = Character.digit(hex[index * 2], 16)
                val low = Character.digit(hex[index * 2 + 1], 16)
                var value = (high shl 4) or low
                if (value > 127) value -= 256
                raw[index] = value.toByte()
            }
            return raw
        }

        @JvmStatic
        fun mightBeEncrypted(text: String?): Boolean =
            !text.isNullOrEmpty() && text.startsWith("{") && text.endsWith("}")
    }

    internal class StreamGobbler(@JvmField var `is`: InputStream) : Thread() {
        override fun run() {
            try {
                BufferedReader(InputStreamReader(`is`)).use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        println(line)
                    }
                }
            } catch (exception: IOException) {
                exception.printStackTrace()
            }
        }
    }
}
