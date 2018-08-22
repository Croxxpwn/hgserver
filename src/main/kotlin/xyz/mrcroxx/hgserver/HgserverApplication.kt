package xyz.mrcroxx.hgserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HgserverApplication

fun main(args: Array<String>) {
    runApplication<HgserverApplication>(*args)
}
