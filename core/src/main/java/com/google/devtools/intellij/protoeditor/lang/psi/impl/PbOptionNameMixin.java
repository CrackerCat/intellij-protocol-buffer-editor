/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.devtools.intellij.protoeditor.lang.psi.impl;

import static com.google.devtools.intellij.protoeditor.lang.psi.ProtoTokenTypes.IDENTIFIER_LITERAL;

import com.google.devtools.intellij.protoeditor.lang.descriptor.Descriptor;
import com.google.devtools.intellij.protoeditor.lang.psi.PbExtensionName;
import com.google.devtools.intellij.protoeditor.lang.psi.PbField;
import com.google.devtools.intellij.protoeditor.lang.psi.PbMessageType;
import com.google.devtools.intellij.protoeditor.lang.psi.PbNamedTypeElement;
import com.google.devtools.intellij.protoeditor.lang.psi.PbOptionName;
import com.google.devtools.intellij.protoeditor.lang.psi.PbOptionOwner;
import com.google.devtools.intellij.protoeditor.lang.psi.PbTypeName;
import com.google.devtools.intellij.protoeditor.lang.psi.SpecialOptionType;
import com.google.devtools.intellij.protoeditor.lang.psi.util.PbPsiImplUtil;
import com.google.devtools.intellij.protoeditor.lang.psi.util.PbPsiUtil;
import com.google.devtools.intellij.protoeditor.lang.resolve.PbOptionNameReference;
import com.google.devtools.intellij.protoeditor.lang.resolve.PbResolveResult;
import com.google.devtools.intellij.protoeditor.lang.resolve.PbSymbolResolver;
import com.google.devtools.intellij.protoeditor.lang.resolve.ResolveFilters;
import com.google.devtools.intellij.protoeditor.lang.util.BuiltInType;
import com.google.devtools.intellij.protoeditor.lang.util.ValueTester;
import com.google.devtools.intellij.protoeditor.lang.util.ValueTester.ValueTesterType;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

abstract class PbOptionNameMixin extends PbElementBase implements PbOptionName {

  private static final Logger logger = Logger.getInstance(PbOptionNameReference.class);

  PbOptionNameMixin(ASTNode node) {
    super(node);
  }

  @Nullable
  @Override
  public PbOptionName getQualifier() {
    return PsiTreeUtil.getChildOfType(this, PbOptionName.class);
  }

  @Nullable
  @Override
  public PbExtensionName getExtensionName() {
    return PsiTreeUtil.getChildOfType(this, PbExtensionName.class);
  }

  @Nullable
  @Override
  public PsiElement getSymbol() {
    return findChildByType(IDENTIFIER_LITERAL);
  }

  @Override
  public PsiReference getReference() {
    // Return a reference if this is not a special built-in option. If this option is an extension,
    // return null and let the ExtensionName element handle the reference.
    if (getExtensionName() != null || isSpecial()) {
      return null;
    }
    return new PbOptionNameReference(this);
  }

  @Nullable
  @Override
  public PsiReference getEffectiveReference() {
    // Return a reference if this is not a special built-in option. If this option is an extension,
    // return the reference from getExtensionName().getEffectiveReference().
    PbExtensionName extensionName = getExtensionName();
    if (extensionName != null) {
      return extensionName.getEffectiveReference();
    }
    return getReference();
  }

  @Override
  @Nullable
  public PbMessageType getQualifierType() {
    return CachedValuesManager.getCachedValue(
        this,
        () -> Result.create(getQualifierTypeNoCache(), PsiModificationTracker.MODIFICATION_COUNT));
  }

  @Nullable
  private PbMessageType getQualifierTypeNoCache() {
    PbOptionName qualifier = getQualifier();
    if (qualifier != null) {
      return getFieldType(getQualifierField(qualifier));
    } else {
      return getDescriptorType();
    }
  }

  @Override
  public boolean isSpecial() {
    return getSpecialType() != null;
  }

  @Override
  public SpecialOptionType getSpecialType() {
    PsiElement symbol = getSymbol();
    if (symbol != null) {
      PbOptionOwner owner = getOwner();
      for (SpecialOptionType type : SpecialOptionType.values()) {
        if (type.isInstance(this, owner)) {
          return type;
        }
      }
    }
    return null;
  }

