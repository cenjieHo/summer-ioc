package cn.hecenjie.simpleioc.beans.factory.support;

import cn.hecenjie.simpleioc.beans.factory.BeansException;
import cn.hecenjie.simpleioc.core.io.Resource;
import cn.hecenjie.simpleioc.core.io.ResourceLoader;

/**
 * @author cenjieHo
 * @since 2019/4/23
 */
public interface BeanDefinitionReader {

    /**
     * @return BeanDefinition 要注册的 Bean 工厂
     */
    BeanDefinitionRegistry getRegistry();

    ResourceLoader getResourceLoader();

    int loadBeanDefinitions(Resource resource) throws BeansException;

    int loadBeanDefinitions(Resource... resources) throws BeansException;

    int loadBeanDefinitions(String location) throws BeansException;

    int loadBeanDefinitions(String... location) throws BeansException;

}
