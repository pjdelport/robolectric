package org.robolectric.internal.bytecode;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.Function;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
public class ShadowWranglerUnitTest {
  private ShadowWrangler shadowWrangler;

  @Before
  public void setup() throws Exception {
    shadowWrangler = new ShadowWrangler(ShadowMap.EMPTY, 23);
  }

  @Test
  public void getInterceptionHandler_whenCallIsNotRecognized_shouldReturnDoNothingHandler() throws Exception {
    MethodSignature methodSignature = MethodSignature.parse("java/lang/Object/unknownMethod()V");
    Function<Object,Object> handler = shadowWrangler.getInterceptionHandler(methodSignature);

    assertThat(handler.call(null, null, new Object[0])).isNull();
  }

  @Test
  public void getInterceptionHandler_whenInterceptingElderOnLinkedHashMap_shouldReturnNonDoNothingHandler() throws Exception {
    MethodSignature methodSignature = MethodSignature.parse("java/util/LinkedHashMap/eldest()Ljava/lang/Object;");
    Function<Object,Object> handler = shadowWrangler.getInterceptionHandler(methodSignature);

    assertThat(handler).isNotSameAs(ShadowWrangler.DO_NOTHING_HANDLER);
  }

  @Test
  public void intercept_elderOnLinkedHashMapHandler_shouldReturnEldestMemberOfLinkedHashMap() throws Throwable {
    LinkedHashMap<Integer, String> map = new LinkedHashMap<>(2);
    map.put(1, "one");
    map.put(2, "two");

    Map.Entry<Integer, String> result = (Map.Entry<Integer, String>)
        shadowWrangler.intercept("java/util/LinkedHashMap/eldest()Ljava/lang/Object;", map, null, getClass());

    Map.Entry<Integer, String> eldestMember = map.entrySet().iterator().next();
    assertThat(result).isEqualTo(eldestMember);
    assertThat(result.getKey()).isEqualTo(1);
    assertThat(result.getValue()).isEqualTo("one");
  }

  @Test
  public void intercept_elderOnLinkedHashMapHandler_shouldReturnNullForEmptyMap() throws Throwable {
    LinkedHashMap<Integer, String> map = new LinkedHashMap<>();

    Map.Entry<Integer, String> result = (Map.Entry<Integer, String>)
        shadowWrangler.intercept("java/util/LinkedHashMap/eldest()Ljava/lang/Object;", map, null, getClass());

    assertThat(result).isNull();
  }

  @Test
  public void shadowClassWithSdkRange() throws Throwable {
    ShadowMap shadowMap = new ShadowMap.Builder().addShadowClasses(ShadowDummyClass.class).build();
    String methodName = internalName(DummyClass.class) + "/methodWithoutRange()V";
    assertThat(new ShadowWrangler(shadowMap, 18).methodInvoked(methodName, false, DummyClass.class)).isNull();
    assertThat(new ShadowWrangler(shadowMap, 19).methodInvoked(methodName, false, DummyClass.class).describe())
        .contains("ShadowDummyClass.methodWithoutRange()");
    assertThat(new ShadowWrangler(shadowMap, 23).methodInvoked(methodName, false, DummyClass.class)).isNull();
  }

  @Test
  public void shadowMethodWithSdkRange() throws Throwable {
    ShadowMap shadowMap = new ShadowMap.Builder().addShadowClasses(ShadowDummyClass.class).build();
    String methodName = internalName(DummyClass.class) + "/methodFor20()V";
    assertThat(new ShadowWrangler(shadowMap, 19).methodInvoked(methodName, false, DummyClass.class)).isNull();
    assertThat(new ShadowWrangler(shadowMap, 20).methodInvoked(methodName, false, DummyClass.class).describe())
        .contains("ShadowDummyClass.methodFor20()");
    assertThat(new ShadowWrangler(shadowMap, 21).methodInvoked(methodName, false, DummyClass.class)).isNull();
  }

  @Test
  public void shadowMethodWithMinSdk() throws Throwable {
    ShadowMap shadowMap = new ShadowMap.Builder().addShadowClasses(ShadowDummyClass.class).build();
    String methodName = internalName(DummyClass.class) + "/methodMin20()V";
    assertThat(new ShadowWrangler(shadowMap, 19).methodInvoked(methodName, false, DummyClass.class)).isNull();
    assertThat(new ShadowWrangler(shadowMap, 20).methodInvoked(methodName, false, DummyClass.class).describe())
        .contains("ShadowDummyClass.methodMin20()");
    assertThat(new ShadowWrangler(shadowMap, 21).methodInvoked(methodName, false, DummyClass.class).describe())
        .contains("ShadowDummyClass.methodMin20()");
  }

  @Test
  public void shadowMethodWithMaxSdk() throws Throwable {
    ShadowMap shadowMap = new ShadowMap.Builder().addShadowClasses(ShadowDummyClass.class).build();
    String methodName = internalName(DummyClass.class) + "/methodMax20()V";
    assertThat(new ShadowWrangler(shadowMap, 19).methodInvoked(methodName, false, DummyClass.class).describe())
        .contains("ShadowDummyClass.methodMax20()");
    assertThat(new ShadowWrangler(shadowMap, 20).methodInvoked(methodName, false, DummyClass.class).describe())
        .contains("ShadowDummyClass.methodMax20()");
    assertThat(new ShadowWrangler(shadowMap, 21).methodInvoked(methodName, false, DummyClass.class)).isNull();
  }

  @Test
  public void shadowConstructor() throws Throwable {
    ShadowMap shadowMap = new ShadowMap.Builder().addShadowClasses(ShadowDummyClass.class).build();
    String methodName = internalName(DummyClass.class) + "/__constructor__()V";
    assertThat(new ShadowWrangler(shadowMap, 19).methodInvoked(methodName, false, DummyClass.class)).isNull();
    assertThat(new ShadowWrangler(shadowMap, 20).methodInvoked(methodName, false, DummyClass.class).describe())
        .contains("ShadowDummyClass.__constructor__()");
    assertThat(new ShadowWrangler(shadowMap, 21).methodInvoked(methodName, false, DummyClass.class)).isNull();
  }

  public static class DummyClass {
  }

  @Implements(value = DummyClass.class, minSdk = 19, maxSdk = 21)
  public static class ShadowDummyClass {
    @Implementation(minSdk = 20, maxSdk = 20)
    public void __constructor__() {
    }
    
    @Implementation
    public void methodWithoutRange() {
    }

    @Implementation(minSdk = 20, maxSdk = 20)
    public void methodFor20() {
    }

    @Implementation(minSdk = 20)
    public void methodMin20() {
    }

    @Implementation(maxSdk = 20)
    public void methodMax20() {
    }
  }

  ///////////////////////

  private class WranglerBuilder extends ShadowMap.Builder {
    ShadowWrangler wranglerFor(int apiLevel) {
      return new ShadowWrangler(build(), apiLevel);
    }

    @Override
    public WranglerBuilder addShadowClasses(Class<?>... shadowClasses) {
      return (WranglerBuilder) super.addShadowClasses(shadowClasses);
    }
  }

  private String internalName(Class clazz) {
    return clazz.getName().replaceAll("\\.", "/");
  }
}
