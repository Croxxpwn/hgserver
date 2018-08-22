package xyz.mrcroxx.hgserver

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class User(
        @Id
        @Column(length = 128)
        val openid: String,
        val nickname: String
)