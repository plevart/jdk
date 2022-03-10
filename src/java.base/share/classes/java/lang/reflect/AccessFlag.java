/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.reflect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Stream;

/**
 * Represents a JVM access or module-related flag on a runtime member,
 * such as a {@linkplain Class class}, {@linkplain Field field}, or
 * {@linkplain Executable method}.
 *
 * <P>JVM access and module-related flags are related to, but distinct
 * from Java language {@linkplain Modifier modifiers}. Some modifiers
 * and access flags have a one-to-one correspondence, such as {@code
 * public}. In other cases, some language-level modifiers do
 * <em>not</em> have an access flag, such as {@code sealed} (JVMS
 * {@jvms 4.7.31}) and some access flags have no corresponding
 * modifier, such as {@linkplain SYNTHETIC synthetic}.
 *
 * <p>The values for the constants representing the access and module
 * flags are taken from sections of <cite>The Java Virtual Machine
 * Specification</cite> including {@jvms 4.1} (class access and
 * property modifiers), {@jvms 4.5} (field access and property flags),
 * {@jvms 4.6} (method access and property flags), {@jvms 4.7.6}
 * (nested class access and property flags), {@jvms 4.7.24} (method
 * parameters), and {@jvms 4.7.25} (module flags and requires,
 * exports, and opens flags).
 *
 * <p>The {@linkplain #mask() mask} values for the different access
 * flags are <em>not</em> distinct. Flags are defined for different
 * kinds of JVM structures and the same bit position has different
 * meanings in different contexts. For example, {@code 0x0000_0040}
 * indicates a {@link #VOLATILE volatile} field but a {@linkplain
 * #BRIDGE bridge method}; {@code 0x0000_0080} indicates a {@link
 * #TRANSIENT transient} field but a {@linkplain #VARARGS variable
 * arity (varargs)} method.
 *
 * <p>The access flag constants are ordered by non-decreasing mask
 * value; that is the mask value of a constant is greater than or
 * equal to the mask value of an immediate neighbor to its (syntactic)
 * left. If new constants are added, this property will be
 * maintained. That implies new constants will not necessarily be
 * added at the end of the existing list.
 *
 * @see java.lang.reflect.Modifier
 * @see java.lang.module.ModuleDescriptor.Modifier
 * @see java.lang.module.ModuleDescriptor.Requires.Modifier
 * @see java.lang.module.ModuleDescriptor.Exports.Modifier
 * @see java.lang.module.ModuleDescriptor.Opens.Modifier
 * @see java.compiler/javax.lang.model.element.Modifier
 * @since 19
 */
@SuppressWarnings("doclint:reference") // cross-module link
public enum AccessFlag {
    /**
     * The access flag {@code ACC_PUBLIC}, corresponding to the source
     * modifier {@link Modifier#PUBLIC public}.
     */
    PUBLIC(Modifier.PUBLIC, true,
           EnumSet.of(Location.CLASS, Location.FIELD, Location.METHOD,
                      Location.INNER_CLASS)),

    /**
     * The access flag {@code ACC_PRIVATE}, corresponding to the
     * source modifier {@link Modifier#PRIVATE private}.
     */
    PRIVATE(Modifier.PRIVATE, true,
            EnumSet.of(Location.FIELD, Location.METHOD, Location.INNER_CLASS)),

    /**
     * The access flag {@code ACC_PROTECTED}, corresponding to the
     * source modifier {@link Modifier#PROTECTED protected}.
     */
    PROTECTED(Modifier.PROTECTED, true,
              EnumSet.of(Location.FIELD, Location.METHOD, Location.INNER_CLASS)),

    /**
     * The access flag {@code ACC_STATIC}, corresponding to the source
     * modifier {@link Modifier#STATIC static}.
     */
    STATIC(Modifier.STATIC, true,
           EnumSet.of(Location.FIELD, Location.METHOD, Location.INNER_CLASS)),

