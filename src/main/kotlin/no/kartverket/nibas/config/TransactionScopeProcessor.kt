package no.kartverket.nibas.config

import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.support.SimpleTransactionScope

@Configuration
class TransactionScopeProcessor : BeanFactoryPostProcessor {
    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        beanFactory.registerScope("transaction", SimpleTransactionScope())
    }
}
