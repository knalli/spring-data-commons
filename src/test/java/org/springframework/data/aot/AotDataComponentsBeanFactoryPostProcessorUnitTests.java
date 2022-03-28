/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.aot;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.data.ManagedTypes;

/**
 * @author Christoph Strobl
 */
class AotDataComponentsBeanFactoryPostProcessorUnitTests {

	@Test // Gh-2593
	void replacesManagedTypesBeanDefinitionUsingSupplierForCtorValue() {

		Supplier<Iterable<Class<?>>> typesSupplier = mock(Supplier.class);

		Mockito.when(typesSupplier.get()).thenReturn(Collections.singleton(DomainType.class));

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("data.managed-types", BeanDefinitionBuilder
				.rootBeanDefinition(ManagedTypes.class).addConstructorArgValue(typesSupplier).getBeanDefinition());

		new AotDataComponentsBeanFactoryPostProcessor().contribute(beanFactory);

		assertThat(beanFactory.getBeanNamesForType(ManagedTypes.class)).hasSize(1);
		verify(typesSupplier).get();

		BeanDefinition beanDefinition = beanFactory.getBeanDefinition("data.managed-types");
		assertThat(beanDefinition.getFactoryMethodName()).isEqualTo("of");
		assertThat(beanDefinition.hasConstructorArgumentValues()).isTrue();
		assertThat(beanDefinition.getConstructorArgumentValues().getArgumentValue(0, null).getValue())
				.isEqualTo(Collections.singleton(DomainType.class));
	}

	@Test // Gh-2593
	void leavesManagedTypesBeanDefinitionNotUsingSupplierForCtorValue() {

		Iterable<Class<?>> types = spy(new LinkedHashSet<>(Collections.singleton(DomainType.class)));

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		AbstractBeanDefinition sourceBD = BeanDefinitionBuilder.rootBeanDefinition(ManagedTypes.class)
				.addConstructorArgValue(types).getBeanDefinition();
		beanFactory.registerBeanDefinition("data.managed-types", sourceBD);

		new AotDataComponentsBeanFactoryPostProcessor().contribute(beanFactory);

		assertThat(beanFactory.getBeanNamesForType(ManagedTypes.class)).hasSize(1);
		verifyNoInteractions(types);

		assertThat(beanFactory.getBeanDefinition("data.managed-types")).isSameAs(sourceBD);
	}

	private static class DomainType {}

}