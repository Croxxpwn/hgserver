package xyz.mrcroxx.hgserver

import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import java.nio.file.Files
import java.nio.file.Paths


@RestController
class WXcontroller(@Autowired val wxService: WXService) {

    val logger = LoggerFactory.getLogger(WXcontroller::class.java)!!

    @GetMapping(value = "/wx")
    fun wxGet(@RequestParam("signature") signature: String,
              @RequestParam("timestamp") timestamp: String,
              @RequestParam("nonce") nonce: String,
              @RequestParam("echostr") echostr: String): String {
        val keys = arrayOf(wxService.token, timestamp, nonce)
        keys.sort()
        val plain = keys[0] + keys[1] + keys[2]
        val code = DigestUtils.sha1Hex(plain)
        if (code == signature) {
            return echostr
        }
        return "false"
    }

    @PostMapping(value = "/wx", consumes = [MediaType.TEXT_XML_VALUE], produces = [MediaType.TEXT_XML_VALUE])
    fun wxPost(@RequestBody wxMessage: WXMessage): WXMessage {
        logger.info(wxMessage.toString())
        return wxService.handle(wxMessage)
    }

}
