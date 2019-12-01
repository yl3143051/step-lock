##分布式锁
用到redis的SETNX 命令（会在redis中存入一个不存在的key  如果key存在不会进行任何操作 并且返回false）