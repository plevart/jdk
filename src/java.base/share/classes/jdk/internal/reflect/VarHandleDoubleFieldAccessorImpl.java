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

import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

abstract class VarHandleDoubleFieldAccessorImpl extends VarHandleFieldAccessorImpl {
    static FieldAccessorImpl fieldAccessor(Field field, VarHandle varHandle, boolean isReadOnly) {
        return Modifier.isStatic(field.getModifiers())
                ? new StaticFieldAccessor(field, varHandle, isReadOnly)
                : new InstanceFieldAccessor(field, varHandle, isReadOnly);
    }

    VarHandleDoubleFieldAccessorImpl(Field field, VarHandle varHandle, boolean isReadOnly, boolean isStatic) {
        super(field, varHandle, isReadOnly, isStatic);
    }

    abstract double getValue(Object obj);
    abstract void setValue(Object obj, double d) throws Throwable;

    static class StaticFieldAccessor extends VarHandleDoubleFieldAccessorImpl {
        StaticFieldAccessor(Field field, VarHandle varHandle, boolean isReadOnly) {
            super(field, varHandle, isReadOnly, true);
        }

        double getValue(Object obj) {
            return (double) varHandle.get();
        }

        void setValue(Object obj, double d) throws Throwable {
            varHandle.set(d);
        }
    }

    static class InstanceFieldAccessor extends VarHandleDoubleFieldAccessorImpl {
        InstanceFieldAccessor(Field field, VarHandle varHandle, boolean isReadOnly) {
            super(field, varHandle, isReadOnly, false);
        }

        double getValue(Object obj) {
            return (double) varHandle.get(obj);
        }

        void setValue(Object obj, double d) throws Throwable {
            varHandle.set(obj, d);
        }
    }

    public Object get(Object obj) throws IllegalArgumentException {
        return Double.valueOf(getDouble(obj));
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
        throw newGetLongIllegalArgumentException();
    }

    public float getFloat(Object obj) throws IllegalArgumentException {
        throw newGetFloatIllegalArgumentException();
    }

    public double getDouble(Object obj) throws IllegalArgumentException {
        try {
            return getValue(obj);
        } catch (IllegalArgumentException|NullPointerException e) {
            throw e;
        } catch (ClassCastException e) {
            throw newGetIllegalArgumentException(obj.getClass());
        } catch (Throwable e) {
            throw new InternalError(e);
        }
    }

    public void set(Object obj, Object value)
            throws IllegalArgumentException, IllegalAccessException
    {
        if (isReadOnly()) {
            ensureObj(obj);     // throw NPE if obj is null on instance field
            throwFinalFieldIllegalAccessException(value);
        }

        if (value == null) {
            throwSetIllegalArgumentException(value);
        }

        if (value instanceof Byte b) {
            setDouble(obj, b.byteValue());
        }
        else if (value instanceof Short s) {
            setDouble(obj, s.shortValue());
        }
        else if (value instanceof Character c) {
            setDouble(obj, c.charValue());
        }
        else if (value instanceof Integer i) {
            setDouble(obj, i.intValue());
        }
        else if (value instanceof Long l) {
            setDouble(obj, l.longValue());
        }
        else if (value instanceof Float f) {
            setDouble(obj, f.floatValue());
        }
        else if (value instanceof Double d) {
            setDouble(obj, d.doubleValue());
        }
        else {
            throwSetIllegalArgumentException(value);
        }
    }

    public void setBoolean(Object obj, boolean z)
        throws IllegalArgumentException, IllegalAccessException
    {
        throwSetIllegalArgumentException(z);
    }

    public void setByte(Object obj, byte b)
        throws IllegalArgumentException, IllegalAccessException
    {
        setDouble(obj, b);
    }

    public void setChar(Object obj, char c)
        throws IllegalArgumentException, IllegalAccessException
    {
        setDouble(obj, c);
    }

    public void setShort(Object obj, short s)
        throws IllegalArgumentException, IllegalAccessException
    {
        setDouble(obj, s);
    }

    public void setInt(Object obj, int i)
        throws IllegalArgumentException, IllegalAccessException
    {
        setDouble(obj, i);
    }

    public void setLong(Object obj, long l)
        throws IllegalArgumentException, IllegalAccessException
    {
        setDouble(obj, l);
    }

    public void setFloat(Object obj, float f)
        throws IllegalArgumentException, IllegalAccessException
    {
        setDouble(obj, f);
    }

    public void setDouble(Object obj, double d)
        throws IllegalArgumentException, IllegalAccessException
    {
        if (isReadOnly()) {
            ensureObj(obj);     // throw NPE if obj is null on instance field
            throwFinalFieldIllegalAccessException(d);
        }
        try {
            setValue(obj, d);
        } catch (IllegalArgumentException|NullPointerException e) {
            throw e;
        } catch (ClassCastException e) {
            throw newSetIllegalArgumentException(obj.getClass());
        } catch (Throwable e) {
            throw new InternalError(e);
        }
    }
}
