package xyz.mrcroxx.hgserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.transaction.annotation.EnableTransactionManagement

@SpringBootApplication
@EnableTransactionManagement
class HgserverApplication

fun main(args: Array<String>) {
    runApplication<HgserverApplication>(*args)
}
