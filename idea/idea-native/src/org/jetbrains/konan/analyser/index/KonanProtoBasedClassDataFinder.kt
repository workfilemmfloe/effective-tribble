package org.jetbrains.konan.analyser.index

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.metadata.KonanLinkData
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.ClassData
import org.jetbrains.kotlin.serialization.deserialization.ClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.getClassId

//todo: Fix in Kotlin plugin
class KonanProtoBasedClassDataFinder(
  proto: KonanLinkData.LinkDataPackageFragment,
  private val nameResolver: NameResolver,
  private val classSource: (ClassId) -> SourceElement = { SourceElement.NO_SOURCE }
) : ClassDataFinder {
  private val classIdToProto =
    proto.classes.classesList.associateBy { klass ->
      nameResolver.getClassId(klass.fqName)
    }

  internal val allClassIds: Collection<ClassId> get() = classIdToProto.keys

  override fun findClassData(classId: ClassId): ClassData? {
    val classProto = classIdToProto[classId] ?: return null
    return ClassData(nameResolver, classProto, classSource(classId))
  }
}
