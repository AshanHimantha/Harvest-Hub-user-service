package com.ashanhimantha.user_service.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SecretsManagerConfig implements BeanFactoryPostProcessor, EnvironmentAware {

    private ConfigurableEnvironment environment;


    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    @Override
    public void setEnvironment(Environment environment) {

        this.environment = (ConfigurableEnvironment) environment;
    }

    private void loadSecretsFromAws(){
        try{
            String awsRegion = environment.getProperty("aws.region", "ap-southeast-2");
            String secretName = environment.getProperty("aws.secrets.database-secret-name", "ecom-postgres");
            String awsProfile = environment.getProperty("aws.profile");


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
