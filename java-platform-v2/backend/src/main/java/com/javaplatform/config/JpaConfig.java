package com.javaplatform.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JpaConfig {
    
    /**
     * Lazy loading configuration for Hibernate
     */
    @Bean
    public FilterRegistrationBean<org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter> 
           openEntityManagerInViewFilter() {
        FilterRegistrationBean<org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter> filterRegBean 
            = new FilterRegistrationBean<>();
        filterRegBean.setFilter(new org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter());
        filterRegBean.setOrder(5);
        return filterRegBean;
    }
}
