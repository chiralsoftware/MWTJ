package chiralsoftware.mwtj.controllers;

import static chiralsoftware.mwtj.controllers.DerUntracker.removeRedirect;
import static chiralsoftware.mwtj.controllers.DerUntracker.removeTrackers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

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
    
    /** Allow uploading of URLs. If it does not start with https:// that is added. */
    @PostMapping("/link")
    public String postUrl(String s, Model model) {
        if(isBlank(s)) return "redirect:/";
        s = s.trim();
        if(s.length() > 500) s = s.substring(0, 500);
        // sorry, not going to deal with HTTP URLs
        if(s.startsWith("http://")) s = s.replaceFirst("http://", "https://"); 
        if(! s.startsWith("https://")) s = "https://" + s; // if someone just entered example.com
        final String removeRedirect = removeRedirect(s);
        if(removeRedirect != null) {
            final Integer key = generateKey();
            cache.put(key, removeRedirect.getBytes());
            return "redirect:/link/" + key;
        }
        
        // it's not a redirect but it might have trackers
        final String removeTrackers = removeTrackers(s);
        if( isBlank(removeTrackers)) return "redirect:/";

        final Integer key = generateKey();
        cache.put(key, removeTrackers.getBytes());
        return "redirect:/link/" + key;
    }
    
    @GetMapping(value = "/link/{number:[\\d]+}")
    public String getLink(@PathVariable int number, Model model) {
        final byte[] bytes = cache.getIfPresent(number);
        if(bytes == null) throw new ResponseStatusException(NOT_FOUND, "file number: " + number + " was not found");
        if(bytes.length > 500) throw new ResponseStatusException(NOT_FOUND, "file number: " + number + " was not valid");
        final String url = new String(bytes);
        if(! url.startsWith("https://")) throw new ResponseStatusException(NOT_FOUND, "file number: " + number + " was not url");
        
        model.addAttribute("link", url);
        return "/link";
    }
    
    
    @ResponseBody
    @PostMapping("/")
    public String saveFile(HttpServletRequest request) throws IOException {
        final byte[] bytes = request.getInputStream().readAllBytes();
        if (bytes.length > maxLength)
            throw new ResponseStatusException(I_AM_A_TEAPOT, "file was too big");
        final Integer key = generateKey();
        cache.put(key, safeUrl(bytes));
        return key.toString();
    }
    
    private Integer generateKey() { return random.nextInt(100000, 999999); }
    
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
        
        final String redirectRemoved = removeRedirect(urlString);
        if(redirectRemoved != null) return redirectRemoved.getBytes();

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