    /**
     * The access flag {@code ACC_FINAL}, corresponding to the source
     * modifier {@link Modifier#FINAL final}.
     */
    FINAL(Modifier.FINAL, true,
          EnumSet.of(Location.CLASS, Location.FIELD, Location.METHOD,
                     Location.INNER_CLASS, Location.METHOD_PARAMETER)),

    /**
     * The access flag {@code ACC_SUPER}.
     */
    SUPER(0x0000_0020, false, EnumSet.of(Location.CLASS)),

    /**
     * The module flag {@code ACC_OPEN}.
     *
     * @see java.lang.module.ModuleDescriptor#isOpen
     */
    OPEN(0x0000_0020, false, EnumSet.of(Location.MODULE)),

    /**
     * The module requires flag {@code ACC_TRANSITIVE}.
     *
     * @see java.lang.module.ModuleDescriptor.Requires.Modifier#TRANSITIVE
     */
    TRANSITIVE(0x0000_0020, false, EnumSet.of(Location.MODULE_REQUIRES)),

    /**
     * The access flag {@code ACC_SYNCHRONIZED}, corresponding to the
     * source modifier {@link Modifier#SYNCHRONIZED synchronized}.
     */
    SYNCHRONIZED(Modifier.SYNCHRONIZED, true, EnumSet.of(Location.METHOD)),

    /**
     * The module requires flag {@code ACC_STATIC_PHASE}.
     *
     * @see java.lang.module.ModuleDescriptor.Requires.Modifier#STATIC
     */
    STATIC_PHASE(0x0000_0040, false, EnumSet.of(Location.MODULE_REQUIRES)),

    /**
     * The access flag {@code ACC_VOLATILE}, corresponding to the
     * source modifier {@link Modifier#VOLATILE volatile}.
     */
    VOLATILE(Modifier.VOLATILE, true, EnumSet.of(Location.FIELD)),

    /**
     * The access flag {@code ACC_BRIDGE}
     *
     * @see Method#isBridge()
     */
    BRIDGE(0x0000_0040, false, EnumSet.of(Location.METHOD)),

    /**
     * The access flag {@code ACC_TRANSIENT}, corresponding to the
     * source modifier {@link Modifier#TRANSIENT transient}.
     */
    TRANSIENT(Modifier.TRANSIENT, true, EnumSet.of(Location.FIELD)),

    /**
     * The access flag {@code ACC_VARARGS}.
     *
     * @see Executable#isVarArgs()
     */
    VARARGS(0x0000_0080, false, EnumSet.of(Location.METHOD)),

    /**
     * The access flag {@code ACC_NATIVE}, corresponding to the source
     * modifier {@link Modifier#NATIVE native}.
     */
    NATIVE(Modifier.NATIVE, true, EnumSet.of(Location.METHOD)),

    /**
     * The access flag {@code ACC_INTERFACE}.
     *
     * @see Class#isInterface()
     */
    INTERFACE(Modifier.INTERFACE, false,
              EnumSet.of(Location.CLASS, Location.INNER_CLASS)),

    /**
     * The access flag {@code ACC_ABSTRACT}, corresponding to the
     * source modifier {@code link Modifier#ABSTRACT abstract}.
     */
    ABSTRACT(Modifier.ABSTRACT, true,
             EnumSet.of(Location.CLASS, Location.METHOD, Location.INNER_CLASS)),

    /**
     * The access flag {@code ACC_STRICT}, corresponding to the source
     * modifier {@link Modifier#STRICT strictfp}.
     */
    STRICT(Modifier.STRICT, true, EnumSet.of(Location.METHOD)),

    /**
     * The access flag {@code ACC_SYNTHETIC}.
     *
     * @see Class#isSynthetic()
     * @see Executable#isSynthetic()
     * @see java.lang.module.ModuleDescriptor.Modifier#SYNTHETIC
     */
    SYNTHETIC(0x0000_1000, false,
              EnumSet.of(Location.CLASS, Location.FIELD, Location.METHOD,
                         Location.INNER_CLASS, Location.METHOD_PARAMETER,
                         Location.MODULE, Location.MODULE_REQUIRES,
                         Location.MODULE_EXPORTS, Location.MODULE_OPENS)),

