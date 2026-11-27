package com.intern.doctor.checker;

import com.intern.doctor.dto.RepoFile;
import com.intern.doctor.vo.RiskVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * 检测 2：Windows 专用脚本（.bat / .cmd / .ps1 / .vbs）
 * 这些脚本在 Linux 服务器上跑不了，属于接入障碍，占 15 分（没有则满分）
 */
@Component
public class WindowsScriptChecker implements Checker {

    private static final String[] WIN_EXT = {".bat", ".cmd", ".ps1", ".vbs"};

    @Override
    public String name() { return "Windows 脚本兼容"; }

    @Override
    public int fullScore() { return 15; }

    @Override
    public int check(List<RepoFile> files, List<RiskVO> risks) {
        int count = 0;
        for (RepoFile f : files) {
            String n = f.getPath().toLowerCase(Locale.ROOT);
            for (String ext : WIN_EXT) {
                if (n.endsWith(ext)) {
                    count++;
                    if (count <= 10) {
                        risks.add(new RiskVO("MEDIUM", "Windows 专用脚本", f.getPath(), 0,
                                "该文件为 Windows 专用脚本，Linux 环境无法直接运行"));
                    }
                    break;
                }
            }
        }
        // 每发现一个扣 5 分，扣完为止
        return Math.max(0, fullScore() - count * 5);
    }

    @Override
    public String detail(int score) {
        return score == fullScore() ? "未发现 Windows 专用脚本" : "发现 Windows 专用脚本，Linux 环境需替换";
    }
}
