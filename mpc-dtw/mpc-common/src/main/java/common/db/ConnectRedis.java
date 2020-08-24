package common.db;

import redis.clients.jedis.Jedis;

/**
 * connect to redis
 * @author maggie liu
 *
 */
public class ConnectRedis {
	
	public Jedis connectDb(String ipAddress){
		Jedis jedis = new Jedis(ipAddress, 6379, 100000);
		
		return jedis;
	}
	
	public Jedis connectDb(String ipAddress, int port){
		Jedis jedis = new Jedis(ipAddress, port, 100000);
		
		return jedis;
	}

}