    /**
     * The access flag {@code ACC_ANNOTATION}.
     *
     * @see Class#isAnnotation()
     */
    ANNOTATION(0x0000_2000, false,
               EnumSet.of(Location.CLASS, Location.INNER_CLASS)),

    /**
     * The access flag {@code ACC_ENUM}.
     *
     * @see Class#isEnum()
     */
    ENUM(0x0000_4000, false,
         EnumSet.of(Location.CLASS, Location.FIELD, Location.INNER_CLASS)),

    /**
     * The access flag {@code ACC_MANDATED}.
     */
    MANDATED(0x0000_8000, false,
             EnumSet.of(Location.METHOD_PARAMETER,
                        Location.MODULE, Location.MODULE_REQUIRES,
                        Location.MODULE_EXPORTS, Location.MODULE_OPENS)),

    /**
     * The access flag {@code ACC_MODULE}.
     */
    MODULE(0x0000_8000, false, EnumSet.of(Location.CLASS));

    // May want to override toString for a different enum constant ->
    // name mapping.

    private final int mask;
    private final boolean sourceModifier;

    private final EnumSet<Location> locations;
    private final Set<Location> unmodifiableLocations;

    AccessFlag(int mask, boolean sourceModifier, EnumSet<Location> locations) {
        this.mask = mask;
        this.sourceModifier = sourceModifier;
        this.locations = locations;
        this.unmodifiableLocations = Collections.unmodifiableSet(locations);
        for (var location : locations) {
            location.allAccessFlags.add(this);
            // assert location.fullMask & mask == 0;
            location.allMask |= mask;
        }
    }

    /**
     * {@return the corresponding integer mask for the access flag}
     */
    public int mask() {
        return mask;
    }

    /**
     * {@return whether or not the flag has a directly corresponding
     * modifier in the Java programming language}
     */
    public boolean sourceModifier() {
        return sourceModifier;
    }

    /**
     * {@return kinds of constructs the flag can be applied to}
     */
    public Set<Location> locations() {
        return unmodifiableLocations;
    }

    /**
     * {@return a set of access flags for the given mask value
     * appropriate for the location in question}
     *
     * @param mask bit mask of access flags
     * @param location context to interpret mask value
     * @throw IllegalArgumentException if the mask contains bit
     * positions not supported for the location in question
     */
    public static Set<AccessFlag> maskToAccessFlags(int mask, Location location) {
        return new AccessFlagSet(mask, location);
    }

    /**
     * A location within a class file where flags can be applied.
     *
     * Note that since these locations represent class file structures
     * rather than language structures many language structures, such
     * as constructors and interfaces, are <em>not</em> present.
     */
    public enum Location {
        /**
         * Class location.
         * @jvms 4.1 The ClassFile Structure
         */
        CLASS,

        /**
         * Field location.
         * @jvms 4.5 Fields
         */
        FIELD,

        /**
         * Method location.
         * @jvms 4.6 Method
         */
        METHOD,

        /**
         * Inner class location.
         * @jvms 4.7.6 The InnerClasses Attribute
         */
        INNER_CLASS,

        /**
         * Method parameter loccation.
         * @jvms 4.7.24. The MethodParameters Attribute
         */
        METHOD_PARAMETER,

        /**
         * Module location
         * @jvms 4.7.25. The Module Attribute
         */
        MODULE,

        /**
         * Module requires location
         * @jvms 4.7.25. The Module Attribute
         */
        MODULE_REQUIRES,

        /**
         * Module exports location
         * @jvms 4.7.25. The Module Attribute
         */
        MODULE_EXPORTS,

        /**
         * Module opens location
         * @jvms 4.7.25. The Module Attribute
         */
        MODULE_OPENS;

        // following two are initialized during initialization of AccessFlag enum constants
        // and should be used only after they are fully initialized (with a HB edge) ...
        private final List<AccessFlag> allAccessFlags = new ArrayList<>(16);
        private int allMask;
    }

