package com.aptivist.kotlin

// PED: Operator overloading - permite usar * como operador personalizado
operator fun String.times(count: Int): String = repeat(count)