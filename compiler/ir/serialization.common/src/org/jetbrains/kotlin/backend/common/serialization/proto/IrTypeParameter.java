// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: compiler/ir/serialization.common/src/KotlinIr.proto

package org.jetbrains.kotlin.backend.common.serialization.proto;

/**
 * Protobuf type {@code org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter}
 */
public final class IrTypeParameter extends
    org.jetbrains.kotlin.protobuf.GeneratedMessageLite implements
    // @@protoc_insertion_point(message_implements:org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter)
    IrTypeParameterOrBuilder {
  // Use IrTypeParameter.newBuilder() to construct.
  private IrTypeParameter(org.jetbrains.kotlin.protobuf.GeneratedMessageLite.Builder builder) {
    super(builder);
    this.unknownFields = builder.getUnknownFields();
  }
  private IrTypeParameter(boolean noInit) { this.unknownFields = org.jetbrains.kotlin.protobuf.ByteString.EMPTY;}

  private static final IrTypeParameter defaultInstance;
  public static IrTypeParameter getDefaultInstance() {
    return defaultInstance;
  }

  public IrTypeParameter getDefaultInstanceForType() {
    return defaultInstance;
  }

  private final org.jetbrains.kotlin.protobuf.ByteString unknownFields;
  private IrTypeParameter(
      org.jetbrains.kotlin.protobuf.CodedInputStream input,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
    initFields();
    int mutable_bitField0_ = 0;
    org.jetbrains.kotlin.protobuf.ByteString.Output unknownFieldsOutput =
        org.jetbrains.kotlin.protobuf.ByteString.newOutput();
    org.jetbrains.kotlin.protobuf.CodedOutputStream unknownFieldsCodedOutput =
        org.jetbrains.kotlin.protobuf.CodedOutputStream.newInstance(
            unknownFieldsOutput);
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          default: {
            if (!parseUnknownField(input, unknownFieldsCodedOutput,
                                   extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
          case 10: {
            org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase.Builder subBuilder = null;
            if (((bitField0_ & 0x00000001) == 0x00000001)) {
              subBuilder = base_.toBuilder();
            }
            base_ = input.readMessage(org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase.PARSER, extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(base_);
              base_ = subBuilder.buildPartial();
            }
            bitField0_ |= 0x00000001;
            break;
          }
          case 18: {
            org.jetbrains.kotlin.backend.common.serialization.proto.String.Builder subBuilder = null;
            if (((bitField0_ & 0x00000002) == 0x00000002)) {
              subBuilder = name_.toBuilder();
            }
            name_ = input.readMessage(org.jetbrains.kotlin.backend.common.serialization.proto.String.PARSER, extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(name_);
              name_ = subBuilder.buildPartial();
            }
            bitField0_ |= 0x00000002;
            break;
          }
          case 24: {
            bitField0_ |= 0x00000004;
            index_ = input.readInt32();
            break;
          }
          case 32: {
            int rawValue = input.readEnum();
            org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance value = org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance.valueOf(rawValue);
            if (value == null) {
              unknownFieldsCodedOutput.writeRawVarint32(tag);
              unknownFieldsCodedOutput.writeRawVarint32(rawValue);
            } else {
              bitField0_ |= 0x00000008;
              variance_ = value;
            }
            break;
          }
          case 42: {
            if (!((mutable_bitField0_ & 0x00000010) == 0x00000010)) {
              superType_ = new java.util.ArrayList<org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex>();
              mutable_bitField0_ |= 0x00000010;
            }
            superType_.add(input.readMessage(org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex.PARSER, extensionRegistry));
            break;
          }
          case 48: {
            bitField0_ |= 0x00000010;
            isReified_ = input.readBool();
            break;
          }
        }
      }
    } catch (org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException(
          e.getMessage()).setUnfinishedMessage(this);
    } finally {
      if (((mutable_bitField0_ & 0x00000010) == 0x00000010)) {
        superType_ = java.util.Collections.unmodifiableList(superType_);
      }
      try {
        unknownFieldsCodedOutput.flush();
      } catch (java.io.IOException e) {
      // Should not happen
      } finally {
        unknownFields = unknownFieldsOutput.toByteString();
      }
      makeExtensionsImmutable();
    }
  }
  public static org.jetbrains.kotlin.protobuf.Parser<IrTypeParameter> PARSER =
      new org.jetbrains.kotlin.protobuf.AbstractParser<IrTypeParameter>() {
    public IrTypeParameter parsePartialFrom(
        org.jetbrains.kotlin.protobuf.CodedInputStream input,
        org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
        throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
      return new IrTypeParameter(input, extensionRegistry);
    }
  };

  @java.lang.Override
  public org.jetbrains.kotlin.protobuf.Parser<IrTypeParameter> getParserForType() {
    return PARSER;
  }

  private int bitField0_;
  public static final int BASE_FIELD_NUMBER = 1;
  private org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase base_;
  /**
   * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase base = 1;</code>
   */
  public boolean hasBase() {
    return ((bitField0_ & 0x00000001) == 0x00000001);
  }
  /**
   * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase base = 1;</code>
   */
  public org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase getBase() {
    return base_;
  }

  public static final int NAME_FIELD_NUMBER = 2;
  private org.jetbrains.kotlin.backend.common.serialization.proto.String name_;
  /**
   * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.String name = 2;</code>
   */
  public boolean hasName() {
    return ((bitField0_ & 0x00000002) == 0x00000002);
  }
  /**
   * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.String name = 2;</code>
   */
  public org.jetbrains.kotlin.backend.common.serialization.proto.String getName() {
    return name_;
  }

  public static final int INDEX_FIELD_NUMBER = 3;
  private int index_;
  /**
   * <code>required int32 index = 3;</code>
   */
  public boolean hasIndex() {
    return ((bitField0_ & 0x00000004) == 0x00000004);
  }
  /**
   * <code>required int32 index = 3;</code>
   */
  public int getIndex() {
    return index_;
  }

  public static final int VARIANCE_FIELD_NUMBER = 4;
  private org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance variance_;
  /**
   * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance variance = 4;</code>
   */
  public boolean hasVariance() {
    return ((bitField0_ & 0x00000008) == 0x00000008);
  }
  /**
   * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance variance = 4;</code>
   */
  public org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance getVariance() {
    return variance_;
  }

  public static final int SUPER_TYPE_FIELD_NUMBER = 5;
  private java.util.List<org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex> superType_;
  /**
   * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex super_type = 5;</code>
   */
  public java.util.List<org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex> getSuperTypeList() {
    return superType_;
  }
  /**
   * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex super_type = 5;</code>
   */
  public java.util.List<? extends org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndexOrBuilder> 
      getSuperTypeOrBuilderList() {
    return superType_;
  }
  /**
   * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex super_type = 5;</code>
   */
  public int getSuperTypeCount() {
    return superType_.size();
  }
  /**
   * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex super_type = 5;</code>
   */
  public org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex getSuperType(int index) {
    return superType_.get(index);
  }
  /**
   * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex super_type = 5;</code>
   */
  public org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndexOrBuilder getSuperTypeOrBuilder(
      int index) {
    return superType_.get(index);
  }

  public static final int IS_REIFIED_FIELD_NUMBER = 6;
  private boolean isReified_;
  /**
   * <code>required bool is_reified = 6;</code>
   */
  public boolean hasIsReified() {
    return ((bitField0_ & 0x00000010) == 0x00000010);
  }
  /**
   * <code>required bool is_reified = 6;</code>
   */
  public boolean getIsReified() {
    return isReified_;
  }

  private void initFields() {
    base_ = org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase.getDefaultInstance();
    name_ = org.jetbrains.kotlin.backend.common.serialization.proto.String.getDefaultInstance();
    index_ = 0;
    variance_ = org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance.IN;
    superType_ = java.util.Collections.emptyList();
    isReified_ = false;
  }
  private byte memoizedIsInitialized = -1;
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    if (!hasBase()) {
      memoizedIsInitialized = 0;
      return false;
    }
    if (!hasName()) {
      memoizedIsInitialized = 0;
      return false;
    }
    if (!hasIndex()) {
      memoizedIsInitialized = 0;
      return false;
    }
    if (!hasVariance()) {
      memoizedIsInitialized = 0;
      return false;
    }
    if (!hasIsReified()) {
      memoizedIsInitialized = 0;
      return false;
    }
    if (!getBase().isInitialized()) {
      memoizedIsInitialized = 0;
      return false;
    }
    if (!getName().isInitialized()) {
      memoizedIsInitialized = 0;
      return false;
    }
    for (int i = 0; i < getSuperTypeCount(); i++) {
      if (!getSuperType(i).isInitialized()) {
        memoizedIsInitialized = 0;
        return false;
      }
    }
    memoizedIsInitialized = 1;
    return true;
  }

  public void writeTo(org.jetbrains.kotlin.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    getSerializedSize();
    if (((bitField0_ & 0x00000001) == 0x00000001)) {
      output.writeMessage(1, base_);
    }
    if (((bitField0_ & 0x00000002) == 0x00000002)) {
      output.writeMessage(2, name_);
    }
    if (((bitField0_ & 0x00000004) == 0x00000004)) {
      output.writeInt32(3, index_);
    }
    if (((bitField0_ & 0x00000008) == 0x00000008)) {
      output.writeEnum(4, variance_.getNumber());
    }
    for (int i = 0; i < superType_.size(); i++) {
      output.writeMessage(5, superType_.get(i));
    }
    if (((bitField0_ & 0x00000010) == 0x00000010)) {
      output.writeBool(6, isReified_);
    }
    output.writeRawBytes(unknownFields);
  }

  private int memoizedSerializedSize = -1;
  public int getSerializedSize() {
    int size = memoizedSerializedSize;
    if (size != -1) return size;

    size = 0;
    if (((bitField0_ & 0x00000001) == 0x00000001)) {
      size += org.jetbrains.kotlin.protobuf.CodedOutputStream
        .computeMessageSize(1, base_);
    }
    if (((bitField0_ & 0x00000002) == 0x00000002)) {
      size += org.jetbrains.kotlin.protobuf.CodedOutputStream
        .computeMessageSize(2, name_);
    }
    if (((bitField0_ & 0x00000004) == 0x00000004)) {
      size += org.jetbrains.kotlin.protobuf.CodedOutputStream
        .computeInt32Size(3, index_);
    }
    if (((bitField0_ & 0x00000008) == 0x00000008)) {
      size += org.jetbrains.kotlin.protobuf.CodedOutputStream
        .computeEnumSize(4, variance_.getNumber());
    }
    for (int i = 0; i < superType_.size(); i++) {
      size += org.jetbrains.kotlin.protobuf.CodedOutputStream
        .computeMessageSize(5, superType_.get(i));
    }
    if (((bitField0_ & 0x00000010) == 0x00000010)) {
      size += org.jetbrains.kotlin.protobuf.CodedOutputStream
        .computeBoolSize(6, isReified_);
    }
    size += unknownFields.size();
    memoizedSerializedSize = size;
    return size;
  }

  private static final long serialVersionUID = 0L;
  @java.lang.Override
  protected java.lang.Object writeReplace()
      throws java.io.ObjectStreamException {
    return super.writeReplace();
  }

  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter parseFrom(
      org.jetbrains.kotlin.protobuf.ByteString data)
      throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter parseFrom(
      org.jetbrains.kotlin.protobuf.ByteString data,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter parseFrom(byte[] data)
      throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter parseFrom(
      byte[] data,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter parseFrom(
      java.io.InputStream input,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter parseDelimitedFrom(
      java.io.InputStream input,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseDelimitedFrom(input, extensionRegistry);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter parseFrom(
      org.jetbrains.kotlin.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return PARSER.parseFrom(input);
  }
  public static org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter parseFrom(
      org.jetbrains.kotlin.protobuf.CodedInputStream input,
      org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return PARSER.parseFrom(input, extensionRegistry);
  }

  public static Builder newBuilder() { return Builder.create(); }
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder(org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter prototype) {
    return newBuilder().mergeFrom(prototype);
  }
  public Builder toBuilder() { return newBuilder(this); }

  /**
   * Protobuf type {@code org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter}
   */
  public static final class Builder extends
      org.jetbrains.kotlin.protobuf.GeneratedMessageLite.Builder<
        org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter, Builder>
      implements
      // @@protoc_insertion_point(builder_implements:org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter)
      org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameterOrBuilder {
    // Construct using org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private void maybeForceBuilderInitialization() {
    }
    private static Builder create() {
      return new Builder();
    }

    public Builder clear() {
      super.clear();
      base_ = org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase.getDefaultInstance();
      bitField0_ = (bitField0_ & ~0x00000001);
      name_ = org.jetbrains.kotlin.backend.common.serialization.proto.String.getDefaultInstance();
      bitField0_ = (bitField0_ & ~0x00000002);
      index_ = 0;
      bitField0_ = (bitField0_ & ~0x00000004);
      variance_ = org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance.IN;
      bitField0_ = (bitField0_ & ~0x00000008);
      superType_ = java.util.Collections.emptyList();
      bitField0_ = (bitField0_ & ~0x00000010);
      isReified_ = false;
      bitField0_ = (bitField0_ & ~0x00000020);
      return this;
    }

    public Builder clone() {
      return create().mergeFrom(buildPartial());
    }

    public org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter getDefaultInstanceForType() {
      return org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter.getDefaultInstance();
    }

    public org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter build() {
      org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter buildPartial() {
      org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter result = new org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter(this);
      int from_bitField0_ = bitField0_;
      int to_bitField0_ = 0;
      if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
        to_bitField0_ |= 0x00000001;
      }
      result.base_ = base_;
      if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
        to_bitField0_ |= 0x00000002;
      }
      result.name_ = name_;
      if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
        to_bitField0_ |= 0x00000004;
      }
      result.index_ = index_;
      if (((from_bitField0_ & 0x00000008) == 0x00000008)) {
        to_bitField0_ |= 0x00000008;
      }
      result.variance_ = variance_;
      if (((bitField0_ & 0x00000010) == 0x00000010)) {
        superType_ = java.util.Collections.unmodifiableList(superType_);
        bitField0_ = (bitField0_ & ~0x00000010);
      }
      result.superType_ = superType_;
      if (((from_bitField0_ & 0x00000020) == 0x00000020)) {
        to_bitField0_ |= 0x00000010;
      }
      result.isReified_ = isReified_;
      result.bitField0_ = to_bitField0_;
      return result;
    }

    public Builder mergeFrom(org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter other) {
      if (other == org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter.getDefaultInstance()) return this;
      if (other.hasBase()) {
        mergeBase(other.getBase());
      }
      if (other.hasName()) {
        mergeName(other.getName());
      }
      if (other.hasIndex()) {
        setIndex(other.getIndex());
      }
      if (other.hasVariance()) {
        setVariance(other.getVariance());
      }
      if (!other.superType_.isEmpty()) {
        if (superType_.isEmpty()) {
          superType_ = other.superType_;
          bitField0_ = (bitField0_ & ~0x00000010);
        } else {
          ensureSuperTypeIsMutable();
          superType_.addAll(other.superType_);
        }
        
      }
      if (other.hasIsReified()) {
        setIsReified(other.getIsReified());
      }
      setUnknownFields(
          getUnknownFields().concat(other.unknownFields));
      return this;
    }

    public final boolean isInitialized() {
      if (!hasBase()) {
        
        return false;
      }
      if (!hasName()) {
        
        return false;
      }
      if (!hasIndex()) {
        
        return false;
      }
      if (!hasVariance()) {
        
        return false;
      }
      if (!hasIsReified()) {
        
        return false;
      }
      if (!getBase().isInitialized()) {
        
        return false;
      }
      if (!getName().isInitialized()) {
        
        return false;
      }
      for (int i = 0; i < getSuperTypeCount(); i++) {
        if (!getSuperType(i).isInitialized()) {
          
          return false;
        }
      }
      return true;
    }

    public Builder mergeFrom(
        org.jetbrains.kotlin.protobuf.CodedInputStream input,
        org.jetbrains.kotlin.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter) e.getUnfinishedMessage();
        throw e;
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }
    private int bitField0_;

    private org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase base_ = org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase.getDefaultInstance();
    /**
     * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase base = 1;</code>
     */
    public boolean hasBase() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }
    /**
     * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase base = 1;</code>
     */
    public org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase getBase() {
      return base_;
    }
    /**
     * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase base = 1;</code>
     */
    public Builder setBase(org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase value) {
      if (value == null) {
        throw new NullPointerException();
      }
      base_ = value;

      bitField0_ |= 0x00000001;
      return this;
    }
    /**
     * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase base = 1;</code>
     */
    public Builder setBase(
        org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase.Builder builderForValue) {
      base_ = builderForValue.build();

      bitField0_ |= 0x00000001;
      return this;
    }
    /**
     * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase base = 1;</code>
     */
    public Builder mergeBase(org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase value) {
      if (((bitField0_ & 0x00000001) == 0x00000001) &&
          base_ != org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase.getDefaultInstance()) {
        base_ =
          org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase.newBuilder(base_).mergeFrom(value).buildPartial();
      } else {
        base_ = value;
      }

      bitField0_ |= 0x00000001;
      return this;
    }
    /**
     * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase base = 1;</code>
     */
    public Builder clearBase() {
      base_ = org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase.getDefaultInstance();

      bitField0_ = (bitField0_ & ~0x00000001);
      return this;
    }

    private org.jetbrains.kotlin.backend.common.serialization.proto.String name_ = org.jetbrains.kotlin.backend.common.serialization.proto.String.getDefaultInstance();
    /**
     * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.String name = 2;</code>
     */
    public boolean hasName() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }
    /**
     * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.String name = 2;</code>
     */
    public org.jetbrains.kotlin.backend.common.serialization.proto.String getName() {
      return name_;
    }
    /**
     * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.String name = 2;</code>
     */
    public Builder setName(org.jetbrains.kotlin.backend.common.serialization.proto.String value) {
      if (value == null) {
        throw new NullPointerException();
      }
      name_ = value;

      bitField0_ |= 0x00000002;
      return this;
    }
    /**
     * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.String name = 2;</code>
     */
    public Builder setName(
        org.jetbrains.kotlin.backend.common.serialization.proto.String.Builder builderForValue) {
      name_ = builderForValue.build();

      bitField0_ |= 0x00000002;
      return this;
    }
    /**
     * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.String name = 2;</code>
     */
    public Builder mergeName(org.jetbrains.kotlin.backend.common.serialization.proto.String value) {
      if (((bitField0_ & 0x00000002) == 0x00000002) &&
          name_ != org.jetbrains.kotlin.backend.common.serialization.proto.String.getDefaultInstance()) {
        name_ =
          org.jetbrains.kotlin.backend.common.serialization.proto.String.newBuilder(name_).mergeFrom(value).buildPartial();
      } else {
        name_ = value;
      }

      bitField0_ |= 0x00000002;
      return this;
    }
    /**
     * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.String name = 2;</code>
     */
    public Builder clearName() {
      name_ = org.jetbrains.kotlin.backend.common.serialization.proto.String.getDefaultInstance();

      bitField0_ = (bitField0_ & ~0x00000002);
      return this;
    }

    private int index_ ;
    /**
     * <code>required int32 index = 3;</code>
     */
    public boolean hasIndex() {
      return ((bitField0_ & 0x00000004) == 0x00000004);
    }
    /**
     * <code>required int32 index = 3;</code>
     */
    public int getIndex() {
      return index_;
    }
    /**
     * <code>required int32 index = 3;</code>
     */
    public Builder setIndex(int value) {
      bitField0_ |= 0x00000004;
      index_ = value;
      
      return this;
    }
    /**
     * <code>required int32 index = 3;</code>
     */
    public Builder clearIndex() {
      bitField0_ = (bitField0_ & ~0x00000004);
      index_ = 0;
      
      return this;
    }

    private org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance variance_ = org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance.IN;
    /**
     * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance variance = 4;</code>
     */
    public boolean hasVariance() {
      return ((bitField0_ & 0x00000008) == 0x00000008);
    }
    /**
     * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance variance = 4;</code>
     */
    public org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance getVariance() {
      return variance_;
    }
    /**
     * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance variance = 4;</code>
     */
    public Builder setVariance(org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance value) {
      if (value == null) {
        throw new NullPointerException();
      }
      bitField0_ |= 0x00000008;
      variance_ = value;
      
      return this;
    }
    /**
     * <code>required .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance variance = 4;</code>
     */
    public Builder clearVariance() {
      bitField0_ = (bitField0_ & ~0x00000008);
      variance_ = org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance.IN;
      
      return this;
    }

    private java.util.List<org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex> superType_ =
      java.util.Collections.emptyList();
    private void ensureSuperTypeIsMutable() {
      if (!((bitField0_ & 0x00000010) == 0x00000010)) {
        superType_ = new java.util.ArrayList<org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex>(superType_);
        bitField0_ |= 0x00000010;
       }
    }

    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex super_type = 5;</code>
     */
    public java.util.List<org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex> getSuperTypeList() {
      return java.util.Collections.unmodifiableList(superType_);
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex super_type = 5;</code>
     */
    public int getSuperTypeCount() {
      return superType_.size();
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex super_type = 5;</code>
     */
    public org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex getSuperType(int index) {
      return superType_.get(index);
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex super_type = 5;</code>
     */
    public Builder setSuperType(
        int index, org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex value) {
      if (value == null) {
        throw new NullPointerException();
      }
      ensureSuperTypeIsMutable();
      superType_.set(index, value);

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex super_type = 5;</code>
     */
    public Builder setSuperType(
        int index, org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex.Builder builderForValue) {
      ensureSuperTypeIsMutable();
      superType_.set(index, builderForValue.build());

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex super_type = 5;</code>
     */
    public Builder addSuperType(org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex value) {
      if (value == null) {
        throw new NullPointerException();
      }
      ensureSuperTypeIsMutable();
      superType_.add(value);

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex super_type = 5;</code>
     */
    public Builder addSuperType(
        int index, org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex value) {
      if (value == null) {
        throw new NullPointerException();
      }
      ensureSuperTypeIsMutable();
      superType_.add(index, value);

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex super_type = 5;</code>
     */
    public Builder addSuperType(
        org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex.Builder builderForValue) {
      ensureSuperTypeIsMutable();
      superType_.add(builderForValue.build());

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex super_type = 5;</code>
     */
    public Builder addSuperType(
        int index, org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex.Builder builderForValue) {
      ensureSuperTypeIsMutable();
      superType_.add(index, builderForValue.build());

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex super_type = 5;</code>
     */
    public Builder addAllSuperType(
        java.lang.Iterable<? extends org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex> values) {
      ensureSuperTypeIsMutable();
      org.jetbrains.kotlin.protobuf.AbstractMessageLite.Builder.addAll(
          values, superType_);

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex super_type = 5;</code>
     */
    public Builder clearSuperType() {
      superType_ = java.util.Collections.emptyList();
      bitField0_ = (bitField0_ & ~0x00000010);

      return this;
    }
    /**
     * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeIndex super_type = 5;</code>
     */
    public Builder removeSuperType(int index) {
      ensureSuperTypeIsMutable();
      superType_.remove(index);

      return this;
    }

    private boolean isReified_ ;
    /**
     * <code>required bool is_reified = 6;</code>
     */
    public boolean hasIsReified() {
      return ((bitField0_ & 0x00000020) == 0x00000020);
    }
    /**
     * <code>required bool is_reified = 6;</code>
     */
    public boolean getIsReified() {
      return isReified_;
    }
    /**
     * <code>required bool is_reified = 6;</code>
     */
    public Builder setIsReified(boolean value) {
      bitField0_ |= 0x00000020;
      isReified_ = value;
      
      return this;
    }
    /**
     * <code>required bool is_reified = 6;</code>
     */
    public Builder clearIsReified() {
      bitField0_ = (bitField0_ & ~0x00000020);
      isReified_ = false;
      
      return this;
    }

    // @@protoc_insertion_point(builder_scope:org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter)
  }

  static {
    defaultInstance = new IrTypeParameter(true);
    defaultInstance.initFields();
  }

  // @@protoc_insertion_point(class_scope:org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter)
}
