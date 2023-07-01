package chiralsoftware.mwtj.controllers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLDecoder;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Arrays;
import java.util.Random;
import static java.util.concurrent.TimeUnit.HOURS;
import java.util.logging.Logger;
import static org.apache.commons.lang3.StringUtils.isBlank;
import org.springframework.http.HttpHeaders;
import static org.springframework.http.HttpStatus.I_AM_A_TEAPOT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import org.springframework.http.MediaType;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import org.springframework.http.ResponseEntity;
import static org.springframework.http.ResponseEntity.ok;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Handle upload and download of files. TODO: make this treat binary as binary
 * Make this guess reasonable content types especially for images and PDFs
 */
@Controller
public class FileController {

    private static final Logger LOG = Logger.getLogger(FileController.class.getName());

    private final Cache<Integer, byte[]> cache
            = CacheBuilder.newBuilder().expireAfterWrite(1, HOURS).build();

    private final ContentInfoUtil contentInfoUtil = new ContentInfoUtil();

    private final Random random = new Random();

    private static final int maxLength = 100 * 1000000; // files up to 100mb - can be useful for videos
    private static final byte[] httpsPrefix = "https://".getBytes();

    @ResponseBody
    @PostMapping("/")
    public String saveFile(HttpServletRequest request) throws IOException {
        final byte[] bytes = request.getInputStream().readAllBytes();
        if (bytes.length > maxLength)
            throw new ResponseStatusException(I_AM_A_TEAPOT, "file was too big");
        final Integer key = random.nextInt(100000, 999999);
        cache.put(key, safeUrl(bytes));
        return key.toString();
    }
    
    private static String removeTrackers(String urlString) {
        if(! urlString.startsWith("https://")) {
            LOG.info("trying to remove trackers from a string that's not a URL...");
            return urlString;
        }
                urlString = urlString.replaceFirst("\\?fbclid=.+$", "");
        urlString = urlString.replaceFirst("\\?igshid=.+$", "");
        // look for utm_ entries like this:
        // https://www.example.com/events/2952617?utm_medium=email&utm_source=sendgrid&utm_campaign=event_announce_en
        urlString = urlString.replaceFirst("\\?utm_[a-z]+=.+$", "");
        return urlString;
    }

    /**
     * In the case where someone posts a URL, let's go ahead and strip out
     * trackers.
     * Actually we might want to always strip all parameters - these are almost
     * always affiliate and tracker. Forums don't use parameters anymore.
     * Exceptions are YouTube and Google queries.
     * Here's an article on tracker links:
     * https://medium.com/@ian-darwin/you-are-sharing-urls-with-tracking-links-please-stop-502c6f54895
     */
    private static byte[] safeUrl(byte bytes[]) {
        // any URL which is so short isn't really a URL probably, and certainly doesn't have trackers
        if(bytes.length <= httpsPrefix.length + 5) return bytes;
        
        if (!Arrays.equals(bytes, 0, httpsPrefix.length, httpsPrefix, 0, httpsPrefix.length))
            return bytes;
        
        // it appears to be a URL - make sure it's not too long so it's not some kind of weird hack
        if(bytes.length > 1000) bytes = Arrays.copyOf(bytes, 1000);
        
        // convert to a string
        String urlString = new String(bytes); 
        urlString = removeTrackers(urlString);
        
        // if this is a google link let's fix it
        if (urlString.startsWith("https://www.google.com/url")||
                urlString.startsWith("https://go.redirectingat.com/")) {
            final MultiValueMap<String, String> params = UriComponentsBuilder.fromUriString(urlString).build().getQueryParams();
            if (params.containsKey("url")) {
                final String urlResultString = params.getFirst("url");
                if (!isBlank(urlResultString)) 
                    return removeTrackers(URLDecoder.decode(urlResultString, UTF_8)).getBytes();
                return bytes;
            }
        }
        
        if (urlString.startsWith("https://adclick.g.doubleclick.net/aclk")) {
            final MultiValueMap<String, String> params = UriComponentsBuilder.fromUriString(urlString).build().getQueryParams();
            if (params.containsKey("adurl")) {
                final String urlResultString = params.getFirst("adurl");
                if (!isBlank(urlResultString))
                    return removeTrackers(URLDecoder.decode(urlResultString, UTF_8)).getBytes();
                return bytes;
            }
            
        }
        if(urlString.startsWith("https://www.amazon.com/")) {
            // everything after /ref= isn't needed
            urlString = urlString.replaceFirst("/ref=.+$", "");
            // amazon links never require parameters either
            urlString = urlString.replaceFirst("\\?.+$", "");
        }
        return urlString.getBytes();
    }

    // add in a head mapping too - although this is automatically handled by the get mapping
    @GetMapping(value = "/{number:[\\d]+}")
    public ResponseEntity<byte[]> getFile(@PathVariable int number) {
        final byte[] bytes = cache.getIfPresent(number);
        if (bytes == null)
            throw new ResponseStatusException(NOT_FOUND, "file number: " + number + " was not found");
        final MediaType mediaType;
        final ContentInfo contentInfo = contentInfoUtil.findMatch(bytes);
        if (contentInfo == null)
            mediaType = TEXT_PLAIN;
        else
            mediaType = MediaType.valueOf(contentInfo.getMimeType());
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType == null ? TEXT_PLAIN : mediaType);
        return ok().headers(headers).body(bytes);
    }

}
