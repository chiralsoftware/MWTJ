package chiralsoftware.mwtj;

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
    
}
