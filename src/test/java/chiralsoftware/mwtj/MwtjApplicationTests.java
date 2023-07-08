package chiralsoftware.mwtj;

import chiralsoftware.mwtj.controllers.FileController;
import chiralsoftware.mwtj.controllers.IndexController;
import java.nio.charset.Charset;
import java.util.logging.Logger;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.MediaType;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
class MwtjApplicationTests {

    private static final Logger LOG = Logger.getLogger(MwtjApplicationTests.class.getName());

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private IndexController indexController;

    @Autowired
    private FileController fileController;

    private Object testing = null;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        assertThat(indexController).isNotNull();
        assertThat(fileController).isNotNull();
//        assertThat(testing).isNotNull();
    }

    @Test
    public void index() throws Exception {
        assertThat(restTemplate.getForObject("http://localhost:" + port + "/", String.class)).contains("One hour file sharing");
    }

    @Test
    public void postFile() throws Exception {
//        LOG.info("about to test mockMvc");
        final MvcResult postResult = mockMvc.perform(post("/").
                content("https://example.com/store/65279?utm_medium=affiliate&utm_source=google.com&clickref=110077778FcNB&utm_content=partnerize")).
                andExpectAll(status().isOk(),
                        content().contentType(new MediaType(TEXT_PLAIN, Charset.forName("UTF-8")))).andReturn();
        final String result = postResult.getResponse().getContentAsString();
//        LOG.info("this was the result: " + result);
        assertThat(result.length() == 6);
        assertThat(result.matches("^\\d+$"));

        final String returnedUrl = mockMvc.perform(get("/" + result)).
                andExpectAll(status().isOk(),
                        content().string("https://example.com/store/65279")
                ).andReturn().getResponse().getContentAsString();
//        LOG.info("the returned url is: " + returnedUrl);
    }

}
