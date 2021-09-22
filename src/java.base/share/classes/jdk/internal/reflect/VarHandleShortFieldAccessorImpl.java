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

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;

final class VarHandleShortFieldAccessorImpl extends VarHandleFieldAccessorImpl {
    static FieldAccessorImpl fieldAccessor(Field field, MethodHandle getter, MethodHandle setter) {
        return new VarHandleShortFieldAccessorImpl(field, getter, setter);
    }

    private VarHandleShortFieldAccessorImpl(Field field, MethodHandle getter, MethodHandle setter) {
        super(field, getter, setter);
    }

    public Object get(Object obj) throws IllegalArgumentException {
        return getShort(obj);
    }

    public boolean getBoolean(Object obj) throws IllegalArgumentException {
        throw newGetBooleanIllegalArgumentException();
    }

    public byte getByte(Object obj) throws IllegalArgumentException {
        throw newGetByteIllegalArgumentException();
    }

    public char getChar(Object obj) throws IllegalArgumentException {
        throw newGetCharIllegalArgumentException();
    }

    @Override
    public short getShort(Object obj) throws IllegalArgumentException {
        try {
            return (short) getter.invokeExact(obj);
        } catch (ClassCastException e) {
            throwSetIllegalArgumentException(obj);
            return 0;
        } catch (Throwable e) {
            throw new InternalError(e);
        }
    }

    public int getInt(Object obj) throws IllegalArgumentException {
        return getShort(obj);
    }

    public long getLong(Object obj) throws IllegalArgumentException {
        return getShort(obj);
    }

    public float getFloat(Object obj) throws IllegalArgumentException {
        return getShort(obj);
    }

    public double getDouble(Object obj) throws IllegalArgumentException {
        return getShort(obj);
    }

    public void set(Object obj, Object value)
        throws IllegalArgumentException, IllegalAccessException {
        if (value instanceof Byte b) {
            setShort(obj, b);
        } else if (value instanceof Short s) {
            setShort(obj, s);
        } else {
            throwSetIllegalArgumentException(value);
        }
    }

    public void setBoolean(Object obj, boolean z)
        throws IllegalArgumentException, IllegalAccessException {
        throwSetIllegalArgumentException(z);
    }

    public void setByte(Object obj, byte b)
        throws IllegalArgumentException, IllegalAccessException {
        setShort(obj, b);
    }

    public void setChar(Object obj, char c)
        throws IllegalArgumentException, IllegalAccessException {
        throwSetIllegalArgumentException(c);
    }

    @Override
    public void setShort(Object obj, short value) throws IllegalArgumentException, IllegalAccessException {
        if (isReadOnly()) {
            throwFinalFieldIllegalAccessException(value);
        }
        try {
            setter.invokeExact(obj, value);
        } catch (ClassCastException e) {
            throwSetIllegalArgumentException(obj);
        } catch (Throwable e) {
            throw new InternalError(e);
        }
    }

    public void setInt(Object obj, int i)
        throws IllegalArgumentException, IllegalAccessException {
        throwSetIllegalArgumentException(i);
    }

    public void setLong(Object obj, long l)
        throws IllegalArgumentException, IllegalAccessException {
        throwSetIllegalArgumentException(l);
    }

    public void setFloat(Object obj, float f)
        throws IllegalArgumentException, IllegalAccessException {
        throwSetIllegalArgumentException(f);
    }

    public void setDouble(Object obj, double d)
        throws IllegalArgumentException, IllegalAccessException {
        throwSetIllegalArgumentException(d);
    }
}
