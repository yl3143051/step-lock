package com.lee.controller;

import com.lee.util.RedisUtil;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
public class RedisController {

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private Redisson redisson;

    @Autowired
    public StringRedisTemplate stringRedisTemplate;

    @GetMapping("testRedis")
    public String testSetString(String key, String value) {
        redisUtil.set(key, value,60L);
        stringRedisTemplate.opsForValue().set("","");
        return "success set string";
    }

    @GetMapping("redisLock")
    public String redisLock() {
        //多台服务下是锁不住的
        synchronized (this) {
            int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock",realStock+"");
                System.out.println("扣减成功，剩余库存为：" + realStock);
            } else {
                System.out.println("扣减失败，库存不足");
            }
        }
        return "end";
    }

    @GetMapping("redisLock2")
    public String redisLock2() {
        String lockKey = "product_001";
        //如果多个请求同时过来 查询redis的时候 因为redis是单线程的 所以这里也会等待
        Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "lee");//等于setnx命令
        if (!result) {
            return "error";
        }
        int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
        if (stock > 0) {
            int realStock = stock - 1;
            stringRedisTemplate.opsForValue().set("stock",realStock+"");
            System.out.println("扣减成功，剩余库存为：" + realStock);
        } else {
            System.out.println("扣减失败，库存不足");
        }
        stringRedisTemplate.delete(lockKey);
        return "end";
    }


    @GetMapping("redisLock3")
    public String redisLock3() {
        String lockKey = "product_001";
        //捕获异常 在finally中执行解锁操作 如果不捕获异常 那么可能再出现异常的情况下 出现死锁
        try {
            //如果多个请求同时过来 查询redis的时候 因为redis是单线程的 所以这里也会等待
            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "lee");
            stringRedisTemplate.expire(lockKey,10, TimeUnit.SECONDS);
            if (!result) {
                return "error";
            }
            int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock",realStock+"");
                System.out.println("扣减成功，剩余库存为：" + realStock);
            } else {
                System.out.println("扣减失败，库存不足");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
        return "end";
    }


    @GetMapping("redisLock4")
    public String redisLock4() {
        String lockKey = "product_001";
        //捕获异常 在finally中执行解锁操作 如果不捕获异常 那么可能再出现异常的情况下 出现死锁
        try {
            //如果多个请求同时过来 查询redis的时候 因为redis是单线程的 所以这里也会等待
//            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "lee");
              //如果程序执行到这里挂了  也会发生死锁
//            stringRedisTemplate.expire(lockKey,30, TimeUnit.SECONDS);
            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "lee",30,TimeUnit.SECONDS);//用这条逻辑 保证以上两条是原子操作
            if (!result) {
                return "error";
            }
            int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock",realStock+"");
                System.out.println("扣减成功，剩余库存为：" + realStock);
            } else {
                System.out.println("扣减失败，库存不足");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
        return "end";
    }


    @GetMapping("redisLock5")
    public String redisLock5() {
        String lockKey = "product_001";
        //捕获异常 在finally中执行解锁操作 如果不捕获异常 那么可能再出现异常的情况下 出现死锁
        String lockValue = UUID.randomUUID().toString();
        try {
            //如果多个请求同时过来 查询redis的时候 因为redis是单线程的 所以这里也会等待
//            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "lee");
            //如果程序执行到这里挂了  也会发生死锁
//            stringRedisTemplate.expire(lockKey,30, TimeUnit.SECONDS);
            /** 但是在高并发场景下 上面方法有可能发生锁失效的问题 所以这里存一个uuid的唯一value值来区分是不是本次请求加的锁*/
            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue,30,TimeUnit.SECONDS);//用这条逻辑 保证以上两条是原子操作
            if (!result) {
                return "error";
            }
            int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock",realStock+"");
                System.out.println("扣减成功，剩余库存为：" + realStock);
            } else {
                System.out.println("扣减失败，库存不足");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (lockValue.equals(stringRedisTemplate.opsForValue().get(lockKey))) {
                stringRedisTemplate.delete(lockKey);
            }

        }
        return "end";
    }

    /**
     * redisson 是用在分布式场景下
     */
    @GetMapping("redisLock6")
    public String redisLock6() {
        String lockKey = "product_001";
        //捕获异常 在finally中执行解锁操作 如果不捕获异常 那么可能再出现异常的情况下 出现死锁
//        String lockValue = UUID.randomUUID().toString();
        RLock redisLock = redisson.getLock(lockKey);
        try {
            //如果多个请求同时过来 查询redis的时候 因为redis是单线程的 所以这里也会等待
//            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "lee");
            //如果程序执行到这里挂了  也会发生死锁
//            stringRedisTemplate.expire(lockKey,30, TimeUnit.SECONDS);
            /** 但是在高并发场景下 上面方法有可能发生锁失效的问题 所以这里存一个uuid的唯一value值来区分是不是本次请求加的锁*/
//            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue,30,TimeUnit.SECONDS);//用这条逻辑 保证以上两条是原子操作
//            if (!result) {
//                return "error";
//            }
            //开启分线程 每隔一段时间 去检查redis中是否还有这个key 重新把超时时间更新成为设定的时间
            redisLock.lock(30,TimeUnit.SECONDS);
            int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock",realStock+"");
                System.out.println("扣减成功，剩余库存为：" + realStock);
            } else {
                System.out.println("扣减失败，库存不足");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            redisLock.unlock();
        }
        return "end";
    }

    /**
     * 如果还想提高性能 可以对库存进行分段 每个端的库存并发的去减
     * 例 redis是集群 可以把每段的库存存到redis集群的不同节点上去进行并发操作
     */
}
