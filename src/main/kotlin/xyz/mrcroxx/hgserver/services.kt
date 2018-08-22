package xyz.mrcroxx.hgserver


import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class WXService {
    @Value("\${wx.appid}")
    lateinit var appid: String

    @Value("\${wx.token}")
    lateinit var token: String


}