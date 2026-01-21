package no.kartverket.nibas.common

import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Scope(TransactionScope.NAME, proxyMode = ScopedProxyMode.TARGET_CLASS)
annotation class TransactionScope {
    companion object {
        const val NAME = "transaction"
    }
}
