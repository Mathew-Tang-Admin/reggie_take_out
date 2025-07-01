package com.itheima.reggie.controller;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UploadFileTest {

    @Test
    void upload() {
        // 获取原始文件名
        String originalFilename = "sdeddffr.jpg";

        assert originalFilename != null;
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

        // 使用UUID重新生成文件名，防止文件名称重复造成文件覆盖
        originalFilename = UUID.randomUUID() + suffix;

        System.out.println(originalFilename);
    }
}