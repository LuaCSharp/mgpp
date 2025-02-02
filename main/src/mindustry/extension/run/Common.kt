@file:JvmMultifileClass
@file:JvmName("ExtensionKt")
@file:Suppress("RemoveRedundantBackticks")

package io.github.liplum.mindustry

import io.github.liplum.dsl.plusAssign
import io.github.liplum.mindustry.*
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import java.io.File

open class Common {
    /** @see [AddCommonSpec.name] */
    var name = ""
    /** @see [AddCommonSpec.startupArgs] */
    val startupArgs = ArrayList<String>()
    /** @see [AddCommonSpec.jvmArgs] */
    val jvmArgs = ArrayList<String>()
    /** @see [AddClientSpec.dataDir] */
    var dataDir: String? = null
    var location: IGameLoc? = null
    var modpack: String? = null
}

abstract class AddCommonSpec<T : Common> {
    protected abstract val proj: Project
    protected abstract val backend: T
    val latest: Notation get() = Notation.latest
    /**
     * *Optional*
     * An empty String as default.
     * It affects gradle task names.
     * ```
     * runClient // if it's empty
     * runClient2 // if second name is still empty
     * runClientFooClient // if [name] is "FooClient"
     * ```
     */
    var name: Any
        get() = backend.name
        set(value) {
            backend.name = formatValidGradleName(value.toString())
        }
    val startupArgs get() = backend.startupArgs
    /**
     * The arguments of JVM.
     *
     * Because Mindustry desktop is based on Lwjgl3, the `-XstartOnFirstThread` will be passed when run on macOS.
     */
    val jvmArgs get() = backend.jvmArgs
    /**
     * *Optional*
     * The name of Mindustry's data directory where to put saves.
     *
     * The default [dataDir] is the same as [name].
     */
    var dataDir: String?
        get() = backend.dataDir
        set(value) {
            backend.dataDir = value
        }
    var modpack: String?
        get() = backend.modpack
        set(value) {
            backend.modpack = value?.let(::formatValidGradleName)
        }

    protected fun IGameLoc.checkAndSet() {
        if (backend.location != null) {
            proj.logger.warn("The game is already set to ${backend.location}, and will be overridden by $this.")
        }
        backend.location = this
    }

    fun github(
        user: String,
        repo: String,
        tag: String,
        file: String,
    ) {
        GitHubGameLoc(
            user = user,
            repo = repo,
            tag = tag,
            file = file,
        ).checkAndSet()
    }

    fun github(props: Map<String, String>) {
        github(
            user = props["user"] ?: "",
            repo = props["repo"] ?: "",
            tag = props["tag"] ?: "",
            file = props["file"] ?: "",
        )
    }
    /**
     * ```kotlin
     * official(version="v141")
     * ```
     */
    abstract fun official(version: String)
    /**
     * ```kotlin
     * official(version=latest)
     * ```
     */
    abstract fun official(version: Notation)
    /**
     * ```groovy
     * official version: "v141"
     * official version: latest
     * ```
     */
    fun official(props: Map<String, Any>) {
        when (val version = props["version"]?.toString()) {
            Notation.latest.toString() -> official(version = latest)
            null -> proj.logger.log(LogLevel.WARN, "No \"version\" given in official(Map<String,Any>)")
            else -> official(version)
        }
    }

    abstract fun be(version: String)
    abstract fun be(version: Notation)

    fun be(props: Map<String, Any>) {
        when (val version = props["version"]?.toString()) {
            Notation.latest.toString() -> be(version = latest)
            null -> proj.logger.log(LogLevel.WARN, "No \"version\" given in be(Map<String,Any>)")
            else -> be(version)
        }
    }

    fun fromLocalDisk(path: String) {
        LocalGameLoc(File(path)).checkAndSet()
    }

    fun fromLocalDisk(file: File) {
        LocalGameLoc(file).checkAndSet()
    }

    fun fromLocalDisk(props: Map<String, Any>) {
        val path = props["path"]
        val file = props["file"]
        if (path != null) {
            LocalGameLoc(File(path as String)).checkAndSet()
        } else if (file != null) {
            LocalGameLoc(file as File).checkAndSet()
        } else {
            proj.logger.log(
                LogLevel.WARN,
                "Neither \"path\" nor \"file\" given in fromLocalDisk(Map<String,Any>)"
            )
        }
    }
}

fun formatValidGradleName(raw: String): String {
    val s = StringBuilder()
    var nextUpper = true
    for (c in raw) {
        if (c in '0'..'9' || c in 'a'..'z' || c in 'A'..'Z') {
            if (nextUpper) {
                s += c.uppercaseChar()
                nextUpper = false
            } else {
                s += c
            }
        } else {
            nextUpper = true
        }
    }
    return s.toString()
}