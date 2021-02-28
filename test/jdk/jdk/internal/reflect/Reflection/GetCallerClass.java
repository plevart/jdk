/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package boot;

public class GetCallerClass {

    public Class<?> missingCallerSensitiveAnnotation() {
        return jdk.internal.reflect.Reflection.getCallerClass();
    }

    @jdk.internal.reflect.CallerSensitive
    public Class<?> getCallerClass() {
        var caller = jdk.internal.reflect.Reflection.getCallerClass();
        System.out.println("getCallerClass called by " + caller);
        return caller;
    }
    private Class<?> cs$getCallerClass(Class<?> caller) {
        System.out.println("cs$getCallerClass called by " + caller);
        return caller;
    }

    @jdk.internal.reflect.CallerSensitive
    public static Class<?> getCallerClassStatic() {
        var caller = jdk.internal.reflect.Reflection.getCallerClass();
        System.out.println("getCallerClassStatic called by " + caller);
        return caller;
    }
    private static Class<?> cs$getCallerClassStatic(Class<?> caller) {
        System.out.println("cs$getCallerClassStatic called by " + caller);
        return caller;
    }

    @jdk.internal.reflect.CallerSensitive
    public Class<?> getCallerClassNoAlt() {
        var caller = jdk.internal.reflect.Reflection.getCallerClass();
        System.out.println("getCallerClassNoAlt called by " + caller);
        return caller;
    }

    @jdk.internal.reflect.CallerSensitive
    public static Class<?> getCallerClassStaticNoAlt() {
        var caller = jdk.internal.reflect.Reflection.getCallerClass();
        System.out.println("getCallerClassStaticNoAlt called by " + caller);
        return caller;
    }
}
