package com.gym.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@TableName("users")
public class User implements Serializable {

    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userID;

    @TableField("name")
    private String name;

    @TableField("date_of_birth")
    private Date dateOfBirth;

    @TableField("address")
    private String address;

    @TableField("email")
    private String email;

    @TableField("password_hash")
    private String passwordHash;

    @TableField("role")
    private Role role;

    @TableField("account_status")
    private AccountStatus accountStatus = AccountStatus.Pending;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public enum Role {
        Member, Trainer, Admin
    }

    public enum AccountStatus {
        Pending, Approved, Suspended
    }
}
