package com.myy.weitutravel.thumb.vo;

import lombok.Data;

@Data
public class Node {
    final String key;
    final int count;

    public Node(String key, int count) {
        this.key = key;
        this.count = count;
    }
}
