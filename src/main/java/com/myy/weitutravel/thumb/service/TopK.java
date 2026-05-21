package com.myy.weitutravel.thumb.service;


import com.myy.weitutravel.thumb.vo.AddResult;
import com.myy.weitutravel.thumb.vo.Item;

import java.util.List;
import java.util.concurrent.BlockingQueue;

public interface TopK {
    AddResult add(String key, int increment);
    List<Item> list();
    BlockingQueue<Item> expelled();
    void fading();
    long total();
}
