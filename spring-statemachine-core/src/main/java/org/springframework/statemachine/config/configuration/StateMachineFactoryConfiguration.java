/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.statemachine.config.configuration;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfig;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.config.builders.StateMachineConfigBuilder;
import org.springframework.statemachine.config.builders.StateMachineStates;
import org.springframework.statemachine.config.builders.StateMachineTransitions;
import org.springframework.statemachine.config.common.annotation.AbstractImportingAnnotationConfiguration;
import org.springframework.statemachine.config.common.annotation.AnnotationConfigurer;

@Configuration
public class StateMachineFactoryConfiguration<S extends Enum<S>, E extends Enum<E>> extends
		AbstractImportingAnnotationConfiguration<StateMachineConfigBuilder<S, E>, StateMachineConfig<S, E>> {

	private final StateMachineConfigBuilder<S, E> builder = new StateMachineConfigBuilder<S, E>();

	@Override
	protected BeanDefinition buildBeanDefinition(AnnotationMetadata importingClassMetadata,
			Class<? extends Annotation> namedAnnotation) throws Exception {
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(StateMachineFactoryDelegatingFactoryBean.class);
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(
				EnableStateMachineFactory.class.getName(), false));
		Boolean contextEvents = attributes.getBoolean("contextEvents");
		beanDefinitionBuilder.addConstructorArgValue(builder);
		beanDefinitionBuilder.addConstructorArgValue(contextEvents);
		return beanDefinitionBuilder.getBeanDefinition();
	}


	@Override
	protected List<Class<? extends Annotation>> getAnnotations() {
		List<Class<? extends Annotation>> types = new ArrayList<Class<? extends Annotation>>();
		types.add(EnableStateMachineFactory.class);
		return types;
	}

	private static class StateMachineFactoryDelegatingFactoryBean<S extends Enum<S>, E extends Enum<E>> implements
			FactoryBean<StateMachineFactory<S, E>>, BeanFactoryAware, InitializingBean {

		private final StateMachineConfigBuilder<S, E> builder;

		private List<AnnotationConfigurer<StateMachineConfig<S, E>, StateMachineConfigBuilder<S, E>>> configurers;

		private BeanFactory beanFactory;

		private StateMachineFactory<S, E> stateMachineFactory;

		private Boolean contextEvents;

		@SuppressWarnings("unused")
		public StateMachineFactoryDelegatingFactoryBean(StateMachineConfigBuilder<S, E> builder, Boolean contextEvents) {
			this.builder = builder;
			this.contextEvents = contextEvents;
		}

		@Override
		public StateMachineFactory<S, E> getObject() throws Exception {
			return stateMachineFactory;
		}

		@Override
		public Class<?> getObjectType() {
			return StateMachineFactory.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

		@Override
		public void afterPropertiesSet() throws Exception {
			for (AnnotationConfigurer<StateMachineConfig<S, E>, StateMachineConfigBuilder<S, E>> configurer : configurers) {
				builder.apply(configurer);
			}
			StateMachineConfig<S, E> stateMachineConfig = builder.getOrBuild();
			StateMachineTransitions<S, E> stateMachineTransitions = stateMachineConfig.getTransitions();
			StateMachineStates<S, E> stateMachineStates = stateMachineConfig.getStates();
			EnumStateMachineFactory<S,E> enumStateMachineFactory = new EnumStateMachineFactory<S, E>(stateMachineTransitions, stateMachineStates);
			enumStateMachineFactory.setBeanFactory(beanFactory);
			enumStateMachineFactory.setContextEventsEnabled(contextEvents);
			this.stateMachineFactory = enumStateMachineFactory;
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

		@Autowired(required=false)
		protected void onConfigurers(
				List<AnnotationConfigurer<StateMachineConfig<S, E>, StateMachineConfigBuilder<S, E>>> configurers)
				throws Exception {
			this.configurers = configurers;
		}

	}

}
