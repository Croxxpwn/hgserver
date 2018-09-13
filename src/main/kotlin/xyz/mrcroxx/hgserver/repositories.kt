package xyz.mrcroxx.hgserver

import org.springframework.data.jpa.repository.JpaRepository


fun <T, S> JpaRepository<T, S>.findOne(id: S): T? {
    val o = findById(id)
    return if (o.isPresent) o.get() else null
}

interface A1Repository : JpaRepository<A1, String>

interface A2Repository : JpaRepository<A2, String>