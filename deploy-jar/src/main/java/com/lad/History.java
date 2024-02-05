package com.lad;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @author Andy
 * @date 2024-2-4 004 13:29
 */

@Data
@AllArgsConstructor
public class History {

    // 指定是哪个框的历史记录，例如host，username，password
    private String name;
    // 历史记录
    private List<String> values;
}
