package com.myy.weitutravel.thumb.service;

import cn.hutool.core.util.HashUtil;
import com.myy.weitutravel.thumb.vo.AddResult;
import com.myy.weitutravel.thumb.vo.Bucket;
import com.myy.weitutravel.thumb.vo.Item;
import com.myy.weitutravel.thumb.vo.Node;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class HeavyKeeper implements TopK {
    private static final int DECAY_TABLE_SIZE = 256;

    private final int k;
    private final int depth;
    private final int width;
    //最小热度阈值，大于minCount 才进入 TopK 候选
    private final int minCount;

    //二维桶数组
    private final Bucket[][] buckets;
    //最小堆
    private final PriorityQueue<Node> minHeap;
    //驱逐队列
    private final BlockingQueue<Item> expelledQueue;

    //指数衰减table
    private final double[] decayTable;
    private final Random random;
    private long total;

    //TopK、宽度、深度、衰减因子/decay  dɪˈkeɪ/、最小热度阈值
    public HeavyKeeper(int k, int width, int depth, double decay, int minCount) {
        this.k = k;
        this.width = width;
        this.depth = depth;
        this.minCount = minCount;

        this.buckets = new Bucket[depth][width];
        for (int i = 0; i < depth; i++) {
            for (int j = 0; j < width; j++) {
                buckets[i][j] = new Bucket();
            }
        }
        this.minHeap = new PriorityQueue<>(Comparator.comparingInt(n -> n.getCount()));
        this.expelledQueue = new LinkedBlockingQueue<>();

        this.decayTable = new double[DECAY_TABLE_SIZE];
        for (int i = 0; i < DECAY_TABLE_SIZE; i++) {
            decayTable[i] = Math.pow(decay, i);
        }
        this.random = new Random();
        this.total = 0;
    }

    @Override
    public AddResult add(String fieldKey, int increment) {
        byte[] keyBytes = fieldKey.getBytes();
        long itemFingerprint = hash(keyBytes);
        int maxCount = 0;

        for (int i = 0; i < depth; i++) {
            int bucketNumber = Math.abs(hash(keyBytes)) % width;   //每行不同哈希
            Bucket bucket = buckets[i][bucketNumber];

            synchronized (bucket) {
                if (bucket.count == 0) {//空桶直接占用
                    bucket.setFingerprint(itemFingerprint);
                    bucket.count = increment;
                    maxCount = Math.max(maxCount, increment);
                } else if (bucket.getFingerprint() == itemFingerprint) {  //相同key   >>>   累加
                    bucket.count += increment;
                    maxCount = Math.max(maxCount, bucket.count);
                } else {            //哈希冲突  >>  指数衰减竞争
                    for (int j = 0; j < increment; j++) {
                        double decay = bucket.count < DECAY_TABLE_SIZE ?
                                decayTable[bucket.getCount()] :
                                decayTable[DECAY_TABLE_SIZE - 1];
                        if (random.nextDouble() < decay) {
                            bucket.count--;
                            if (bucket.getCount() == 0) {
                                bucket.setFingerprint(itemFingerprint);
                                bucket.setCount(increment - j);
                                maxCount = Math.max(maxCount, bucket.getCount());
                                break;
                            }
                        }
                    }
                }
            }
        }

        total += increment;

        //避免低频元素频繁操作堆
        if (maxCount < minCount) {
            return new AddResult(null, false, null);
        }

        synchronized (minHeap) {
            boolean isHot = false;
            String expelled = null;

            Optional<Node> existing = minHeap.stream()
                    .filter(n -> n.getKey().equals(fieldKey))
                    .findFirst();

            if (existing.isPresent()) {
                minHeap.remove(existing.get());
                minHeap.add(new Node(fieldKey, maxCount));
                isHot = true;
            } else {
                if (minHeap.size() < k || maxCount >= Objects.requireNonNull(minHeap.peek()).getCount()) {
                    if (minHeap.size() >= k) {
                        expelled = minHeap.poll().getKey();
                        expelledQueue.offer(new Item(expelled, maxCount));
                    }
                    Node newNode = new Node(fieldKey, maxCount);
                    minHeap.add(newNode);
                    isHot = true;
                }
            }

            return new AddResult(expelled, isHot, fieldKey);
        }
    }

    @Override
    public List<Item> list() {
        synchronized (minHeap) {
            List<Item> result = new ArrayList<>(minHeap.size());
            for (Node node : minHeap) {
                result.add(new Item(node.getKey(), node.getCount()));
            }
            result.sort(Comparator.comparing(Item::count));
            //result.sort((a, b) -> Integer.compare(b.count(), a.count()));
            return result;
        }
    }

    @Override
    public BlockingQueue<Item> expelled() {
        return expelledQueue;
    }

    @Override
    public void fading() {
        for (Bucket[] row : buckets) {
            for (Bucket bucket : row) {
                synchronized (bucket) {
                    bucket.setCount(bucket.getCount() >> 1);
                }
            }
        }

        synchronized (minHeap) {
            PriorityQueue<Node> newHeap = new PriorityQueue<>(Comparator.comparingInt(Node::getCount));
            for (Node node : minHeap) {
                newHeap.add(new Node(node.getKey(), node.getCount() >> 1));
            }
            minHeap.clear();
            minHeap.addAll(newHeap);
        }

        total = total >> 1;
    }

    @Override
    public long total() {
        return total;
    }


    private static int hash(byte[] data) {
        return HashUtil.murmur32(data);
    }

}


