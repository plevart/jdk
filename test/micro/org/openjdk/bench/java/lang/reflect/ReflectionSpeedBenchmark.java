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

package org.openjdk.bench.java.lang.reflect;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(value = 1, warmups = 0)
public class ReflectionSpeedBenchmark {

    static final Method staticMethodConst;
    static final Method instanceMethodConts;

    static Method staticMethodVar;
    static Method instanceMethodVar;

    static {
        try {
            staticMethodVar = staticMethodConst = ReflectionSpeedBenchmark.class.getDeclaredMethod("sumStatic", int.class, int.class);
            instanceMethodVar = instanceMethodConts = ReflectionSpeedBenchmark.class.getDeclaredMethod("sumInstance", int.class, int.class);
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
    }

    private int a, b;

    @Setup(Level.Iteration)
    public void setup() {
        a = ThreadLocalRandom.current().nextInt(1024, Integer.MAX_VALUE);
        b = ThreadLocalRandom.current().nextInt(1024, Integer.MAX_VALUE);
    }

    public static int sumStatic(int a, int b) {
        return a + b;
    }

    public int sumInstance(int a, int b) {
        return a + b;
    }

    @Benchmark
    public int staticDirect() {
        return sumStatic(a, b);
    }

    @Benchmark
    public int instanceDirect() {
        return sumInstance(a, b);
    }

    @Benchmark
    public int staticReflectiveConst() {
        try {
            return (Integer) staticMethodConst.invoke(null, a, b);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError(e);
        }
    }

    @Benchmark
    public int instanceReflectiveConst() {
        try {
            return (Integer) instanceMethodConts.invoke(this, a, b);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError(e);
        }
    }

    @Benchmark
    public int staticReflectiveVar() {
        try {
            return (Integer) staticMethodVar.invoke(null, a, b);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError(e);
        }
    }

    @Benchmark
    public int instanceReflectiveVar() {
        try {
            return (Integer) instanceMethodVar.invoke(this, a, b);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError(e);
        }
    }
}
