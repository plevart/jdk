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
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Field;

import static java.lang.invoke.MethodType.methodType;

class MethodHandleLongFieldAccessorImpl extends MethodHandleFieldAccessorImpl {
    private final MethodHandle getter_J;
    private final MethodHandle setter_J;
    MethodHandleLongFieldAccessorImpl(Field field, MethodHandle getter, MethodHandle setter) {
        super(field, getter, setter);
        this.getter_J = getter.asType(methodType(long.class, Object.class));
        this.setter_J = setter != null ? setter.asType(methodType(void.class, Object.class, long.class))
                : null;
    }
    public Object get(Object obj) throws IllegalArgumentException {
        return Long.valueOf(getLong(obj));
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

    public int getInt(Object obj) throws IllegalArgumentException {
        throw newGetIntIllegalArgumentException();
    }

    public long getLong(Object obj) throws IllegalArgumentException {
        ensureObj(obj);
        try {
            return (long) getter_J.invokeExact(obj);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (ClassCastException|NullPointerException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (Throwable e) {
            throw new InternalError(e);
        }    }

    public float getFloat(Object obj) throws IllegalArgumentException {
        return getLong(obj);
    }

    public double getDouble(Object obj) throws IllegalArgumentException {
        return getLong(obj);
    }

    public void setBoolean(Object obj, boolean z)
        throws IllegalArgumentException, IllegalAccessException
    {
        throwSetIllegalArgumentException(z);
    }

    public void setByte(Object obj, byte b)
        throws IllegalArgumentException, IllegalAccessException
    {
        setLong(obj, b);
    }

    public void setChar(Object obj, char c)
        throws IllegalArgumentException, IllegalAccessException
    {
        setLong(obj, c);
    }

    public void setShort(Object obj, short s)
        throws IllegalArgumentException, IllegalAccessException
    {
        setLong(obj, s);
    }

    public void setInt(Object obj, int i)
        throws IllegalArgumentException, IllegalAccessException
    {
        setLong(obj, i);
    }

    public void setLong(Object obj, long l)
        throws IllegalArgumentException, IllegalAccessException
    {
        ensureObj(obj);
        if (isReadOnly) {
            throwFinalFieldIllegalAccessException(l);
        }
        try {
            setter_J.invokeExact(obj, l);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (ClassCastException|NullPointerException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (WrongMethodTypeException e) {
            e.printStackTrace();
            throwSetIllegalArgumentException(l);
        } catch (Throwable e) {
            throw new InternalError(e);
        }
    }

    public void setFloat(Object obj, float f)
        throws IllegalArgumentException, IllegalAccessException
    {
        throwSetIllegalArgumentException(f);
    }

    public void setDouble(Object obj, double d)
        throws IllegalArgumentException, IllegalAccessException
    {
        throwSetIllegalArgumentException(d);
    }
}
