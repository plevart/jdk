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
import jdk.internal.access.JavaLangReflectAccess;
import jdk.internal.access.SharedSecrets;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;

import static java.lang.invoke.MethodType.methodType;

final class DirectMethodAccessorImpl extends MethodAccessorImpl {
    static MethodAccessor newDirectMethodAccessor(Method m) {
        try {
            if (Reflection.isCallerSensitive(m)) {
                Method altmethod = findAltCallerSensitiveMethod(m);
                if (altmethod != null) {
                    MethodHandle target = directMethodHandle(altmethod.getDeclaringClass(), altmethod, true);
                    return new DirectMethodAccessorImpl(altmethod, m, target);
                }
            }
            Method method = JLRA.copyMethod(m);
            MethodHandle target = directMethodHandle(method.getDeclaringClass(), method, false);
            return new DirectMethodAccessorImpl(method, null, target);
        } catch (IllegalAccessException e) {
            throw new InternalError(e);
        }
    }

    private final Method method;
    private final Method csm;
    private final MethodHandle target;      // target method handle bound to the declaring class of the method
    private DirectMethodAccessorImpl(Method method, Method csm, MethodHandle target) {
        this.method = method;
        this.csm = csm;
        this.target = target;
    }

    @Override
    public Object invoke(Object obj, Object[] args)
            throws IllegalArgumentException, InvocationTargetException {
        try {
            return target.invokeExact(obj, args);
        } catch (IllegalArgumentException|InvocationTargetException e) {
            throw e;
        } catch (ClassCastException|NullPointerException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (Throwable e) {
            throw new InvocationTargetException(e);
        }
    }

    private volatile CallerSensitiveMethodHandleCache csmCache;

    /*
     * This prototype two ways to invoke CSM for performance comparsion.
     * 1. Direct invocation of the method handle of CSM.  The target MH is bound
     *    with the caller class (by injecting an invoker class which will be the
     *    caller class of the CSM via MH).  A new MH is created for a different
     *    caller.  CSM.invoke does stack walking twice to find the caller class
     *    (1) Method::invoke (2) CSM is invoked via MH.
     *
     * 2. Direct invocation of an implementation method of CSM that takes
     *    the caller class as the first argument.  The target MH is invoked
     *    for any caller class. This method invocation does stack walking only
     *    once (Method::invoke).  The target MH is not a caller-sensitive method.
     */
    @Override
    public Object invoke(Class<?> caller, Object obj, Object[] args)
            throws IllegalArgumentException, InvocationTargetException {
        MethodHandle dmh;
        if (csm == null) {
            // direct invocation of the CSM
            assert Reflection.isCallerSensitive(method);

            // direct method handle to the caller-sensitive method invoked by the given caller
            dmh = csmCache != null ? csmCache.methodHandle(caller) : null;
            if (dmh == null) {
                try {
                    dmh = directMethodHandle(caller, method, false);
                    // push MH into cache
                    csmCache = new CallerSensitiveMethodHandleCache(caller, dmh);
                } catch (IllegalAccessException e) {
                    throw new InternalError(e);
                }
            }
        } else {
            assert Reflection.isCallerSensitive(csm) && method.getParameterCount() == csm.getParameterCount()+1;
            dmh = target;
        }

        try {
            return csm != null ? dmh.invokeExact(obj, caller, args) : dmh.invokeExact(obj, args);
        } catch (IllegalArgumentException|InvocationTargetException e) {
            throw e;
        } catch (ClassCastException|NullPointerException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (Throwable e) {
            throw new InvocationTargetException(e);
        }
    }

    class CallerSensitiveMethodHandleCache {
        final WeakReference<Class<?>> callerRef;
        final WeakReference<MethodHandle> targetRef;
        CallerSensitiveMethodHandleCache(Class<?> caller, MethodHandle target) {
            this.callerRef = new WeakReference<>(caller);
            this.targetRef = new WeakReference<>(target);
        }

        MethodHandle methodHandle(Class<?> caller) {
            if (callerRef.refersTo(caller)) {
                return targetRef.get();
            }
            return null;
        }
    }

    private static MethodHandle directMethodHandle(Class<?> caller, Method method, boolean injectCallerParam) throws IllegalAccessException {
        // access check has already been performed before getting MethodAccess
        // suppress the access check when unreflecting the method into a DirectMethodHandle
        // Lookup does not have the implicit readability
        PrivilegedAction<Method> pa = () -> {
            method.setAccessible(true);
            return method;
        };
        AccessController.doPrivileged(pa);

        MethodHandle mh = JLIA.unreflectMethod(caller, method);
        int paramCount = mh.type().parameterCount();

        // invoke method with an exception handler that throws InvocationTargetException
        mh = MethodHandles.catchException(mh, Throwable.class, WRAP.asType(methodType(mh.type().returnType(), Throwable.class)));
        if (Modifier.isStatic(method.getModifiers())) {
            // static method
            if (injectCallerParam) {
                MethodHandle spreader = mh.asSpreader(Object[].class, paramCount - 1);
                spreader = MethodHandles.dropArguments(spreader, 0, Object.class);
                mh = spreader.asType(methodType(Object.class, Object.class, Class.class, Object[].class));
            } else {
                MethodHandle spreader = mh.asSpreader(Object[].class, paramCount);
                spreader = MethodHandles.dropArguments(spreader, 0, Object.class);
                mh = spreader.asType(methodType(Object.class, Object.class, Object[].class));
            }
        } else {
            // instance method
            if (injectCallerParam) {
                MethodHandle spreader = mh.asSpreader(2, Object[].class, paramCount - 2);
                mh = spreader.asType(methodType(Object.class, Object.class, Class.class, Object[].class));
            } else {
                MethodHandle spreader = mh.asSpreader(Object[].class, paramCount - 1);
                mh = spreader.asType(methodType(Object.class, Object.class, Object[].class));
            }
        }
        return mh;
    }

    private static Method findAltCallerSensitiveMethod(Method method) {
        String name = "reflected$" + method.getName();
        Class<?>[] paramTypes = method.getParameterTypes();
        int paramCount = paramTypes.length;
        Class<?>[] newParamTypes = new Class<?>[paramCount + 1];
        newParamTypes[0] = Class.class;
        if (paramCount > 0) {
            System.arraycopy(paramTypes, 0, newParamTypes, 1, paramCount);
        }
        try {
            return method.getDeclaringClass()
                         .getDeclaredMethod(name, newParamTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    // make this package-private to workaround a bug in Reflection::getCallerClass
    // that skips this class and the lookup class is ReflectionFactory instead
    static Object wrap(Throwable e) throws InvocationTargetException {
        throw new InvocationTargetException(e);
    }

    private static final JavaLangInvokeAccess JLIA;
    private static final JavaLangReflectAccess JLRA;
    private static final MethodHandle WRAP;
    static {
        try {
            JLIA = SharedSecrets.getJavaLangInvokeAccess();
            JLRA = SharedSecrets.getJavaLangReflectAccess();
            WRAP = MethodHandles.lookup().findStatic(DirectMethodAccessorImpl.class, "wrap",
                                                     methodType(Object.class, Throwable.class));
        } catch (NoSuchMethodException|IllegalAccessException e) {
            throw new InternalError(e);
        }
    }
}
