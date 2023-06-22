package chiralsoftware.mwtj.controllers;

import java.util.logging.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 *
 */
@Controller
public class IndexController {

    private static final Logger LOG = Logger.getLogger(IndexController.class.getName());
    
    @GetMapping("/")
    public String getIndex() {
        return "index"; 
    }
    
}
