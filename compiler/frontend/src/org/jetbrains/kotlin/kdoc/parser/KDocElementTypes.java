/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.kdoc.parser;

import org.jetbrains.kotlin.kdoc.psi.impl.KDocName;
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection;
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag;

public class KDocElementTypes {
    public static final KDocElementType KDOC_SECTION = new KDocElementType("KDOC_SECTION", KDocSection.class);
    public static final KDocElementType KDOC_TAG = new KDocElementType("KDOC_TAG", KDocTag.class);
    public static final KDocElementType KDOC_NAME = new KDocElementType("KDOC_NAME", KDocName.class);
}
