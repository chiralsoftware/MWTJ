package chiralsoftware.mwtj;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.j256.simplemagic.ContentType;
import java.util.Random;
import static java.util.concurrent.TimeUnit.HOURS;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

/**
 *
 */
@Configuration
public class BeansConfig {
    
    @Bean
    MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
    
    @Bean
    ContentInfoUtil contentInfoUtil() {
        return new ContentInfoUtil();
    }
    
}
