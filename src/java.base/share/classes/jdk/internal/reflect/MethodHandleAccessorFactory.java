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

import jdk.internal.access.JavaLangInvokeAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.Hidden;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static java.lang.invoke.MethodType.methodType;

final class MethodHandleAccessorFactory {
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();

    static MethodAccessorImpl newMethodAccessor(Method method) {
        // ExceptionInInitializerError may be thrown during class initialization
        // Ensure class initialized outside the invocation of method handle
        // so that EIIE is propagated (not wrapped with ITE)
        UNSAFE.ensureClassInitialized(method.getDeclaringClass());
        try {
            boolean callerSensitive = Reflection.isCallerSensitive(method);
            if (callerSensitive) {
                DirectMethodAccessorImpl accessor = findMethodWithLeadingCaller(method);
                if (accessor != null) {
                    return accessor;
                }
            }
            return findMethod(method, callerSensitive);
        } catch (IllegalAccessException e) {
            throw new InternalError(e);
        }
    }

    static ConstructorAccessorImpl newConstructorAccessor(Constructor<?> ctor) {
        // ExceptionInInitializerError may be thrown during class initialization
        // Ensure class initialized outside the invocation of method handle
        // so that EIIE is propagated (not wrapped with ITE)
        UNSAFE.ensureClassInitialized(ctor.getDeclaringClass());
        try {
            MethodHandle mh = JLIA.unreflectConstructor(ctor);
            int paramCount = mh.type().parameterCount();
            MethodHandle target = MethodHandles.catchException(mh, Throwable.class,
                    WRAP.asType(methodType(mh.type().returnType(), Throwable.class)));
            target = target.asSpreader(Object[].class, paramCount)
                           .asType(methodType(Object.class, Object[].class));
            return new DirectConstructorAccessorImpl(ctor, target);
        } catch (IllegalAccessException e) {
            throw new InternalError(e);
        }
    }

    static FieldAccessorImpl newFieldAccessor(Field field, boolean isReadOnly) {
        // ExceptionInInitializerError may be thrown during class initialization
        // Ensure class initialized outside the invocation of method handle
        // so that EIIE is propagated (not wrapped with ITE)
        UNSAFE.ensureClassInitialized(field.getDeclaringClass());
        try {
            MethodHandle getter = JLIA.unreflectField(field, false);
            MethodHandle setter = isReadOnly ? null : JLIA.unreflectField(field, true);

            if (Modifier.isStatic(field.getModifiers())) {
                // static field
                getter = MethodHandles.dropArguments(getter, 0, Object.class)
                                      .asType(methodType(Object.class, Object.class));
                if (setter != null) {
                    setter = MethodHandles.dropArguments(setter, 0, Object.class)
                                          .asType(methodType(void.class, Object.class, Object.class));
                }
            }
            Class<?> type = field.getType();
            if (type == Boolean.TYPE) {
                return new MethodHandleBooleanFieldAccessorImpl(field, getter, setter);
            } else if (type == Byte.TYPE) {
                return new MethodHandleByteFieldAccessorImpl(field, getter, setter);
            } else if (type == Short.TYPE) {
                return new MethodHandleShortFieldAccessorImpl(field, getter, setter);
            } else if (type == Character.TYPE) {
                return new MethodHandleCharacterFieldAccessorImpl(field, getter, setter);
            } else if (type == Integer.TYPE) {
                return new MethodHandleIntegerFieldAccessorImpl(field, getter, setter);
            } else if (type == Long.TYPE) {
                return new MethodHandleLongFieldAccessorImpl(field, getter, setter);
            } else if (type == Float.TYPE) {
                return new MethodHandleFloatFieldAccessorImpl(field, getter, setter);
            } else if (type == Double.TYPE) {
                return new MethodHandleDoubleFieldAccessorImpl(field, getter, setter);
            } else {
                return new MethodHandleObjectFieldAccessorImpl(field, getter, setter);
            }
        } catch (IllegalAccessException e) {
            throw new InternalError(e);
        }
    }

