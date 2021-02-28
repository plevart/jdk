/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.*;
import jdk.internal.misc.Unsafe;
import jdk.internal.misc.VM;
import sun.reflect.misc.ReflectUtil;

/** Used only for the first few invocations of a Method; afterward,
    switches to bytecode-based implementation */

class NativeMethodAccessorImpl extends MethodAccessorImpl {
     private static final Unsafe U = Unsafe.getUnsafe();
     private static final long GENERATED_OFFSET
        = U.objectFieldOffset(NativeMethodAccessorImpl.class, "generated");

    private final Method method;
    private final Method cs$method;
    private DelegatingMethodAccessorImpl parent;
    private int numInvocations;
    private volatile int generated;

    NativeMethodAccessorImpl(Method method) {
        Method cs$method;
        if (Reflection.isCallerSensitive(method)) {
            String cs$name = "cs$" + method.getName();
            Class<?>[] ptypes = method.getParameterTypes();
            Class<?>[] cs$ptypes = new Class<?>[ptypes.length + 1];
            cs$ptypes[0] = Class.class;
            System.arraycopy(ptypes, 0, cs$ptypes, 1, ptypes.length);
            try {
                cs$method = method.getDeclaringClass().getDeclaredMethod(cs$name, cs$ptypes);
            } catch (NoSuchMethodException e) {
                cs$method = null;
            }
        } else {
            cs$method = null;
        }
        this.method = method;
        this.cs$method = cs$method;
    }

    @Override
    public Object invoke(Object obj, Object[] args)
        throws IllegalArgumentException, InvocationTargetException
    {
        if (cs$method != null) {
            throw new InternalError("cs$method invoked without explicit caller: " + method);
        }
        maybeSwapDelegate(method);
        return invoke0(method, obj, args);
    }

    @Override
    public Object invoke(Class<?> caller, Object obj, Object[] args)
        throws IllegalArgumentException, InvocationTargetException
    {
        if (cs$method != null) {
            maybeSwapDelegate(method);
            Object[] cs$args = new Object[args == null ? 1 : args.length + 1];
            cs$args[0] = caller;
            if (args != null) {
                System.arraycopy(args, 0, cs$args, 1, args.length);
            }
            return invoke0(cs$method, obj, cs$args);
        } else {
            // @CS method but no cs$method entrypoint - insert reflective invoker
            return super.invoke(caller, obj, args);
        }
    }

    private void maybeSwapDelegate(Method method) {
        try {
            if (VM.isModuleSystemInited()
                && ReflectionFactory.useDirectMethodHandle()
                && generated == 0
                && U.compareAndSetInt(this, GENERATED_OFFSET, 0, 1)) {
                parent.setDelegate(
                    MethodHandleAccessorFactory.newMethodAccessor(method));
            } else if (!ReflectionFactory.useDirectMethodHandle()
                       && ++numInvocations > ReflectionFactory.inflationThreshold()
                       && !method.getDeclaringClass().isHidden()
                       && !ReflectUtil.isVMAnonymousClass(method.getDeclaringClass())
                       && generated == 0
                       && U.compareAndSetInt(this, GENERATED_OFFSET, 0, 1)) {
                parent.setDelegate(
                    GeneratedMethodAccessorFactory.newMethodAccessor(method));
            }
        } catch (Throwable t) {
            // Throwable happens in generateMethod, restore generated to 0
            generated = 0;
            throw t;
        }
    }

    void setParent(DelegatingMethodAccessorImpl parent) {
        this.parent = parent;
    }

    private static native Object invoke0(Method m, Object obj, Object[] args);
}
