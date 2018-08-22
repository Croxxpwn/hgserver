package xyz.mrcroxx.hgserver

import org.springframework.data.jpa.repository.JpaRepository


interface UserRepository : JpaRepository<User, String>