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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.invoke.MethodType.methodType;

/** <P> Package-private implementation of the MethodAccessor interface
    which has access to all classes and all fields, regardless of
    language restrictions. See MagicAccessor. </P>

    <P> This class is known to the VM; do not change its name without
    also changing the VM's code. </P>

    <P> NOTE: ALL methods of subclasses are skipped during security
    walks up the stack. The assumption is that the only such methods
    that will persistently show up on the stack are the implementing
    methods for java.lang.reflect.Method.invoke(). </P>
*/
final class DirectMethodAccessorImpl extends MethodAccessorImpl {
    static MethodAccessor newDirectMethodAccessor(Method m) {
        // access check has already been performed before getting MethodAccess
        // suppress the access check when unreflecting the method into a DirectMethodHandle
        PrivilegedAction<Method> pa = () -> {
            Method method = JLRA.copyMethod(m);
            method.setAccessible(true);
            return method;
        };
        Method method = AccessController.doPrivileged(pa);
        return new DirectMethodAccessorImpl(method);
    }

    private final Method method;
    private DirectMethodAccessorImpl(Method m) {
        this.method = m;
    }

    @Override
    public Object invoke(Object obj, Object[] args)
            throws IllegalArgumentException, InvocationTargetException {
        try {
            MethodHandle target = directMethodHandle(method.getDeclaringClass(), method);
            return target.invokeExact(obj, args);
        } catch (IllegalArgumentException|InvocationTargetException e) {
            throw e;
        } catch (ClassCastException|NullPointerException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (Throwable e) {
            throw new InvocationTargetException(e);
        }
    }

    @Override
    public Object invoke(Class<?> caller, Object obj, Object[] args)
            throws IllegalArgumentException, InvocationTargetException {
        try {
            MethodHandle target = directMethodHandle(caller, method);
            return target.invokeExact(obj, args);
        } catch (IllegalArgumentException|InvocationTargetException e) {
            throw e;
        } catch (ClassCastException|NullPointerException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (Throwable e) {
            throw new InvocationTargetException(e);
        }
    }

    private MethodHandle directMethodHandle(Class<?> caller, Method method) throws IllegalAccessException {
        ConcurrentHashMap<Method, MethodHandle> methods = directMethodHandleMap(caller);
        // direct method handle to target method
        MethodHandle dmh = methods.get(method);
        if (dmh == null) {
            MethodHandle mh = JLIA.unreflectMethod(caller, method);
            int paramCount = mh.type().parameterCount();

            // invoke method with an exception handler that throws InvocationTargetException
            mh = MethodHandles.catchException(mh, Throwable.class, WRAP.asType(methodType(mh.type().returnType(), Throwable.class)));
            if (Modifier.isStatic(method.getModifiers())) {
                // static method
                MethodHandle spreader = mh.asSpreader(Object[].class, paramCount);
                spreader = MethodHandles.dropArguments(spreader, 0, Object.class);
                mh = spreader.asType(methodType(Object.class, Object.class, Object[].class));
            } else {
                // instance method
                MethodHandle spreader = mh.asSpreader(Object[].class, paramCount - 1);
                mh = spreader.asType(methodType(Object.class, Object.class, Object[].class));
            }

            // push MH into cache
            MethodHandle cached = methods.putIfAbsent(method, mh);
            if (cached != null) {
                dmh = cached;
            } else {
                dmh = mh;
            }
        }
        return dmh;
    }

    /**
     * A cache of Method -> MethodHandle
     *
     * For non-caller-sensitive methods, the direct method handles are cached
     * in the map of the declaring class of the method.
     *
     * For caller-sensitive methods, the direct method handles are cached
     * in the map of the caller class invoking Method::invoke.
     *
     * ## TODO: caller class will hold strong references to methods in another class.
     * should they be weak reference so that some entries can be GC'ed if
     * the Method object is no longer reachable.
     */
    private static final ClassValue<ConcurrentHashMap<Method, MethodHandle>> DMH_MAP = new ClassValue<>() {
        @Override
        protected ConcurrentHashMap<Method, MethodHandle> computeValue(Class<?> type) {
            return new ConcurrentHashMap<>(4);
        }
    };

    private static ConcurrentHashMap<Method, MethodHandle> directMethodHandleMap(Class<?> caller) {
        return DMH_MAP.get(caller);
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