    private static DirectMethodAccessorImpl findMethod(Method method, boolean callerSensitive) throws IllegalAccessException {
        MethodType mtype = methodType(method.getReturnType(), method.getParameterTypes());
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        MethodHandle dmh = isStatic
                           ? JLIA.findStatic(method.getDeclaringClass(), method.getName(), mtype)
                           : JLIA.findVirtual(method.getDeclaringClass(), method.getName(), mtype);

        if (callerSensitive) {
            return new DirectMethodAccessorImpl.CallerSensitiveWithInvoker(makeTarget(dmh, isStatic, false));
        } else {
            return new DirectMethodAccessorImpl(makeTarget(dmh, isStatic, false));
        }
    }

    private static DirectMethodAccessorImpl findMethodWithLeadingCaller(Method method) throws IllegalAccessException {
        String name = "cs$" + method.getName();
        MethodType mtype = methodType(method.getReturnType(), method.getParameterTypes()).insertParameterTypes(0, Class.class);
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        MethodHandle dmh = isStatic
                           ? JLIA.findStatic(method.getDeclaringClass(), name, mtype)
                           : JLIA.findVirtual(method.getDeclaringClass(), name, mtype);
        return dmh != null
               ? new DirectMethodAccessorImpl.CallerSensitiveWithLeadingCaller(makeTarget(dmh, isStatic, true))
               : null;
    }

    /**
     * Transform given dmh to a target MH. Given dmh can either be static or not
     * which means that it either doesn't have or has a leading 'this' argument.
     * Optional 'this' argument either is followed by 'caller' argument
     * (if hasLeadingCaller is true) or not. Method parameters are followed last.<p>
     * Depending on 'hasLeadingCaller' value, the transformed MH will either
     * be of the following signature (hasLeadingCaller == true):
     * <pre>
     *     (Object, Class, Object[])Object
     * </pre>
     * or the following (hasLeadingCaller == false):
     * <pre>
     *     (Object, Object[])Object
     * </pre>
     *
     * @param dmh given DirectMethodHandle
     * @param isStatic whether given dmh represents static method or not
     * @param hasLeadingCaller whether given dmh represents a method with leading
     *                         caller Class parameter
     * @return transformed dmh to be used as a target in direct method accessors
     */
    static MethodHandle makeTarget(MethodHandle dmh, boolean isStatic, boolean hasLeadingCaller) {
        int paramCount = dmh.type().parameterCount();
        // wrap any thrown exception with InvocationTargetException and re-throw it
        MethodHandle target = MethodHandles.catchException(dmh, Throwable.class,
                WRAP.asType(methodType(dmh.type().returnType(), Throwable.class)));
        // spread the last "real" parameters (not counting leading 'this' and 'caller')
        target = target.asSpreader(Object[].class,
                                   paramCount - (isStatic ? 0 : 1) - (hasLeadingCaller ? 1 : 0));
        if (isStatic) {
            // add leading 'this' parameter to static method which is then ignored
            target = MethodHandles.dropArguments(target, 0, Object.class);
        }
        // adapt to specified type
        return target.asType(hasLeadingCaller
                             ? methodType(Object.class, Object.class, Class.class, Object[].class)
                             : methodType(Object.class, Object.class, Object[].class));
    }

    // make this package-private to workaround a bug in Reflection::getCallerClass
    // that skips this class and the lookup class is ReflectionFactory instead
    // this frame is hidden not to alter the stacktrace for InvocationTargetException
    // so that the stack trace of its cause is the same as ITE.
    @Hidden
    static Object wrap(Throwable e) throws InvocationTargetException {
        throw new InvocationTargetException(e);
    }

    private static final JavaLangInvokeAccess JLIA;
    private static final MethodHandle WRAP;
    static {
        try {
            JLIA = SharedSecrets.getJavaLangInvokeAccess();
            WRAP = MethodHandles.lookup().findStatic(MethodHandleAccessorFactory.class, "wrap",
                                                     methodType(Object.class, Throwable.class));
        } catch (NoSuchMethodException|IllegalAccessException e) {
            throw new InternalError(e);
        }
    }
}
