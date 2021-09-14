/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.internal.reflect;

import jdk.internal.vm.annotation.Stable;

import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;

abstract class VarHandleFieldAccessorImpl extends FieldAccessorImpl {
    private static final int IS_READ_ONLY_BIT = 0x0001;
    private static final int IS_STATIC_BIT = 0x0002;
    private static final int NONZERO_BIT = 0x8000;

    private @Stable final int fieldFlags;
    protected @Stable final VarHandle varHandle;

    protected VarHandleFieldAccessorImpl(Field field, VarHandle varHandle, boolean isReadOnly, boolean isStatic) {
        super(field);
        this.fieldFlags = (isReadOnly ? IS_READ_ONLY_BIT : 0) |
                          (isStatic ? IS_STATIC_BIT : 0) |
                          NONZERO_BIT;
        this.varHandle = varHandle;
    }

    protected final boolean isReadOnly() {
        return (fieldFlags & IS_READ_ONLY_BIT) == IS_READ_ONLY_BIT;
    }

    protected final void ensureObj(Object o) {
        if ((fieldFlags & IS_STATIC_BIT) == 0) {
            // for compatibility, check the receiver object first
            // throw NullPointerException if o is null
            if (!field.getDeclaringClass().isAssignableFrom(o.getClass())) {
                throwSetIllegalArgumentException(o);
            }
        }
    }

    /**
     * IllegalArgumentException because Field::get on the specified object, which
     * is not an instance of the class or interface declaring the underlying method
     */
    protected IllegalArgumentException newGetIllegalArgumentException(Class<?> type) {
        return new IllegalArgumentException(getMessage(true, type.getName()));
    }

    /**
     * IllegalArgumentException because Field::set on the specified object, which
     * is not an instance of the class or interface declaring the underlying method
     */
    protected IllegalArgumentException newSetIllegalArgumentException(Class<?> type) {
        return new IllegalArgumentException(getMessage(false, type.getName()));
    }


}
