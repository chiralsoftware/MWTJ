package chiralsoftware.mwtj.controllers;

import com.google.common.collect.ImmutableSortedMap;
import static com.google.common.collect.Ordering.natural;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.logging.Logger;
import static org.apache.commons.lang3.StringUtils.isBlank;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Utility methods to remove trackers from URLs.
 * Todo: maybe add a feature to unwrap redirect URLs like: https://t.co/aFsmOoCU9x . This URL just returns a 301 with a Location: header
 */
final class DerUntracker {

    private static final Logger LOG = Logger.getLogger(DerUntracker.class.getName());
    
    static ImmutableSortedMap<String,String> prefixAndParameters =
            new ImmutableSortedMap.Builder<String,String>(natural()).
                    put("https://www.google.com/url", "url").
                    put("https://go.redirectingat.com/", "url"). 
                    put("https://adclick.g.doubleclick.net/aclk", "adurl").
                    // https://www.avantlink.com/click.php?ctc=gearreviews%2Fbest-winter-gloves_amcid-rbANirAFmeN6ArliYeVAF&merchant_id=b5770911-39dc-46ac-ba0f-b49dbb30c5c7&tt=cl&url=https%3A%2F%2Fwww.backcountry.com%2Fthe-north-face-etip-denali-gloves-mens&website_id=2ea4ea95-bcd0-4bf8-a848-64c4dd59a76d
                    put("https://www.avantlink.com/click.php", "url").
                    put("https://target.georiot.com/Proxy.ashx", "GR_URL"). // GeniusLink, https://geniuslink.com 
                    build();
    
        
    static String removeTrackers(String urlString) {
        if(! urlString.startsWith("https://")) return urlString;
        
        urlString = urlString.replaceFirst("\\?fbclid=.+$", "");
        urlString = urlString.replaceFirst("\\?igshid=.+$", "");
        // look for utm_ entries like this:
        // https://www.example.com/events/2952617?utm_medium=email&utm_source=sendgrid&utm_campaign=event_announce_en
        urlString = urlString.replaceFirst("\\?utm_[a-z]+=.+$", "");

        if(urlString.startsWith("https://www.amazon.com/")) {
            // everything after /ref= isn't needed
            urlString = urlString.replaceFirst("/ref=.+$", "");
            // amazon links never require parameters either
            urlString = urlString.replaceFirst("\\?.+$", "");
        }

        return urlString;
    }
    
    
    /** Take a string and remove redirect-based tracker from it. It returns a fixed URL if it detected a redirect,
     null otherwise. If it returns non-null, that's a final result to return. Otherwise, continue 
     with the tracker removal.
     * Example: https://go.redirectingat.com/?id=31959X896062&xs=1&url=https%3A%2F%2Fwww.matchesfashion.com%2Fus%2Fproducts%2FStefan-Cooke-Martlett-stud-embellished-leather-Derby-shoes-1494292&sref=https%3A%2F%2Fwww.gearpatrol.com%2Fstyle%2Fshoes-boots%2Fa42168034%2Fbest-new-boots-shoes-sneakers-2022%2F
     * 
     */
    static String removeRedirect(String urlString)  {
        if(urlString == null) return null;
        for(String s : prefixAndParameters.keySet()) {
            if(urlString.startsWith(s)) {
                final String paramName = prefixAndParameters.get(s);
                final MultiValueMap<String, String> params = UriComponentsBuilder.fromUriString(urlString).build().getQueryParams();
                if(params.containsKey(paramName)) {
                    String urlResultString = params.getFirst(paramName);
                    if(isBlank(urlResultString)) return null;
                    try {
                        urlResultString = URLDecoder.decode(urlResultString, "UTF-8");
                    } catch(UnsupportedEncodingException uee) { 
                        return null;
                    }
                    return removeTrackers(urlResultString);
                }
            }
        }
        // it wasn't one of the known redirecters 
        return null;
    }
    
}
