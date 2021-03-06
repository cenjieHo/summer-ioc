package cn.hecenjie.simpleioc.beans.factory.support;

import cn.hecenjie.simpleioc.beans.factory.BeanFactory;
import cn.hecenjie.simpleioc.beans.factory.BeansException;
import cn.hecenjie.simpleioc.beans.factory.config.AbstractBeanDefinition;
import cn.hecenjie.simpleioc.beans.factory.config.BeanDefinition;

import java.util.HashSet;
import java.util.Set;

/**
 * @author cenjieHo
 * @since 2019/4/25
 */
public abstract class AbstractBeanFactory extends DefaultSingletonBeanRegistry implements BeanFactory {

    /** 当前正在创建的原型模式 */
    private final ThreadLocal<Object> prototypesCurrentlyInCreation = new ThreadLocal<>();

    private ClassLoader beanClassLoader = ClassLoader.getSystemClassLoader();

    public Object getBean(String name) throws BeansException {
        return doGetBean(name, null, null, false);
    }

    /**
     * @param name 要获取的 Bean 的名字
     * @param requiredType 要获取的 Bean 的类型
     * @param args 创建 Bean 时传递的参数，仅限于创建 Bean 时使用
     * @param typeCheckOnly 是否为类型检查
     * @param <T>
     * @return
     * @throws BeansException
     */
    protected <T> T doGetBean(final String name, final Class<T> requiredType,
                              final Object[] args, boolean typeCheckOnly) throws BeansException {

        // 这里省略了 FactoryBean 相关的处理，不支持通过 getObject 方法获取 Bean
        String beanName = name;
        Object bean;

        Object sharedInstance = getSingleton(beanName); // 先从单例缓存中尝试获取
        if (sharedInstance == null || args != null) {   // 如果单例缓存中不存在该 Bean

            if (isPrototypeCurrentlyInCreation(beanName)) {
                // 因为不解决原型 Bean 的循环依赖，所以当前正在创建的原型bean中包含该beanName，那么直接抛出异常
                throw new BeansException("Prototype bean '" + beanName + "' currently in creation");
            }

            // 这里省略了合并父类 Bean 得到 RootBeanDefinition 的过程

            AbstractBeanDefinition bd = (AbstractBeanDefinition) getBeanDefinition(beanName);

            // 这里省略了先处理所依赖的 Bean 的过程，也就是 depends-on 属性

            // Bean 实例化
            // Bean 的作用域默认为singleton，不同的作用域有不同的初始化策略。
            if (bd.isSingleton()) {    // 单例模式
                // 以下 getSingleton() 方法从头开始加载 Bean，与上面的 getSingleton() 从缓存中获取不是一回事
                sharedInstance = getSingleton(beanName, () -> {
                    try {
                        // 核心：createBean()方法，所有Bean实例的创建，都会委托给该方法实现
                        return createBean(beanName, bd, args);
                    } catch (BeansException ex) {
                        // todo：destroySingleton(beanName);
                        throw ex;
                    }
                });
                bean = sharedInstance;  // 省略了FactoryBean相关的操作
            } else if (bd.isPrototype()) {  // 原型模式
                // 因为原型模式不涉及缓存，所以加载过程比较简单，直接创建一个新的 Bean 实例就可以了。
                Object prototypeInstance = null;
               try {
                   beforePrototypeCreation(beanName);   // 加载前置处理，就是将其标记为正在创建中
                   // 创建 Bean 对象
                   prototypeInstance = createBean(beanName, bd, args);
               } finally {
                   afterPrototypeCreation(beanName);    // 加载后置处理，就是移除表示正在创建中的标记
               }
                bean = prototypeInstance;  // 省略了FactoryBean相关的操作
            } else {    // 其它作用域：request、session、global session
                // 暂不支持，直接抛出异常
                throw new BeansException("Scopes other than singleton and prototype are not supported");
            }

        } else{
            bean = sharedInstance;
        }
        return (T) bean;

    }

    protected boolean isPrototypeCurrentlyInCreation(String beanName) {
        Object curVal = this.prototypesCurrentlyInCreation.get();
        return (curVal != null &&
                (curVal.equals(beanName) || (curVal instanceof Set && ((Set<?>) curVal).contains(beanName))));	// 相等或包含则返回true
    }

    protected Class<?> resolveBeanClass(final AbstractBeanDefinition bd, String beanName) throws ClassNotFoundException {
        if (bd.hasBeanClass()) {
            return bd.getBeanClass();
        } else {
            return doResolveBeanClass(bd);
        }
    }

    protected Class<?> doResolveBeanClass(AbstractBeanDefinition bd) throws ClassNotFoundException {
        String className = bd.getBeanClassName();
        if (className == null) {
            throw new BeansException("ClassName must not be null");
        }
        Class<?> resolvedClass = Class.forName(className);
        bd.setBeanClass(resolvedClass);
        return resolvedClass;
    }

    public ClassLoader getBeanClassLoader() {
        return this.beanClassLoader;
    }

    protected void beforePrototypeCreation(String beanName) {
        Object curVal = this.prototypesCurrentlyInCreation.get();
        if (curVal == null) {
            // 如果为空，那么直接将当前正在创建的加入即可
            this.prototypesCurrentlyInCreation.set(beanName);
        } else if (curVal instanceof String) {
            // 如果不为空，且为String类型，那么说明当前正在创建的原型模式只有一个，
            // 这时候创建一个集合，将原来的与当前的都加入到集合中去
            Set<String> beanNameSet = new HashSet<>(2);
            beanNameSet.add((String) curVal);
            beanNameSet.add(beanName);
            this.prototypesCurrentlyInCreation.set(beanNameSet);
        } else {
            // 如果不为空，且为set类型，那么说明当前正在创建的原型模式不止一个，直接加入集合即可
            Set<String> beanNameSet = (Set<String>) curVal;
            beanNameSet.add(beanName);
        }
    }

    protected void afterPrototypeCreation(String beanName) {
        Object curVal = this.prototypesCurrentlyInCreation.get();
        if (curVal instanceof String) {
            // 如果为String，说明当前正在创建的原型模式只有一个，直接移除即可
            this.prototypesCurrentlyInCreation.remove();
        } else if (curVal instanceof Set) {
            // 如果为Set，说明当前正在创建的原型模式有多个，在集合中移除beanName即可
            Set<String> beanNameSet = (Set<String>) curVal;
            beanNameSet.remove(beanName);
            if (beanNameSet.isEmpty()) {
                this.prototypesCurrentlyInCreation.remove();
            }
        }
    }


    protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

    protected abstract Object createBean(String beanName, AbstractBeanDefinition bd, Object[] args)
            throws BeansException;

}
