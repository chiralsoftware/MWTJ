package chiralsoftware.mwtj.controllers;

import chiralsoftware.mwtj.CacheBean.CacheMap;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.j256.simplemagic.ContentType;
import static com.j256.simplemagic.ContentType.JPEG;
import static com.j256.simplemagic.ContentType.PNG;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Logger;
import static javax.imageio.ImageIO.read;
import static javax.imageio.ImageIO.write;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import static org.springframework.http.MediaType.IMAGE_JPEG;
import static org.springframework.http.MediaType.IMAGE_PNG;
import org.springframework.http.ResponseEntity;
import static org.springframework.http.ResponseEntity.notFound;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller to post and get images
 */
@Controller
public class ImageController {

    private static final Logger LOG = Logger.getLogger(ImageController.class.getName());
    
    @Autowired
    private CacheMap cacheMap;

    @Autowired
    private ContentInfoUtil contentInfoUtil;
    
    @PostMapping("/image")
    public String handleFormUpload(@RequestParam("file") MultipartFile file, Integer resize,
            RedirectAttributes redirectAttributes) throws IOException {
        if(file.isEmpty()) {
            LOG.info("the file was empty");
            redirectAttributes.addAttribute("message", "file was empty");
            return "redirect:/";
        }
        
        LOG.finest("The uploaded file: "  + file.getContentType() + " was:" + file.getSize() + " bytes");
        LOG.finest("the image size is: " + resize);
        
        final byte[] rawImage = file.getBytes();
        final ContentInfo contentInfo = contentInfoUtil.findMatch(rawImage);
        if(contentInfo == null) {
            redirectAttributes.addAttribute("message", "couldn't find content info");
            return "redirect:/";
        }
        final ContentType contentType = contentInfo.getContentType();
        if(contentType == null) {
            redirectAttributes.addAttribute("message", "couldn't find content type");
            return "redirect:/";
        }
        if(! (contentType.equals(PNG) || contentInfo.getContentType().equals(JPEG))) {
            redirectAttributes.addAttribute("message", "content type was not PNG or JPEG");
            return "redirect:/";
        }
        
        final BufferedImage bim = read(file.getInputStream());
        final String formatName = switch(contentType) {
            case PNG -> "PNG";
            case JPEG -> "JPEG";
            default -> throw new IllegalStateException("can't figure out the content type string");
        };
        
        if(bim.getWidth() < resize && bim.getHeight()< resize) {
            // store this image as it is
            final Integer key = cacheMap.put(writeImage(bim,formatName));
            return "redirect:/image/" + key;
        }
        return "redirect:/";
    }
    
    private static byte[] writeImage(BufferedImage bim, String formatName) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        write(bim, formatName, baos);
        return baos.toByteArray();
    }
    
    
    @GetMapping(value = "/image/{number:[\\d]+}")
    @ResponseBody
    public ResponseEntity<byte[]> getImage(@PathVariable int number) {
        final ContentType contentType = cacheMap.contentType(number);
        if(contentType == null) return notFound().build();

        final MediaType mediaType = switch(contentType) {
            case PNG -> IMAGE_PNG;
            case JPEG -> IMAGE_JPEG;
            default -> null;
        };
        if(mediaType == null) return notFound().build();
        
        final byte[] bytes = cacheMap.get(number);
        if(bytes == null) return notFound().build(); // could only happen if the content expires from the cache just before this line
        return ResponseEntity.ok().contentLength(bytes.length).contentType(mediaType).body(bytes);
    }
    
}
