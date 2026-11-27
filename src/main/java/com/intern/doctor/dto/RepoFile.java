package com.intern.doctor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仓库中的一个文件（路径 + 内容）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepoFile {
    /** 仓库内相对路径，如 src/main/java/App.java */
    private String path;
    /** 文件内容，二进制或读取失败时为空串 */
    private String content;
}
