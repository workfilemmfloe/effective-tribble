// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: compiler/ir/serialization.common/src/KotlinIr.proto

package org.jetbrains.kotlin.backend.common.serialization.proto;

public interface IdSignatureOrBuilder extends
    // @@protoc_insertion_point(interface_extends:org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature)
    org.jetbrains.kotlin.protobuf.MessageLiteOrBuilder {

  /**
   * <code>optional .org.jetbrains.kotlin.backend.common.serialization.proto.CommonIdSignature public_sig = 1;</code>
   */
  boolean hasPublicSig();
  /**
   * <code>optional .org.jetbrains.kotlin.backend.common.serialization.proto.CommonIdSignature public_sig = 1;</code>
   */
  org.jetbrains.kotlin.backend.common.serialization.proto.CommonIdSignature getPublicSig();

  /**
   * <code>optional .org.jetbrains.kotlin.backend.common.serialization.proto.FileLocalIdSignature private_sig = 2;</code>
   */
  boolean hasPrivateSig();
  /**
   * <code>optional .org.jetbrains.kotlin.backend.common.serialization.proto.FileLocalIdSignature private_sig = 2;</code>
   */
  org.jetbrains.kotlin.backend.common.serialization.proto.FileLocalIdSignature getPrivateSig();

  /**
   * <code>optional .org.jetbrains.kotlin.backend.common.serialization.proto.AccessorIdSignature accessor_sig = 3;</code>
   */
  boolean hasAccessorSig();
  /**
   * <code>optional .org.jetbrains.kotlin.backend.common.serialization.proto.AccessorIdSignature accessor_sig = 3;</code>
   */
  org.jetbrains.kotlin.backend.common.serialization.proto.AccessorIdSignature getAccessorSig();

  /**
   * <code>optional int32 scoped_local_sig = 4;</code>
   */
  boolean hasScopedLocalSig();
  /**
   * <code>optional int32 scoped_local_sig = 4;</code>
   */
  int getScopedLocalSig();

  /**
   * <code>optional .org.jetbrains.kotlin.backend.common.serialization.proto.CompositeSignature composite_sig = 5;</code>
   */
  boolean hasCompositeSig();
  /**
   * <code>optional .org.jetbrains.kotlin.backend.common.serialization.proto.CompositeSignature composite_sig = 5;</code>
   */
  org.jetbrains.kotlin.backend.common.serialization.proto.CompositeSignature getCompositeSig();

  /**
   * <code>optional .org.jetbrains.kotlin.backend.common.serialization.proto.LocalSignature local_sig = 6;</code>
   */
  boolean hasLocalSig();
  /**
   * <code>optional .org.jetbrains.kotlin.backend.common.serialization.proto.LocalSignature local_sig = 6;</code>
   */
  org.jetbrains.kotlin.backend.common.serialization.proto.LocalSignature getLocalSig();

  /**
   * <code>optional .org.jetbrains.kotlin.backend.common.serialization.proto.FileSignature file_sig = 7;</code>
   */
  boolean hasFileSig();
  /**
   * <code>optional .org.jetbrains.kotlin.backend.common.serialization.proto.FileSignature file_sig = 7;</code>
   */
  org.jetbrains.kotlin.backend.common.serialization.proto.FileSignature getFileSig();
}