  @Nullable
  @Override
  public ValueTester getBuiltInValueTester() {
    BuiltInType type = getBuiltInType();
    if (type == null) {
      return null;
    }
    SpecialOptionType specialType = getSpecialType();
    if (specialType == SpecialOptionType.FIELD_DEFAULT) {
      return type.getValueTester(ValueTesterType.DEFAULT);
    } else {
      return type.getValueTester(ValueTesterType.OPTION);
    }
  }

  @Nullable
  @Override
  public BuiltInType getBuiltInType() {
    SpecialOptionType specialType = getSpecialType();
    if (specialType == SpecialOptionType.FIELD_DEFAULT) {
      PbOptionOwner owner = getOwner();
      if (!(owner instanceof PbField)) {
        return null;
      }
      PbTypeName typeName = ((PbField) owner).getTypeName();
      if (typeName == null) {
        return null;
      }
      BuiltInType builtInType = typeName.getBuiltInType();
      if (builtInType == null) {
        return null;
      }
      return builtInType;
    } else if (specialType == SpecialOptionType.FIELD_JSON_NAME) {
      return BuiltInType.STRING;
    }

    // Other non-special options
    PsiReference ref = getEffectiveReference();
    if (ref == null) {
      return null;
    }
    PsiElement optionField = ref.resolve();
    if (!(optionField instanceof PbField)) {
      return null;
    }
    PbTypeName typeName = ((PbField) optionField).getTypeName();
    if (typeName == null) {
      return null;
    }
    BuiltInType builtInType = typeName.getBuiltInType();
    if (builtInType == null) {
      return null;
    }
    return builtInType;
  }

  @Nullable
  @Override
  public PbNamedTypeElement getNamedType() {
    PbTypeName typeName = null;

    SpecialOptionType specialType = getSpecialType();
    if (specialType == SpecialOptionType.FIELD_DEFAULT) {
      PbOptionOwner owner = getOwner();
      if (!(owner instanceof PbField)) {
        return null;
      }
      typeName = ((PbField) owner).getTypeName();
    } else if (specialType == null) {
      PsiReference ref = getEffectiveReference();
      if (ref == null) {
        return null;
      }
      PsiElement optionField = ref.resolve();
      if (!(optionField instanceof PbField)) {
        return null;
      }
      typeName = ((PbField) optionField).getTypeName();
    }

    if (typeName != null) {
      PsiReference ref = typeName.getEffectiveReference();
      if (ref == null) {
        return null;
      }
      PsiElement resolved = ref.resolve();
      if (resolved instanceof PbNamedTypeElement) {
        return (PbNamedTypeElement) resolved;
      }
    }

    return null;
  }

  @Nullable
  private static PbMessageType getFieldType(PbField field) {
    if (field == null) {
      return null;
    }
    PbTypeName type = field.getTypeName();
    if (type == null) {
      return null;
    }
    return PbPsiUtil.resolveRefToType(type.getEffectiveReference(), PbMessageType.class);
  }

  @Nullable
  private PbMessageType getDescriptorType() {
    PbOptionOwner owner = getOwner();
    if (owner == null) {
      // This shouldn't be possible with the proto BNF.
      logger.error("PbOptionName has no PbOptionOwner: " + this);
      return null;
    }
    Descriptor descriptor = Descriptor.locate(getPbFile());
    if (descriptor == null) {
      return null;
    }
    return PbSymbolResolver.forFile(descriptor.getFile())
        .resolveName(
            owner.getDescriptorOptionsTypeName(descriptor), ResolveFilters.packageOrMessage())
        .stream()
        .map(PbResolveResult::getElement)
        .filter(element -> element instanceof PbMessageType)
        .map(element -> (PbMessageType) element)
        .findFirst()
        .orElse(null);
  }

  @Nullable
  private static PbField getQualifierField(PbOptionName qualifier) {
    PsiReference ref = qualifier.getEffectiveReference();
    if (ref != null) {
      return PbPsiUtil.resolveRefToType(ref, PbField.class);
    }
    return null;
  }

  @Nullable
  private PbOptionOwner getOwner() {
    return PbPsiImplUtil.getOptionOwner(this);
  }
}
