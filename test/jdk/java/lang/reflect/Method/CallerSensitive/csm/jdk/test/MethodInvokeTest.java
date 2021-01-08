/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package jdk.test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.function.Supplier;

/**
 * This test invokes StackWalker::getCallerClass via static reference,
 * reflection, MethodHandle, lambda.  Also verify that
 * StackWalker::getCallerClass can't be called from @CallerSensitive method.
 */
public class MethodInvokeTest {
    static final Policy DEFAULT_POLICY = Policy.getPolicy();
    private static final String CSM_CALLER_METHOD = "caller";

    public static void main(String... args) throws Throwable {
        boolean sm = false;
        if (args.length > 0 && args[0].equals("sm")) {
            sm = true;
            PermissionCollection perms = new Permissions();
            Policy.setPolicy(new Policy() {
                @Override
                public boolean implies(ProtectionDomain domain, Permission p) {
                    return perms.implies(p) ||
                        DEFAULT_POLICY.implies(domain, p);
                }
            });
            System.setSecurityManager(new SecurityManager());
        }

        System.err.format("Test %s security manager.%n",
                          sm ? "with" : "without");

        MethodInvokeTest test = new MethodInvokeTest();
        // test static call to java.util.CSM::caller
        test.staticMethodCall();
        // test java.lang.reflect.Method call
        test.reflectMethodCall();
        // test java.lang.invoke.MethodHandle
        test.invokeMethodHandle();
        // test method ref
        test.lambda();
    }

    void staticMethodCall() {
        assertEquals(java.util.CSM.caller(), MethodInvokeTest.class);
    }

    void reflectMethodCall() throws Throwable {
        Method csm = java.util.CSM.class.getMethod(CSM_CALLER_METHOD);

        checkInjectedInvoker(Caller1.invoke(csm), Caller1.class);
        checkInjectedInvoker(Caller2.invoke(csm), Caller2.class);
    }

    void invokeMethodHandle() throws Throwable {
        checkInjectedInvoker(Caller1.invokeExact(), Caller1.class);
        checkInjectedInvoker(Caller2.invokeExact(), Caller2.class);
    }

    void lambda() {
        Class<?> caller = LambdaTest.caller.get();
        LambdaTest.checkLambdaProxyClass(caller);

        caller = LambdaTest.caller();
        LambdaTest.checkLambdaProxyClass(caller);
    }

    static class Caller1 {
        static Class<?> invoke(Method csm) throws ReflectiveOperationException {
            return (Class<?>)csm.invoke(null);
        }
        static Class<?> invokeExact() throws Throwable {
            MethodHandle mh = MethodHandles.lookup().findStatic(java.util.CSM.class,
                    CSM_CALLER_METHOD, MethodType.methodType(Class.class));
            return (Class<?>)mh.invokeExact();
        }
    }

    static class Caller2 {
        static Class<?> invoke(Method csm) throws ReflectiveOperationException {
            return (Class<?>)csm.invoke(null);
        }
        static Class<?> invokeExact() throws Throwable {
            MethodHandle mh = MethodHandles.lookup().findStatic(java.util.CSM.class,
                    CSM_CALLER_METHOD, MethodType.methodType(Class.class));
            return (Class<?>)mh.invokeExact();
        }
    }

    static class LambdaTest {
        static Supplier<Class<?>> caller = java.util.CSM::caller;

        static Class<?> caller() {
            return caller.get();
        }

        /*
         * The class calling the caller-sensitive method is the lambda proxy class
         * generated for LambdaTest.
         */
        static void checkLambdaProxyClass(Class<?> caller) {
            assertTrue(caller.isHidden(), caller + " should be a hidden class");
            assertEquals(caller.getModule(), LambdaTest.class.getModule());

            int index = caller.getName().indexOf('/');
            String cn = caller.getName().substring(0, index);
            assertTrue(cn.startsWith(LambdaTest.class.getName() + "$$Lambda$"), caller + " should be a lambda proxy class");
        }
    }

    /*
     * The class calling the direct method handle of the caller-sensitive class
     * is the InjectedInvoker class generated for the given caller.
     */
    static void checkInjectedInvoker(Class<?> invoker, Class<?> caller) {
        assertTrue(invoker.isHidden(), invoker + " should be a hidden class");
        assertEquals(invoker.getModule(), caller.getModule());

        int index = invoker.getName().indexOf('/');
        String cn = invoker.getName().substring(0, index);
        assertEquals(cn, caller.getName() + "$$InjectedInvoker");
    }

    static void assertTrue(boolean value, String msg) {
        if (!value) {
            throw new RuntimeException(msg);
        }
    }
    static void assertEquals(Object o1, Object o2) {
        if (!o1.equals(o2)) {
            throw new RuntimeException(o1 + " != " + o2);
        }
    }
}
