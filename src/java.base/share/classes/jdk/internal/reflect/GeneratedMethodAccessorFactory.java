package jdk.internal.reflect;

import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.vm.annotation.Hidden;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class GeneratedMethodAccessorFactory {

    static MethodAccessorImpl newMethodAccessor(Method method) {
        if (Reflection.isCallerSensitive(method)) {
            String cs$name = "cs$" + method.getName();
            Class<?>[] ptypes = method.getParameterTypes();
            Class<?>[] cs$ptypes = new Class<?>[ptypes.length + 1];
            cs$ptypes[0] = Class.class;
            System.arraycopy(ptypes, 0, cs$ptypes, 1, ptypes.length);
            try {
                return new CsMethodAccessorAdapter(
                    method.getDeclaringClass().getDeclaredMethod(cs$name, cs$ptypes));
            } catch (NoSuchMethodException e) {
                // fall through...
            }
        }
        return generateMethodAccessor(method);
    }

    private static MethodAccessorImpl generateMethodAccessor(Method method) {
        return (MethodAccessorImpl)
            new MethodAccessorGenerator().generateMethod(
                method.getDeclaringClass(),
                method.getName(),
                method.getParameterTypes(),
                method.getReturnType(),
                method.getExceptionTypes(),
                method.getModifiers()
            );
    }

    private static class CsMethodAccessorAdapter extends MethodAccessorImpl {
        private final Method cs$method;
        private final MethodAccessor cs$accessor;

        private CsMethodAccessorAdapter(Method cs$method) {
            this.cs$method = cs$method;
            this.cs$accessor = generateMethodAccessor(cs$method);
        }

        @Override
        public Object invoke(Object obj, Object[] args) throws IllegalArgumentException, InvocationTargetException {
            throw new InternalError("cs$method invoked without explicit caller: " + cs$method);
        }

        @Override
        @ForceInline
        @Hidden
        public Object invoke(Class<?> caller, Object obj, Object[] args) throws IllegalArgumentException, InvocationTargetException {
            Object[] cs$args = new Object[args == null ? 1 : args.length + 1];
            cs$args[0] = caller;
            if (args != null) {
                System.arraycopy(args, 0, cs$args, 1, args.length);
            }
            return cs$accessor.invoke(obj, cs$args);
        }
    }
}