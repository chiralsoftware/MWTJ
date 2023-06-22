package chiralsoftware.mwtj.controllers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Random;
import static java.util.concurrent.TimeUnit.HOURS;
import java.util.logging.Logger;
import org.springframework.http.HttpHeaders;
import static org.springframework.http.HttpStatus.I_AM_A_TEAPOT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import org.springframework.http.MediaType;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handle upload and download of files.
 * TODO: make this treat binary as binary
 * Make this guess reasonable content types especially for images and PDFs
 */
@Controller
public class FileController {

    private static final Logger LOG = Logger.getLogger(FileController.class.getName());
    
    private final Cache<Integer,byte[]> cache = 
            CacheBuilder.newBuilder().expireAfterWrite(1, HOURS).build();
    
    private final ContentInfoUtil contentInfoUtil = new ContentInfoUtil();
    
    private final Random random = new Random();
    
    private static final int maxLength = 3000000;
    
    private Integer makeKey() {
        return random.nextInt(100000, 999999);
    }
    
    @ResponseBody
    @PostMapping("/")
    public String saveFile(HttpServletRequest request) throws IOException {
        final byte[] bytes = request.getInputStream().readAllBytes();
        if(bytes.length > maxLength) throw new ResponseStatusException(I_AM_A_TEAPOT, "file was too big");
        final Integer key = makeKey();
        cache.put(key, bytes);
        return key.toString();
    }
    
    // add in a head mapping too - although this is automatically handled by the get mapping
    
    @GetMapping(value="/{number:[\\d]+}")
    public ResponseEntity<byte[]> getFile(@PathVariable int number) {
        LOG.info("i'm looking for number: " + number);
        final byte[] bytes = cache.getIfPresent(number);
        if(bytes == null) throw new ResponseStatusException(NOT_FOUND, "file number: " + number + " was not found");
        final MediaType mediaType;
        final ContentInfo contentInfo = contentInfoUtil.findMatch(bytes);
        if(contentInfo == null) mediaType = TEXT_PLAIN;
        else mediaType = MediaType.valueOf(contentInfo.getMimeType());
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType == null ? TEXT_PLAIN : mediaType);
        final ResponseEntity<byte[]> result = ResponseEntity.ok().headers(headers).body(bytes);
        return result;
    }
    
}
