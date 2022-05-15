// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: compiler/ir/serialization.common/src/KotlinIr.proto

package org.jetbrains.kotlin.backend.common.serialization.proto;

public interface IrTypeParameterOrBuilder extends
    // @@protoc_insertion_point(interface_extends:org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter)
    org.jetbrains.kotlin.protobuf.MessageLiteOrBuilder {

  /**
   * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase base = 1;</code>
   */
  boolean hasBase();
  /**
   * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase base = 1;</code>
   */
  org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase getBase();

  /**
   * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.String name = 2;</code>
   */
  boolean hasName();
  /**
   * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.String name = 2;</code>
   */
  org.jetbrains.kotlin.backend.common.serialization.proto.String getName();

  /**
   * <code>required int32 index = 3;</code>
   */
  boolean hasIndex();
  /**
   * <code>required int32 index = 3;</code>
   */
  int getIndex();

  /**
   * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance variance = 4;</code>
   */
  boolean hasVariance();
  /**
   * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance variance = 4;</code>
   */
  org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance getVariance();

  /**
   * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex super_type = 5;</code>
   */
  java.util.List<org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex> 
      getSuperTypeList();
  /**
   * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex super_type = 5;</code>
   */
  org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex getSuperType(int index);
  /**
   * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex super_type = 5;</code>
   */
  int getSuperTypeCount();

  /**
   * <code>required bool is_reified = 6;</code>
   */
  boolean hasIsReified();
  /**
   * <code>required bool is_reified = 6;</code>
   */
  boolean getIsReified();
}