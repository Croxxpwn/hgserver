package xyz.mrcroxx.hgserver

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import xyz.mrcroxx.hgserver.WXService.Companion.STATUS_DEFAULT
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period

/**
 * @Author Croxx
 * @Date 2018/8/23
 * @Description
 */

@Service
@Transactional
class Act1CP(@Autowired val redisService: RedisService,
             @Autowired val a1Repository: A1Repository,
             @Autowired val emojiFactory: EmojiFactory) : Act {
    override val name: String
        get() = "A1"
    override val actName: String
        get() = "一周CP"
    override val entry: List<String>
        get() = listOf("cp", "CP")
    override val description: String
        get() = "一周CP"
    override val timeStart: LocalDateTime
        get() = LocalDateTime.of(2018, 9, 8, 0, 0, 0)
    override val timeEnd: LocalDateTime
        get() = LocalDateTime.of(2018, 9, 14, 23, 59, 59)

    enum class Status(val value: String) {
        DEFAULT("ACT1_DEFAULT"),
        HINT("ACT1_HINT"),
        FILL_SCHOOL("ACT1_FILL_SCHOOL"),
        FILL_GRADE("ACT1_FILL_GRADE"),
        FILL_NAME("ACT1_FILL_NAME"),
        FILL_SEX("ACT1_FILL_SEX"),
        FILL_SEXTO("ACT1_FILL_SEXTO"),
        FILL_DESCRIPTION("ACT1_FILL_DESCRIPTION"),
        FILL_CONTACT("ACT1_FILL_CONTACT"),
        FILL_CONFIRM("ACT1_FILL_CONFIRM");

        companion object {
            private val map = Status.values().associateBy(Status::value)
            fun fromString(value: String) = map[value]
            fun hasStatus(value: String): Boolean = map.contains(value)
        }
    }

    override fun hasStatus(value: String): Boolean {
        return Status.hasStatus(value)
    }


    override fun handle(wxMessage: WXMessage, statusValue: String): WXMessage {
        val status: Status = if (statusValue == STATUS_DEFAULT) Status.DEFAULT else Status.fromString(statusValue)!!
        return when (status) {
            Status.DEFAULT -> handleDefault(wxMessage)
            Status.HINT -> handleHint(wxMessage)
            Status.FILL_SCHOOL -> handleFillSchool(wxMessage)
            Status.FILL_GRADE -> handleFillGrade(wxMessage)
            Status.FILL_NAME -> handleFillName(wxMessage)
            Status.FILL_SEX -> handleFillSex(wxMessage)
            Status.FILL_SEXTO -> handleFillSexTo(wxMessage)
            Status.FILL_DESCRIPTION -> handleFillDescription(wxMessage)
            Status.FILL_CONTACT -> handleFillContact(wxMessage)
            Status.FILL_CONFIRM -> handleFillConfirm(wxMessage)
        }
    }

    fun handleDefault(wxMessage: WXMessage): WXMessage {
        val openid = wxMessage.fromUserName
        val data: A1? = a1Repository.findOne(wxMessage.fromUserName!!)
        if (data != null) {
            return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                    msgType = WXService.TYPE_TEXT, content = """
                    |您已报名航概·一周CP活动
                    |
                    |学校：${data.school}
                    |
                    |年级：${data.grade}
                    |
                    |姓名：${data.name}
                    |
                    |性别：${data.sex}
                    |
                    |取向：${data.sexto}
                    |
                    |简介：${data.description}
                    |
                    |联系方式：${data.contact}
                    |
                    |请等待匹配结果公布哦~
                """.trimMargin())
        } else {
            redisService.setStatus(openid!!, Status.HINT.value)
            return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                    msgType = WXService.TYPE_TEXT, content = """
                    |确定参加航概的一周CP活动吗？
                    |
                    |回复[报名] 回复[取消]
                """.trimMargin())
        }
    }

    fun handleHint(wxMessage: WXMessage): WXMessage {
        val openid = wxMessage.fromUserName!!
        when (wxMessage.content) {
            "报名" -> {
                redisService.setStatus(openid, Status.FILL_SCHOOL.value)
                return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                        msgType = WXService.TYPE_TEXT, content = """
                    |请回复你的学校名~
                    |
                    |如[BUAA]
                """.trimMargin())
            }
            "取消" -> {
                redisService.setStatus(openid, STATUS_DEFAULT)
                return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                        msgType = WXService.TYPE_TEXT, content = """
                    |期待下次遇见你~
                """.trimMargin())
            }
            else -> {
                return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                        msgType = WXService.TYPE_TEXT, content = """
                    |确定参加航概的一周CP活动吗？
                    |
                    |回复[报名] 回复[取消]
                """.trimMargin())
            }
        }
    }

    fun handleFillSchool(wxMessage: WXMessage): WXMessage {
        val openid = wxMessage.fromUserName!!
        val data = redisService.getActData(name, openid)?.toA1()
                ?: A1(openid, null, null, null, null, null, null, null, 0, null)
        data.school = wxMessage.content
        redisService.setActData(name, openid, data.toJson(), -1)
        redisService.setStatus(openid, Status.FILL_GRADE.value)
        return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = WXService.TYPE_TEXT, content = """
                    |请回复你的年级~
                    |
                    |如[大一]
                """.trimMargin())
    }

    fun handleFillGrade(wxMessage: WXMessage): WXMessage {
        val openid = wxMessage.fromUserName!!
        val data = redisService.getActData(name, openid)?.toA1() ?: return handleError(wxMessage)
        data.grade = wxMessage.content
        redisService.setActData(name, openid, data.toJson(), -1)
        redisService.setStatus(openid, Status.FILL_NAME.value)
        return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = WXService.TYPE_TEXT, content = """
                    |请回复你的名字~
                    |
                    |如[航概君]
                """.trimMargin())
    }

    fun handleFillName(wxMessage: WXMessage): WXMessage {
        val openid = wxMessage.fromUserName!!
        val data = redisService.getActData(name, openid)?.toA1() ?: return handleError(wxMessage)
        data.name = wxMessage.content
        redisService.setActData(name, openid, data.toJson(), -1)
        redisService.setStatus(openid, Status.FILL_SEX.value)
        return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = WXService.TYPE_TEXT, content = """
                    |请回复你的性别~
                    |
                    |如[男]或[女]
                """.trimMargin())
    }

    fun handleFillSex(wxMessage: WXMessage): WXMessage {
        val openid = wxMessage.fromUserName!!
        val data = redisService.getActData(name, openid)?.toA1() ?: return handleError(wxMessage)
        data.sex = wxMessage.content
        redisService.setActData(name, openid, data.toJson(), -1)
        redisService.setStatus(openid, Status.FILL_SEXTO.value)
        return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = WXService.TYPE_TEXT, content = """
                    |请回复你理想CP的性别~
                    |
                    |如[男]或[女]
                """.trimMargin())
    }

    fun handleFillSexTo(wxMessage: WXMessage): WXMessage {
        val openid = wxMessage.fromUserName!!
        val data = redisService.getActData(name, openid)?.toA1() ?: return handleError(wxMessage)
        data.sexto = wxMessage.content
        redisService.setActData(name, openid, data.toJson(), -1)
        redisService.setStatus(openid, Status.FILL_DESCRIPTION.value)
        return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = WXService.TYPE_TEXT, content = """
                    |请回复你的一句话简介~
                    |
                    |简介可以包括自己的性格、爱好、特长等~
                    |
                    |如[我是高冷的航概君]
                """.trimMargin())
    }

    fun handleFillDescription(wxMessage: WXMessage): WXMessage {
        val openid = wxMessage.fromUserName!!
        val data = redisService.getActData(name, openid)?.toA1() ?: return handleError(wxMessage)
        data.description = wxMessage.content
        redisService.setActData(name, openid, data.toJson(), -1)
        redisService.setStatus(openid, Status.FILL_CONTACT.value)
        return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = WXService.TYPE_TEXT, content = """
                    |请回复你的联系方式~
                    |
                    |如[微信:hanggai-wechat]
                """.trimMargin())
    }

    fun handleFillContact(wxMessage: WXMessage): WXMessage {
        val openid = wxMessage.fromUserName!!
        val data = redisService.getActData(name, openid)?.toA1() ?: return handleError(wxMessage)
        data.contact = wxMessage.content
        redisService.setActData(name, openid, data.toJson(), -1)
        redisService.setStatus(openid, Status.FILL_CONFIRM.value)
        return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = WXService.TYPE_TEXT, content = """
                    |请核对您的报名信息~
                    |
                    |学校：${data.school}
                    |
                    |年级：${data.grade}
                    |
                    |姓名：${data.name}
                    |
                    |性别：${data.sex}
                    |
                    |取向：${data.sexto}
                    |
                    |简介：${data.description}
                    |
                    |联系方式：${data.contact}
                    |
                    |回复[确定] 回复[取消]
                """.trimMargin())
    }

    fun handleFillConfirm(wxMessage: WXMessage): WXMessage {
        val openid = wxMessage.fromUserName!!
        val data = redisService.getActData(name, openid)?.toA1() ?: return handleError(wxMessage)
        when (wxMessage.content) {
            "确定" -> {
                a1Repository.save(data)
                redisService.setStatus(openid, STATUS_DEFAULT)
                redisService.setActData(name, openid, "", 1)
                return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                        msgType = WXService.TYPE_TEXT, content = """
                    |报名成功~您的报名信息如下：
                    |
                    |学校：${data.school}
                    |
                    |年级：${data.grade}
                    |
                    |姓名：${data.name}
                    |
                    |性别：${data.sex}
                    |
                    |取向：${data.sexto}
                    |
                    |简介：${data.description}
                    |
                    |联系方式：${data.contact}
                    |
                    |请耐心等待匹配结果公布~
                """.trimMargin())
            }
            "取消" -> {
                redisService.setStatus(openid, STATUS_DEFAULT)
                return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                        msgType = WXService.TYPE_TEXT, content = """
                    |期待下次遇见你~
                """.trimMargin())
            }
            else -> {
                return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                        msgType = WXService.TYPE_TEXT, content = """
                    |请核对您的报名信息~
                    |
                    |学校：${data.school}
                    |
                    |年级：${data.grade}
                    |
                    |姓名：${data.name}
                    |
                    |性别：${data.sex}
                    |
                    |取向：${data.sexto}
                    |
                    |简介：${data.description}
                    |
                    |联系方式：${data.contact}
                    |
                    |回复[确定] 回复[取消]
                """.trimMargin())
            }
        }
    }


    fun handleError(wxMessage: WXMessage): WXMessage {
        redisService.setStatus(wxMessage.fromUserName!!, STATUS_DEFAULT)
        return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = WXService.TYPE_TEXT, content = """
                    |航概君不小心把数据弄丢了(ಥ_ಥ)
                    |
                    |请返回重试~
                """.trimMargin())
    }


}


