package be.kunlabora.crafters.kunlaquota

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["be.kunlabora"])
class KunlaquotaApplication

fun main(args: Array<String>) {
	runApplication<KunlaquotaApplication>(*args)
}
