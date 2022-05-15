package org.jetbrains.konan.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.openapi.vfs.*
import org.jetbrains.konan.KotlinNativeToolchain
import org.jetbrains.konan.settings.KonanModelProvider.RELOAD_TOPIC
import org.jetbrains.kotlin.utils.addIfNotNull
import java.nio.file.Path

private const val LINK_DATA: String = "linkdata"

abstract class KonanProjectComponent(val project: Project) : ProjectComponent {
  companion object {
    fun getInstance(project: Project): KonanProjectComponent = project.getComponent(KonanProjectComponent::class.java)
  }

  protected var libraryPaths: Set<String> = emptySet()

  override fun getComponentName(): String = "Kotlin/Native Project component"

  override fun projectOpened() {
    VirtualFileManager.getInstance().addVirtualFileListener(object : VirtualFileListener {
      override fun fileCreated(event: VirtualFileEvent) {
        val filePath = event.file.path
        if (libraryPaths.contains(filePath)) reloadLibraries()
      }
    }, project)

    val connection = project.messageBus.connect(project)
    connection.subscribe(RELOAD_TOPIC, Runnable {
      ApplicationManager.getApplication().invokeLater {
        KotlinNativeToolchain.BUNDLED.ensureExists(project)
        reloadLibraries()
      }
    })

    if (looksLikeKotlinNativeProject()) {
      reloadLibraries()
    }
  }

  open fun reloadLibraries(): Unit = synchronized(this) {
    val libraryPaths: Set<Path> = collectLibraryPaths()
    this.libraryPaths = libraryPaths.asSequence().map(Path::toString).toSet()

//    for (konanModelProvider in KonanModelProvider.EP_NAME.extensions) {
//      if (konanModelProvider.reloadLibraries(project, libraryPaths)) return
//    }

    val module = ModuleManager.getInstance(project).modules.firstOrNull() ?: return
    val modifiableModel = ModuleRootManager.getInstance(module).modifiableModel
    val libraryTable: LibraryTable = modifiableModel.moduleLibraryTable
    libraryTable.libraries.forEach { libraryTable.removeLibrary(it) }

    val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
    val jarFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.JAR_PROTOCOL)

    for (path: Path in libraryPaths) {

      fun createLibrary(vfs: VirtualFileSystem, linkDataPath: String) {
        val linkData = vfs.refreshAndFindFileByPath(linkDataPath) ?: return

        val library = libraryTable.createLibrary(path.fileName.toString())
        val libraryModifiableModel = library.modifiableModel

        libraryModifiableModel.addRoot(linkData, OrderRootType.CLASSES)

        runWriteAction {
          libraryModifiableModel.commit()
        }
      }

      val libraryRootPath = path.toString()
      val libraryRoot: VirtualFile = localFileSystem.refreshAndFindFileByPath(libraryRootPath) ?: continue

      if (libraryRoot.isDirectory) {
        createLibrary(localFileSystem, "$libraryRootPath/$LINK_DATA")
      }
      else { //zip
        createLibrary(jarFileSystem, "$libraryRootPath!/$LINK_DATA")
      }
    }

    runWriteAction {
      modifiableModel.commit()
    }
  }

  protected open fun collectLibraryPaths(): MutableSet<Path> {
    val konanPaths = KonanPaths.getInstance(project)
    return mutableSetOf<Path>().apply {
      addIfNotNull(konanPaths.konanStdlib())
      addAll(konanPaths.konanPlatformLibraries())
    }
  }

  abstract fun looksLikeKotlinNativeProject(): Boolean
}