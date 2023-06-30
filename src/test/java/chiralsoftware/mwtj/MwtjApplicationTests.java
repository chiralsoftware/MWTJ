package chiralsoftware.mwtj;

import chiralsoftware.mwtj.controllers.FileController;
import chiralsoftware.mwtj.controllers.IndexController;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import org.springframework.boot.test.web.client.TestRestTemplate;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class MwtjApplicationTests {
    
    @Value("${local.server.port}")
    private int port;

    @Autowired
    private IndexController indexController;
    
    @Autowired
    private FileController fileController;
    
    private Object testing = null;
    
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
        assertThat(indexController).isNotNull();
        assertThat(fileController).isNotNull();
        assertThat(restTemplate.getForObject("http://localhost:" + port + "/", String.class)).contains("One hour file sharing");
//        assertThat(testing).isNotNull();
    }
    
    @Test
    public void index() throws Exception {
        
    }

}