@Service
@Transactional
class Act2CPMission(@Autowired val redisService: RedisService,
                    @Autowired val emojiFactory: EmojiFactory,
                    @Autowired val a1Repository: A1Repository,
                    @Autowired val a2Repository: A2Repository,
                    @Autowired val fileService: FileService) : Act {

    override val name: String
        get() = "A2"
    override val actName: String
        get() = "CP任务"
    override val entry: List<String>
        get() = listOf("任务", "查询")
    override val description: String
        get() = "一周CP挑战任务"
    override val timeStart: LocalDateTime
        get() = LocalDateTime.of(2018, 9, 15, 18, 0, 0)
    override val timeEnd: LocalDateTime
        get() = LocalDateTime.of(2018, 9, 23, 23, 59, 59)
    val timeMission: LocalDateTime = LocalDateTime.of(2018, 9, 17, 0, 0, 0)

    val missions: List<String> = listOf("*",
            "Day 1 : 给ta取一个独特的称呼，互相介绍，互道晚安。",
            "Day 2 : 与ta互换最喜欢的歌单，听ta喜欢听的歌。",
            "Day 3 : 依据你对ta的印象，结合想象，给ta画一幅画像。",
            "Day 4 : 与ta一起吃一顿早餐。(如果不在一个校区，可以吧今天吃的早餐拍下来介绍给ta)",
            "Day 5 : 与ta一起上自习。（如果不在一个校区，那么约一个时间一起自习，互相监督）",
            "Day 6 : 给ta买一份奶茶，一起慢慢享用。（如果不在一个校区，可以为ta叫一份奶茶外卖呦）",
            "Day 7 : 与ta一起出去玩，可以是你们喜欢的任何地方。")

    enum class Status(val value: String) {
        DEFAULT("ACT2_DEFAULT"), UPLOAD("ACT2_UPLOAD"), CONFIRM("ACT2_CONFIRM");

        companion object {
            private val map = Status.values().associateBy(Status::value)
            fun fromString(value: String) = map[value]
            fun hasStatus(value: String): Boolean = map.contains(value)
        }
    }

    override fun hasStatus(value: String): Boolean = Status.hasStatus(value)


    override fun handle(wxMessage: WXMessage, statusValue: String): WXMessage {
        if (wxMessage.content == "查询") {
            val data: A1 = a1Repository.findOne(wxMessage.fromUserName!!)
                    ?: return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                            msgType = WXService.TYPE_TEXT, content = """
                    |抱歉，您没有报名一周CP活动
                """.trimMargin())
            when (data.matched) {
                A1.MATCH_MISS -> return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                        msgType = WXService.TYPE_TEXT, content = """
                    |抱歉，您匹配未成功，请期待下次的一周CP活动~
                """.trimMargin())
                A1.MATCH_UNKNOWN -> return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                        msgType = WXService.TYPE_TEXT, content = """
                    |匹配暂未生效，请耐心等待~
                """.trimMargin())
                A1.MATCH_MATCHED -> {
                    val cpdata: A1 = a1Repository.findOne(data.cpopenid!!)!!
                    return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                            msgType = WXService.TYPE_TEXT, content = """
                    |恭喜你匹配成功! ta的报名信息如下呦~
                    |
                    |学校：${cpdata.school}
                    |
                    |年级：${cpdata.grade}
                    |
                    |姓名：${cpdata.name}
                    |
                    |性别：${cpdata.sex}
                    |
                    |取向：${cpdata.sexto}
                    |
                    |简介：${cpdata.description}
                    |
                    |联系方式：${cpdata.contact}
                    |
                    |
                """.trimMargin())
                }
                else -> handleError(wxMessage)
            }

        }

        val status: Status = if (statusValue == STATUS_DEFAULT) Status.DEFAULT else Status.fromString(statusValue)!!
        when (status) {
            Status.DEFAULT -> {
                val data: A1 = a1Repository.findOne(wxMessage.fromUserName!!)
                        ?: return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                                msgType = WXService.TYPE_TEXT, content = """
                    |抱歉，您没有报名一周CP活动
                """.trimMargin())
                when (data.matched) {
                    A1.MATCH_MISS -> return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                            msgType = WXService.TYPE_TEXT, content = """
                    |抱歉，您匹配未成功，请期待下次的一周CP活动~
                """.trimMargin())
                    A1.MATCH_UNKNOWN -> return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                            msgType = WXService.TYPE_TEXT, content = """
                    |匹配暂未生效，请耐心等待~
                """.trimMargin())
                    A1.MATCH_MATCHED -> return handleMission(wxMessage)
                    else -> return handleError(wxMessage)
                }
            }
            Status.UPLOAD -> return handleUpload(wxMessage)
            Status.CONFIRM -> return handleConfirm(wxMessage)
            else -> return handleError(wxMessage)
        }
    }

    fun getTaskid() = Period.between(timeMission.toLocalDate(), LocalDate.now()).days + 1


    fun handleMission(wxMessage: WXMessage): WXMessage {
        val day = getTaskid()
        if (day <= 0) {
            return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                    msgType = WXService.TYPE_TEXT, content = """
                    |一周CP任务还未开始，敬请期待~
                """.trimMargin())
        } else if (day >= 8) {
            return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                    msgType = WXService.TYPE_TEXT, content = """
                    |一周CP任务已经结束~
                """.trimMargin())
        }
        val openid = wxMessage.fromUserName!!
        var data = a2Repository.findOne(openid)
        if (data == null) {
            data = A2(openid, false, false, false, false, false, false, false)
            a2Repository.save(data)
        }
        val finish = when (day) {
            1 -> data.task1
            2 -> data.task2
            3 -> data.task3
            4 -> data.task4
            5 -> data.task5
            6 -> data.task6
            7 -> data.task7
            else -> false
        }
        if (finish) {
            return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                    msgType = WXService.TYPE_TEXT, content = """
                    |进度:${if (data.task1)"√" else "○"}${if (data.task2)"√" else "○"}${if (data.task3)"√" else "○"}${if (data.task4)"√" else "○"}${if (data.task5)"√" else "○"}${if (data.task6)"√" else "○"}${if (data.task7)"√" else "○"}
                    |
                    |您已经完成了今天的CP任务~
                """.trimMargin())
        }
        redisService.setStatus(openid, Status.UPLOAD.value)
        return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = WXService.TYPE_TEXT, content = """
                    |进度:${if (data.task1)"√" else "○"}${if (data.task2)"√" else "○"}${if (data.task3)"√" else "○"}${if (data.task4)"√" else "○"}${if (data.task5)"√" else "○"}${if (data.task6)"√" else "○"}${if (data.task7)"√" else "○"}
                    |
                    |你还没完成今天的任务呦~
                    |
                    |请回复图片完成今日份的CP任务~
                    |
                    |${missions[day]}
                    |
                    |(暂不支持gif图片)
                """.trimMargin())
    }

    fun handleUpload(wxMessage: WXMessage): WXMessage {
        val openid: String = wxMessage.fromUserName!!
        val taskid = getTaskid()
        if (wxMessage.msgType != WXService.TYPE_IMAGE) return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = WXService.TYPE_TEXT, content = """
                    |暂时不支持这个格式的图片，请重试(ಥ_ಥ)
                    |
                    |(暂不支持gif图片)
                """.trimMargin())
        fileService.saveImageFromUrl(wxMessage.picUrl!!, getMissionImageDir(), getMissionImagePath(taskid, openid))
        redisService.setStatus(openid, Status.CONFIRM.value)
        return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = WXService.TYPE_TEXT, content = """
                    |确定是这张图片吗？
                    |
                    |回复[确定] 回复[修改]
                """.trimMargin())
    }

    fun handleConfirm(wxMessage: WXMessage): WXMessage {
        val openid: String = wxMessage.fromUserName!!
        val day = getTaskid()
        when (wxMessage.content) {
            "确定" -> {
                val data: A2 = a2Repository.findOne(openid) ?: return handleError(wxMessage)
                when (day) {
                    1 -> data.task1 = true
                    2 -> data.task2 = true
                    3 -> data.task3 = true
                    4 -> data.task4 = true
                    5 -> data.task5 = true
                    6 -> data.task6 = true
                    7 -> data.task7 = true
                }
                a2Repository.save(data)
                redisService.setStatus(openid, STATUS_DEFAULT)
                return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                        msgType = WXService.TYPE_TEXT, content = """
                            |进度:${if (data.task1)"√" else "○"}${if (data.task2)"√" else "○"}${if (data.task3)"√" else "○"}${if (data.task4)"√" else "○"}${if (data.task5)"√" else "○"}${if (data.task6)"√" else "○"}${if (data.task7)"√" else "○"}
                            |
                            |恭喜~今天的任务完成了~
                """.trimMargin())
            }
            "修改" -> {
                redisService.setStatus(openid, Status.UPLOAD.value)
                return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                        msgType = WXService.TYPE_TEXT, content = """
                    |请回复图片完成今日份的CP任务~
                    |
                    |${missions[day]}
                    |
                    |(暂不支持gif图片)
                """.trimMargin())
            }
            else -> return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                    msgType = WXService.TYPE_TEXT, content = """
                    |确定是这张图片吗？
                    |
                    |回复[确定] 回复[修改]
                """.trimMargin())
        }
    }

    fun handleError(wxMessage: WXMessage): WXMessage {
        redisService.setStatus(wxMessage.fromUserName!!, STATUS_DEFAULT)
        return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = WXService.TYPE_TEXT, content = """
                    |航概君不小心把数据弄丢了(ಥ_ಥ)
                    |
                    |请返回重试~
                """.trimMargin())
    }

    fun getMissionImagePath(taskid: Int, openid: String): String = "${openid}_task_${taskid}"

    fun getMissionImageDir(): String = "a2"
}
