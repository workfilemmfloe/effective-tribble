// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: core/serialization/src/builtins.debug.proto

package org.jetbrains.kotlin.serialization.builtins;

public final class DebugBuiltInsProtoBuf {
  private DebugBuiltInsProtoBuf() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registry.add(org.jetbrains.kotlin.serialization.builtins.DebugBuiltInsProtoBuf.className);
    registry.add(org.jetbrains.kotlin.serialization.builtins.DebugBuiltInsProtoBuf.classAnnotation);
    registry.add(org.jetbrains.kotlin.serialization.builtins.DebugBuiltInsProtoBuf.callableAnnotation);
    registry.add(org.jetbrains.kotlin.serialization.builtins.DebugBuiltInsProtoBuf.compileTimeValue);
    registry.add(org.jetbrains.kotlin.serialization.builtins.DebugBuiltInsProtoBuf.parameterAnnotation);
  }
  public static final int CLASS_NAME_FIELD_NUMBER = 150;
  /**
   * <code>extend .org.jetbrains.kotlin.serialization.Package { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      org.jetbrains.kotlin.serialization.DebugProtoBuf.Package,
      java.util.List<java.lang.Integer>> className = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        java.lang.Integer.class,
        null);
  public static final int CLASS_ANNOTATION_FIELD_NUMBER = 150;
  /**
   * <code>extend .org.jetbrains.kotlin.serialization.Class { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      org.jetbrains.kotlin.serialization.DebugProtoBuf.Class,
      java.util.List<org.jetbrains.kotlin.serialization.DebugProtoBuf.Annotation>> classAnnotation = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        org.jetbrains.kotlin.serialization.DebugProtoBuf.Annotation.class,
        org.jetbrains.kotlin.serialization.DebugProtoBuf.Annotation.getDefaultInstance());
  public static final int CALLABLE_ANNOTATION_FIELD_NUMBER = 150;
  /**
   * <code>extend .org.jetbrains.kotlin.serialization.Callable { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      org.jetbrains.kotlin.serialization.DebugProtoBuf.Callable,
      java.util.List<org.jetbrains.kotlin.serialization.DebugProtoBuf.Annotation>> callableAnnotation = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        org.jetbrains.kotlin.serialization.DebugProtoBuf.Annotation.class,
        org.jetbrains.kotlin.serialization.DebugProtoBuf.Annotation.getDefaultInstance());
  public static final int COMPILE_TIME_VALUE_FIELD_NUMBER = 151;
  /**
   * <code>extend .org.jetbrains.kotlin.serialization.Callable { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      org.jetbrains.kotlin.serialization.DebugProtoBuf.Callable,
      org.jetbrains.kotlin.serialization.DebugProtoBuf.Annotation.Argument.Value> compileTimeValue = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        org.jetbrains.kotlin.serialization.DebugProtoBuf.Annotation.Argument.Value.class,
        org.jetbrains.kotlin.serialization.DebugProtoBuf.Annotation.Argument.Value.getDefaultInstance());
  public static final int PARAMETER_ANNOTATION_FIELD_NUMBER = 150;
  /**
   * <code>extend .org.jetbrains.kotlin.serialization.Callable.ValueParameter { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      org.jetbrains.kotlin.serialization.DebugProtoBuf.Callable.ValueParameter,
      java.util.List<org.jetbrains.kotlin.serialization.DebugProtoBuf.Annotation>> parameterAnnotation = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        org.jetbrains.kotlin.serialization.DebugProtoBuf.Annotation.class,
        org.jetbrains.kotlin.serialization.DebugProtoBuf.Annotation.getDefaultInstance());

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n+core/serialization/src/builtins.debug." +
      "proto\022+org.jetbrains.kotlin.serializatio" +
      "n.builtins\032.core/serialization/src/descr" +
      "iptors.debug.proto:D\n\nclass_name\022+.org.j" +
      "etbrains.kotlin.serialization.Package\030\226\001" +
      " \003(\005B\002\020\001:t\n\020class_annotation\022).org.jetbr" +
      "ains.kotlin.serialization.Class\030\226\001 \003(\0132." +
      ".org.jetbrains.kotlin.serialization.Anno" +
      "tation:z\n\023callable_annotation\022,.org.jetb" +
      "rains.kotlin.serialization.Callable\030\226\001 \003",
      "(\0132..org.jetbrains.kotlin.serialization." +
      "Annotation:\210\001\n\022compile_time_value\022,.org." +
      "jetbrains.kotlin.serialization.Callable\030" +
      "\227\001 \001(\0132=.org.jetbrains.kotlin.serializat" +
      "ion.Annotation.Argument.Value:\212\001\n\024parame" +
      "ter_annotation\022;.org.jetbrains.kotlin.se" +
      "rialization.Callable.ValueParameter\030\226\001 \003" +
      "(\0132..org.jetbrains.kotlin.serialization." +
      "AnnotationB\027B\025DebugBuiltInsProtoBuf"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
      new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
        public com.google.protobuf.ExtensionRegistry assignDescriptors(
            com.google.protobuf.Descriptors.FileDescriptor root) {
          descriptor = root;
          className.internalInit(descriptor.getExtensions().get(0));
          classAnnotation.internalInit(descriptor.getExtensions().get(1));
          callableAnnotation.internalInit(descriptor.getExtensions().get(2));
          compileTimeValue.internalInit(descriptor.getExtensions().get(3));
          parameterAnnotation.internalInit(descriptor.getExtensions().get(4));
          return null;
        }
      };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          org.jetbrains.kotlin.serialization.DebugProtoBuf.getDescriptor(),
        }, assigner);
  }

  // @@protoc_insertion_point(outer_class_scope)
}