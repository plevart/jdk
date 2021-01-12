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
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class DirectMethodAccessorImpl extends MethodAccessorImpl {
    private final Method method;
    private final MethodHandle target;      // target method handle bound to the declaring class of the method
    DirectMethodAccessorImpl(Method method, MethodHandle target) {
        this.method = method;
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
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (Throwable e) {
            throw new InvocationTargetException(e);
        }
    }

    private volatile CallerSensitiveMethodHandleCache csmCache;

    /*
     * This method invokes the method handle of a caller-sensitive method.
     * The caller parameter is the caller class invoking Method::invoke.
     *
     * This method creates a new MH for each different caller class.
     * The target MH is bound with the caller class and an invoker class
     * is injected such that the invoker class will be the caller class
     * of the CSM via MH.  The injected invoker class is in the same runtime
     * package, has the same defining class loader and protection domain
     * as the caller parameter.
     *
     * CSM.invoke does stack walking once to find the caller class
     * in this implementation.  The invocation of the CSM may also call
     * Reflection::getCallerClass another time.
     *
     * One option to improve the performance improvement: we can define
     * a private method for each CSM that takes a caller class as the first
     * argument.  This DirectMethodHandleAccessor can eagerly lookup such
     * private method for a caller-sensitive method as the target.
     * This method will call the target MH with the caller class such that
     * the invocation of the caller-sensitive method will do stack walking
     * at most once.
     */
    @Override
    public Object invoke(Class<?> caller, Object obj, Object[] args)
            throws IllegalArgumentException, InvocationTargetException {
        // direct invocation of the CSM
        assert Reflection.isCallerSensitive(method);

        // direct method handle to the caller-sensitive method invoked by the given caller
        MethodHandle dmh = csmCache != null ? csmCache.methodHandle(caller) : null;
        if (dmh == null) {
            try {
                dmh = MethodHandleAccessorFactory.directMethodHandle(caller, method);
                // push MH into cache
                csmCache = new CallerSensitiveMethodHandleCache(caller, dmh);
            } catch (IllegalAccessException e) {
                throw new InternalError(e);
            }
        }

        try {
            return dmh.invokeExact(obj, args);
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
}