    /**
     * An immutable Set based on a bit-mask constructed by ORing single-bit masks
     * of elements and a Location which is a source of the universe of elements.
     */
    private static final class AccessFlagSet implements java.util.Set<AccessFlag> {
        private final int mask;
        private final Location location;

        private AccessFlagSet(int mask, Location location) {
            int unmatchedMask = mask & ~location.allMask;
            if (unmatchedMask != 0) {
                throw new IllegalArgumentException("Unmatched bit position(s) 0x" +
                                                   Integer.toHexString(unmatchedMask) +
                                                   " for location " + location);
            }
            this.mask = mask;
            this.location = location;
        }

        @Override
        public int size() {
            return Integer.bitCount(mask);
        }

        @Override
        public boolean isEmpty() {
            return mask == 0;
        }

        @Override
        public boolean contains(Object o) {
            return o instanceof AccessFlag af &&
                   (af.mask & mask) != 0 &&
                   af.locations.contains(location);
        }

        @Override
        public Iterator<AccessFlag> iterator() {
            // have to wrap it to prevent modification
            return new Iterator<>() {
                private final Iterator<AccessFlag> i = location.allAccessFlags.iterator();
                private AccessFlag next;

                @Override
                public boolean hasNext() {
                    if (next != null) {
                        return true;
                    } else {
                        while (i.hasNext()) {
                            var af = i.next();
                            if ((af.mask & mask) != 0) {
                                next = af;
                                return true;
                            }
                        }
                        return false;
                    }
                }

                @Override
                public AccessFlag next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    var af = next;
                    next = null;
                    return af;
                }
            };
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            for (var o : c) {
                if (!contains(o)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Object[] toArray() {
            return stream().toArray();
        }

        @Override
        @SuppressWarnings({"unchecked", "SuspiciousToArrayCall", "SimplifyStreamApiCallChains"})
        public <T> T[] toArray(T[] a) {
            return stream().toArray(len -> {
                if (a.length >= len) {
                    if (a.length > len) {
                        a[len] = null;
                    }
                    return a;
                } else {
                    return (T[])Array.newInstance(a.getClass().getComponentType(), len);
                }
            });
        }

        @Override
        @SuppressWarnings("SimplifyStreamApiCallChains")
        public void forEach(Consumer<? super AccessFlag> action) {
            stream().forEach(action);
        }

        /** @noinspection SuspiciousToArrayCall*/
        @Override
        @SuppressWarnings({"SuspiciousToArrayCall", "SimplifyStreamApiCallChains"})
        public <T> T[] toArray(IntFunction<T[]> generator) {
            return stream().toArray(generator);
        }

        @Override
        public Stream<AccessFlag> stream() {
            return location
                .allAccessFlags
                .stream()
                .filter(af -> (af.mask & mask) != 0);
        }

        @Override
        public Stream<AccessFlag> parallelStream() {
            return stream().parallel();
        }

        @Override
        public Spliterator<AccessFlag> spliterator() {
            return stream().spliterator();
        }

        // Object methods

        @Override
        public int hashCode() {
            return location.ordinal() + 31 * mask;
        }

        @Override
        public boolean equals(Object obj) {
            return
                obj instanceof AccessFlagSet afSet
                ? (this.mask == afSet.mask &&
                   this.location == afSet.location)
                : (obj instanceof Set<?> set &&
                   this.size() == set.size() &&
                   containsAll(set));
        }

        @Override
        public String toString() {
            return Arrays.toString(toArray());
        }

        // unsupported methods

        @Override
        public boolean add(AccessFlag accessFlag) {
            throw uoe();
        }

        @Override
        public boolean remove(Object o) {
            throw uoe();
        }

        @Override
        public boolean addAll(Collection<? extends AccessFlag> c) {
            throw uoe();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw uoe();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw uoe();
        }

        @Override
        public void clear() {
            throw uoe();
        }

        private static UnsupportedOperationException uoe() {
            return new UnsupportedOperationException();
        }
    }
}
