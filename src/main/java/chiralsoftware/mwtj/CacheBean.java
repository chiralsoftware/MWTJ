package chiralsoftware.mwtj;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.j256.simplemagic.ContentType;
import java.util.Random;
import static java.util.concurrent.TimeUnit.HOURS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 */
@Configuration
public class CacheBean {

    @Bean
    CacheMap cacheMap(@Autowired ContentInfoUtil contentInfoUtil) {
        return new CacheMap(contentInfoUtil);
    }

    public static final int maxLength = 100 * 1000000; // files up to 100mb - can be useful for videos
    
    public static final class CacheMap {
        private final Cache<Integer,byte[]> cache = CacheBuilder.newBuilder().expireAfterWrite(1, HOURS).build();
        private final Random random = new Random();
        private final ContentInfoUtil contentInfoUtil;
        
        private CacheMap(ContentInfoUtil contentInfoUtil) {
            this.contentInfoUtil = contentInfoUtil;
        }

        public int put(byte[] bytes) {
            if(bytes == null) throw new NullPointerException("can't cache null bytes");
            if(bytes.length == 0) throw new IllegalArgumentException("can't cache an empty array");
            if(bytes.length > maxLength) throw new IllegalArgumentException("file size: " + bytes.length + " was greater than maximum size: " + maxLength);
            final Integer key = random.nextInt(100000, 999999);
            cache.put(key, bytes);
            return key;
        };
        
        /** Find the object in the cache or return null */
        public byte[] get(int key) {
            return cache.getIfPresent(key);
        }
        
        /** Returns null if the content type can't be found for any reason */
        public ContentType contentType(int key) {
            final byte[] bytes = cache.getIfPresent(key);
            if(bytes == null) return null;
            
            final ContentInfo contentInfo = contentInfoUtil.findMatch(bytes);
            if(contentInfo == null) return null;
            return contentInfo.getContentType();
        }
    }
            
}
