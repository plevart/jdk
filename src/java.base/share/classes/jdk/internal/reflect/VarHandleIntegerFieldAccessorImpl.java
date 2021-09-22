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

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

final class VarHandleIntegerFieldAccessorImpl extends FieldAccessorImpl {
    static FieldAccessorImpl fieldAccessor(Field field, MethodHandle getter, MethodHandle setter) {
        return new VarHandleIntegerFieldAccessorImpl(field, getter, setter);
    }

    @Stable
    private static final MethodHandle[] getters = new MethodHandle[1024];
    @Stable
    private static final MethodHandle[] setters = new MethodHandle[1024];
    private static final AtomicInteger seq = new AtomicInteger();

    @Stable
    private final int index;

    private VarHandleIntegerFieldAccessorImpl(Field field, MethodHandle getter, MethodHandle setter) {
        super(field);
        int i = seq.getAndIncrement();
        getters[i] = getter;
        setters[i] = setter;
        index = i + 1;
    }

    public Object get(Object obj) throws IllegalArgumentException {
        return getInt(obj);
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

    public short getShort(Object obj) throws IllegalArgumentException {
        throw newGetShortIllegalArgumentException();
    }

    @Override
    public int getInt(Object obj) throws IllegalArgumentException {
        try {
            return (int) getters[index-1].invokeExact(obj);
        } catch (ClassCastException e) {
            throwSetIllegalArgumentException(obj);
            return 0;
        } catch (Throwable e) {
            throw new InternalError(e);
        }
    }

    public long getLong(Object obj) throws IllegalArgumentException {
        return getInt(obj);
    }

    public float getFloat(Object obj) throws IllegalArgumentException {
        return getInt(obj);
    }

    public double getDouble(Object obj) throws IllegalArgumentException {
        return getInt(obj);
    }

    public void set(Object obj, Object value)
        throws IllegalArgumentException, IllegalAccessException {
        if (value instanceof Byte b) {
            setInt(obj, b);
        } else if (value instanceof Short s) {
            setInt(obj, s);
        } else if (value instanceof Character c) {
            setInt(obj, c);
        } else if (value instanceof Integer i) {
            setInt(obj, i);
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
        setInt(obj, b);
    }

    public void setChar(Object obj, char c)
        throws IllegalArgumentException, IllegalAccessException {
        setInt(obj, c);
    }

    public void setShort(Object obj, short s)
        throws IllegalArgumentException, IllegalAccessException {
        setInt(obj, s);
    }

    @Override
    public void setInt(Object obj, int value) throws IllegalArgumentException, IllegalAccessException {
        var setter = setters[index-1];
        if (setter == null) {
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
