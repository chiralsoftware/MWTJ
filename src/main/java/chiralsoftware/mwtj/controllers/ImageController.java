package chiralsoftware.mwtj.controllers;

import java.util.logging.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller to post and get images
 */
@Controller
public class ImageController {

    private static final Logger LOG = Logger.getLogger(ImageController.class.getName());
    
    @PostMapping("/image")
    public String handleFormUpload(@RequestParam("name") String name,
			@RequestParam("file") MultipartFile file) {
        if(file.isEmpty()) {
            LOG.info("the file was empty");
            return "redirect:/";
        }
        return null;
    }
    
}
