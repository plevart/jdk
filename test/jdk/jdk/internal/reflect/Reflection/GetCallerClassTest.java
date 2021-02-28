/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8010117
 * @summary Test if the VM enforces Reflection.getCallerClass
 *          be called by methods annotated with CallerSensitive plus
 *          test reflective and method handle based invocation of caller-sensitive
 *          methods with or without alternative cs$method entrypoints
 * @modules java.base/jdk.internal.reflect
 * @build SetupGetCallerClass boot.GetCallerClass
 * @run driver SetupGetCallerClass
 * @run main/othervm -Xbootclasspath/a:bcp GetCallerClassTest
 */

import boot.GetCallerClass;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;

public class GetCallerClassTest {
    // boot.GetCallerClass is in bootclasspath
    private static final Class<GetCallerClass> gccCl = GetCallerClass.class;
    private final GetCallerClass gcc = new GetCallerClass();

    public static void main(String[] args) throws Exception {
        GetCallerClassTest gcct = new GetCallerClassTest();
        // ensure methods are annotated with @CallerSensitive and verify Reflection.isCallerSensitive()
        ensureAnnotationPresent(GetCallerClassTest.class, "testNonSystemMethod", false);

        ensureAnnotationPresent(gccCl, "getCallerClass", true);
        ensureAnnotationPresent(gccCl, "getCallerClassStatic", true);
        ensureAnnotationPresent(gccCl, "getCallerClassNoAlt", true);
        ensureAnnotationPresent(gccCl, "getCallerClassStaticNoAlt", true);

        // call Reflection.getCallerClass from bootclasspath without @CS
        gcct.testMissingCallerSensitiveAnnotation();
        // call Reflection.getCallerClass from classpath with @CS
        gcct.testNonSystemMethod();
        // call Reflection.getCallerClass from bootclasspath with @CS
        gcct.testCallerSensitiveMethods();
        // call @CS methods using reflection
        gcct.testCallerSensitiveMethodsUsingReflection();
        // call @CS methods using method handles
        gcct.testCallerSensitiveMethodsUsingMethodHandles();
    }

    private static void ensureAnnotationPresent(Class<?> c, String name, boolean cs)
        throws NoSuchMethodException
    {
        Method m = c.getDeclaredMethod(name);
        if (!m.isAnnotationPresent(CallerSensitive.class)) {
            throw new RuntimeException("@CallerSensitive not present in method " + m);
        }
        if (Reflection.isCallerSensitive(m) != cs) {
            throw new RuntimeException("Unexpected: isCallerSensitive returns " +
                Reflection.isCallerSensitive(m));
        }
    }

    private void testMissingCallerSensitiveAnnotation() {
        System.out.println("\ntestMissingCallerSensitiveAnnotation...");
        try {
            gcc.missingCallerSensitiveAnnotation();
            throw new RuntimeException("shouldn't have succeeded");
        } catch (InternalError e) {
            if (e.getMessage().startsWith("CallerSensitive annotation expected")) {
                System.out.println("Expected error: " + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    @CallerSensitive
    private void testNonSystemMethod() {
        System.out.println("\ntestNonSystemMethod...");
        try {
            Class<?> c = Reflection.getCallerClass();
            throw new RuntimeException("shouldn't have succeeded");
        } catch (InternalError e) {
            if (e.getMessage().startsWith("CallerSensitive annotation expected")) {
                System.out.println("Expected error: " + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    private void testCallerSensitiveMethods() {
        System.out.println("\ntestCallerSensitiveMethods...");
        Class<?> caller;

        caller = gcc.getCallerClass();
        if (caller != GetCallerClassTest.class) {
            throw new RuntimeException("mismatched caller: " + caller);
        }

        caller = GetCallerClass.getCallerClassStatic();
        if (caller != GetCallerClassTest.class) {
            throw new RuntimeException("mismatched caller: " + caller);
        }
    }

    private void testCallerSensitiveMethodsUsingReflection() {
        System.out.println("\ntestCallerSensitiveMethodsUsingReflection...");

        try {
            Class<?> caller;

            caller = (Class<?>) gccCl.getDeclaredMethod("getCallerClass").invoke(gcc);
            if (caller != GetCallerClassTest.class) {
                throw new RuntimeException("mismatched caller: " + caller);
            }

            caller = (Class<?>) gccCl.getDeclaredMethod("getCallerClassStatic").invoke(null);
            if (caller != GetCallerClassTest.class) {
                throw new RuntimeException("mismatched caller: " + caller);
            }

            caller = (Class<?>) gccCl.getDeclaredMethod("getCallerClassNoAlt").invoke(gcc);
            if (!caller.isNestmateOf(GetCallerClassTest.class)) {
                throw new RuntimeException("mismatched caller: " + caller);
            }

            caller = (Class<?>) gccCl.getDeclaredMethod("getCallerClassStaticNoAlt").invoke(null);
            if (!caller.isNestmateOf(GetCallerClassTest.class)) {
                throw new RuntimeException("mismatched caller: " + caller);
            }
        } catch (ReflectiveOperationException|SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private void testCallerSensitiveMethodsUsingMethodHandles() {
        System.out.println("\ntestCallerSensitiveMethodsUsingMethodHandles...");

        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodType mt = MethodType.methodType(Class.class);
            Class<?> caller;

            caller = (Class<?>) lookup.findVirtual(gccCl, "getCallerClass", mt).invokeExact(gcc);
            if (caller != GetCallerClassTest.class) {
                throw new RuntimeException("mismatched caller: " + caller);
            }

            caller = (Class<?>) lookup.findStatic(gccCl, "getCallerClassStatic", mt).invokeExact();
            if (caller != GetCallerClassTest.class) {
                throw new RuntimeException("mismatched caller: " + caller);
            }

            caller = (Class<?>) lookup.findVirtual(gccCl, "getCallerClassNoAlt", mt).invokeExact(gcc);
            if (!caller.isNestmateOf(GetCallerClassTest.class)) {
                throw new RuntimeException("mismatched caller: " + caller);
            }

            caller = (Class<?>) lookup.findStatic(gccCl, "getCallerClassStaticNoAlt", mt).invokeExact();
            if (!caller.isNestmateOf(GetCallerClassTest.class)) {
                throw new RuntimeException("mismatched caller: " + caller);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
