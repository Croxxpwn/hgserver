package xyz.mrcroxx.hgserver

import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.io.Serializable
import java.util.concurrent.TimeUnit


@RestController
class WXcontroller(@Autowired val wxService: WXService,
                   @Autowired val stringRedisTemplate: StringRedisTemplate) {

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
        logger.debug(wxMessage.toString())
        return wxService.handle(wxMessage)
    }
}