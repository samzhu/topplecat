package io.github.samzhu.topplecat.junit;

import static net.bytebuddy.matcher.ElementMatchers.isBridge;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.isFinalizer;
import static net.bytebuddy.matcher.ElementMatchers.isSynthetic;
import static net.bytebuddy.matcher.ElementMatchers.isVirtual;
import static net.bytebuddy.matcher.ElementMatchers.not;

import io.github.samzhu.topplecat.core.ToppleCatException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

/** Internal Byte Buddy backend for concrete {@link ToppleStage} proxies. */
final class StageProxyFactory {
  static final String STRATEGY = "MethodHandles.privateLookupIn + ClassLoadingStrategy.UsingLookup";

  private static final Map<ProxyCacheKey, Class<?>> PROXY_CLASSES = new ConcurrentHashMap<>();
  private static final Map<Object, Binding> BINDINGS =
      Collections.synchronizedMap(new IdentityHashMap<>());

  <T extends ToppleStage> T create(Class<T> authorStage, ToppleScenarioSession session) {
    accessibleNoArgConstructor(authorStage);
    Class<?> proxyClass =
        PROXY_CLASSES.computeIfAbsent(
            new ProxyCacheKey(authorStage, authorStage.getClassLoader()),
            ignored -> defineProxy(authorStage));
    try {
      @SuppressWarnings("unchecked")
      Constructor<T> proxyConstructor = (Constructor<T>) proxyClass.getDeclaredConstructor();
      if (!proxyConstructor.canAccess(null)) {
        proxyConstructor.setAccessible(true);
      }
      T proxy = proxyConstructor.newInstance();
      Binding binding = session.register(proxy, authorStage);
      BINDINGS.put(proxy, binding);
      return proxy;
    } catch (ReflectiveOperationException exception) {
      throw new ToppleCatException(
          "Cannot construct a ToppleScenario Stage proxy for " + authorStage.getName() + ".",
          exception);
    }
  }

  static void unbind(Object proxy) {
    BINDINGS.remove(proxy);
  }

  static int cacheSize() {
    return PROXY_CLASSES.size();
  }

  static Class<?> proxyClass(Class<?> authorStage) {
    return PROXY_CLASSES.get(new ProxyCacheKey(authorStage, authorStage.getClassLoader()));
  }

  private static <T> Constructor<T> accessibleNoArgConstructor(Class<T> type) {
    try {
      Constructor<T> constructor = type.getDeclaredConstructor();
      if (!constructor.canAccess(null)) {
        constructor.setAccessible(true);
      }
      return constructor;
    } catch (ReflectiveOperationException exception) {
      throw new ToppleCatException(
          "Cannot create a ToppleScenario Stage proxy for "
              + type.getName()
              + ": an accessible no-argument constructor is required.",
          exception);
    }
  }

  private static Class<?> defineProxy(Class<?> authorStage) {
    try {
      return new ByteBuddy()
          .subclass(authorStage)
          .method(
              isVirtual()
                  .and(not(isFinalizer()))
                  .and(not(isDeclaredBy(Object.class)))
                  .and(not(isDeclaredBy(ToppleStage.class)))
                  .and(not(isBridge()))
                  .and(not(isSynthetic())))
          .intercept(MethodDelegation.to(Interceptor.class))
          .make()
          .load(
              authorStage.getClassLoader(),
              ClassLoadingStrategy.UsingLookup.of(
                  MethodHandles.privateLookupIn(authorStage, MethodHandles.lookup())))
          .getLoaded();
    } catch (ReflectiveOperationException | RuntimeException exception) {
      throw new ToppleCatException(
          "Cannot define a ToppleScenario Stage proxy for "
              + authorStage.getName()
              + " using "
              + STRATEGY
              + ".",
          exception);
    }
  }

  /** Byte Buddy callback. Bindings are removed by Scenario teardown. */
  public static final class Interceptor {
    private Interceptor() {}

    @RuntimeType
    public static Object intercept(
        @This Object proxy,
        @Origin Method origin,
        @AllArguments Object[] arguments,
        @SuperCall Callable<?> superCall)
        throws Throwable {
      Binding binding = BINDINGS.get(proxy);
      if (binding == null) {
        throw new ToppleCatException(
            "A ToppleScenario Stage proxy was invoked after its session ended.");
      }
      return binding.session().intercept(binding, origin, arguments, superCall);
    }
  }

  record ProxyCacheKey(Class<?> originalStage, ClassLoader classLoader) {}

  record Binding(ToppleScenarioSession session, Object proxy, Class<?> authorStage) {}
}
