package com.bbangbat.common.id

import org.hibernate.annotations.IdGeneratorType

@IdGeneratorType(TsidGenerator::class)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
annotation class Tsid
